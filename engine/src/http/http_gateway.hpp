#ifndef MOVIES_ENGINE_HTTP_HTTP_GATEWAY_HPP
#define MOVIES_ENGINE_HTTP_HTTP_GATEWAY_HPP

#include <atomic>
#include <cstdint>
#include <list>
#include <memory>
#include <mutex>
#include <string>
#include <string_view>
#include <thread>

#include "http/byte_source.hpp"

namespace movies::http {

// Serves streams to the player over HTTP on 127.0.0.1, on a port the system picks. One thread per
// connection, with a bound on how many run at once.
class HttpGateway {
public:
    // Throws EngineError(internal) when the loopback socket cannot be opened.
    explicit HttpGateway(ByteCatalog& catalog);
    ~HttpGateway();
    HttpGateway(const HttpGateway&) = delete;
    HttpGateway& operator=(const HttpGateway&) = delete;

    [[nodiscard]] std::uint16_t port() const { return port_; }
    // Ends in "/video.<ext>" of the played file, so a player that reads the URL sees the format.
    [[nodiscard]] std::string url_for(std::uint64_t stream, std::string_view file_name) const;
    // Stops accepting and waits for every connection to finish.
    void stop();

private:
    struct Connection {
        std::thread thread;
        std::shared_ptr<std::atomic<bool>> done;
    };

    void accept_loop();
    void start_connection(int descriptor);
    void join_finished();

    ByteCatalog& catalog_;
    int listener_ = -1;
    std::uint16_t port_ = 0;
    std::atomic<bool> stopping_{false};
    std::mutex mutex_;
    std::list<Connection> connections_;
    std::thread acceptor_;
};

}

#endif
