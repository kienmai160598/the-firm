#include "storage/payload_disk_io.hpp"

#include <cstdlib>

#include <boost/asio/post.hpp>
#include <libtorrent/error_code.hpp>
#include <libtorrent/file_storage.hpp>
#include <libtorrent/peer_request.hpp>
#include <libtorrent/storage_defs.hpp>

namespace movies::storage {
namespace {

lt::storage_error error(boost::system::error_code code, lt::operation_t operation) {
    lt::storage_error result(code);
    result.operation = operation;
    return result;
}

}

PayloadDiskIo::PayloadDiskIo(lt::io_context& network, std::shared_ptr<PayloadStore> store)
    : network_(network), store_(std::move(store)), hash_pool_(2) {}

PayloadDiskIo::~PayloadDiskIo() { hash_pool_.join(); }

const PayloadDiskIo::Torrent& PayloadDiskIo::torrent(lt::storage_index_t storage) const {
    return *torrents_[storage];
}

lt::storage_holder PayloadDiskIo::new_torrent(const lt::storage_params& params,
                                              const std::shared_ptr<void>&) {
    lt::storage_index_t slot = torrents_.end_index();
    if (free_slots_.empty()) {
        torrents_.emplace_back();
    } else {
        slot = free_slots_.back();
        free_slots_.pop_back();
    }
    torrents_[slot] = Torrent{params.info_hash, &params.files};
    store_->open(params.info_hash, {params.files.total_size(), params.files.piece_length()});
    return {slot, *this};
}

void PayloadDiskIo::remove_torrent(lt::storage_index_t storage) {
    store_->close(torrent(storage).key);
    torrents_[storage].reset();
    free_slots_.push_back(storage);
}

void PayloadDiskIo::async_read(
    lt::storage_index_t storage, const lt::peer_request& request,
    std::function<void(lt::disk_buffer_holder, const lt::storage_error&)> handler,
    lt::disk_job_flags_t) {
    // A copy, so a piece evicted meanwhile cannot pull the bytes from under libtorrent.
    auto* buffer = static_cast<char*>(std::malloc(std::size_t(request.length)));
    const bool held = buffer != nullptr
        && store_->read(torrent(storage).key, int(request.piece), request.start,
                        {buffer, std::size_t(request.length)});
    if (!held) {
        std::free(buffer);
        // libtorrent ignores operation_aborted: the peer simply does not get this block.
        const auto failure = error(boost::asio::error::operation_aborted, lt::operation_t::file_read);
        post_to_network([handler = std::move(handler), failure] { handler({}, failure); });
        return;
    }
    post_to_network([this, handler = std::move(handler), buffer, length = request.length] {
        handler(lt::disk_buffer_holder(*this, buffer, length), {});
    });
}

bool PayloadDiskIo::async_write(lt::storage_index_t storage, const lt::peer_request& request,
                                const char* buffer, std::shared_ptr<lt::disk_observer>,
                                std::function<void(const lt::storage_error&)> handler,
                                lt::disk_job_flags_t) {
    const auto result = store_->write(torrent(storage).key, int(request.piece), request.start,
                                      {buffer, std::size_t(request.length)});
    lt::storage_error outcome;
    if (result == WriteResult::over_budget) {
        // libtorrent disconnects the peer that sent the block and keeps the torrent running.
        outcome = error(boost::system::errc::make_error_code(boost::system::errc::not_enough_memory),
                        lt::operation_t::file_write);
    } else if (result == WriteResult::unknown_torrent) {
        outcome = error(boost::asio::error::operation_aborted, lt::operation_t::file_write);
    }
    post_to_network([handler = std::move(handler), outcome] { handler(outcome); });
    return false;
}

void PayloadDiskIo::async_clear_piece(lt::storage_index_t storage, lt::piece_index_t piece,
                                      std::function<void(lt::piece_index_t)> handler) {
    store_->clear(torrent(storage).key, int(piece));
    post_to_network([handler = std::move(handler), piece] { handler(piece); });
}

void PayloadDiskIo::free_disk_buffer(char* buffer) { std::free(buffer); }

lt::disk_io_constructor_type payload_disk_io_constructor(std::shared_ptr<PayloadStore> store) {
    return [store](lt::io_context& network, const lt::settings_interface&, lt::counters&) {
        return std::make_unique<PayloadDiskIo>(network, store);
    };
}

}
