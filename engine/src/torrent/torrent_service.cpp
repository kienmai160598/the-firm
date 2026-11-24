#include "torrent/torrent_service.hpp"

#include <libtorrent/add_torrent_params.hpp>
#include <libtorrent/magnet_uri.hpp>
#include <libtorrent/torrent_flags.hpp>

#include "core/engine_error.hpp"

namespace movies::torrent {
namespace {

lt::add_torrent_params stream_params(const std::string& magnet_uri, const std::string& save_path) {
    lt::error_code error;
    lt::add_torrent_params params = lt::parse_magnet_uri(magnet_uri, error);
    if (error) {
        throw core::EngineError(core::ErrorCode::invalid_argument,
                                "invalid magnet link: " + error.message());
    }
    params.save_path = save_path;
    // Nothing downloads until a stream asks for pieces, and then in order.
    params.flags |= lt::torrent_flags::default_dont_download | lt::torrent_flags::sequential_download;
    params.flags &= ~(lt::torrent_flags::auto_managed | lt::torrent_flags::paused);
    return params;
}

}

TorrentService::TorrentService(lt::session& session, std::filesystem::path save_path)
    : session_(session), save_path_(std::move(save_path)) {}

lt::torrent_handle TorrentService::acquire(const std::string& magnet_uri) {
    auto params = stream_params(magnet_uri, save_path_.string());
    std::scoped_lock lock(mutex_);
    lt::torrent_handle torrent = session_.find_torrent(params.info_hashes.get_best());
    if (!torrent.is_valid()) {
        lt::error_code error;
        torrent = session_.add_torrent(std::move(params), error);
        if (error) {
            throw core::EngineError(core::ErrorCode::internal,
                                    "cannot add torrent: " + error.message());
        }
    }
    ++users_[torrent];
    return torrent;
}

void TorrentService::release(const lt::torrent_handle& torrent) {
    std::scoped_lock lock(mutex_);
    const auto user = users_.find(torrent);
    if (user == users_.end() || --user->second > 0) return;
    users_.erase(user);
    session_.remove_torrent(torrent);
}

}
