#include <doctest/doctest.h>

#include <filesystem>
#include <string>

#include "movies_engine/movies_engine.h"

namespace {

// A torrent nobody seeds: it stays resolving.
constexpr const char* lonely_magnet =
    "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=lonely";

class EngineFixture {
public:
    EngineFixture() {
        directory_ = std::filesystem::temp_directory_path() / "movies-engine-c-api-stream-test";
        const auto path = directory_.string();
        const movies_engine_config config{.state_directory = path.c_str()};
        REQUIRE(movies_engine_create(&config, &engine_) == MOVIES_ENGINE_OK);
    }
    ~EngineFixture() {
        movies_engine_destroy(engine_);
        std::filesystem::remove_all(directory_);
    }

protected:
    movies_engine* engine_ = nullptr;

private:
    std::filesystem::path directory_;
};

}

TEST_CASE_FIXTURE(EngineFixture, "an opened stream resolves until metadata arrives, then closes") {
    uint64_t stream = 0;
    REQUIRE(movies_engine_open_stream(engine_, lonely_magnet, MOVIES_ENGINE_ANY_FILE, &stream)
            == MOVIES_ENGINE_OK);
    movies_engine_stream_info info{};

    REQUIRE(movies_engine_get_stream_info(engine_, stream, &info) == MOVIES_ENGINE_OK);

    CHECK(info.state == MOVIES_ENGINE_STREAM_RESOLVING);
    CHECK(std::string(info.url).empty());
    CHECK(movies_engine_close_stream(engine_, stream) == MOVIES_ENGINE_OK);
    CHECK(movies_engine_get_stream_info(engine_, stream, &info) == MOVIES_ENGINE_NOT_FOUND);
}

TEST_CASE_FIXTURE(EngineFixture, "a bad magnet link and an unknown stream fail with a status") {
    uint64_t stream = 0;
    movies_engine_stream_info info{};

    CHECK(movies_engine_open_stream(engine_, "not a magnet", 0, &stream)
          == MOVIES_ENGINE_INVALID_ARGUMENT);
    CHECK(std::string(movies_engine_last_error()).find("invalid magnet link") != std::string::npos);
    CHECK(movies_engine_get_stream_info(engine_, 999, &info) == MOVIES_ENGINE_NOT_FOUND);
    CHECK(movies_engine_close_stream(engine_, 999) == MOVIES_ENGINE_NOT_FOUND);
    CHECK(movies_engine_open_stream(engine_, nullptr, 0, &stream) == MOVIES_ENGINE_INVALID_ARGUMENT);
}
