#include "storage/memory/memory_payload_store.hpp"

#include <cstring>

namespace movies::storage {

MemoryPayloadStore::MemoryPayloadStore(std::int64_t byte_cap) : byte_cap_(byte_cap) {}

void MemoryPayloadStore::open(const TorrentKey& key, PieceLayout layout) {
    std::scoped_lock lock(mutex_);
    torrents_.try_emplace(key, Torrent{layout, {}});
}

void MemoryPayloadStore::close(const TorrentKey& key) {
    std::scoped_lock lock(mutex_);
    const auto torrent = torrents_.find(key);
    if (torrent == torrents_.end()) return;
    for (const auto& [index, buffer] : torrent->second.pieces) bytes_used_ -= buffer->size;
    torrents_.erase(torrent);
}

WriteResult MemoryPayloadStore::write(const TorrentKey& key, int piece, int offset,
                                      std::span<const char> data) {
    std::scoped_lock lock(mutex_);
    const auto torrent = torrents_.find(key);
    if (torrent == torrents_.end()) return WriteResult::unknown_torrent;
    const int size = torrent->second.layout.piece_size(piece);
    if (offset < 0 || offset + std::int64_t(data.size()) > size) return WriteResult::unknown_torrent;

    auto& buffer = torrent->second.pieces[piece];
    if (!buffer) {
        if (bytes_used_ + size > byte_cap_) {
            torrent->second.pieces.erase(piece);
            return WriteResult::over_budget;
        }
        buffer = std::make_shared<PieceBuffer>(PieceBuffer{std::unique_ptr<char[]>(new char[size]), size});
        bytes_used_ += size;
    }
    std::memcpy(buffer->bytes.get() + offset, data.data(), data.size());
    return WriteResult::written;
}

bool MemoryPayloadStore::read(const TorrentKey& key, int piece, int offset,
                              std::span<char> out) const {
    const auto buffer = find(key, piece);
    if (!buffer || offset < 0 || offset + std::int64_t(out.size()) > buffer->size) return false;
    std::memcpy(out.data(), buffer->bytes.get() + offset, out.size());
    return true;
}

std::shared_ptr<const PieceBuffer> MemoryPayloadStore::piece(const TorrentKey& key,
                                                             int piece) const {
    return find(key, piece);
}

void MemoryPayloadStore::clear(const TorrentKey& key, int piece) {
    std::scoped_lock lock(mutex_);
    const auto torrent = torrents_.find(key);
    if (torrent == torrents_.end()) return;
    const auto buffer = torrent->second.pieces.find(piece);
    if (buffer == torrent->second.pieces.end()) return;
    bytes_used_ -= buffer->second->size;
    torrent->second.pieces.erase(buffer);
}

std::vector<int> MemoryPayloadStore::resident_pieces(const TorrentKey& key) const {
    std::scoped_lock lock(mutex_);
    std::vector<int> pieces;
    const auto torrent = torrents_.find(key);
    if (torrent == torrents_.end()) return pieces;
    for (const auto& [index, buffer] : torrent->second.pieces) pieces.push_back(index);
    return pieces;
}

std::int64_t MemoryPayloadStore::bytes_used() const {
    std::scoped_lock lock(mutex_);
    return bytes_used_;
}

std::shared_ptr<PieceBuffer> MemoryPayloadStore::find(const TorrentKey& key, int piece) const {
    std::scoped_lock lock(mutex_);
    const auto torrent = torrents_.find(key);
    if (torrent == torrents_.end()) return nullptr;
    const auto buffer = torrent->second.pieces.find(piece);
    return buffer == torrent->second.pieces.end() ? nullptr : buffer->second;
}

}
