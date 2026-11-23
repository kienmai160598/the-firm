#include "movies_engine/movies_engine.h"

#include <exception>
#include <new>
#include <string>

#include "core/engine.hpp"
#include "core/engine_error.hpp"

struct movies_engine {
    movies::core::Engine engine;
};

namespace {

thread_local std::string last_error;

movies_engine_status fail(const movies_engine_status status, const char* message) {
    try {
        last_error = message;
    } catch (...) {
        last_error.clear();
    }
    return status;
}

movies_engine_status status_for(const movies::core::ErrorCode code) {
    switch (code) {
    case movies::core::ErrorCode::invalid_argument:
        return MOVIES_ENGINE_INVALID_ARGUMENT;
    case movies::core::ErrorCode::internal:
        return MOVIES_ENGINE_INTERNAL_ERROR;
    }
    return MOVIES_ENGINE_INTERNAL_ERROR;
}

// Runs one ABI call: clears the last error, and turns every exception into a status so none
// crosses the extern "C" boundary.
template <typename Call>
movies_engine_status guarded(Call&& call) noexcept {
    last_error.clear();
    try {
        call();
        return MOVIES_ENGINE_OK;
    } catch (const movies::core::EngineError& error) {
        return fail(status_for(error.code()), error.what());
    } catch (const std::bad_alloc&) {
        return fail(MOVIES_ENGINE_INTERNAL_ERROR, "out of memory");
    } catch (const std::exception& error) {
        return fail(MOVIES_ENGINE_INTERNAL_ERROR, error.what());
    } catch (...) {
        return fail(MOVIES_ENGINE_INTERNAL_ERROR, "unknown error");
    }
}

}

extern "C" {

const char* movies_engine_version(void) {
    return MOVIES_ENGINE_VERSION;
}

const char* movies_engine_last_error(void) {
    return last_error.c_str();
}

movies_engine_status movies_engine_create(
    const movies_engine_config* config,
    movies_engine** engine
) {
    if (engine == nullptr) {
        return fail(MOVIES_ENGINE_INVALID_ARGUMENT, "engine output is NULL");
    }
    *engine = nullptr;
    if (config == nullptr || config->state_directory == nullptr) {
        return fail(MOVIES_ENGINE_INVALID_ARGUMENT, "config or state directory is NULL");
    }
    return guarded([&] {
        *engine = new movies_engine{
            movies::core::Engine(movies::core::EngineConfig(config->state_directory)),
        };
    });
}

void movies_engine_destroy(movies_engine* engine) {
    delete engine;
}

}
