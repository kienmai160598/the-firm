#ifndef MOVIES_ENGINE_CORE_ENGINE_CONFIG_HPP
#define MOVIES_ENGINE_CORE_ENGINE_CONFIG_HPP

#include <filesystem>

namespace movies::core {

// Settings checked once at the edge, so services can trust every field.
class EngineConfig {
public:
    // Throws EngineError(invalid_argument) when the directory is empty or relative.
    explicit EngineConfig(std::filesystem::path state_directory);

    [[nodiscard]] const std::filesystem::path& state_directory() const noexcept {
        return state_directory_;
    }

private:
    std::filesystem::path state_directory_;
};

}

#endif
