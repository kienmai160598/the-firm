#include "http/http_connection.hpp"

#include <algorithm>
#include <string>
#include <vector>

#include "http/http_request.hpp"
#include "http/media_type.hpp"

namespace movies::http {
namespace {

constexpr auto head_timeout = std::chrono::seconds(10);
constexpr std::size_t body_chunk_bytes = 256 * 1024;

std::string status_head(int code, const char* reason) {
    return "HTTP/1.1 " + std::to_string(code) + " " + reason + "\r\nContent-Length: 0\r\n"
        "Connection: close\r\n\r\n";
}

std::string body_head(std::int64_t size, const ByteRange& range, bool partial,
                      std::string_view type) {
    std::string head = partial ? "HTTP/1.1 206 Partial Content\r\n" : "HTTP/1.1 200 OK\r\n";
    head += "Content-Type: " + std::string(type) + "\r\nAccept-Ranges: bytes\r\n";
    head += "Content-Length: " + std::to_string(range.length()) + "\r\n";
    if (partial) {
        head += "Content-Range: bytes " + std::to_string(range.first) + "-"
            + std::to_string(range.last) + "/" + std::to_string(size) + "\r\n";
    }
    return head + "Connection: close\r\n\r\n";
}

void send_body(Socket& socket, ByteReader& reader, const ByteRange& range,
               const std::atomic<bool>& stopping) {
    std::vector<char> buffer(body_chunk_bytes);
    std::int64_t position = range.first;
    while (position <= range.last && !stopping) {
        const auto wanted = std::size_t(std::min<std::int64_t>(range.last - position + 1, std::int64_t(buffer.size())));
        const auto result = reader.read(position, {buffer.data(), wanted});
        if (result.status != ReadStatus::ok || result.bytes == 0) return;
        if (!socket.send_all({buffer.data(), result.bytes}, stopping)) return;
        position += std::int64_t(result.bytes);
    }
}

void send_text(Socket& socket, const std::string& text, const std::atomic<bool>& stopping) {
    socket.send_all({text.data(), text.size()}, stopping);
}

}

void serve_connection(Socket& socket, ByteCatalog& catalog, const std::atomic<bool>& stopping) {
    const auto head = socket.read_head(stopping, head_timeout);
    if (!head) return;
    const auto request = parse_request(*head);
    if (!request) return send_text(socket, status_head(400, "Bad Request"), stopping);
    if (request->method != "GET" && request->method != "HEAD") {
        return send_text(socket, status_head(405, "Method Not Allowed"), stopping);
    }
    const auto stream = stream_id_of(request->target);
    const auto size = stream ? catalog.size_of(*stream) : std::nullopt;
    if (!size) return send_text(socket, status_head(404, "Not Found"), stopping);

    ByteRange range{0, *size - 1};
    if (request->range) {
        const auto asked = parse_range(*request->range, *size);
        if (!asked) return send_text(socket, status_head(416, "Range Not Satisfiable"), stopping);
        range = *asked;
    }
    const auto reader = request->method == "GET" ? catalog.open_reader(*stream, range.first) : nullptr;
    if (request->method == "GET" && !reader) {
        return send_text(socket, status_head(404, "Not Found"), stopping);
    }
    send_text(socket, body_head(*size, range, request->range.has_value(), media_type_for(request->target)), stopping);
    if (reader && *size > 0) send_body(socket, *reader, range, stopping);
}

void refuse_connection(Socket& socket, const std::atomic<bool>& stopping) {
    send_text(socket, status_head(503, "Service Unavailable"), stopping);
}

}
