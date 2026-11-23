#include "torrent/file_choice.hpp"

#include <algorithm>
#include <array>
#include <cctype>
#include <string_view>

namespace movies::torrent {
namespace {

constexpr std::array<std::string_view, 13> video_extensions{
    ".mkv", ".mp4", ".m4v", ".avi", ".mov", ".webm", ".ts", ".m2ts", ".wmv", ".flv", ".mpg",
    ".mpeg", ".vob",
};

bool looks_like_video(const std::string& path) {
    const auto dot = path.find_last_of('.');
    if (dot == std::string::npos) return false;
    std::string extension = path.substr(dot);
    std::transform(extension.begin(), extension.end(), extension.begin(),
                   [](unsigned char letter) { return char(std::tolower(letter)); });
    return std::find(video_extensions.begin(), video_extensions.end(), extension)
        != video_extensions.end();
}

std::optional<int> largest(const std::vector<FileEntry>& files, bool video_only) {
    std::optional<int> best;
    for (int index = 0; index < int(files.size()); ++index) {
        const auto& file = files[std::size_t(index)];
        if (file.pad || (video_only && !looks_like_video(file.path))) continue;
        if (!best || file.size > files[std::size_t(*best)].size) best = index;
    }
    return best;
}

}

std::optional<int> choose_file(const std::vector<FileEntry>& files, int requested) {
    if (requested == any_file) {
        const auto video = largest(files, true);
        return video ? video : largest(files, false);
    }
    if (requested < 0 || requested >= int(files.size())) return std::nullopt;
    if (files[std::size_t(requested)].pad) return std::nullopt;
    return requested;
}

}
