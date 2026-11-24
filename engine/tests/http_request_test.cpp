#include <doctest/doctest.h>

#include "http/http_request.hpp"

using namespace movies::http;

TEST_CASE("a request head gives its method, target and Range header") {
    const auto request = parse_request(
        "GET /stream/7/movie.mkv HTTP/1.1\r\nHost: 127.0.0.1\r\nrange:  bytes=100-\r\n");

    REQUIRE(request);
    CHECK(request->method == "GET");
    CHECK(request->target == "/stream/7/movie.mkv");
    CHECK(request->range == "bytes=100-");
    CHECK_FALSE(parse_request("GARBAGE"));
}

TEST_CASE("stream ids come from /stream/<id> paths only") {
    CHECK(stream_id_of("/stream/42") == 42u);
    CHECK(stream_id_of("/stream/42/name.mkv") == 42u);
    CHECK_FALSE(stream_id_of("/stream/"));
    CHECK_FALSE(stream_id_of("/streams/42"));
    CHECK_FALSE(stream_id_of("/stream/4x2"));
}

TEST_CASE("byte ranges clip to the file, and ranges past the end or malformed fail") {
    constexpr std::int64_t size = 1000;

    CHECK(parse_range("bytes=0-99", size)->length() == 100);
    CHECK(parse_range("bytes=900-", size)->last == 999);
    CHECK(parse_range("bytes=900-5000", size)->last == 999);
    CHECK(parse_range("bytes=-100", size)->first == 900);
    CHECK(parse_range("bytes=-5000", size)->first == 0);
    CHECK_FALSE(parse_range("bytes=1000-", size));
    CHECK_FALSE(parse_range("bytes=5-1", size));
    CHECK_FALSE(parse_range("bytes=0-1,5-9", size));
    CHECK_FALSE(parse_range("items=0-1", size));
}
