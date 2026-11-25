#include <doctest/doctest.h>

#include "session/session_settings.hpp"

using lt::settings_pack;
using movies::session::session_settings;
using movies::session::SessionOptions;

// The settings that let a stream reach its first piece before the player's 20 s start deadline.
// Measured from a Mac on a real swarm (2026-11-25, a 15.9 GB film with 16 MiB pieces): with
// libtorrent's defaults the first piece took 45 s to over 90 s in 5 runs; with these 8 to 30 s,
// median 14 s, in 11 runs.
TEST_CASE("the public session connects fast and moves stalled requests to other peers") {
    const auto pack = session_settings(SessionOptions{});

    CHECK(pack.get_int(settings_pack::connection_speed) >= 200);
    CHECK(pack.get_int(settings_pack::torrent_connect_boost) >= 200);
    CHECK(pack.get_int(settings_pack::peer_connect_timeout) <= 3);
    CHECK(pack.get_int(settings_pack::request_timeout) <= 5);
    CHECK_FALSE(pack.get_bool(settings_pack::enable_outgoing_utp));
    CHECK(pack.get_bool(settings_pack::announce_to_all_tiers));
    CHECK(pack.get_bool(settings_pack::announce_to_all_trackers));
}

TEST_CASE("a stream that counts as finished keeps its seeds") {
    CHECK_FALSE(session_settings(SessionOptions{}).get_bool(settings_pack::close_redundant_connections));
}

TEST_CASE("the loopback session used by tests stays off the public network") {
    const auto pack = session_settings(SessionOptions{.local_only = true});

    CHECK(pack.get_str(settings_pack::listen_interfaces) == "127.0.0.1:0");
    CHECK_FALSE(pack.get_bool(settings_pack::enable_dht));
    CHECK_FALSE(pack.get_bool(settings_pack::enable_upnp));
}
