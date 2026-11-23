#ifndef MOVIES_ENGINE_SESSION_SESSION_SETTINGS_HPP
#define MOVIES_ENGINE_SESSION_SESSION_SETTINGS_HPP

#include <libtorrent/settings_pack.hpp>

namespace movies::session {

struct SessionOptions {
    // Tests: listen on loopback only, no DHT, local discovery or port mapping.
    bool local_only = false;
};

lt::settings_pack session_settings(const SessionOptions& options);

}

#endif
