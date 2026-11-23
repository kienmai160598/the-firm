import Foundation

struct PlaybackAdvisory: Equatable {
    private static let genreLimit = 3

    let ageRating: String?
    let genres: [String]

    init?(ageRating: String?, genres: [String]) {
        let trimmedRating = ageRating?.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanGenres = genres
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        guard trimmedRating?.isEmpty == false || !cleanGenres.isEmpty else { return nil }
        self.ageRating = trimmedRating?.isEmpty == false ? trimmedRating : nil
        self.genres = Array(cleanGenres.prefix(Self.genreLimit))
    }

    var genreLine: String? {
        genres.isEmpty ? nil : genres.joined(separator: ", ")
    }
}
