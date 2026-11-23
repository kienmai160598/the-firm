#include "api/stream_info_copy.hpp"

#include <algorithm>
#include <cstring>

namespace movies::api {
namespace {

template <std::size_t Size> void copy_text(const std::string& from, char (&to)[Size]) {
    const std::size_t length = std::min(from.size(), Size - 1);
    std::memcpy(to, from.data(), length);
    to[length] = '\0';
}

movies_engine_stream_state state_code(stream::StreamState state) {
    switch (state) {
    case stream::StreamState::resolving:
        return MOVIES_ENGINE_STREAM_RESOLVING;
    case stream::StreamState::ready:
        return MOVIES_ENGINE_STREAM_READY;
    case stream::StreamState::failed:
        return MOVIES_ENGINE_STREAM_FAILED;
    }
    return MOVIES_ENGINE_STREAM_FAILED;
}

}

void copy_stream_info(const core::StreamInfo& from, movies_engine_stream_info& to) {
    to = {};
    to.state = state_code(from.snapshot.state);
    to.file_index = from.snapshot.file_index;
    to.file_size = from.snapshot.file_size;
    copy_text(from.url, to.url);
    copy_text(from.snapshot.file_name, to.file_name);
    copy_text(from.snapshot.error, to.error);
}

}
