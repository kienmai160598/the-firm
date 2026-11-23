#ifndef MOVIES_ENGINE_STREAM_STREAM_SERVICE_HPP
#define MOVIES_ENGINE_STREAM_STREAM_SERVICE_HPP

#include <condition_variable>
#include <memory>
#include <mutex>
#include <optional>

#include <libtorrent/torrent_info.hpp>

#include "http/byte_source.hpp"
#include "session/events.hpp"
#include "storage/payload_store.hpp"
#include "stream/stream.hpp"
#include "torrent/torrent_service.hpp"

namespace movies::stream {

// Streams files of torrents: resolves the file once metadata arrives, steers which pieces download
// from where HTTP readers are, and evicts pieces no reader needs.
class StreamService final : public session::EventSink, public http::ByteCatalog {
public:
    StreamService(torrent::TorrentService& torrents, std::shared_ptr<storage::PayloadStore> store,
                  StreamLimits limits);
    ~StreamService() override;
    StreamService(const StreamService&) = delete;
    StreamService& operator=(const StreamService&) = delete;

    std::uint64_t open(const std::string& magnet_uri, int file_index);
    [[nodiscard]] std::optional<StreamSnapshot> snapshot(std::uint64_t id) const;
    bool close(std::uint64_t id);
    // Wakes every blocked reader; reads fail from now on.
    void shutdown();

    void on_event(const session::Event& event) override;

    [[nodiscard]] std::optional<std::int64_t> size_of(std::uint64_t id) const override;
    std::unique_ptr<http::ByteReader> open_reader(std::uint64_t id, std::int64_t offset) override;

    http::ReadResult read(Stream& stream, std::uint64_t reader, std::int64_t offset,
                          std::span<char> out);
    void forget_reader(Stream& stream, std::uint64_t reader);

private:
    void resolve(const std::shared_ptr<Stream>& stream,
                 const std::shared_ptr<const lt::torrent_info>& metadata);
    void on_piece(const lt::torrent_handle& torrent, int piece);
    void on_failure(const lt::torrent_handle& torrent, const std::string& message);
    void move_reader(Stream& stream, std::uint64_t reader, std::int64_t offset);
    void replan(Stream& stream);
    void evict_if_needed(Stream& stream);
    bool wait_for_piece(std::unique_lock<std::mutex>& lock, Stream& stream, int piece,
                        std::chrono::steady_clock::time_point deadline);

    torrent::TorrentService& torrents_;
    const std::shared_ptr<storage::PayloadStore> store_;
    const StreamLimits limits_;
    mutable std::mutex mutex_;
    std::condition_variable piece_arrived_;
    std::map<std::uint64_t, std::shared_ptr<Stream>> streams_;
    std::uint64_t next_stream_ = 1;
    std::uint64_t next_reader_ = 1;
    bool stopping_ = false;
};

}

#endif
