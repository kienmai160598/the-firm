#ifndef MOVIES_ENGINE_CORE_ENGINE_HPP
#define MOVIES_ENGINE_CORE_ENGINE_HPP

#include "core/engine_config.hpp"

namespace movies::core {

// Composition root: builds the services from the config and owns their lifetime. Services
// arrive in later slices; the C ABI only ever talks to this class.
class Engine {
public:
    explicit Engine(EngineConfig config);

    Engine(const Engine&) = delete;
    Engine& operator=(const Engine&) = delete;

    [[nodiscard]] const EngineConfig& config() const noexcept { return config_; }

private:
    EngineConfig config_;
};

}

#endif
