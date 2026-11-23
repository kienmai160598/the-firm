import XCTest
@testable import NuvioTv

final class TopShelfStoreTests: XCTestCase {
    private var savedShelf: TopShelfShelf?

    override func setUp() {
        savedShelf = TopShelfStore.load()
    }

    override func tearDownWithError() throws {
        _ = try TopShelfStore.save(savedShelf)
    }

    func testSavedShelfReadsBackAndASecondSaveReportsNoChange() throws {
        let shelf = TopShelfShelf(title: "Continue Watching", items: [
            TopShelfItem(key: "tt1", title: "Dune", imageUrl: URL(string: "https://img.test/1.jpg"), progress: 0.4),
        ])
        _ = try TopShelfStore.save(nil)

        XCTAssertTrue(try TopShelfStore.save(shelf))
        XCTAssertEqual(TopShelfStore.load(), shelf)
        XCTAssertFalse(try TopShelfStore.save(shelf))
    }

    func testSavingNothingClearsTheShelf() throws {
        let shelf = TopShelfShelf(title: "Continue Watching", items: [
            TopShelfItem(key: "tt1", title: "Dune", imageUrl: nil, progress: 0),
        ])
        _ = try TopShelfStore.save(shelf)

        XCTAssertTrue(try TopShelfStore.save(nil))
        XCTAssertNil(TopShelfStore.load())
    }

    func testResumeLinkCarriesTheItemKey() throws {
        let key = "series:tt0944947:1:3 & more"

        let url = try XCTUnwrap(TopShelfLink.resume(key: key))

        XCTAssertEqual(TopShelfLink.resumeKey(from: url), key)
        XCTAssertNil(TopShelfLink.resumeKey(from: URL(string: "movies://other?key=x")!))
    }
}
