"""Downloads WebTorrent's free torrents and reads their info hash, files and trackers."""

import hashlib
import urllib.request

from bencode import bdecode, info_span

TORRENT_URL = "https://webtorrent.io/torrents/{slug}.torrent"
PEER_TRACKER_SCHEMES = ("udp://", "http://", "https://")


def tracker_sources(torrent):
    trackers = [
        tracker.decode("utf-8")
        for tier in torrent.get(b"announce-list", [])
        for tracker in tier
    ]
    return [
        "tracker:" + tracker
        for tracker in trackers
        if tracker.startswith(PEER_TRACKER_SCHEMES)
    ]


def load_torrent(slug):
    url = TORRENT_URL.format(slug=slug)
    with urllib.request.urlopen(url, timeout=30) as response:
        torrent_bytes = response.read()

    torrent, _ = bdecode(torrent_bytes)
    span_start, span_end = info_span(torrent_bytes)
    info_hash = hashlib.sha1(torrent_bytes[span_start:span_end]).hexdigest()
    return torrent, info_hash


def torrent_files(torrent):
    """Returns (path, length) per file; a single-file torrent is its own only file."""
    info = torrent[b"info"]
    if b"files" not in info:
        return [(info[b"name"].decode("utf-8"), info[b"length"])]

    return [
        ("/".join(part.decode("utf-8") for part in entry[b"path"]), entry[b"length"])
        for entry in info[b"files"]
    ]
