#include "session/session_settings.hpp"

#include <libtorrent/alert.hpp>

namespace movies::session {
namespace {

constexpr int peer_connection_limit = 200;
// Connection attempts per second, and the extra burst a torrent gets when it is added. A magnet
// brings hundreds of peers and most are gone; with libtorrent's 30 a stream had 3 to 5 live ones
// after 20 s (2026-11-25).
constexpr int connection_attempts_per_second = 200;
constexpr int new_torrent_connect_boost = 200;
// A dead peer frees its attempt after this instead of libtorrent's 15 s.
constexpr int peer_connect_timeout_s = 3;
// A request a peer has not answered in this time goes to another peer. libtorrent's 60 s let one
// slow peer hold blocks of the first piece while later pieces finished: the reader, which needs
// that piece whole, waited past the player's 20 s start deadline.
constexpr int block_request_timeout_s = 5;

}

lt::settings_pack session_settings(const SessionOptions& options) {
    using lt::settings_pack;
    settings_pack pack;
    pack.set_int(settings_pack::alert_mask, lt::alert_category::status | lt::alert_category::error
                                                | lt::alert_category::piece_progress);
    pack.set_str(settings_pack::user_agent, "Movies/" MOVIES_ENGINE_VERSION);
    pack.set_int(settings_pack::connections_limit, peer_connection_limit);
    // A stream wants only a window of pieces, so the torrent often counts as finished. With this
    // on, libtorrent would then drop every seed and tell peers it only uploads.
    pack.set_bool(settings_pack::close_redundant_connections, false);

    // Reach the swarm fast: a stream cannot start before its first piece is whole.
    pack.set_int(settings_pack::connection_speed, connection_attempts_per_second);
    pack.set_int(settings_pack::torrent_connect_boost, new_torrent_connect_boost);
    pack.set_int(settings_pack::peer_connect_timeout, peer_connect_timeout_s);
    pack.set_int(settings_pack::request_timeout, block_request_timeout_s);
    // Outgoing uTP to an unreachable peer waits out its timeout before the TCP attempt; TCP alone
    // connects sooner. Incoming uTP stays on.
    pack.set_bool(settings_pack::enable_outgoing_utp, false);
    // Every tracker of the magnet link, not only the first that answers.
    pack.set_bool(settings_pack::announce_to_all_tiers, true);
    pack.set_bool(settings_pack::announce_to_all_trackers, true);

    const bool public_network = !options.local_only;
    pack.set_str(settings_pack::listen_interfaces,
                 public_network ? "0.0.0.0:0,[::]:0" : "127.0.0.1:0");
    pack.set_bool(settings_pack::enable_dht, public_network);
    pack.set_bool(settings_pack::enable_lsd, public_network);
    pack.set_bool(settings_pack::enable_upnp, public_network);
    pack.set_bool(settings_pack::enable_natpmp, public_network);
    return pack;
}

}
