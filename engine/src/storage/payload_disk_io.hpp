#ifndef MOVIES_ENGINE_STORAGE_PAYLOAD_DISK_IO_HPP
#define MOVIES_ENGINE_STORAGE_PAYLOAD_DISK_IO_HPP

#include <memory>
#include <optional>
#include <vector>

#include <boost/asio/post.hpp>
#include <boost/asio/thread_pool.hpp>
#include <libtorrent/aux_/vector.hpp>
#include <libtorrent/disk_interface.hpp>
#include <libtorrent/io_context.hpp>
#include <libtorrent/session_params.hpp>

#include "storage/payload_store.hpp"

namespace movies::storage {

// libtorrent's disk interface on top of a PayloadStore. libtorrent calls it on its network thread
// and expects every handler to be posted back to that thread. Hashing runs on a small pool so a
// 16 MiB piece does not stall the network thread.
class PayloadDiskIo final : public lt::disk_interface, public lt::buffer_allocator_interface {
public:
    PayloadDiskIo(lt::io_context& network, std::shared_ptr<PayloadStore> store);
    ~PayloadDiskIo() override;

    lt::storage_holder new_torrent(const lt::storage_params& params,
                                   const std::shared_ptr<void>& torrent) override;
    void remove_torrent(lt::storage_index_t storage) override;

    void async_read(lt::storage_index_t storage, const lt::peer_request& request,
                    std::function<void(lt::disk_buffer_holder, const lt::storage_error&)> handler,
                    lt::disk_job_flags_t flags) override;
    bool async_write(lt::storage_index_t storage, const lt::peer_request& request,
                     const char* buffer, std::shared_ptr<lt::disk_observer> observer,
                     std::function<void(const lt::storage_error&)> handler,
                     lt::disk_job_flags_t flags) override;
    void async_hash(lt::storage_index_t storage, lt::piece_index_t piece,
                    lt::span<lt::sha256_hash> v2, lt::disk_job_flags_t flags,
                    std::function<void(lt::piece_index_t, const lt::sha1_hash&,
                                       const lt::storage_error&)> handler) override;
    void async_hash2(lt::storage_index_t storage, lt::piece_index_t piece, int offset,
                     lt::disk_job_flags_t flags,
                     std::function<void(lt::piece_index_t, const lt::sha256_hash&,
                                        const lt::storage_error&)> handler) override;
    void async_clear_piece(lt::storage_index_t storage, lt::piece_index_t piece,
                           std::function<void(lt::piece_index_t)> handler) override;

    // Jobs a store without files has nothing to do for: each reports success.
    void async_move_storage(lt::storage_index_t storage, std::string path, lt::move_flags_t flags,
                            std::function<void(lt::status_t, const std::string&,
                                               const lt::storage_error&)> handler) override;
    void async_release_files(lt::storage_index_t storage, std::function<void()> handler) override;
    void async_check_files(lt::storage_index_t storage, const lt::add_torrent_params* resume_data,
                           lt::aux::vector<std::string, lt::file_index_t> links,
                           std::function<void(lt::status_t, const lt::storage_error&)> handler) override;
    void async_stop_torrent(lt::storage_index_t storage, std::function<void()> handler) override;
    void async_rename_file(lt::storage_index_t storage, lt::file_index_t file, std::string name,
                           std::function<void(const std::string&, lt::file_index_t,
                                              const lt::storage_error&)> handler) override;
    void async_delete_files(lt::storage_index_t storage, lt::remove_flags_t options,
                            std::function<void(const lt::storage_error&)> handler) override;
    void async_set_file_priority(
        lt::storage_index_t storage, lt::aux::vector<lt::download_priority_t, lt::file_index_t> priorities,
        std::function<void(const lt::storage_error&,
                           lt::aux::vector<lt::download_priority_t, lt::file_index_t>)> handler) override;

    void update_stats_counters(lt::counters& counters) const override;
    std::vector<lt::open_file_state> get_status(lt::storage_index_t storage) const override;
    void abort(bool wait) override;
    void submit_jobs() override;
    void settings_updated() override;

    void free_disk_buffer(char* buffer) override;

private:
    struct Torrent {
        TorrentKey key;
        const lt::file_storage* files;
    };

    const Torrent& torrent(lt::storage_index_t storage) const;
    template <typename Handler> void post_to_network(Handler&& handler) {
        boost::asio::post(network_, std::forward<Handler>(handler));
    }

    lt::io_context& network_;
    std::shared_ptr<PayloadStore> store_;
    boost::asio::thread_pool hash_pool_;
    lt::aux::vector<std::optional<Torrent>, lt::storage_index_t> torrents_;
    std::vector<lt::storage_index_t> free_slots_;
};

lt::disk_io_constructor_type payload_disk_io_constructor(std::shared_ptr<PayloadStore> store);

}

#endif
