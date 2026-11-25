#ifndef MOVIES_ENGINE_STORAGE_MEMORY_MEMORY_PAYLOAD_STORE_HPP
#define MOVIES_ENGINE_STORAGE_MEMORY_MEMORY_PAYLOAD_STORE_HPP

#include <cstdint>
#include <map>
#include <mutex>

#include "storage/payload_store.hpp"
#include "storage/written_ranges.hpp"

namespace movies::storage {

// Keeps every piece in RAM, so streaming writes nothing to flash. A piece takes its full size from
// its first block on, and no write may take the total over the byte cap.
class MemoryPayloadStore final : public PayloadStore {
public:
    explicit MemoryPayloadStore(std::int64_t byte_cap);

    void open(const TorrentKey& key, PieceLayout layout) override;
    void close(const TorrentKey& key) override;
    WriteResult write(const TorrentKey& key, int piece, int offset,
                      std::span<const char> data) override;
    bool read(const TorrentKey& key, int piece, int offset, std::span<char> out) const override;
    int read_written(const TorrentKey& key, int piece, int offset, std::span<char> out) const override;
    [[nodiscard]] std::shared_ptr<const PieceBuffer> piece(const TorrentKey& key,
                                                           int piece) const override;
    void clear(const TorrentKey& key, int piece) override;
    [[nodiscard]] std::vector<int> resident_pieces(const TorrentKey& key) const override;
    [[nodiscard]] std::int64_t bytes_used() const override;

private:
    struct Torrent {
        PieceLayout layout;
        std::map<int, std::shared_ptr<PieceBuffer>> pieces;
        std::map<int, WrittenRanges> written;  // pieces[piece]'s bytes that have arrived
    };

    std::shared_ptr<PieceBuffer> find(const TorrentKey& key, int piece) const;

    const std::int64_t byte_cap_;
    mutable std::mutex mutex_;
    std::map<TorrentKey, Torrent> torrents_;
    std::int64_t bytes_used_ = 0;
};

}

#endif
