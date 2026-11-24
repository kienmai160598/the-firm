#include <doctest/doctest.h>

#include "core/engine_config.hpp"
#include "core/engine_error.hpp"

using movies::core::EngineConfig;
using movies::core::EngineError;
using movies::core::ErrorCode;

namespace {

ErrorCode error_code_for(const char* directory) {
    try {
        EngineConfig config(directory);
    } catch (const EngineError& error) {
        return error.code();
    }
    FAIL("EngineConfig accepted " << directory);
    return ErrorCode::internal;
}

}

TEST_CASE("an absolute state directory is accepted") {
    const EngineConfig config("/tmp/movies-engine");

    CHECK(config.state_directory() == "/tmp/movies-engine");
}

TEST_CASE("an empty or relative state directory is an invalid argument") {
    CHECK(error_code_for("") == ErrorCode::invalid_argument);
    CHECK(error_code_for("relative/state") == ErrorCode::invalid_argument);
}
