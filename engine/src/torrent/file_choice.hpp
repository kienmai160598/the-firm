#ifndef MOVIES_ENGINE_TORRENT_FILE_CHOICE_HPP
#define MOVIES_ENGINE_TORRENT_FILE_CHOICE_HPP

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace movies::torrent {

struct FileEntry {
    std::string path;
    std::int64_t size = 0;
    bool pad = false;
};

inline constexpr int any_file = -1;

// The file a stream plays: the requested one, or with any_file the largest video file (the
// largest file when none looks like video). Empty when the request names no real file.
std::optional<int> choose_file(const std::vector<FileEntry>& files, int requested);

}

#endif
