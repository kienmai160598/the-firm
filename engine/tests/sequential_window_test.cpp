#include <doctest/doctest.h>

#include "stream/sequential_window.hpp"

using namespace movies::stream;

namespace {

// Ten pieces of 100 bytes; the file starts 50 bytes into piece 0 and ends in piece 8.
const FileSpan file{.offset = 50, .size = 800, .piece_length = 100, .num_pieces = 10};
const WindowBudget budget{.ahead_bytes = 300, .behind_bytes = 100, .urgent_pieces = 1};

}

TEST_CASE("file positions map to the torrent pieces that hold them") {
    CHECK(file.piece_at(0) == 0);
    CHECK(file.piece_at(49) == 0);
    CHECK(file.piece_at(50) == 1);
    CHECK(file.piece_at(799) == 8);
    CHECK(file.pieces_between(700, 5000) == std::vector<int>{7, 8});
    CHECK(file.pieces_between(800, 900).empty());
}

TEST_CASE("a reader wants the bytes ahead of it, the first piece urgently, nothing else") {
    const auto plan = plan_window(file, {250}, budget);

    CHECK(plan.priorities == std::vector<std::uint8_t>{0, 0, 0, 7, 4, 4, 0, 0, 0, 0});
    CHECK(plan.urgent == std::vector<int>{3});
}

TEST_CASE("two readers want both windows, and the window stops at the end of the file") {
    const auto plan = plan_window(file, {0, 700}, budget);

    CHECK(plan.priorities == std::vector<std::uint8_t>{7, 4, 4, 4, 0, 0, 0, 7, 4, 0});
    CHECK(plan.urgent == std::vector<int>{0, 7});
}

TEST_CASE("eviction keeps what is behind and ahead of a reader and drops the rest") {
    const std::vector<int> resident{0, 1, 2, 3, 4, 5, 6, 9};

    const auto evict = pieces_to_evict(file, {450}, budget, resident);

    CHECK(evict == std::vector<int>{0, 1, 2, 3, 9});
}
