#include "core/engine.hpp"

#include <system_error>
#include <utility>

#include "core/engine_error.hpp"
#include "storage/memory/memory_payload_store.hpp"

namespace movies::core {
namespace {

const EngineConfig& with_state_directory(const EngineConfig& config) {
    std::error_code error;
    std::filesystem::create_directories(config.state_directory(), error);
    if (error) {
        throw EngineError(
            ErrorCode::internal,
            "cannot create state directory " + config.state_directory().string() + ": " +
                error.message()
        );
    }
    return config;
}

}

Engine::Engine(EngineConfig config)
    : config_(with_state_directory(config)),
      store_(std::make_shared<storage::MemoryPayloadStore>(config_.tuning().store_cap_bytes)),
      session_({.local_only = config_.tuning().local_only}, store_),
      torrents_(session_.session(), config_.state_directory()),
      streams_(torrents_, store_, config_.tuning().limits),
      gateway_(streams_) {
    session_.start(streams_);
}

// Readers wake and fail first, then HTTP threads end, then no more events arrive.
Engine::~Engine() {
    streams_.shutdown();
    gateway_.stop();
    session_.stop();
}

std::uint64_t Engine::open_stream(const std::string& magnet_uri, int file_index) {
    return streams_.open(magnet_uri, file_index);
}

std::optional<StreamInfo> Engine::stream_info(std::uint64_t stream) const {
    auto snapshot = streams_.snapshot(stream);
    if (!snapshot) return std::nullopt;
    const bool ready = snapshot->state == stream::StreamState::ready;
    auto url = ready ? gateway_.url_for(stream, snapshot->file_name) : std::string();
    return StreamInfo{std::move(*snapshot), std::move(url)};
}

bool Engine::close_stream(std::uint64_t stream) {
    return streams_.close(stream);
}

}
