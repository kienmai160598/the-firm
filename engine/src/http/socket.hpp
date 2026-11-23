#ifndef MOVIES_ENGINE_HTTP_SOCKET_HPP
#define MOVIES_ENGINE_HTTP_SOCKET_HPP

#include <atomic>
#include <chrono>
#include <optional>
#include <span>
#include <string>

namespace movies::http {

// Owns a connected TCP socket. Every wait is bounded and gives up once `stopping` is set.
class Socket {
public:
    explicit Socket(int descriptor);
    ~Socket();
    Socket(const Socket&) = delete;
    Socket& operator=(const Socket&) = delete;

    // The bytes up to and without the blank line that ends a request head.
    std::optional<std::string> read_head(const std::atomic<bool>& stopping,
                                         std::chrono::milliseconds timeout);
    bool send_all(std::span<const char> bytes, const std::atomic<bool>& stopping);

private:
    // True once the socket is ready for `events`, false on timeout, stop or error.
    bool wait(short events, const std::atomic<bool>& stopping,
              std::chrono::steady_clock::time_point deadline) const;

    const int descriptor_;
};

}

#endif
