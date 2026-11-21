"""Builds a static Stremio-protocol addon that lists openly licensed films only.

The films are the Blender open movies published as free torrents by the WebTorrent
project (https://webtorrent.io/free-torrents). Their swarms are large enough for speed
and cache tests; the archive.org copies of the same films have close to no seeders.

Usage: python3 -I build_addon.py <output_dir>
Serve the output with: python3 -I -m http.server <port> --directory <output_dir>
"""

import json
import pathlib
import sys

# python3 -I leaves the script's own folder off sys.path, and torrents.py lives there.
sys.path.append(str(pathlib.Path(__file__).resolve().parent))
from torrents import load_torrent, torrent_files, tracker_sources  # noqa: E402

ID_PREFIX = "legaltest-"
CATALOG_ID = "legal-test"

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
    torrent, info_hash = load_torrent(film["slug"])
    files = torrent_files(torrent)
    paths = [path for path, _ in files]
    if film["file"] not in paths:
        raise SystemExit(f"{film['file']} is not in the torrent of {film['slug']}")

    video_index = paths.index(film["file"])
    video_bytes = files[video_index][1]
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
    if len(sys.argv) != 2:
        raise SystemExit("usage: python3 -I build_addon.py <output_dir>")

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
