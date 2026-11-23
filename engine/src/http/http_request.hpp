#ifndef MOVIES_ENGINE_HTTP_HTTP_REQUEST_HPP
#define MOVIES_ENGINE_HTTP_HTTP_REQUEST_HPP

#include <cstdint>
#include <optional>
#include <string>
#include <string_view>

namespace movies::http {

struct Request {
    std::string method;
    std::string target;
    std::optional<std::string> range;
};

// Inclusive byte range of a file.
struct ByteRange {
    std::int64_t first = 0;
    std::int64_t last = 0;

    [[nodiscard]] std::int64_t length() const { return last - first + 1; }
};

// The request line and the Range header of a request head, or empty when the line is malformed.
std::optional<Request> parse_request(std::string_view head);

// The stream id of "/stream/<id>", with or without a trailing "/<name>".
std::optional<std::uint64_t> stream_id_of(std::string_view target);

// One range of a "bytes=" header ("a-b", "a-" or "-n") within a file of `size` bytes. Empty when
// the header is malformed, lists several ranges, or starts past the end.
std::optional<ByteRange> parse_range(std::string_view header, std::int64_t size);

}

#endif
