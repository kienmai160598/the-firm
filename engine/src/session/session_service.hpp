#ifndef MOVIES_ENGINE_SESSION_SESSION_SERVICE_HPP
#define MOVIES_ENGINE_SESSION_SESSION_SERVICE_HPP

#include <atomic>
#include <memory>
#include <thread>

#include <libtorrent/session.hpp>

#include "session/events.hpp"
#include "session/session_settings.hpp"
#include "storage/payload_store.hpp"

namespace movies::session {

// Owns the libtorrent session and the thread that turns its alerts into events.
class SessionService {
public:
    SessionService(const SessionOptions& options, std::shared_ptr<storage::PayloadStore> store);
    ~SessionService();
    SessionService(const SessionService&) = delete;
    SessionService& operator=(const SessionService&) = delete;

    lt::session& session() { return session_; }

    // Starts delivering events to the sink, on the alert thread, until stop().
    void start(EventSink& sink);
    void stop();

private:
    void run(EventSink& sink);

    lt::session session_;
    std::atomic<bool> stopping_{false};
    std::thread alert_thread_;
};

}

#endif
