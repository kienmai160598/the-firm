#ifndef MOVIES_ENGINE_TORRENT_TORRENT_SERVICE_HPP
#define MOVIES_ENGINE_TORRENT_TORRENT_SERVICE_HPP

#include <filesystem>
#include <map>
#include <mutex>
#include <string>

#include <libtorrent/session.hpp>
#include <libtorrent/torrent_handle.hpp>

namespace movies::torrent {

// Adds torrents to the session and removes them when the last stream lets go.
class TorrentService {
public:
    TorrentService(lt::session& session, std::filesystem::path save_path);

    // Throws EngineError(invalid_argument) for a magnet link libtorrent cannot parse.
    lt::torrent_handle acquire(const std::string& magnet_uri);
    void release(const lt::torrent_handle& torrent);

private:
    lt::session& session_;
    const std::filesystem::path save_path_;
    std::mutex mutex_;
    std::map<lt::torrent_handle, int> users_;
};

}

#endif
