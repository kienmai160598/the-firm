#include "http/http_request.hpp"

#include <algorithm>
#include <cctype>
#include <charconv>

namespace movies::http {
namespace {

constexpr std::string_view stream_prefix = "/stream/";
constexpr std::string_view range_unit = "bytes=";

std::string lowercase(std::string_view text) {
    std::string result(text);
    std::transform(result.begin(), result.end(), result.begin(),
                   [](unsigned char letter) { return char(std::tolower(letter)); });
    return result;
}

std::string_view trim(std::string_view text) {
    while (!text.empty() && (text.front() == ' ' || text.front() == '\t')) text.remove_prefix(1);
    while (!text.empty() && (text.back() == ' ' || text.back() == '\t' || text.back() == '\r')) {
        text.remove_suffix(1);
    }
    return text;
}

template <typename Number> std::optional<Number> number(std::string_view text) {
    Number value{};
    const auto [end, error] = std::from_chars(text.data(), text.data() + text.size(), value);
    if (error != std::errc() || end != text.data() + text.size() || text.empty()) return std::nullopt;
    return value;
}

}

std::optional<Request> parse_request(std::string_view head) {
    const auto line_end = head.find("\r\n");
    const std::string_view line = head.substr(0, line_end);
    const auto first_space = line.find(' ');
    const auto second_space = line.find(' ', first_space + 1);
    if (first_space == std::string_view::npos || second_space == std::string_view::npos) {
        return std::nullopt;
    }
    Request request{std::string(line.substr(0, first_space)),
                    std::string(line.substr(first_space + 1, second_space - first_space - 1)),
                    std::nullopt};
    std::size_t position = line_end == std::string_view::npos ? head.size() : line_end + 2;
    while (position < head.size()) {
        const auto end = std::min(head.find("\r\n", position), head.size());
        const std::string_view header = head.substr(position, end - position);
        const auto colon = header.find(':');
        if (colon != std::string_view::npos && lowercase(trim(header.substr(0, colon))) == "range") {
            request.range = std::string(trim(header.substr(colon + 1)));
        }
        position = end + 2;
    }
    return request;
}

std::optional<std::uint64_t> stream_id_of(std::string_view target) {
    if (!target.starts_with(stream_prefix)) return std::nullopt;
    target.remove_prefix(stream_prefix.size());
    return number<std::uint64_t>(target.substr(0, target.find('/')));
}

std::optional<ByteRange> parse_range(std::string_view header, std::int64_t size) {
    if (!header.starts_with(range_unit) || size <= 0) return std::nullopt;
    header.remove_prefix(range_unit.size());
    const auto dash = header.find('-');
    if (dash == std::string_view::npos || header.find(',') != std::string_view::npos) return std::nullopt;
    const auto start = header.substr(0, dash);
    const auto end = header.substr(dash + 1);
    if (start.empty()) {
        const auto suffix = number<std::int64_t>(end);
        if (!suffix || *suffix <= 0) return std::nullopt;
        return ByteRange{std::max<std::int64_t>(size - *suffix, 0), size - 1};
    }
    const auto first = number<std::int64_t>(start);
    if (!first || *first >= size) return std::nullopt;
    if (end.empty()) return ByteRange{*first, size - 1};
    const auto last = number<std::int64_t>(end);
    if (!last || *last < *first) return std::nullopt;
    return ByteRange{*first, std::min(*last, size - 1)};
}

}
