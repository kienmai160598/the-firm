#include "core/engine_config.hpp"

#include "core/engine_error.hpp"

#include <string>
#include <utility>

namespace movies::core {
namespace {

void require(const bool condition, const std::string& message) {
    if (!condition) throw EngineError(ErrorCode::invalid_argument, message);
}

std::int64_t chosen(const std::int64_t value, const std::int64_t fallback, const char* name) {
    require(value >= 0, std::string(name) + " is negative: " + std::to_string(value));
    return value == 0 ? fallback : value;
}

void check_budget(const EngineTuning& tuning) {
    const auto cap = tuning.store_cap_bytes;
    const auto& window = tuning.limits.window;
    const auto eviction = tuning.limits.eviction_threshold_bytes;
    require(cap > 0 && window.ahead_bytes > 0 && window.behind_bytes > 0 && eviction > 0,
            "every memory budget value must be positive");
    // Written as a difference: all three are positive, so it cannot overflow.
    require(window.ahead_bytes < cap - window.behind_bytes,
            "ahead (" + std::to_string(window.ahead_bytes) + ") plus behind (" +
                std::to_string(window.behind_bytes) + ") must be below the store cap (" +
                std::to_string(cap) + ")");
    require(eviction < cap, "the eviction threshold (" + std::to_string(eviction) +
                                ") must be below the store cap (" + std::to_string(cap) + ")");
}

}

EngineTuning with_budget(EngineTuning tuning, const MemoryBudget& budget) {
    auto& window = tuning.limits.window;
    tuning.store_cap_bytes = chosen(budget.store_cap_bytes, tuning.store_cap_bytes, "store cap");
    window.ahead_bytes = chosen(budget.ahead_bytes, window.ahead_bytes, "ahead");
    window.behind_bytes = chosen(budget.behind_bytes, window.behind_bytes, "behind");
    tuning.limits.eviction_threshold_bytes = chosen(
        budget.eviction_threshold_bytes, tuning.limits.eviction_threshold_bytes, "eviction threshold"
    );
    return tuning;
}

EngineConfig::EngineConfig(std::filesystem::path state_directory, EngineTuning tuning)
    : state_directory_(std::move(state_directory)), tuning_(tuning) {
    if (state_directory_.empty()) {
        throw EngineError(ErrorCode::invalid_argument, "state directory is empty");
    }
    if (!state_directory_.is_absolute()) {
        throw EngineError(
            ErrorCode::invalid_argument,
            "state directory is not absolute: " + state_directory_.string()
        );
    }
    check_budget(tuning_);
}

}
