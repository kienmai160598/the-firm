#ifndef MOVIES_ENGINE_HTTP_MEDIA_TYPE_HPP
#define MOVIES_ENGINE_HTTP_MEDIA_TYPE_HPP

#include <string>
#include <string_view>

namespace movies::http {

// The lowercase extension of the last path segment ("mkv" for "/a/Movie.MKV"), or empty when it
// has none or holds anything but letters and digits.
std::string extension_of(std::string_view path);

// The Content-Type for a path with that extension. AVPlayer reads the format from it when the URL
// has no extension it knows, and refuses application/octet-stream.
std::string_view media_type_for(std::string_view path);

}

#endif
