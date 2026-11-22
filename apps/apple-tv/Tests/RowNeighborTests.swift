import XCTest
@testable import NuvioTv

final class RowNeighborTests: XCTestCase {
    func testFocusMovesToTheNextCardElseThePreviousElseNowhere() {
        let keys = ["a", "b", "c"]

        let neighbors = ["a", "b", "c", "x"].map { RowNeighbor.key(beside: $0, in: keys) }

        XCTAssertEqual(neighbors, ["b", "c", "b", nil])
        XCTAssertNil(RowNeighbor.key(beside: "a", in: ["a"]))
    }
}
