#include <doctest/doctest.h>

#include <filesystem>
#include <string>

#include "movies_engine/movies_engine.h"

namespace {

std::filesystem::path fresh_state_directory() {
    const auto directory = std::filesystem::temp_directory_path() / "movies-engine-c-api-test";
    std::filesystem::remove_all(directory);
    return directory;
}

}

TEST_CASE("the version is the project's semantic version") {
    CHECK(std::string(movies_engine_version()) == MOVIES_ENGINE_TEST_VERSION);
}

TEST_CASE("create makes the state directory and destroy releases the engine") {
    const auto directory = fresh_state_directory();
    const auto path = directory.string();
    const movies_engine_config config{.state_directory = path.c_str()};
    movies_engine* engine = nullptr;

    const auto status = movies_engine_create(&config, &engine);

    REQUIRE(status == MOVIES_ENGINE_OK);
    CHECK(engine != nullptr);
    CHECK(std::string(movies_engine_last_error()).empty());
    CHECK(std::filesystem::is_directory(directory));
    movies_engine_destroy(engine);
    std::filesystem::remove_all(directory);
}

TEST_CASE("a relative state directory fails with a status and a message, not an exception") {
    const movies_engine_config config{.state_directory = "relative/state"};
    movies_engine* engine = nullptr;

    const auto status = movies_engine_create(&config, &engine);

    CHECK(status == MOVIES_ENGINE_INVALID_ARGUMENT);
    CHECK(engine == nullptr);
    CHECK(std::string(movies_engine_last_error()).find("not absolute") != std::string::npos);
}

TEST_CASE("NULL arguments are invalid and destroy accepts NULL") {
    movies_engine* engine = nullptr;

    CHECK(movies_engine_create(nullptr, &engine) == MOVIES_ENGINE_INVALID_ARGUMENT);
    CHECK(movies_engine_create(nullptr, nullptr) == MOVIES_ENGINE_INVALID_ARGUMENT);
    movies_engine_destroy(nullptr);
}

TEST_CASE("a memory budget that does not fit fails create with a status and a message") {
    const auto directory = fresh_state_directory();
    const auto path = directory.string();
    // A 64 MiB cap with the default 256 MiB ahead.
    const movies_engine_config config{.state_directory = path.c_str(), .store_cap_bytes = 64 << 20};
    movies_engine* engine = nullptr;

    const auto status = movies_engine_create(&config, &engine);

    CHECK(status == MOVIES_ENGINE_INVALID_ARGUMENT);
    CHECK(engine == nullptr);
    CHECK(std::string(movies_engine_last_error()).find("below the store cap") != std::string::npos);
}

TEST_CASE("a memory budget that fits creates the engine") {
    const auto directory = fresh_state_directory();
    const auto path = directory.string();
    const movies_engine_config config{
        .state_directory = path.c_str(),
        .store_cap_bytes = 64 << 20,
        .ahead_bytes = 32 << 20,
        .behind_bytes = 16 << 20,
        .eviction_threshold_bytes = 48 << 20,
    };
    movies_engine* engine = nullptr;

    REQUIRE(movies_engine_create(&config, &engine) == MOVIES_ENGINE_OK);
    CHECK(engine != nullptr);
    movies_engine_destroy(engine);
    std::filesystem::remove_all(directory);
}
