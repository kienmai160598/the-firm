import XCTest
@testable import NuvioTv

final class LocalHttpRequestTests: XCTestCase {
    func testFormPostIsReadOnceTheWholeBodyArrived() {
        let body = "url=https%3A%2F%2Faddon.test%2Fmanifest.json"
        let head = "POST /add?t=abc HTTP/1.1\r\nHost: tv\r\nContent-Length: \(body.utf8.count)\r\n\r\n"

        XCTAssertEqual(LocalHttpRequest.parse(Data((head + body.prefix(10)).utf8)), .incomplete)
        guard case let .complete(request) = LocalHttpRequest.parse(Data((head + body).utf8)) else {
            return XCTFail("Request not parsed")
        }

        XCTAssertEqual(request.method, "POST")
        XCTAssertEqual(request.path, "/add")
        XCTAssertEqual(request.query["t"], "abc")
        XCTAssertEqual(request.form["url"], "https://addon.test/manifest.json")
    }

    func testHeadersWithoutTheBlankLineWaitAndGarbageIsRejected() {
        XCTAssertEqual(LocalHttpRequest.parse(Data("GET / HTTP/1.1\r\nHost: tv\r\n".utf8)), .incomplete)
        XCTAssertEqual(LocalHttpRequest.parse(Data("NONSENSE\r\n\r\n".utf8)), .invalid)
        XCTAssertEqual(LocalHttpRequest.parse(Data(count: LocalHttpRequest.maxSize + 1)), .invalid)
    }
}
