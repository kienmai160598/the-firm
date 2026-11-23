// The reader side of StreamService: HTTP readers, the window they steer, and eviction.
#include <algorithm>

#include "stream/stream_reader.hpp"
#include "stream/stream_service.hpp"

namespace movies::stream {
namespace {

// libtorrent tries to meet each deadline; the first urgent piece is due now.
constexpr int deadline_step_ms = 100;
// How often a waiting reader asks libtorrent directly, in case a piece event was missed.
constexpr auto have_check_interval = std::chrono::seconds(1);

}

std::unique_ptr<http::ByteReader> StreamService::open_reader(std::uint64_t id, std::int64_t offset) {
    std::scoped_lock lock(mutex_);
    const auto found = streams_.find(id);
    if (found == streams_.end() || found->second->snapshot.state != StreamState::ready) return nullptr;
    auto& stream = *found->second;
    const std::uint64_t reader = next_reader_++;
    stream.readers[reader] = offset;
    stream.ever_read = true;
    replan(stream);
    return std::make_unique<StreamReader>(*this, found->second, reader);
}

void StreamService::forget_reader(Stream& stream, std::uint64_t reader) {
    std::scoped_lock lock(mutex_);
    stream.readers.erase(reader);
}

http::ReadResult StreamService::read(Stream& stream, std::uint64_t reader, std::int64_t offset,
                                     std::span<char> out) {
    std::unique_lock lock(mutex_);
    if (offset < 0 || offset >= stream.file.size || out.empty()) return {http::ReadStatus::closed, 0};
    move_reader(stream, reader, offset);
    const int piece = stream.file.piece_at(offset);
    const auto deadline = std::chrono::steady_clock::now() + limits_.read_timeout;
    while (wait_for_piece(lock, stream, piece, deadline)) {
        const std::int64_t position = stream.file.offset + offset;
        const std::int64_t piece_start = std::int64_t(piece) * stream.file.piece_length;
        const std::int64_t length = std::min({std::int64_t(out.size()),
                                              piece_start + stream.file.piece_length - position,
                                              stream.file.size - offset});
        const auto key = stream.key;
        lock.unlock();
        const auto target = out.first(std::size_t(length));
        if (store_->read(key, piece, int(position - piece_start), target)) {
            return {http::ReadStatus::ok, target.size()};
        }
        lock.lock();
        stream.have[std::size_t(piece)] = false;  // evicted since the piece arrived
    }
    const bool gone = stopping_ || stream.closed || stream.snapshot.state == StreamState::failed;
    return {gone ? http::ReadStatus::closed : http::ReadStatus::timed_out, 0};
}

bool StreamService::wait_for_piece(std::unique_lock<std::mutex>& lock, Stream& stream, int piece,
                                   std::chrono::steady_clock::time_point deadline) {
    const auto slot = std::size_t(piece);
    while (true) {
        if (stopping_ || stream.closed || stream.snapshot.state == StreamState::failed) return false;
        if (stream.have[slot]) return true;
        const auto now = std::chrono::steady_clock::now();
        if (now >= deadline) return false;
        piece_arrived_.wait_until(lock, std::min(deadline, now + have_check_interval));
        if (stream.have[slot]) continue;
        const auto torrent = stream.torrent;
        lock.unlock();
        const bool held = torrent.have_piece(lt::piece_index_t(piece));
        lock.lock();
        if (held) stream.have[slot] = true;
    }
}

void StreamService::move_reader(Stream& stream, std::uint64_t reader, std::int64_t offset) {
    auto& position = stream.readers[reader];
    const bool same_piece = stream.file.piece_at(position) == stream.file.piece_at(offset);
    position = offset;
    if (same_piece) return;
    replan(stream);
    evict_if_needed(stream);
}

void StreamService::replan(Stream& stream) {
    std::vector<std::int64_t> positions;
    for (const auto& [reader, position] : stream.readers) positions.push_back(position);
    // Before the player connects, fetch the start of the file. Between two connections keep the
    // last plan: the player reconnects at once after a seek.
    if (positions.empty()) {
        if (stream.ever_read) return;
        positions.push_back(0);
    }
    const auto plan = plan_window(stream.file, positions, limits_.window);
    std::vector<lt::download_priority_t> priorities(plan.priorities.begin(), plan.priorities.end());
    stream.torrent.prioritize_pieces(priorities);
    stream.torrent.clear_piece_deadlines();
    int due_ms = 0;
    for (const int piece : plan.urgent) {
        if (stream.have[std::size_t(piece)]) continue;
        stream.torrent.set_piece_deadline(lt::piece_index_t(piece), due_ms);
        due_ms += deadline_step_ms;
    }
}

void StreamService::evict_if_needed(Stream& stream) {
    if (stream.readers.empty() || store_->bytes_used() <= limits_.eviction_threshold_bytes) return;
    std::vector<std::int64_t> positions;
    for (const auto& [reader, position] : stream.readers) positions.push_back(position);
    const auto evict = pieces_to_evict(stream.file, positions, limits_.window,
                                       store_->resident_pieces(stream.key));
    if (evict.empty()) return;
    std::vector<lt::piece_index_t> pieces;
    for (const int piece : evict) {
        stream.have[std::size_t(piece)] = false;
        pieces.emplace_back(piece);
    }
    stream.torrent.drop_pieces(pieces);
}

}
