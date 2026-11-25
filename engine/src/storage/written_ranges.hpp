#ifndef MOVIES_ENGINE_STORAGE_WRITTEN_RANGES_HPP
#define MOVIES_ENGINE_STORAGE_WRITTEN_RANGES_HPP

#include <map>

namespace movies::storage {

// The byte ranges of one piece that have been written, merged where they touch. Blocks arrive out
// of order from several peers; a reader may take the bytes that follow its offset without a gap
// before the whole piece is in and hash checked.
class WrittenRanges {
public:
    // Records [start, end).
    void add(int start, int end);
    // The end of the gapless run of written bytes that holds offset, or offset itself when that
    // byte has not been written.
    [[nodiscard]] int written_end(int offset) const;

private:
    std::map<int, int> ranges_;  // start to end, disjoint and not touching
};

}

#endif
