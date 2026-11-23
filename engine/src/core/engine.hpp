#ifndef MOVIES_ENGINE_CORE_ENGINE_HPP
#define MOVIES_ENGINE_CORE_ENGINE_HPP

#include <cstdint>
#include <memory>
#include <optional>
#include <string>

#include "core/engine_config.hpp"
#include "http/http_gateway.hpp"
#include "session/session_service.hpp"
#include "storage/payload_store.hpp"
#include "stream/stream_service.hpp"
#include "torrent/torrent_service.hpp"

namespace movies::core {

struct StreamInfo {
    stream::StreamSnapshot snapshot;
    std::string url;  // empty until the stream is ready
};

// Composition root: builds the services from the config and owns their lifetime. The C ABI only
// ever talks to this class.
class Engine {
public:
    explicit Engine(EngineConfig config);
    ~Engine();

    Engine(const Engine&) = delete;
    Engine& operator=(const Engine&) = delete;

    [[nodiscard]] const EngineConfig& config() const noexcept { return config_; }

    // Throws EngineError(invalid_argument) for a magnet link that does not parse.
    std::uint64_t open_stream(const std::string& magnet_uri, int file_index);
    [[nodiscard]] std::optional<StreamInfo> stream_info(std::uint64_t stream) const;
    bool close_stream(std::uint64_t stream);
    // Bytes of payload held in memory right now.
    [[nodiscard]] std::int64_t payload_bytes() const { return store_->bytes_used(); }

private:
    EngineConfig config_;
    std::shared_ptr<storage::PayloadStore> store_;
    session::SessionService session_;
    torrent::TorrentService torrents_;
    stream::StreamService streams_;
    http::HttpGateway gateway_;
};

}

#endif
