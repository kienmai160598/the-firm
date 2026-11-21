"""Builds a static Stremio-protocol addon that lists openly licensed films only.

The films are the Blender open movies published as free torrents by the WebTorrent
project (https://webtorrent.io/free-torrents). Their swarms are large enough for speed
and cache tests; the archive.org copies of the same films have close to no seeders.

Usage: python3 -I build_addon.py <output_dir>
Serve the output with: python3 -I -m http.server <port> --directory <output_dir>
"""

import hashlib
import json
import pathlib
import sys
import urllib.request

TORRENT_URL = "https://webtorrent.io/torrents/{slug}.torrent"
ID_PREFIX = "legaltest-"
CATALOG_ID = "legal-test"
PEER_TRACKER_SCHEMES = ("udp://", "http://", "https://")

CLAIMED_HEAVY_BYTES = 4 * 1024 * 1024 * 1024

FILMS = [
    {
        "slug": "sintel",
        "name": "Sintel (CC-BY, Blender)",
        "file": "Sintel.mp4",
        "runtime": "15 min",
    },
    {
        "slug": "big-buck-bunny",
        "name": "Big Buck Bunny (CC-BY, Blender)",
        "file": "Big Buck Bunny.mp4",
        "runtime": "10 min",
    },
    {
        "slug": "cosmos-laundromat",
        "name": "Cosmos Laundromat (CC-BY, Blender)",
        "file": "Cosmos Laundromat.mp4",
        "runtime": "12 min",
    },
    {
        "slug": "tears-of-steel",
        "name": "Tears of Steel (CC-BY, Blender)",
        "file": "Tears of Steel.webm",
        "runtime": "12 min",
    },
]


def bdecode(data, position=0):
    marker = data[position:position + 1]

    if marker == b"i":
        end = data.index(b"e", position)
        return int(data[position + 1:end]), end + 1

    if marker == b"l":
        position += 1
        items = []
        while data[position:position + 1] != b"e":
            item, position = bdecode(data, position)
            items.append(item)
        return items, position + 1

    if marker == b"d":
        position += 1
        entries = {}
        while data[position:position + 1] != b"e":
            key, position = bdecode(data, position)
            value_start = position
            value, position = bdecode(data, position)
            entries[key] = value
            if key == b"info":
                entries[b"__info_span"] = (value_start, position)
        return entries, position + 1

    colon = data.index(b":", position)
    length = int(data[position:colon])
    start = colon + 1
    return data[start:start + length], start + length


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


def load_torrent(film):
    url = TORRENT_URL.format(slug=film["slug"])
    with urllib.request.urlopen(url, timeout=30) as response:
        torrent_bytes = response.read()

    torrent, _ = bdecode(torrent_bytes)
    span_start, span_end = torrent[b"__info_span"]
    info_hash = hashlib.sha1(torrent_bytes[span_start:span_end]).hexdigest()
    return torrent, info_hash


def file_paths(torrent):
    return [
        "/".join(part.decode("utf-8") for part in entry[b"path"])
        for entry in torrent[b"info"][b"files"]
    ]


def claimed_heavy_streams(info_hash, other_file_indexes, sources):
    claimed_gigabytes = CLAIMED_HEAVY_BYTES // (1024 * 1024 * 1024)
    claims = [
        {
            "title": "Ranking test, not a video, structured size claim",
            "behaviorHints": {"videoSize": CLAIMED_HEAVY_BYTES},
        },
        {
            "title": f"Ranking test, not a video, text size claim {claimed_gigabytes} GB",
        },
    ]
    return [
        {
            "name": "Legal test",
            "infoHash": info_hash,
            "fileIdx": file_index,
            "sources": sources,
            **claim,
        }
        for file_index, claim in zip(other_file_indexes, claims)
    ]


def film_streams(film):
    torrent, info_hash = load_torrent(film)
    paths = file_paths(torrent)
    if film["file"] not in paths:
        raise SystemExit(f"{film['file']} is not in the torrent of {film['slug']}")

    video_index = paths.index(film["file"])
    video_bytes = torrent[b"info"][b"files"][video_index][b"length"]
    sources = tracker_sources(torrent)
    other_file_indexes = [index for index in range(len(paths)) if index != video_index]
    video_stream = {
        "name": "Legal test",
        "infoHash": info_hash,
        "fileIdx": video_index,
        "title": f"{film['file']}, {video_bytes // (1024 * 1024)} MB",
        "sources": sources,
        "behaviorHints": {"filename": film["file"], "videoSize": video_bytes},
    }

    return claimed_heavy_streams(info_hash, other_file_indexes, sources) + [video_stream]


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2))


def main():
    output = pathlib.Path(sys.argv[1])
    metas = []

    for film in FILMS:
        film_id = ID_PREFIX + film["slug"]
        streams = film_streams(film)
        meta = {
            "id": film_id,
            "type": "movie",
            "name": film["name"],
            "runtime": film["runtime"],
        }
        metas.append(meta)

        write_json(output / "meta" / "movie" / f"{film_id}.json", {"meta": meta})
        write_json(output / "stream" / "movie" / f"{film_id}.json", {"streams": streams})
        for stream in streams:
            print(film_id, stream["infoHash"], stream["fileIdx"], stream["title"])

    manifest = {
        "id": "local.legal.test",
        "version": "1.0.0",
        "name": "Legal test torrents",
        "description": "Openly licensed films for playback tests",
        "resources": ["catalog", "meta", "stream"],
        "types": ["movie"],
        "idPrefixes": [ID_PREFIX],
        "catalogs": [{"type": "movie", "id": CATALOG_ID, "name": "Legal test torrents"}],
    }
    write_json(output / "manifest.json", manifest)
    write_json(output / "catalog" / "movie" / f"{CATALOG_ID}.json", {"metas": metas})


if __name__ == "__main__":
    main()
