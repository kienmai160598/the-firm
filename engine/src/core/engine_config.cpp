#include "core/engine_config.hpp"

#include "core/engine_error.hpp"

#include <utility>

namespace movies::core {

EngineConfig::EngineConfig(std::filesystem::path state_directory)
    : state_directory_(std::move(state_directory)) {
    if (state_directory_.empty()) {
        throw EngineError(ErrorCode::invalid_argument, "state directory is empty");
    }
    if (!state_directory_.is_absolute()) {
        throw EngineError(
            ErrorCode::invalid_argument,
            "state directory is not absolute: " + state_directory_.string()
        );
    }
}

}
