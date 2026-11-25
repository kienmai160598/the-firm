#include <doctest/doctest.h>

#include <cstdint>

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

namespace {

using movies::core::EngineTuning;
using movies::core::mebibyte;
using movies::core::MemoryBudget;
using movies::core::with_budget;

ErrorCode error_code_for(const MemoryBudget& budget) {
    try {
        EngineConfig config("/tmp/movies-engine", with_budget(EngineTuning{}, budget));
    } catch (const EngineError& error) {
        return error.code();
    }
    FAIL("EngineConfig accepted the budget");
    return ErrorCode::internal;
}

MemoryBudget budget(std::int64_t cap, std::int64_t ahead, std::int64_t behind, std::int64_t eviction) {
    return {.store_cap_bytes = cap, .ahead_bytes = ahead, .behind_bytes = behind,
            .eviction_threshold_bytes = eviction};
}

}

TEST_CASE("a budget of zeros keeps the default tuning") {
    const auto tuning = with_budget(EngineTuning{}, MemoryBudget{});
    const EngineTuning defaults;

    CHECK(tuning.store_cap_bytes == defaults.store_cap_bytes);
    CHECK(tuning.limits.window.ahead_bytes == defaults.limits.window.ahead_bytes);
    CHECK(tuning.limits.window.behind_bytes == defaults.limits.window.behind_bytes);
    CHECK(tuning.limits.eviction_threshold_bytes == defaults.limits.eviction_threshold_bytes);
}

TEST_CASE("a budget replaces the fields it sets and keeps the rest") {
    const auto tuning = with_budget(EngineTuning{}, budget(192 * mebibyte, 96 * mebibyte, 0, 144 * mebibyte));

    const EngineConfig config("/tmp/movies-engine", tuning);

    CHECK(config.tuning().store_cap_bytes == 192 * mebibyte);
    CHECK(config.tuning().limits.window.ahead_bytes == 96 * mebibyte);
    CHECK(config.tuning().limits.window.behind_bytes == EngineTuning{}.limits.window.behind_bytes);
    CHECK(config.tuning().limits.eviction_threshold_bytes == 144 * mebibyte);
}

TEST_CASE("a budget that does not fit under its cap is an invalid argument") {
    // A small cap alone keeps the default 256 MiB ahead, which no longer fits.
    CHECK(error_code_for(budget(64 * mebibyte, 0, 0, 0)) == ErrorCode::invalid_argument);
    CHECK(error_code_for(budget(64 * mebibyte, 48 * mebibyte, 16 * mebibyte, 32 * mebibyte))
          == ErrorCode::invalid_argument);
    CHECK(error_code_for(budget(64 * mebibyte, 32 * mebibyte, 16 * mebibyte, 64 * mebibyte))
          == ErrorCode::invalid_argument);
    CHECK(error_code_for(budget(0, -1, 0, 0)) == ErrorCode::invalid_argument);
    CHECK(error_code_for(budget(INT64_MAX, INT64_MAX, INT64_MAX, 0)) == ErrorCode::invalid_argument);
}
