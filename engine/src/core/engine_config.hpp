#ifndef MOVIES_ENGINE_CORE_ENGINE_CONFIG_HPP
#define MOVIES_ENGINE_CORE_ENGINE_CONFIG_HPP

#include <cstdint>
#include <filesystem>

#include "stream/stream.hpp"

namespace movies::core {

inline constexpr std::int64_t mebibyte = 1024 * 1024;

// E1's fixed memory budget, the default when the host passes none. E3 replaces it with one
// measured from the device.
struct EngineTuning {
    std::int64_t store_cap_bytes = 512 * mebibyte;
    stream::StreamLimits limits{
        .window = {.ahead_bytes = 256 * mebibyte, .behind_bytes = 32 * mebibyte, .urgent_pieces = 4,
                   .tail_bytes = 8 * mebibyte},
        .eviction_threshold_bytes = 384 * mebibyte,
        .read_timeout = std::chrono::seconds(60),
    };
    // Tests: loopback only, no DHT, local discovery or port mapping.
    bool local_only = false;
};

// The memory budget a host may pass through the C ABI. A field left at 0 keeps the default.
struct MemoryBudget {
    std::int64_t store_cap_bytes = 0;
    std::int64_t ahead_bytes = 0;
    std::int64_t behind_bytes = 0;
    std::int64_t eviction_threshold_bytes = 0;
};

// `tuning` with every non-zero field of `budget` in place of its own. Throws
// EngineError(invalid_argument) for a negative field; EngineConfig checks the result.
EngineTuning with_budget(EngineTuning tuning, const MemoryBudget& budget);

// Settings checked once at the edge, so services can trust every field.
class EngineConfig {
public:
    // Throws EngineError(invalid_argument) when the directory is empty or relative, or when the
    // memory budget does not fit: a value is not positive, ahead plus behind reaches the cap, or
    // the eviction threshold reaches the cap.
    explicit EngineConfig(std::filesystem::path state_directory, EngineTuning tuning = {});

    [[nodiscard]] const std::filesystem::path& state_directory() const noexcept {
        return state_directory_;
    }
    [[nodiscard]] const EngineTuning& tuning() const noexcept { return tuning_; }

private:
    std::filesystem::path state_directory_;
    EngineTuning tuning_;
};

}

#endif
