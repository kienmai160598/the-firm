#include <doctest/doctest.h>

#include <chrono>
#include <filesystem>
#include <thread>

#include "core/engine.hpp"
#include "core/engine_error.hpp"
#include "support/http_client.hpp"
#include "support/loopback_seeder.hpp"

using movies::core::Engine;
using movies::core::EngineConfig;
using movies::core::EngineTuning;
using movies::stream::StreamState;

namespace {

constexpr int piece_length = 64 * 1024;

std::filesystem::path fresh_directory(const std::string& name) {
    const auto directory = std::filesystem::temp_directory_path() / ("movies-engine-" + name);
    std::filesystem::remove_all(directory);
    return directory;
}

EngineTuning loopback_tuning() {
    EngineTuning tuning;
    tuning.local_only = true;
    return tuning;
}

std::string wait_until_ready(Engine& engine, std::uint64_t stream) {
    const auto deadline = std::chrono::steady_clock::now() + std::chrono::seconds(30);
    while (std::chrono::steady_clock::now() < deadline) {
        const auto info = engine.stream_info(stream);
        REQUIRE(info);
        REQUIRE(info->snapshot.state != StreamState::failed);
        if (info->snapshot.state == StreamState::ready) return info->url;
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
    }
    FAIL("the stream never became ready");
    return {};
}

}

TEST_CASE("a torrent streams over HTTP with the seeder's bytes, from the start and after seeks") {
    const auto root = fresh_directory("integration");
    const movies::test::LoopbackSeeder seeder(root / "seeder", 3 * 1024 * 1024 + 123, piece_length);
    Engine engine(EngineConfig(root / "engine", loopback_tuning()));

    const auto stream = engine.open_stream(seeder.magnet(), -1);
    const auto url = wait_until_ready(engine, stream);
    const auto& payload = seeder.payload();

    const auto whole = movies::test::http_get(url);
    CHECK(url.ends_with("/video.mkv"));
    CHECK(whole.status == 200);
    CHECK(whole.head.find("Content-Type: video/x-matroska\r\n") != std::string::npos);
    CHECK(whole.body == payload);

    const auto tail = movies::test::http_get(url, "3000000-");
    CHECK(tail.status == 206);
    CHECK(tail.body == payload.substr(3000000));

    const auto middle = movies::test::http_get(url, "100000-299999");
    CHECK(middle.status == 206);
    CHECK(middle.body == payload.substr(100000, 200000));

    CHECK(engine.close_stream(stream));
    CHECK_FALSE(engine.stream_info(stream));
    std::filesystem::remove_all(root);
}

TEST_CASE("a file four times the memory cap streams whole, and memory stays under the cap") {
    const auto root = fresh_directory("budget");
    const movies::test::LoopbackSeeder seeder(root / "seeder", 64 * piece_length, piece_length);
    auto tuning = loopback_tuning();
    tuning.store_cap_bytes = 16 * piece_length;
    tuning.limits.window = {.ahead_bytes = 6 * piece_length, .behind_bytes = piece_length, .urgent_pieces = 2};
    tuning.limits.eviction_threshold_bytes = 10 * piece_length;
    Engine engine(EngineConfig(root / "engine", tuning));

    const auto stream = engine.open_stream(seeder.magnet(), -1);
    const auto whole = movies::test::http_get(wait_until_ready(engine, stream));

    CHECK(whole.status == 200);
    CHECK(whole.body == seeder.payload());
    CHECK(engine.payload_bytes() <= tuning.store_cap_bytes);
    std::filesystem::remove_all(root);
}

TEST_CASE("a host budget with a small cap streams and seeks, and memory stays under the cap") {
    // The ratios shared/torrent's budgetFor ships (ahead half the cap, eviction at three
    // quarters), on a cap only 16 pieces wide, passed the way the C ABI passes them.
    const auto root = fresh_directory("host-budget");
    const auto file_size = std::int64_t{64} * piece_length;
    const movies::test::LoopbackSeeder seeder(root / "seeder", file_size, piece_length);
    const auto cap = std::int64_t{16} * piece_length;
    const movies::core::MemoryBudget budget{.store_cap_bytes = cap, .ahead_bytes = cap / 2,
                                            .behind_bytes = 2 * piece_length,
                                            .eviction_threshold_bytes = cap * 3 / 4};
    Engine engine(EngineConfig(root / "engine", movies::core::with_budget(loopback_tuning(), budget)));
    const auto url = wait_until_ready(engine, engine.open_stream(seeder.magnet(), -1));
    const auto& payload = seeder.payload();

    const auto whole = movies::test::http_get(url);
    CHECK(whole.body == payload);
    CHECK(engine.payload_bytes() <= cap);

    for (const std::int64_t start : {std::int64_t{60} * piece_length, std::int64_t{3} * piece_length,
                                     std::int64_t{40} * piece_length + 17}) {
        const auto range = std::to_string(start) + "-" + std::to_string(start + piece_length - 1);
        const auto part = movies::test::http_get(url, range);
        CHECK(part.status == 206);
        CHECK(part.body == payload.substr(static_cast<std::size_t>(start), piece_length));
        CHECK(engine.payload_bytes() <= cap);
    }
    std::filesystem::remove_all(root);
}

TEST_CASE("a magnet link that does not parse is refused") {
    const auto root = fresh_directory("bad-magnet");
    Engine engine(EngineConfig(root, loopback_tuning()));

    CHECK_THROWS_AS(engine.open_stream("magnet:?nothing", -1), movies::core::EngineError);
    std::filesystem::remove_all(root);
}
