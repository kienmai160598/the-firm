#include "session/events.hpp"

#include <libtorrent/alert_types.hpp>

namespace movies::session {

std::optional<Event> to_event(const lt::alert& alert) {
    if (const auto* metadata = lt::alert_cast<lt::metadata_received_alert>(&alert)) {
        return MetadataReceived{metadata->handle};
    }
    if (const auto* piece = lt::alert_cast<lt::piece_finished_alert>(&alert)) {
        return PieceFinished{piece->handle, int(piece->piece_index)};
    }
    if (const auto* failure = lt::alert_cast<lt::torrent_error_alert>(&alert)) {
        return TorrentFailed{failure->handle, failure->error.message()};
    }
    return std::nullopt;
}

}
