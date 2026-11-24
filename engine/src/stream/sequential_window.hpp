#ifndef MOVIES_ENGINE_STREAM_SEQUENTIAL_WINDOW_HPP
#define MOVIES_ENGINE_STREAM_SEQUENTIAL_WINDOW_HPP

#include <cstdint>
#include <vector>

namespace movies::stream {

// Where a file sits in the torrent's byte space.
struct FileSpan {
    std::int64_t offset = 0;
    std::int64_t size = 0;
    int piece_length = 0;
    int num_pieces = 0;

    // The piece holding byte `position` of the file.
    [[nodiscard]] int piece_at(std::int64_t position) const;
    // The torrent pieces from `first` byte of the file up to `last` byte, clipped to the file.
    [[nodiscard]] std::vector<int> pieces_between(std::int64_t first, std::int64_t last) const;
};

struct WindowBudget {
    std::int64_t ahead_bytes = 0;   // downloaded ahead of each reader
    std::int64_t behind_bytes = 0;  // kept behind each reader for small seeks back
    int urgent_pieces = 0;          // first pieces ahead of each reader, fetched with deadlines
};

inline constexpr std::uint8_t skip_priority = 0;
inline constexpr std::uint8_t window_priority = 4;
inline constexpr std::uint8_t urgent_priority = 7;

struct WindowPlan {
    std::vector<std::uint8_t> priorities;  // one per torrent piece
    std::vector<int> urgent;               // in the order they are needed
};

// E1's plain sequential window: each reader (a byte position in the file) wants the bytes ahead of
// it, the first few urgently; everything else is skipped.
WindowPlan plan_window(const FileSpan& file, const std::vector<std::int64_t>& readers,
                       const WindowBudget& budget);

// Resident pieces no reader needs: outside every reader's [behind, ahead] range.
std::vector<int> pieces_to_evict(const FileSpan& file, const std::vector<std::int64_t>& readers,
                                 const WindowBudget& budget, const std::vector<int>& resident);

}

#endif
