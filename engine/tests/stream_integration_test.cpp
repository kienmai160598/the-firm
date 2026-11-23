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
    CHECK(whole.status == 200);
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

TEST_CASE("a magnet link that does not parse is refused") {
    const auto root = fresh_directory("bad-magnet");
    Engine engine(EngineConfig(root, loopback_tuning()));

    CHECK_THROWS_AS(engine.open_stream("magnet:?nothing", -1), movies::core::EngineError);
    std::filesystem::remove_all(root);
}
