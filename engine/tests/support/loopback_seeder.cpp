#include "support/loopback_seeder.hpp"

#include <chrono>
#include <fstream>
#include <random>
#include <stdexcept>
#include <thread>

#include <libtorrent/add_torrent_params.hpp>
#include <libtorrent/bencode.hpp>
#include <libtorrent/create_torrent.hpp>
#include <libtorrent/magnet_uri.hpp>
#include <libtorrent/torrent_flags.hpp>
#include <libtorrent/torrent_info.hpp>

namespace movies::test {
namespace {

constexpr const char* file_name = "movie.mkv";

lt::settings_pack loopback_settings() {
    lt::settings_pack pack;
    pack.set_str(lt::settings_pack::listen_interfaces, "127.0.0.1:0");
    pack.set_bool(lt::settings_pack::enable_dht, false);
    pack.set_bool(lt::settings_pack::enable_lsd, false);
    pack.set_bool(lt::settings_pack::enable_upnp, false);
    pack.set_bool(lt::settings_pack::enable_natpmp, false);
    return pack;
}

std::string random_bytes(std::int64_t size) {
    std::mt19937 generator(20261124);
    std::string bytes(std::size_t(size), '\0');
    for (auto& byte : bytes) byte = char(generator());
    return bytes;
}

std::shared_ptr<lt::torrent_info> make_torrent(const std::filesystem::path& directory,
                                               int piece_length) {
    lt::file_storage files;
    lt::add_files(files, (directory / file_name).string());
    lt::create_torrent creator(files, piece_length);
    lt::set_piece_hashes(creator, directory.string());
    std::vector<char> encoded;
    lt::bencode(std::back_inserter(encoded), creator.generate());
    return std::make_shared<lt::torrent_info>(encoded, lt::from_span);
}

std::uint16_t wait_for_port(const lt::session& session) {
    for (int attempt = 0; attempt < 100; ++attempt) {
        if (const auto port = session.listen_port()) return port;
        std::this_thread::sleep_for(std::chrono::milliseconds(20));
    }
    throw std::runtime_error("seeder never listened");
}

}

LoopbackSeeder::LoopbackSeeder(const std::filesystem::path& directory, std::int64_t size,
                               int piece_length)
    : session_(loopback_settings()), payload_(random_bytes(size)) {
    std::filesystem::create_directories(directory);
    std::ofstream(directory / file_name, std::ios::binary).write(payload_.data(), size);

    lt::add_torrent_params params;
    params.ti = make_torrent(directory, piece_length);
    params.save_path = directory.string();
    params.flags |= lt::torrent_flags::seed_mode;
    session_.add_torrent(params);
    magnet_ = lt::make_magnet_uri(*params.ti) + "&x.pe=127.0.0.1:"
        + std::to_string(wait_for_port(session_));
}

}
