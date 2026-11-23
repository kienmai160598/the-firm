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
    const movies_engine_config config{path.c_str()};
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
    const movies_engine_config config{"relative/state"};
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
