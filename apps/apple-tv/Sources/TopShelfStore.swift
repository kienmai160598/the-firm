import Foundation
import Security

/// One Continue Watching card on the Apple TV home screen Top Shelf.
struct TopShelfItem: Codable, Equatable {
    let key: String
    let title: String
    let imageUrl: URL?
    let progress: Double
}

struct TopShelfShelf: Codable, Equatable {
    let title: String
    let items: [TopShelfItem]
}

enum TopShelfStoreError: Error {
    case missingAccessGroup
    case keychain(OSStatus)
}

/// The app writes the Continue Watching shelf here and the Top Shelf extension reads it. Compiled
/// into both targets, which share one keychain access group. A keychain group rather than an App
/// Group: the team's wildcard provisioning profile allows keychain groups, while an App Group
/// needs an explicit App ID registered with it.
enum TopShelfStore {
    /// Info.plist key holding the shared group, `$(AppIdentifierPrefix)com.kien.movies.tvos.shared`.
    private static let accessGroupKey = "TopShelfKeychainGroup"
    private static let service = "com.kien.movies.tvos.topshelf"
    private static let account = "continueWatching"

    static func load() -> TopShelfShelf? {
        guard var query = try? baseQuery() else { return nil }
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data
        else { return nil }
        return try? JSONDecoder().decode(TopShelfShelf.self, from: data)
    }

    /// Returns true when the stored shelf changed, so the caller only wakes the system when needed.
    static func save(_ shelf: TopShelfShelf?) throws -> Bool {
        guard shelf != load() else { return false }
        let query = try baseQuery()
        let deleted = SecItemDelete(query as CFDictionary)
        guard deleted == errSecSuccess || deleted == errSecItemNotFound else {
            throw TopShelfStoreError.keychain(deleted)
        }
        guard let shelf else { return true }
        var item = query
        item[kSecValueData as String] = try JSONEncoder().encode(shelf)
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        let added = SecItemAdd(item as CFDictionary, nil)
        guard added == errSecSuccess else { throw TopShelfStoreError.keychain(added) }
        return true
    }

    private static func baseQuery() throws -> [String: Any] {
        guard let group = Bundle.main.object(forInfoDictionaryKey: accessGroupKey) as? String else {
            throw TopShelfStoreError.missingAccessGroup
        }
        return [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecAttrAccessGroup as String: group,
        ]
    }
}

/// `movies://continue?key=<item key>`: opens the Continue Watching item and resumes it.
enum TopShelfLink {
    static let scheme = "movies"
    private static let resumeHost = "continue"
    private static let keyName = "key"

    static func resume(key: String) -> URL? {
        var components = URLComponents()
        components.scheme = scheme
        components.host = resumeHost
        components.queryItems = [URLQueryItem(name: keyName, value: key)]
        return components.url
    }

    static func resumeKey(from url: URL) -> String? {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        guard components?.scheme == scheme, components?.host == resumeHost else { return nil }
        return components?.queryItems?.first { $0.name == keyName }?.value
    }
}
