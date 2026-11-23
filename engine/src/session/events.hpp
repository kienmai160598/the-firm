#ifndef MOVIES_ENGINE_SESSION_EVENTS_HPP
#define MOVIES_ENGINE_SESSION_EVENTS_HPP

#include <optional>
#include <string>
#include <variant>

#include <libtorrent/alert.hpp>
#include <libtorrent/torrent_handle.hpp>

namespace movies::session {

struct MetadataReceived {
    lt::torrent_handle torrent;
};

struct PieceFinished {
    lt::torrent_handle torrent;
    int piece = 0;
};

struct TorrentFailed {
    lt::torrent_handle torrent;
    std::string message;
};

// The libtorrent alerts the engine acts on, as plain values that outlive the alert.
using Event = std::variant<MetadataReceived, PieceFinished, TorrentFailed>;

std::optional<Event> to_event(const lt::alert& alert);

class EventSink {
public:
    virtual ~EventSink() = default;
    virtual void on_event(const Event& event) = 0;
};

}

#endif
