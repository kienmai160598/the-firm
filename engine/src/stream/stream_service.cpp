#include "stream/stream_service.hpp"

#include <libtorrent/file_storage.hpp>

#include "torrent/file_choice.hpp"

namespace movies::stream {
namespace {

std::vector<torrent::FileEntry> file_entries(const lt::file_storage& files) {
    std::vector<torrent::FileEntry> entries;
    for (const auto index : files.file_range()) {
        entries.push_back({files.file_path(index), files.file_size(index), files.pad_file_at(index)});
    }
    return entries;
}

}

StreamService::StreamService(torrent::TorrentService& torrents,
                             std::shared_ptr<storage::PayloadStore> store, StreamLimits limits)
    : torrents_(torrents), store_(std::move(store)), limits_(limits) {}

StreamService::~StreamService() { shutdown(); }

std::uint64_t StreamService::open(const std::string& magnet_uri, int file_index) {
    const lt::torrent_handle torrent = torrents_.acquire(magnet_uri);
    auto stream = std::make_shared<Stream>();
    stream->torrent = torrent;
    stream->requested_file = file_index;
    {
        std::scoped_lock lock(mutex_);
        stream->id = next_stream_++;
        streams_[stream->id] = stream;
    }
    // A torrent opened before already has metadata, and no metadata event will come for it.
    if (auto metadata = torrent.torrent_file()) resolve(stream, metadata);
    return stream->id;
}

std::optional<StreamSnapshot> StreamService::snapshot(std::uint64_t id) const {
    std::scoped_lock lock(mutex_);
    const auto stream = streams_.find(id);
    if (stream == streams_.end()) return std::nullopt;
    return stream->second->snapshot;
}

bool StreamService::close(std::uint64_t id) {
    std::shared_ptr<Stream> stream;
    {
        std::scoped_lock lock(mutex_);
        const auto found = streams_.find(id);
        if (found == streams_.end()) return false;
        stream = found->second;
        stream->closed = true;
        streams_.erase(found);
    }
    piece_arrived_.notify_all();
    torrents_.release(stream->torrent);
    return true;
}

void StreamService::shutdown() {
    {
        std::scoped_lock lock(mutex_);
        stopping_ = true;
    }
    piece_arrived_.notify_all();
}

void StreamService::on_event(const session::Event& event) {
    if (const auto* metadata = std::get_if<session::MetadataReceived>(&event)) {
        const auto info = metadata->torrent.torrent_file();
        std::vector<std::shared_ptr<Stream>> waiting;
        {
            std::scoped_lock lock(mutex_);
            for (const auto& [id, stream] : streams_) {
                if (stream->torrent == metadata->torrent) waiting.push_back(stream);
            }
        }
        for (const auto& stream : waiting) resolve(stream, info);
    } else if (const auto* piece = std::get_if<session::PieceFinished>(&event)) {
        on_piece(piece->torrent, piece->piece);
    } else if (const auto* failure = std::get_if<session::TorrentFailed>(&event)) {
        on_failure(failure->torrent, failure->message);
    }
}

void StreamService::resolve(const std::shared_ptr<Stream>& stream,
                            const std::shared_ptr<const lt::torrent_info>& metadata) {
    if (!metadata) return;
    const auto& files = metadata->files();
    const auto chosen = torrent::choose_file(file_entries(files), stream->requested_file);

    std::scoped_lock lock(mutex_);
    if (stream->snapshot.state != StreamState::resolving) return;
    if (!chosen) {
        stream->snapshot.state = StreamState::failed;
        stream->snapshot.error = "the torrent has no file " + std::to_string(stream->requested_file);
        return;
    }
    const lt::file_index_t index(*chosen);
    stream->file = {files.file_offset(index), files.file_size(index), files.piece_length(),
                    files.num_pieces()};
    stream->key = metadata->info_hashes().get_best();
    stream->have.assign(std::size_t(files.num_pieces()), false);
    stream->snapshot = {StreamState::ready, *chosen, stream->file.size, files.file_name(index).to_string(), {}};
    replan(*stream);
}

void StreamService::on_piece(const lt::torrent_handle& torrent, int piece) {
    {
        std::scoped_lock lock(mutex_);
        for (const auto& [id, stream] : streams_) {
            if (stream->torrent != torrent || stream->snapshot.state != StreamState::ready) continue;
            if (piece < 0 || piece >= int(stream->have.size())) continue;
            stream->have[std::size_t(piece)] = true;
            evict_if_needed(*stream);
        }
    }
    piece_arrived_.notify_all();
}

void StreamService::on_failure(const lt::torrent_handle& torrent, const std::string& message) {
    {
        std::scoped_lock lock(mutex_);
        for (const auto& [id, stream] : streams_) {
            if (stream->torrent != torrent) continue;
            stream->snapshot.state = StreamState::failed;
            stream->snapshot.error = message;
        }
    }
    piece_arrived_.notify_all();
}

std::optional<std::int64_t> StreamService::size_of(std::uint64_t id) const {
    std::scoped_lock lock(mutex_);
    const auto stream = streams_.find(id);
    if (stream == streams_.end() || stream->second->snapshot.state != StreamState::ready) {
        return std::nullopt;
    }
    return stream->second->file.size;
}

}
