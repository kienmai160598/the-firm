#include "storage/written_ranges.hpp"

#include <algorithm>
#include <iterator>

namespace movies::storage {

void WrittenRanges::add(int start, int end) {
    if (end <= start) return;
    // The first range that may touch [start, end) is the last one starting at or before start.
    auto first = ranges_.upper_bound(start);
    if (first != ranges_.begin() && std::prev(first)->second >= start) --first;
    auto last = first;
    while (last != ranges_.end() && last->first <= end) {
        start = std::min(start, last->first);
        end = std::max(end, last->second);
        ++last;
    }
    ranges_.erase(first, last);
    ranges_.emplace(start, end);
}

int WrittenRanges::written_end(int offset) const {
    auto range = ranges_.upper_bound(offset);
    if (range == ranges_.begin()) return offset;
    --range;
    return range->second > offset ? range->second : offset;
}

}
