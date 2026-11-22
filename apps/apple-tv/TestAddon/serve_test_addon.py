"""Serves the test addon written by make_test_addon.py, with HTTP Range support.

Usage:
    python3 -I apps/apple-tv/TestAddon/serve_test_addon.py /tmp/nuvio-test-addon 8765

Routes:
    /<path>                      the file, honouring "Range: bytes=start-end" (206 replies)
    /flaky/<cut>/<rate>/<path>   the file sent at <rate> bytes per second; the connection
                                 is closed once byte offset <cut> is reached, so playback
                                 fails part way through
    /stall/<anything>            accepts the request and never answers

Files without an extension get the Content-Type listed in media/content-types.json.
Only binds to 127.0.0.1.
"""

import http.server
import json
import mimetypes
import pathlib
import re
import sys
import time
import urllib.parse

CHUNK_BYTES = 16 * 1024
STALL_SECONDS = 3600
RANGE_PATTERN = re.compile(r"bytes=(\d*)-(\d*)$")


class ByteRange:
    def __init__(self, start, end, total):
        self.start = start
        self.end = end
        self.total = total

    @property
    def length(self):
        return self.end - self.start + 1


def parse_range(header, total):
    match = RANGE_PATTERN.match(header.strip()) if header else None
    if not match:
        return None
    first, last = match.groups()
    if first == "":
        suffix_length = min(int(last), total)
        return ByteRange(total - suffix_length, total - 1, total)
    end = min(int(last), total - 1) if last else total - 1
    return ByteRange(int(first), end, total)


class TestAddonHandler(http.server.BaseHTTPRequestHandler):
    root = pathlib.Path(".")
    content_types = {}

    def do_HEAD(self):
        self.serve(send_body=False)

    def do_GET(self):
        self.serve(send_body=True)

    def serve(self, send_body):
        path = urllib.parse.unquote(self.path.split("?")[0])
        parts = path.lstrip("/").split("/")
        if parts[0] == "stall":
            time.sleep(STALL_SECONDS)
            return
        if parts[0] == "flaky" and len(parts) > 3:
            cut, rate = int(parts[1]), int(parts[2])
            self.send_file("/".join(parts[3:]), send_body, cut=cut, rate=rate)
            return
        self.send_file("/".join(parts), send_body)

    def send_file(self, relative_path, send_body, cut=None, rate=None):
        path = (self.root / relative_path).resolve()
        if self.root not in path.parents or not path.is_file():
            self.send_error(404)
            return
        total = path.stat().st_size
        byte_range = parse_range(self.headers.get("Range"), total)
        if byte_range and byte_range.start >= total:
            self.send_response(416)
            self.send_header("Content-Range", f"bytes */{total}")
            self.end_headers()
            return
        self.send_headers(path, byte_range, total)
        if send_body:
            start = byte_range.start if byte_range else 0
            end = byte_range.end if byte_range else total - 1
            self.copy_bytes(path, start, end, cut, rate)

    def send_headers(self, path, byte_range, total):
        if byte_range:
            self.send_response(206)
            self.send_header("Content-Range",
                             f"bytes {byte_range.start}-{byte_range.end}/{total}")
            self.send_header("Content-Length", str(byte_range.length))
        else:
            self.send_response(200)
            self.send_header("Content-Length", str(total))
        self.send_header("Content-Type", self.content_type(path))
        self.send_header("Accept-Ranges", "bytes")
        self.end_headers()

    def content_type(self, path):
        if path.name in self.content_types:
            return self.content_types[path.name]
        guessed, _ = mimetypes.guess_type(path.name)
        return guessed or "application/octet-stream"

    def copy_bytes(self, path, start, end, cut, rate):
        with path.open("rb") as source:
            source.seek(start)
            offset = start
            while offset <= end:
                if cut is not None and offset >= cut:
                    self.close_connection = True
                    self.connection.close()
                    return
                limit = end + 1 if cut is None else min(end + 1, cut)
                chunk = source.read(min(CHUNK_BYTES, limit - offset))
                if not chunk:
                    return
                try:
                    self.wfile.write(chunk)
                except (BrokenPipeError, ConnectionResetError):
                    return
                offset += len(chunk)
                if rate:
                    time.sleep(len(chunk) / rate)

    def log_message(self, format, *args):
        sys.stderr.write(f"{self.address_string()} {format % args} "
                         f"range={self.headers.get('Range')}\n")


def main():
    root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/nuvio-test-addon")
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 8765
    TestAddonHandler.root = root.resolve()
    types_file = root / "media" / "content-types.json"
    if types_file.is_file():
        TestAddonHandler.content_types = json.loads(types_file.read_text())
    server = http.server.ThreadingHTTPServer(("127.0.0.1", port), TestAddonHandler)
    server.daemon_threads = True
    print(f"Serving {root} on http://127.0.0.1:{port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
