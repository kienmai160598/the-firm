#include "http/socket.hpp"

#include <poll.h>
#include <sys/socket.h>
#include <unistd.h>

#include <algorithm>

namespace movies::http {
namespace {

constexpr std::size_t max_head_bytes = 8192;
constexpr int poll_slice_ms = 250;
constexpr auto send_timeout = std::chrono::seconds(30);
constexpr std::string_view head_end = "\r\n\r\n";

#ifdef MSG_NOSIGNAL
constexpr int send_flags = MSG_NOSIGNAL;
#else
constexpr int send_flags = 0;
#endif

}

Socket::Socket(int descriptor) : descriptor_(descriptor) {
#ifdef SO_NOSIGPIPE
    // A player that hangs up must not kill the app with SIGPIPE.
    int on = 1;
    setsockopt(descriptor_, SOL_SOCKET, SO_NOSIGPIPE, &on, sizeof on);
#endif
}

Socket::~Socket() { ::close(descriptor_); }

bool Socket::wait(short events, const std::atomic<bool>& stopping,
                  std::chrono::steady_clock::time_point deadline) const {
    while (!stopping && std::chrono::steady_clock::now() < deadline) {
        pollfd target{descriptor_, events, 0};
        const int ready = ::poll(&target, 1, poll_slice_ms);
        if (ready < 0) return false;
        if (ready > 0) return (target.revents & events) != 0;
    }
    return false;
}

std::optional<std::string> Socket::read_head(const std::atomic<bool>& stopping,
                                             std::chrono::milliseconds timeout) {
    const auto deadline = std::chrono::steady_clock::now() + timeout;
    std::string head;
    char chunk[1024];
    while (head.size() < max_head_bytes) {
        if (!wait(POLLIN, stopping, deadline)) return std::nullopt;
        const auto received = ::recv(descriptor_, chunk, sizeof chunk, 0);
        if (received <= 0) return std::nullopt;
        head.append(chunk, std::size_t(received));
        const auto end = head.find(head_end);
        if (end != std::string::npos) return head.substr(0, end);
    }
    return std::nullopt;
}

bool Socket::send_all(std::span<const char> bytes, const std::atomic<bool>& stopping) {
    const auto deadline = std::chrono::steady_clock::now() + send_timeout;
    while (!bytes.empty()) {
        if (!wait(POLLOUT, stopping, deadline)) return false;
        const auto sent = ::send(descriptor_, bytes.data(), bytes.size(), send_flags);
        if (sent <= 0) return false;
        bytes = bytes.subspan(std::size_t(sent));
    }
    return true;
}

}
