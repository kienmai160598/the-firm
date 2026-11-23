import Foundation
import Network
import os

/// One HTTP request read off a local socket. Only what the addon page needs: method, path, query
/// and a small body.
struct LocalHttpRequest: Equatable {
    let method: String
    let path: String
    let query: [String: String]
    let body: Data

    enum Parse: Equatable {
        case incomplete
        case invalid
        case complete(LocalHttpRequest)
    }

    static let maxSize = 16 * 1024
    private static let headerEnd = Data("\r\n\r\n".utf8)

    static func parse(_ data: Data) -> Parse {
        guard data.count <= maxSize else { return .invalid }
        guard let end = data.range(of: headerEnd) else { return .incomplete }
        guard let head = String(data: data[..<end.lowerBound], encoding: .utf8) else { return .invalid }
        let lines = head.components(separatedBy: "\r\n")
        let parts = lines[0].split(separator: " ")
        guard parts.count == 3, let target = URLComponents(string: String(parts[1])) else {
            return .invalid
        }
        let body = data[end.upperBound...]
        guard body.count >= contentLength(lines) else { return .incomplete }
        return .complete(LocalHttpRequest(
            method: String(parts[0]),
            path: target.path,
            query: pairs(target.queryItems),
            body: Data(body.prefix(contentLength(lines)))
        ))
    }

    /// Fields of an `application/x-www-form-urlencoded` body.
    var form: [String: String] {
        let text = String(decoding: body, as: UTF8.self).replacingOccurrences(of: "+", with: " ")
        return Self.pairs(URLComponents(string: "?" + text)?.queryItems)
    }

    private static func contentLength(_ lines: [String]) -> Int {
        let header = lines.dropFirst().first { $0.lowercased().hasPrefix("content-length:") }
        let value = header?.split(separator: ":", maxSplits: 1).last
        return value.flatMap { Int($0.trimmingCharacters(in: .whitespaces)) } ?? 0
    }

    private static func pairs(_ items: [URLQueryItem]?) -> [String: String] {
        Dictionary((items ?? []).map { ($0.name, $0.value ?? "") }) { first, _ in first }
    }
}

struct LocalHttpResponse {
    let status: Int
    let contentType: String
    let body: Data

    static func text(_ status: Int, _ message: String) -> LocalHttpResponse {
        LocalHttpResponse(status: status, contentType: "text/plain; charset=utf-8", body: Data(message.utf8))
    }

    var bytes: Data {
        let reason = HTTPURLResponse.localizedString(forStatusCode: status).capitalized
        let head = "HTTP/1.1 \(status) \(reason)\r\n" +
            "Content-Type: \(contentType)\r\n" +
            "Content-Length: \(body.count)\r\n" +
            "Cache-Control: no-store\r\n" +
            "Connection: close\r\n\r\n"
        return Data(head.utf8) + body
    }
}

/// A one-request-per-connection HTTP server on the local network, run on the main queue.
@MainActor
final class LocalHttpServer {
    typealias Handler = @MainActor (LocalHttpRequest, @escaping (LocalHttpResponse) -> Void) -> Void

    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "LocalHttpServer")
    private let handler: Handler
    private var listener: NWListener?

    init(handler: @escaping Handler) {
        self.handler = handler
    }

    /// Starts listening on a free port and reports it once the socket is ready, or nil on failure.
    func start(onReady: @escaping @MainActor (UInt16?) -> Void) {
        guard listener == nil else { return }
        do {
            let listener = try NWListener(using: .tcp)
            listener.stateUpdateHandler = { [weak self] state in
                MainActor.assumeIsolated { self?.handleState(state, onReady: onReady) }
            }
            listener.newConnectionHandler = { [weak self] connection in
                MainActor.assumeIsolated { self?.accept(connection) }
            }
            listener.start(queue: .main)
            self.listener = listener
        } catch {
            Self.log.error("Listener failed: \(error.localizedDescription, privacy: .public)")
            onReady(nil)
        }
    }

    func stop() {
        listener?.cancel()
        listener = nil
    }

    private func handleState(_ state: NWListener.State, onReady: @MainActor (UInt16?) -> Void) {
        switch state {
        case .ready:
            onReady(listener?.port?.rawValue)
        case let .failed(error):
            Self.log.error("Listener stopped: \(error.localizedDescription, privacy: .public)")
            stop()
            onReady(nil)
        default:
            break
        }
    }

    private func accept(_ connection: NWConnection) {
        connection.start(queue: .main)
        receive(on: connection, buffer: Data())
    }

    private func receive(on connection: NWConnection, buffer: Data) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: LocalHttpRequest.maxSize) {
            [weak self] data, _, isComplete, error in
            MainActor.assumeIsolated {
                self?.handleChunk(data, isComplete: isComplete || error != nil, buffer: buffer, connection)
            }
        }
    }

    private func handleChunk(_ data: Data?, isComplete: Bool, buffer: Data, _ connection: NWConnection) {
        let buffer = buffer + (data ?? Data())
        switch LocalHttpRequest.parse(buffer) {
        case let .complete(request):
            handler(request) { response in
                connection.send(content: response.bytes, completion: .contentProcessed { _ in
                    connection.cancel()
                })
            }
        case .incomplete where !isComplete:
            receive(on: connection, buffer: buffer)
        case .incomplete, .invalid:
            connection.send(content: LocalHttpResponse.text(400, "Bad request").bytes,
                            completion: .contentProcessed { _ in connection.cancel() })
        }
    }
}

enum LocalNetworkAddress {
    /// The TV's IPv4 address on Ethernet or Wi-Fi, Ethernet first.
    static func current() -> String? {
        var first: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&first) == 0, let first else { return nil }
        defer { freeifaddrs(first) }
        var found: [String: String] = [:]
        for pointer in sequence(first: first, next: { $0.pointee.ifa_next }) {
            let entry = pointer.pointee
            guard let address = entry.ifa_addr, address.pointee.sa_family == UInt8(AF_INET) else {
                continue
            }
            let name = String(cString: entry.ifa_name)
            var host = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            let result = getnameinfo(address, socklen_t(address.pointee.sa_len), &host,
                                     socklen_t(host.count), nil, 0, NI_NUMERICHOST)
            if result == 0 { found[name] = String(cString: host) }
        }
        return found["en0"] ?? found["en1"]
    }
}
