#include "storage/payload_store.hpp"

#include <algorithm>

namespace movies::storage {

int PieceLayout::num_pieces() const {
    if (piece_length <= 0) return 0;
    return int((total_size + piece_length - 1) / piece_length);
}

int PieceLayout::piece_size(int piece) const {
    if (piece < 0 || piece >= num_pieces()) return 0;
    const std::int64_t start = std::int64_t(piece) * piece_length;
    return int(std::min<std::int64_t>(piece_length, total_size - start));
}

}
