#ifndef MOVIES_ENGINE_HTTP_BYTE_SOURCE_HPP
#define MOVIES_ENGINE_HTTP_BYTE_SOURCE_HPP

#include <cstddef>
#include <cstdint>
#include <memory>
#include <optional>
#include <span>

namespace movies::http {

enum class ReadStatus { ok, timed_out, closed };

struct ReadResult {
    ReadStatus status = ReadStatus::closed;
    std::size_t bytes = 0;
};

// One HTTP response body being read. Its position tells the engine what to download next.
class ByteReader {
public:
    virtual ~ByteReader() = default;
    // Blocks until at least one byte at `offset` is available, then copies up to out.size().
    virtual ReadResult read(std::int64_t offset, std::span<char> out) = 0;
};

// What the gateway serves: a stream id maps to a file of known size.
class ByteCatalog {
public:
    virtual ~ByteCatalog() = default;
    [[nodiscard]] virtual std::optional<std::int64_t> size_of(std::uint64_t stream) const = 0;
    virtual std::unique_ptr<ByteReader> open_reader(std::uint64_t stream, std::int64_t offset) = 0;
};

}

#endif
