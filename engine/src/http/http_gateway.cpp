#include "http/http_gateway.hpp"

#include <arpa/inet.h>
#include <netinet/in.h>
#include <poll.h>
#include <sys/socket.h>
#include <unistd.h>

#include <cerrno>
#include <cstring>

#include "core/engine_error.hpp"
#include "http/http_connection.hpp"

namespace movies::http {
namespace {

constexpr int listen_backlog = 16;
constexpr std::size_t max_connections = 16;
constexpr int accept_poll_ms = 250;

[[noreturn]] void fail(const char* step) {
    throw core::EngineError(core::ErrorCode::internal,
                            std::string("HTTP gateway cannot ") + step + ": " + std::strerror(errno));
}

}

HttpGateway::HttpGateway(ByteCatalog& catalog) : catalog_(catalog) {
    listener_ = ::socket(AF_INET, SOCK_STREAM, 0);
    if (listener_ < 0) fail("open a socket");
    sockaddr_in address{};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    address.sin_port = 0;
    socklen_t length = sizeof address;
    if (::bind(listener_, reinterpret_cast<sockaddr*>(&address), sizeof address) != 0
        || ::listen(listener_, listen_backlog) != 0
        || ::getsockname(listener_, reinterpret_cast<sockaddr*>(&address), &length) != 0) {
        ::close(listener_);
        fail("listen on 127.0.0.1");
    }
    port_ = ntohs(address.sin_port);
    acceptor_ = std::thread([this] { accept_loop(); });
}

HttpGateway::~HttpGateway() { stop(); }

std::string HttpGateway::url_for(std::uint64_t stream) const {
    return "http://127.0.0.1:" + std::to_string(port_) + "/stream/" + std::to_string(stream);
}

void HttpGateway::stop() {
    if (stopping_.exchange(true)) return;
    if (acceptor_.joinable()) acceptor_.join();
    ::close(listener_);
    std::scoped_lock lock(mutex_);
    for (auto& connection : connections_) connection.thread.join();
    connections_.clear();
}

void HttpGateway::accept_loop() {
    while (!stopping_) {
        pollfd listener{listener_, POLLIN, 0};
        if (::poll(&listener, 1, accept_poll_ms) <= 0) continue;
        const int descriptor = ::accept(listener_, nullptr, nullptr);
        if (descriptor >= 0) start_connection(descriptor);
    }
}

void HttpGateway::start_connection(int descriptor) {
    std::scoped_lock lock(mutex_);
    join_finished();
    if (connections_.size() >= max_connections) {
        Socket socket(descriptor);
        refuse_connection(socket, stopping_);
        return;
    }
    auto done = std::make_shared<std::atomic<bool>>(false);
    connections_.push_back({std::thread([this, descriptor, done] {
                                Socket socket(descriptor);
                                serve_connection(socket, catalog_, stopping_);
                                *done = true;
                            }),
                            done});
}

void HttpGateway::join_finished() {
    for (auto connection = connections_.begin(); connection != connections_.end();) {
        if (!*connection->done) {
            ++connection;
            continue;
        }
        connection->thread.join();
        connection = connections_.erase(connection);
    }
}

}
