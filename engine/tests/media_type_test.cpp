#include <doctest/doctest.h>

#include "http/media_type.hpp"

using namespace movies::http;

TEST_CASE("the extension comes from the last path segment, lowercased") {
    CHECK(extension_of("/stream/7/video.MKV") == "mkv");
    CHECK(extension_of("Big Buck Bunny.1080p.mp4") == "mp4");
    CHECK(extension_of("/stream/7") == "");
    CHECK(extension_of("/stream.d/7") == "");
    CHECK(extension_of("movie.m p4") == "");
}

TEST_CASE("a known extension gives its media type, anything else octet-stream") {
    CHECK(media_type_for("/stream/7/video.mp4") == "video/mp4");
    CHECK(media_type_for("/stream/7/video.mkv") == "video/x-matroska");
    CHECK(media_type_for("/stream/7") == "application/octet-stream");
    CHECK(media_type_for("/stream/7/video.iso") == "application/octet-stream");
}
