#include "support/http_client.hpp"

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>

#include <stdexcept>

namespace movies::test {
namespace {

constexpr std::string_view host_prefix = "http://127.0.0.1:";

int connect_to(std::uint16_t port) {
    const int descriptor = ::socket(AF_INET, SOCK_STREAM, 0);
    timeval timeout{60, 0};
    setsockopt(descriptor, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof timeout);
    sockaddr_in address{};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    address.sin_port = htons(port);
    if (::connect(descriptor, reinterpret_cast<sockaddr*>(&address), sizeof address) != 0) {
        ::close(descriptor);
        throw std::runtime_error("cannot connect to the gateway");
    }
    return descriptor;
}

}

HttpResponse http_get(const std::string& url, const std::string& range) {
    if (url.rfind(host_prefix, 0) != 0) throw std::runtime_error("not a loopback url: " + url);
    const auto path_start = url.find('/', host_prefix.size());
    const auto port = std::uint16_t(std::stoi(url.substr(host_prefix.size(), path_start - host_prefix.size())));
    std::string request = "GET " + url.substr(path_start) + " HTTP/1.1\r\nHost: 127.0.0.1\r\n";
    if (!range.empty()) request += "Range: bytes=" + range + "\r\n";
    request += "\r\n";

    const int descriptor = connect_to(port);
    ::send(descriptor, request.data(), request.size(), 0);
    std::string raw;
    char chunk[65536];
    ssize_t received = 0;
    while ((received = ::recv(descriptor, chunk, sizeof chunk, 0)) > 0) raw.append(chunk, std::size_t(received));
    ::close(descriptor);

    const auto head_end = raw.find("\r\n\r\n");
    if (head_end == std::string::npos) return {};
    HttpResponse response{std::stoi(raw.substr(9, 3)), raw.substr(0, head_end), raw.substr(head_end + 4)};
    return response;
}

}
