#ifndef MOVIES_ENGINE_CORE_ENGINE_CONFIG_HPP
#define MOVIES_ENGINE_CORE_ENGINE_CONFIG_HPP

#include <cstdint>
#include <filesystem>

#include "stream/stream.hpp"

namespace movies::core {

inline constexpr std::int64_t mebibyte = 1024 * 1024;

// E1's fixed memory budget. E3 replaces it with one measured from the device.
struct EngineTuning {
    std::int64_t store_cap_bytes = 512 * mebibyte;
    stream::StreamLimits limits{
        .window = {.ahead_bytes = 256 * mebibyte, .behind_bytes = 32 * mebibyte, .urgent_pieces = 4},
        .eviction_threshold_bytes = 384 * mebibyte,
        .read_timeout = std::chrono::seconds(60),
    };
    // Tests: loopback only, no DHT, local discovery or port mapping.
    bool local_only = false;
};

// Settings checked once at the edge, so services can trust every field.
class EngineConfig {
public:
    // Throws EngineError(invalid_argument) when the directory is empty or relative.
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
