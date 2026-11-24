#ifndef MOVIES_ENGINE_STORAGE_PAYLOAD_STORE_HPP
#define MOVIES_ENGINE_STORAGE_PAYLOAD_STORE_HPP

#include <cstdint>
#include <memory>
#include <span>
#include <vector>

#include <libtorrent/sha1_hash.hpp>

namespace movies::storage {

// The torrent's best info hash, the same one libtorrent hands its disk interface.
using TorrentKey = lt::sha1_hash;

struct PieceLayout {
    std::int64_t total_size = 0;
    int piece_length = 0;

    [[nodiscard]] int num_pieces() const;
    [[nodiscard]] int piece_size(int piece) const;
};

struct PieceBuffer {
    std::unique_ptr<char[]> bytes;
    int size = 0;

    [[nodiscard]] std::span<const char> view() const { return {bytes.get(), std::size_t(size)}; }
};

enum class WriteResult { written, over_budget, unknown_torrent };

// Where piece payload lives: in memory (E1) or on disk (E5). Thread safe: libtorrent's network
// thread writes, hash threads and HTTP readers read.
class PayloadStore {
public:
    virtual ~PayloadStore() = default;

    virtual void open(const TorrentKey& key, PieceLayout layout) = 0;
    virtual void close(const TorrentKey& key) = 0;
    virtual WriteResult write(const TorrentKey& key, int piece, int offset,
                              std::span<const char> data) = 0;
    // Copies exactly out.size() bytes, or returns false when the piece is not held.
    virtual bool read(const TorrentKey& key, int piece, int offset, std::span<char> out) const = 0;
    // The whole piece, kept alive by the pointer even if the piece is cleared meanwhile.
    [[nodiscard]] virtual std::shared_ptr<const PieceBuffer> piece(const TorrentKey& key,
                                                                   int piece) const = 0;
    virtual void clear(const TorrentKey& key, int piece) = 0;
    [[nodiscard]] virtual std::vector<int> resident_pieces(const TorrentKey& key) const = 0;
    [[nodiscard]] virtual std::int64_t bytes_used() const = 0;
};

}

#endif
