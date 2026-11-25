import Foundation
import Libmpv
import QuartzCore
import os

enum MpvEvent {
    case fileLoaded
    case audioReconfigured
    case endOfFile
    case failed(String)
    case number(name: String, value: Double)
    case flag(name: String, value: Bool)
}

final class MpvHandle {
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "Mpv")

    private let handle: OpaquePointer
    private let layer: CAMetalLayer
    private let queue = DispatchQueue(label: "com.kien.movies.tvos.mpv", qos: .userInitiated)
    private let onEvent: (MpvEvent) -> Void
    private var isClosed = false
    private var isDestroyed = false

    init?(
        layer: CAMetalLayer,
        options: [(name: String, value: String)],
        onEvent: @escaping (MpvEvent) -> Void
    ) {
        guard let handle = mpv_create() else { return nil }
        self.handle = handle
        self.layer = layer
        self.onEvent = onEvent
        var windowId = Int64(Int(bitPattern: Unmanaged.passUnretained(layer).toOpaque()))
        check(mpv_set_option(handle, "wid", MPV_FORMAT_INT64, &windowId), "wid")
        for (name, value) in options {
            setOption(name, value)
        }
        check(mpv_request_log_messages(handle, "warn"), "log level")
        guard mpv_initialize(handle) >= 0 else {
            mpv_terminate_destroy(handle)
            return nil
        }
        listenForWakeups()
    }

    func setOption(_ name: String, _ value: String) {
        check(mpv_set_option_string(handle, name, value), name)
    }

    func observeNumber(_ name: String) {
        observe(name, format: MPV_FORMAT_DOUBLE)
    }

    func observeFlag(_ name: String) {
        observe(name, format: MPV_FORMAT_FLAG)
    }

    private func observe(_ name: String, format: mpv_format) {
        guard !isClosed else { return }
        check(mpv_observe_property(handle, 0, name, format), name)
    }

    func command(_ arguments: String...) {
        run(arguments) { handle, cArguments in mpv_command(handle, cArguments) }
    }

    func commandInBackground(_ arguments: String...) {
        run(arguments) { handle, cArguments in mpv_command_async(handle, 0, cArguments) }
    }

    private func run(
        _ arguments: [String],
        _ send: (OpaquePointer, UnsafeMutablePointer<UnsafePointer<CChar>?>) -> Int32
    ) {
        guard !isClosed else { return }
        var cArguments: [UnsafePointer<CChar>?] = arguments.map { UnsafePointer(strdup($0)) }
        cArguments.append(nil)
        defer { cArguments.compactMap { $0 }.forEach { free(UnsafeMutablePointer(mutating: $0)) } }
        let status = cArguments.withUnsafeMutableBufferPointer { buffer in
            send(handle, buffer.baseAddress!)
        }
        check(status, arguments.first ?? "command")
    }

    func setFlag(_ name: String, _ value: Bool) {
        guard !isClosed else { return }
        var flag: Int32 = value ? 1 : 0
        check(mpv_set_property(handle, name, MPV_FORMAT_FLAG, &flag), name)
    }

    func setString(_ name: String, _ value: String) {
        guard !isClosed else { return }
        check(mpv_set_property_string(handle, name, value), name)
    }

    func number(_ name: String) -> Double? {
        guard !isClosed else { return nil }
        var value = Double()
        return mpv_get_property(handle, name, MPV_FORMAT_DOUBLE, &value) >= 0 ? value : nil
    }

    func string(_ name: String) -> String? {
        guard !isClosed, let cString = mpv_get_property_string(handle, name) else { return nil }
        defer { mpv_free(cString) }
        return String(cString: cString)
    }

    func close() {
        guard !isClosed else { return }
        isClosed = true
        mpv_set_wakeup_callback(handle, nil, nil)
        queue.async { [self] in
            isDestroyed = true
            mpv_terminate_destroy(handle)
            Self.log.notice("Destroyed mpv")
        }
    }

    private func listenForWakeups() {
        let context = Unmanaged.passUnretained(self).toOpaque()
        mpv_set_wakeup_callback(handle, { context in
            guard let context else { return }
            Unmanaged<MpvHandle>.fromOpaque(context).takeUnretainedValue().scheduleDrain()
        }, context)
    }

    private func scheduleDrain() {
        queue.async { [weak self] in self?.drainEvents() }
    }

    private func drainEvents() {
        while !isDestroyed, let event = mpv_wait_event(handle, 0)?.pointee,
              event.event_id != MPV_EVENT_NONE {
            guard let mapped = Self.map(event) else { continue }
            DispatchQueue.main.async { [weak self] in self?.deliver(mapped) }
        }
    }

    private func deliver(_ event: MpvEvent) {
        guard !isClosed else { return }
        onEvent(event)
    }

    private static func map(_ event: mpv_event) -> MpvEvent? {
        switch event.event_id {
        case MPV_EVENT_FILE_LOADED:
            return .fileLoaded
        case MPV_EVENT_AUDIO_RECONFIG:
            return .audioReconfigured
        case MPV_EVENT_END_FILE:
            return endFile(event)
        case MPV_EVENT_PROPERTY_CHANGE:
            return propertyChange(event)
        case MPV_EVENT_LOG_MESSAGE:
            logMessage(event)
            return nil
        default:
            return nil
        }
    }

    private static func endFile(_ event: mpv_event) -> MpvEvent? {
        guard let data = event.data else { return nil }
        let endFile = data.assumingMemoryBound(to: mpv_event_end_file.self).pointee
        switch endFile.reason {
        case MPV_END_FILE_REASON_EOF:
            return .endOfFile
        case MPV_END_FILE_REASON_ERROR:
            return .failed(String(cString: mpv_error_string(endFile.error)))
        default:
            return nil
        }
    }

    private static func propertyChange(_ event: mpv_event) -> MpvEvent? {
        guard let data = event.data else { return nil }
        let property = data.assumingMemoryBound(to: mpv_event_property.self).pointee
        guard let value = property.data else { return nil }
        let name = String(cString: property.name)
        switch property.format {
        case MPV_FORMAT_DOUBLE:
            return .number(name: name, value: value.assumingMemoryBound(to: Double.self).pointee)
        case MPV_FORMAT_FLAG:
            return .flag(name: name, value: value.assumingMemoryBound(to: Int32.self).pointee != 0)
        default:
            return nil
        }
    }

    private static func logMessage(_ event: mpv_event) {
        guard let data = event.data else { return }
        let message = data.assumingMemoryBound(to: mpv_event_log_message.self).pointee
        let prefix = String(cString: message.prefix)
        let text = String(cString: message.text).trimmingCharacters(in: .newlines)
        log.warning("[\(prefix, privacy: .public)] \(text, privacy: .public)")
    }

    private func check(_ status: Int32, _ name: String) {
        guard status < 0 else { return }
        let reason = String(cString: mpv_error_string(status))
        Self.log.error("mpv \(name, privacy: .public): \(reason, privacy: .public)")
    }
}
