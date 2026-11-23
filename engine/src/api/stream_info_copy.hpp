#ifndef MOVIES_ENGINE_API_STREAM_INFO_COPY_HPP
#define MOVIES_ENGINE_API_STREAM_INFO_COPY_HPP

#include "core/engine.hpp"
#include "movies_engine/movies_engine.h"

namespace movies::api {

// Fills the fixed-size C struct, cutting strings that do not fit.
void copy_stream_info(const core::StreamInfo& from, movies_engine_stream_info& to);

}

#endif
