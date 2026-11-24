#include "http/media_type.hpp"

#include <array>
#include <cctype>
#include <utility>

namespace movies::http {
namespace {

constexpr std::string_view fallback_type = "application/octet-stream";
constexpr std::size_t max_extension_length = 8;

constexpr std::array<std::pair<std::string_view, std::string_view>, 9> types{{
    {"mp4", "video/mp4"},
    {"m4v", "video/x-m4v"},
    {"mov", "video/quicktime"},
    {"mkv", "video/x-matroska"},
    {"webm", "video/webm"},
    {"avi", "video/x-msvideo"},
    {"ts", "video/mp2t"},
    {"m2ts", "video/mp2t"},
    {"mp3", "audio/mpeg"},
}};

}

std::string extension_of(std::string_view path) {
    const auto segment = path.substr(path.find_last_of('/') + 1);
    const auto dot = segment.find_last_of('.');
    if (dot == std::string_view::npos) return {};
    const auto extension = segment.substr(dot + 1);
    if (extension.empty() || extension.size() > max_extension_length) return {};
    std::string lower;
    for (const char c : extension) {
        if (!std::isalnum(static_cast<unsigned char>(c))) return {};
        lower += char(std::tolower(static_cast<unsigned char>(c)));
    }
    return lower;
}

std::string_view media_type_for(std::string_view path) {
    const auto extension = extension_of(path);
    for (const auto& [known, type] : types) {
        if (known == extension) return type;
    }
    return fallback_type;
}

}
