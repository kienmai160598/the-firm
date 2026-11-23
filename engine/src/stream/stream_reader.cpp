#include "stream/stream_reader.hpp"

#include "stream/stream_service.hpp"

namespace movies::stream {

StreamReader::StreamReader(StreamService& service, std::shared_ptr<Stream> stream,
                           std::uint64_t id)
    : service_(service), stream_(std::move(stream)), id_(id) {}

StreamReader::~StreamReader() { service_.forget_reader(*stream_, id_); }

http::ReadResult StreamReader::read(std::int64_t offset, std::span<char> out) {
    return service_.read(*stream_, id_, offset, out);
}

}
