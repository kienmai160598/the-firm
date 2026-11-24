#include <doctest/doctest.h>

#include "torrent/file_choice.hpp"

using movies::torrent::any_file;
using movies::torrent::choose_file;
using movies::torrent::FileEntry;

namespace {

const std::vector<FileEntry> files{
    {"Movie/sample.MKV", 50, false},
    {"Movie/movie.mkv", 900, false},
    {"Movie/extras.zip", 5000, false},
    {".pad/1", 10, true},
};

}

TEST_CASE("any file means the largest video file, even when another file is larger") {
    CHECK(choose_file(files, any_file) == 1);
}

TEST_CASE("without a video file the largest file plays") {
    const std::vector<FileEntry> archives{{"a.zip", 5, false}, {"b.rar", 9, false}};

    CHECK(choose_file(archives, any_file) == 1);
}

TEST_CASE("a requested file plays as is, and a missing or padding file does not") {
    CHECK(choose_file(files, 0) == 0);
    CHECK(choose_file(files, 3) == std::nullopt);
    CHECK(choose_file(files, 4) == std::nullopt);
    CHECK(choose_file(files, -2) == std::nullopt);
}
