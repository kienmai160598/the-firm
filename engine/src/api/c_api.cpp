#include "movies_engine/movies_engine.h"

#include <exception>
#include <new>
#include <string>

#include "api/stream_info_copy.hpp"
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
    case movies::core::ErrorCode::not_found:
        return MOVIES_ENGINE_NOT_FOUND;
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

movies_engine_status movies_engine_open_stream(
    movies_engine* engine,
    const char* magnet_uri,
    int32_t file_index,
    uint64_t* stream
) {
    if (engine == nullptr || magnet_uri == nullptr || stream == nullptr) {
        return fail(MOVIES_ENGINE_INVALID_ARGUMENT, "engine, magnet link or stream output is NULL");
    }
    return guarded([&] { *stream = engine->engine.open_stream(magnet_uri, file_index); });
}

movies_engine_status movies_engine_get_stream_info(
    movies_engine* engine,
    uint64_t stream,
    movies_engine_stream_info* info
) {
    if (engine == nullptr || info == nullptr) {
        return fail(MOVIES_ENGINE_INVALID_ARGUMENT, "engine or info output is NULL");
    }
    return guarded([&] {
        const auto found = engine->engine.stream_info(stream);
        if (!found) throw movies::core::EngineError(movies::core::ErrorCode::not_found, "no such stream");
        movies::api::copy_stream_info(*found, *info);
    });
}

movies_engine_status movies_engine_close_stream(movies_engine* engine, uint64_t stream) {
    if (engine == nullptr) return fail(MOVIES_ENGINE_INVALID_ARGUMENT, "engine is NULL");
    return guarded([&] {
        if (!engine->engine.close_stream(stream)) {
            throw movies::core::EngineError(movies::core::ErrorCode::not_found, "no such stream");
        }
    });
}

}
