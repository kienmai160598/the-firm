#include <doctest/doctest.h>

#include <string>
#include <vector>

#include "storage/memory/memory_payload_store.hpp"

using movies::storage::MemoryPayloadStore;
using movies::storage::PieceLayout;
using movies::storage::TorrentKey;
using movies::storage::WriteResult;

namespace {

constexpr int piece_length = 64;
const TorrentKey key("aaaaaaaaaaaaaaaaaaaa");

std::span<const char> bytes(const std::string& text) { return {text.data(), text.size()}; }

}

TEST_CASE("the last piece is shorter when the payload does not fill it") {
    const PieceLayout layout{150, piece_length};

    CHECK(layout.num_pieces() == 3);
    CHECK(layout.piece_size(0) == 64);
    CHECK(layout.piece_size(2) == 22);
    CHECK(layout.piece_size(3) == 0);
}

TEST_CASE("bytes written to a piece read back from any offset") {
    MemoryPayloadStore store(1024);
    store.open(key, {150, piece_length});

    REQUIRE(store.write(key, 1, 10, bytes("hello")) == WriteResult::written);
    std::string out(3, '\0');
    const bool held = store.read(key, 1, 12, {out.data(), out.size()});

    CHECK(held);
    CHECK(out == "llo");
}

TEST_CASE("a piece takes its full size from the first block and the cap holds") {
    MemoryPayloadStore store(2 * piece_length);
    store.open(key, {1000, piece_length});

    CHECK(store.write(key, 0, 0, bytes("a")) == WriteResult::written);
    CHECK(store.write(key, 1, 0, bytes("b")) == WriteResult::written);
    CHECK(store.write(key, 2, 0, bytes("c")) == WriteResult::over_budget);
    CHECK(store.write(key, 1, 5, bytes("d")) == WriteResult::written);
    CHECK(store.bytes_used() == 2 * piece_length);
    CHECK(store.resident_pieces(key) == std::vector<int>{0, 1});
}

TEST_CASE("clearing a piece frees its bytes and makes reads fail") {
    MemoryPayloadStore store(piece_length);
    store.open(key, {1000, piece_length});
    store.write(key, 0, 0, bytes("a"));
    const auto kept = store.piece(key, 0);

    store.clear(key, 0);

    char out = 0;
    CHECK_FALSE(store.read(key, 0, 0, {&out, 1}));
    CHECK(store.bytes_used() == 0);
    CHECK(store.write(key, 3, 0, bytes("b")) == WriteResult::written);
    CHECK(kept->view()[0] == 'a');
}

TEST_CASE("closing a torrent frees all its pieces") {
    MemoryPayloadStore store(1024);
    store.open(key, {1000, piece_length});
    store.write(key, 0, 0, bytes("a"));
    store.write(key, 4, 0, bytes("b"));

    store.close(key);

    CHECK(store.bytes_used() == 0);
    CHECK(store.write(key, 0, 0, bytes("a")) == WriteResult::unknown_torrent);
}

TEST_CASE("writes and reads outside a piece are refused") {
    MemoryPayloadStore store(1024);
    store.open(key, {150, piece_length});

    CHECK(store.write(key, 2, 20, bytes("abc")) == WriteResult::unknown_torrent);
    store.write(key, 2, 0, bytes("abc"));
    std::string out(30, '\0');
    CHECK_FALSE(store.read(key, 2, 0, {out.data(), out.size()}));
}

TEST_CASE("a reader gets the written bytes after its offset up to the first gap") {
    MemoryPayloadStore store(1024);
    store.open(key, {150, piece_length});
    store.write(key, 0, 0, bytes("abcd"));
    store.write(key, 0, 8, bytes("ijkl"));
    std::string out(16, '\0');

    const int copied = store.read_written(key, 0, 1, {out.data(), out.size()});

    CHECK(copied == 3);
    CHECK(out.substr(0, 3) == "bcd");
}

TEST_CASE("a block arriving in the gap joins the runs on both sides") {
    MemoryPayloadStore store(1024);
    store.open(key, {150, piece_length});
    store.write(key, 0, 0, bytes("abcd"));
    store.write(key, 0, 8, bytes("ijkl"));
    store.write(key, 0, 4, bytes("efgh"));
    std::string out(16, '\0');

    const int copied = store.read_written(key, 0, 2, {out.data(), out.size()});

    CHECK(copied == 10);
    CHECK(out.substr(0, 10) == "cdefghijkl");
}

TEST_CASE("nothing is read where the byte at the offset has not arrived") {
    MemoryPayloadStore store(1024);
    store.open(key, {150, piece_length});
    store.write(key, 0, 8, bytes("ijkl"));
    char out[4] = {};

    CHECK(store.read_written(key, 0, 0, {out, 4}) == 0);
    CHECK(store.read_written(key, 0, 12, {out, 4}) == 0);
    CHECK(store.read_written(key, 1, 0, {out, 4}) == 0);
}

TEST_CASE("a read of written bytes stops at the size asked for") {
    MemoryPayloadStore store(1024);
    store.open(key, {150, piece_length});
    store.write(key, 0, 0, bytes("abcdefgh"));
    char out[3] = {};

    CHECK(store.read_written(key, 0, 2, {out, 3}) == 3);
    CHECK(std::string(out, 3) == "cde");
}

TEST_CASE("a cleared piece forgets which of its bytes were written") {
    MemoryPayloadStore store(1024);
    store.open(key, {150, piece_length});
    store.write(key, 0, 0, bytes("abcd"));

    store.clear(key, 0);
    store.write(key, 0, 8, bytes("ijkl"));

    char out[4] = {};
    CHECK(store.read_written(key, 0, 0, {out, 4}) == 0);
}
