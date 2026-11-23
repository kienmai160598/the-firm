#include "core/engine.hpp"

#include <system_error>
#include <utility>

#include "core/engine_error.hpp"

namespace movies::core {

Engine::Engine(EngineConfig config) : config_(std::move(config)) {
    std::error_code error;
    std::filesystem::create_directories(config_.state_directory(), error);
    if (error) {
        throw EngineError(
            ErrorCode::internal,
            "cannot create state directory " + config_.state_directory().string() + ": " +
                error.message()
        );
    }
}

}
