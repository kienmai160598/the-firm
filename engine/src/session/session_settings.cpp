#include "session/session_settings.hpp"

#include <libtorrent/alert.hpp>

namespace movies::session {
namespace {

constexpr int peer_connection_limit = 200;
constexpr int loopback_connect_timeout_s = 2;

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

    const bool public_network = !options.local_only;
    pack.set_str(settings_pack::listen_interfaces,
                 public_network ? "0.0.0.0:0,[::]:0" : "127.0.0.1:0");
    pack.set_bool(settings_pack::enable_dht, public_network);
    pack.set_bool(settings_pack::enable_lsd, public_network);
    pack.set_bool(settings_pack::enable_upnp, public_network);
    pack.set_bool(settings_pack::enable_natpmp, public_network);
    // uTP to a loopback peer times out before libtorrent falls back to TCP; fail it fast.
    if (!public_network) pack.set_int(settings_pack::peer_connect_timeout, loopback_connect_timeout_s);
    return pack;
}

}
