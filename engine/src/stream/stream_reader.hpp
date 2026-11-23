#ifndef MOVIES_ENGINE_STREAM_STREAM_READER_HPP
#define MOVIES_ENGINE_STREAM_STREAM_READER_HPP

#include <memory>

#include "http/byte_source.hpp"
#include "stream/stream.hpp"

namespace movies::stream {

class StreamService;

// One HTTP response reading a stream. Leaving the reader frees its demand window.
class StreamReader final : public http::ByteReader {
public:
    StreamReader(StreamService& service, std::shared_ptr<Stream> stream, std::uint64_t id);
    ~StreamReader() override;
    StreamReader(const StreamReader&) = delete;
    StreamReader& operator=(const StreamReader&) = delete;

    http::ReadResult read(std::int64_t offset, std::span<char> out) override;

private:
    StreamService& service_;
    const std::shared_ptr<Stream> stream_;
    const std::uint64_t id_;
};

}

#endif
