#ifndef MOVIES_ENGINE_STORAGE_PIECE_HASHING_HPP
#define MOVIES_ENGINE_STORAGE_PIECE_HASHING_HPP

#include <span>

#include <libtorrent/sha1_hash.hpp>

namespace movies::storage {

// SHA-1 of a whole piece (BitTorrent v1).
lt::sha1_hash hash_v1(std::span<const char> piece);

// SHA-256 of each 16 KiB block in the first piece_size2 bytes of a piece (BitTorrent v2).
void hash_v2_blocks(std::span<const char> piece, int piece_size2, std::span<lt::sha256_hash> out);

// SHA-256 of the one v2 block that starts at offset.
lt::sha256_hash hash_v2_block(std::span<const char> piece, int piece_size2, int offset);

}

#endif
