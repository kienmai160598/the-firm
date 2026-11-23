// The hash jobs and the jobs a store without files answers at once.
#include <boost/asio/post.hpp>
#include <libtorrent/error_code.hpp>
#include <libtorrent/file_storage.hpp>

#include "storage/payload_disk_io.hpp"
#include "storage/piece_hashing.hpp"

namespace movies::storage {
namespace {

lt::storage_error missing_piece() {
    lt::storage_error result(boost::asio::error::operation_aborted);
    result.operation = lt::operation_t::file_read;
    return result;
}

}

void PayloadDiskIo::async_hash(
    lt::storage_index_t storage, lt::piece_index_t piece, lt::span<lt::sha256_hash> v2,
    lt::disk_job_flags_t flags,
    std::function<void(lt::piece_index_t, const lt::sha1_hash&, const lt::storage_error&)> handler) {
    const auto& entry = torrent(storage);
    auto buffer = store_->piece(entry.key, int(piece));
    const int piece_size2 = v2.empty() ? 0 : entry.files->piece_size2(piece);
    const bool v1 = bool(flags & lt::disk_interface::v1_hash);
    boost::asio::post(hash_pool_, [this, buffer = std::move(buffer), piece_size2, v1, v2, piece,
                                   handler = std::move(handler)]() mutable {
        lt::sha1_hash hash;
        lt::storage_error outcome;
        if (!buffer) {
            outcome = missing_piece();
        } else {
            if (v1) hash = hash_v1(buffer->view());
            hash_v2_blocks(buffer->view(), piece_size2, {v2.data(), std::size_t(v2.size())});
        }
        post_to_network([handler = std::move(handler), piece, hash, outcome] {
            handler(piece, hash, outcome);
        });
    });
}

void PayloadDiskIo::async_hash2(
    lt::storage_index_t storage, lt::piece_index_t piece, int offset, lt::disk_job_flags_t,
    std::function<void(lt::piece_index_t, const lt::sha256_hash&, const lt::storage_error&)> handler) {
    const auto& entry = torrent(storage);
    auto buffer = store_->piece(entry.key, int(piece));
    const int piece_size2 = entry.files->piece_size2(piece);
    boost::asio::post(hash_pool_, [this, buffer = std::move(buffer), piece_size2, offset, piece,
                                   handler = std::move(handler)]() mutable {
        lt::sha256_hash hash;
        lt::storage_error outcome;
        if (buffer) hash = hash_v2_block(buffer->view(), piece_size2, offset);
        else outcome = missing_piece();
        post_to_network([handler = std::move(handler), piece, hash, outcome] {
            handler(piece, hash, outcome);
        });
    });
}

void PayloadDiskIo::async_move_storage(
    lt::storage_index_t, std::string path, lt::move_flags_t,
    std::function<void(lt::status_t, const std::string&, const lt::storage_error&)> handler) {
    post_to_network([handler = std::move(handler), path = std::move(path)] {
        handler(lt::status_t::no_error, path, {});
    });
}

void PayloadDiskIo::async_release_files(lt::storage_index_t, std::function<void()> handler) {
    if (handler) post_to_network(std::move(handler));
}

void PayloadDiskIo::async_check_files(
    lt::storage_index_t, const lt::add_torrent_params*, lt::aux::vector<std::string, lt::file_index_t>,
    std::function<void(lt::status_t, const lt::storage_error&)> handler) {
    // Nothing survives a restart, so there is never anything to check.
    post_to_network([handler = std::move(handler)] { handler(lt::status_t::no_error, {}); });
}

void PayloadDiskIo::async_stop_torrent(lt::storage_index_t, std::function<void()> handler) {
    if (handler) post_to_network(std::move(handler));
}

void PayloadDiskIo::async_rename_file(
    lt::storage_index_t, lt::file_index_t file, std::string name,
    std::function<void(const std::string&, lt::file_index_t, const lt::storage_error&)> handler) {
    post_to_network([handler = std::move(handler), file, name = std::move(name)] {
        handler(name, file, {});
    });
}

void PayloadDiskIo::async_delete_files(lt::storage_index_t, lt::remove_flags_t,
                                       std::function<void(const lt::storage_error&)> handler) {
    post_to_network([handler = std::move(handler)] { handler({}); });
}

void PayloadDiskIo::async_set_file_priority(
    lt::storage_index_t, lt::aux::vector<lt::download_priority_t, lt::file_index_t> priorities,
    std::function<void(const lt::storage_error&,
                       lt::aux::vector<lt::download_priority_t, lt::file_index_t>)> handler) {
    // An error here would pause the torrent, so accept every priority.
    post_to_network([handler = std::move(handler), priorities = std::move(priorities)]() mutable {
        handler({}, std::move(priorities));
    });
}

void PayloadDiskIo::update_stats_counters(lt::counters&) const {}

std::vector<lt::open_file_state> PayloadDiskIo::get_status(lt::storage_index_t) const { return {}; }

void PayloadDiskIo::abort(bool wait) {
    if (wait) hash_pool_.join();
}

void PayloadDiskIo::submit_jobs() {}

void PayloadDiskIo::settings_updated() {}

}
