#include "session/session_service.hpp"

#include <chrono>
#include <vector>

#include <libtorrent/session_params.hpp>

#include "storage/payload_disk_io.hpp"

namespace movies::session {
namespace {

constexpr auto alert_wait = std::chrono::milliseconds(250);

lt::session_params session_params(const SessionOptions& options,
                                  std::shared_ptr<storage::PayloadStore> store) {
    lt::session_params params(session_settings(options));
    params.disk_io_constructor = storage::payload_disk_io_constructor(std::move(store));
    return params;
}

}

SessionService::SessionService(const SessionOptions& options,
                               std::shared_ptr<storage::PayloadStore> store)
    : session_(session_params(options, std::move(store))) {}

SessionService::~SessionService() { stop(); }

void SessionService::start(EventSink& sink) {
    alert_thread_ = std::thread([this, &sink] { run(sink); });
}

void SessionService::stop() {
    stopping_ = true;
    if (alert_thread_.joinable()) alert_thread_.join();
}

void SessionService::run(EventSink& sink) {
    std::vector<lt::alert*> alerts;
    while (!stopping_) {
        session_.wait_for_alert(alert_wait);
        session_.pop_alerts(&alerts);
        for (const auto* alert : alerts) {
            if (const auto event = to_event(*alert)) sink.on_event(*event);
        }
    }
}

}
