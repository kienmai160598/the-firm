// The reader side of StreamService: HTTP readers, the window they steer, and eviction.
#include <algorithm>

#include "stream/stream_reader.hpp"
#include "stream/stream_service.hpp"

namespace movies::stream {
namespace {

// libtorrent tries to meet each deadline; the first urgent piece is due now.
constexpr int deadline_step_ms = 100;
// How often a waiting reader looks for new blocks. A reader takes the bytes after its offset as
// soon as their blocks are written, without waiting for the whole piece and its hash check: with
// 16 MiB pieces and a slow swarm a whole piece can take longer than the player waits for its
// first byte. A block from a piece that then fails its check has already gone to the player.
constexpr auto written_check_interval = std::chrono::milliseconds(50);

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
    const auto& window = limits_.window;
    if (offset >= stream.file.size - window.tail_bytes || offset >= window.ahead_bytes) {
        stream.tail_done = true;
    }
    move_reader(stream, reader, offset);
    const int piece = stream.file.piece_at(offset);
    const std::int64_t position = stream.file.offset + offset;
    const std::int64_t piece_start = std::int64_t(piece) * stream.file.piece_length;
    const std::int64_t length = std::min({std::int64_t(out.size()),
                                          piece_start + stream.file.piece_length - position,
                                          stream.file.size - offset});
    const auto target = out.first(std::size_t(length));
    const auto key = stream.key;
    const auto deadline = std::chrono::steady_clock::now() + limits_.read_timeout;
    while (!is_gone(stream)) {
        lock.unlock();
        const int copied = store_->read_written(key, piece, int(position - piece_start), target);
        lock.lock();
        if (copied > 0) return {http::ReadStatus::ok, std::size_t(copied)};
        const auto now = std::chrono::steady_clock::now();
        if (now >= deadline) return {http::ReadStatus::timed_out, 0};
        piece_arrived_.wait_until(lock, std::min(deadline, now + written_check_interval));
    }
    return {http::ReadStatus::closed, 0};
}

bool StreamService::is_gone(const Stream& stream) const {
    return stopping_ || stream.closed || stream.snapshot.state == StreamState::failed;
}

void StreamService::move_reader(Stream& stream, std::uint64_t reader, std::int64_t offset) {
    auto& position = stream.readers[reader];
    const bool same_piece = stream.file.piece_at(position) == stream.file.piece_at(offset);
    position = offset;
    if (same_piece) return;
    replan(stream);
    evict_if_needed(stream);
}

std::vector<std::int64_t> StreamService::reader_positions(const Stream& stream) const {
    std::vector<std::int64_t> positions;
    for (const auto& [reader, position] : stream.readers) positions.push_back(position);
    return positions;
}

void StreamService::replan(Stream& stream) {
    // Between two connections keep the last plan: the player reconnects at once after a seek.
    if (stream.readers.empty() && stream.ever_read) return;
    const auto positions =
        window_positions(stream.file, reader_positions(stream), limits_.window, stream.tail_done);
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
    // The tail is not kept from eviction: a box store is small, and a player that needs it reads it
    // within seconds of connecting.
    const auto evict = pieces_to_evict(stream.file, reader_positions(stream), limits_.window,
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
