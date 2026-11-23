import XCTest
@testable import NuvioTv

final class PlaybackAdvisoryTests: XCTestCase {
    func testShowsRatingAndUpToThreeGenres() {
        let advisory = PlaybackAdvisory(
            ageRating: " PG-13 ",
            genres: ["Action", " ", "Drama", "Thriller", "Crime"]
        )

        XCTAssertEqual(advisory?.ageRating, "PG-13")
        XCTAssertEqual(advisory?.genreLine, "Action, Drama, Thriller")
    }

    func testGenresAloneWhenThereIsNoRating() {
        let advisory = PlaybackAdvisory(ageRating: "", genres: ["Comedy"])

        XCTAssertNil(advisory?.ageRating)
        XCTAssertEqual(advisory?.genreLine, "Comedy")
    }

    func testNothingToShowWithoutRatingOrGenres() {
        XCTAssertNil(PlaybackAdvisory(ageRating: nil, genres: []))
    }
}
