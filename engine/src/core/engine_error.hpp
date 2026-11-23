#ifndef MOVIES_ENGINE_CORE_ENGINE_ERROR_HPP
#define MOVIES_ENGINE_CORE_ENGINE_ERROR_HPP

#include <stdexcept>
#include <string>

namespace movies::core {

enum class ErrorCode {
    invalid_argument,
    internal,
    not_found,
};

// The one exception type services throw. The C ABI maps the code to a status.
class EngineError : public std::runtime_error {
public:
    EngineError(ErrorCode code, const std::string& message)
        : std::runtime_error(message), code_(code) {}

    [[nodiscard]] ErrorCode code() const noexcept { return code_; }

private:
    ErrorCode code_;
};

}

#endif
