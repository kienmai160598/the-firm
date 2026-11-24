#ifndef MOVIES_ENGINE_HTTP_HTTP_CONNECTION_HPP
#define MOVIES_ENGINE_HTTP_HTTP_CONNECTION_HPP

#include <atomic>

#include "http/byte_source.hpp"
#include "http/socket.hpp"

namespace movies::http {

// Answers one GET or HEAD request on the socket, then closes it (no keep-alive: a player opens a
// new connection for each seek).
void serve_connection(Socket& socket, ByteCatalog& catalog, const std::atomic<bool>& stopping);

// Turns away a connection over the limit.
void refuse_connection(Socket& socket, const std::atomic<bool>& stopping);

}

#endif
