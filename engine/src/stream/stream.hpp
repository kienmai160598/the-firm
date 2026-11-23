#ifndef MOVIES_ENGINE_STREAM_STREAM_HPP
#define MOVIES_ENGINE_STREAM_STREAM_HPP

#include <chrono>
#include <cstdint>
#include <map>
#include <string>
#include <vector>

#include <libtorrent/torrent_handle.hpp>

#include "storage/payload_store.hpp"
#include "stream/sequential_window.hpp"

namespace movies::stream {

enum class StreamState { resolving, ready, failed };

// What the host may know about a stream.
struct StreamSnapshot {
    StreamState state = StreamState::resolving;
    int file_index = -1;
    std::int64_t file_size = 0;
    std::string file_name;
    std::string error;
};

struct StreamLimits {
    WindowBudget window;
    std::int64_t eviction_threshold_bytes = 0;  // evict unneeded pieces once the store holds more
    std::chrono::milliseconds read_timeout{0};
};

// One played file of one torrent. Guarded by the StreamService mutex.
struct Stream {
    std::uint64_t id = 0;
    lt::torrent_handle torrent;
    int requested_file = -1;
    StreamSnapshot snapshot;
    FileSpan file;
    storage::TorrentKey key;
    std::vector<bool> have;                       // verified pieces, per torrent piece
    std::map<std::uint64_t, std::int64_t> readers;  // reader id to its byte position in the file
    bool ever_read = false;
    bool closed = false;
};

}

#endif
