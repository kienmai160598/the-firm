#include "storage/piece_hashing.hpp"

#include <algorithm>

#include <libtorrent/disk_interface.hpp>
#include <libtorrent/hasher.hpp>

namespace movies::storage {

lt::sha1_hash hash_v1(std::span<const char> piece) {
    return lt::hasher(piece.data(), int(piece.size())).final();
}

lt::sha256_hash hash_v2_block(std::span<const char> piece, int piece_size2, int offset) {
    const int length = std::max(0, std::min(lt::default_block_size, piece_size2 - offset));
    return lt::hasher256(piece.data() + offset, length).final();
}

void hash_v2_blocks(std::span<const char> piece, int piece_size2, std::span<lt::sha256_hash> out) {
    for (std::size_t block = 0; block < out.size(); ++block) {
        out[block] = hash_v2_block(piece, piece_size2, int(block) * lt::default_block_size);
    }
}

}
