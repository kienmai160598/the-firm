#ifndef MOVIES_ENGINE_TESTS_LOOPBACK_SEEDER_HPP
#define MOVIES_ENGINE_TESTS_LOOPBACK_SEEDER_HPP

#include <cstdint>
#include <filesystem>
#include <string>

#include <libtorrent/session.hpp>

namespace movies::test {

// A plain libtorrent session on 127.0.0.1 seeding one generated file from disk.
class LoopbackSeeder {
public:
    LoopbackSeeder(const std::filesystem::path& directory, std::int64_t size, int piece_length);

    // A magnet link that names this seeder as a peer (x.pe), so no tracker or DHT is needed.
    [[nodiscard]] const std::string& magnet() const { return magnet_; }
    [[nodiscard]] const std::string& payload() const { return payload_; }

private:
    lt::session session_;
    std::string payload_;
    std::string magnet_;
};

}

#endif
