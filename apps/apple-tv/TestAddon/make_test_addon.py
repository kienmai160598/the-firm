"""Writes a static Stremio addon that serves Creative Commons Blender films.

Usage:
    python3 -I apps/apple-tv/TestAddon/make_test_addon.py /tmp/nuvio-test-addon
    python3 -I apps/apple-tv/TestAddon/serve_test_addon.py /tmp/nuvio-test-addon 8765

Then launch the DEBUG app with -MoviesDebugAddonUrl http://127.0.0.1:8765/manifest.json.
All films are CC BY 3.0 (Blender Foundation), hosted by the Internet Archive.

The series "tvostest-series" is SYNTHETIC TEST DATA made up for this addon: it is not a
real show. Each of its three episodes points at one of the CC films so the series flow
(resume, next up, per-episode progress) can be tested end to end.

More test streams check the mpv player and playback errors: an .mkv file made with
ffmpeg (HEVC 10-bit, AC-3 and DTS audio, an ASS subtitle track) that mpv must play, a
"broken.mp4" file of random bytes the player must fail on, and two links without a file
extension that the app has to probe: a file of random bytes and a link the server never
answers.

Two synthetic test movies hold media generated locally with ffmpeg from its built-in
test pattern (no third-party content):
    tvostest-codecs    extensionless MP4 files in codecs Apple TV may not decode, plus an
                       H.264 control, so the app's isPlayable probe runs
    tvostest-dropout   a 3 minute H.264 file sent slowly through /flaky/, where the
                       server closes the connection part way, so playback fails mid-way

serve_test_addon.py supports HTTP Range requests and the /flaky/ and /stall/ routes these
streams need; a plain static file server does not.
"""

import json
import pathlib
import shutil
import subprocess
import sys

ARCHIVE = "https://archive.org/download"
ARCHIVE_IMAGE = "https://archive.org/services/img"
LICENSE = "CC BY 3.0, Blender Foundation, via archive.org"
LOCAL_HOST = "http://127.0.0.1"
LOCAL_MEDIA = f"{LOCAL_HOST}:8765/media"
STALLED_LINK = f"{LOCAL_HOST}:8765/stall/stalled-stream"
FLAKY_ROUTE = f"{LOCAL_HOST}:8765/flaky"
DROPOUT_FILE = "dropout-test-pattern.mp4"
DROPOUT_SECONDS = 180
DROPOUT_CUT_FRACTION = 0.3
DROPOUT_RATE_FACTOR = 1.5
TEST_PATTERN = "testsrc2=size=640x360:rate=30"
TEST_TONE = "sine=frequency=440:sample_rate=48000"
SECOND_TONE = "sine=frequency=660:sample_rate=48000"
MKV_SAMPLE = "mkv-hevc10-ac3-dts-ass.mkv"
MKV_SECONDS = 120
ASS_SUBTITLES = """[Script Info]
ScriptType: v4.00+
PlayResX: 1280
PlayResY: 720

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, OutlineColour, Bold, Outline, Alignment
Style: Default,Helvetica,48,&H0000FFFF,&H00000000,1,3,2

[Events]
Format: Layer, Start, End, Style, Text
Dialogue: 0,0:00:00.00,0:02:00.00,Default,Phụ đề ASS thử nghiệm (test subtitle)
"""

CODEC_SAMPLES = [
    ("codec-h264-control", "H.264 control", ["-c:v", "libx264", "-pix_fmt", "yuv420p"]),
    ("codec-mpeg2", "MPEG-2 video", ["-c:v", "mpeg2video"]),
    ("codec-mpeg1", "MPEG-1 video", ["-c:v", "mpeg1video"]),
]

TEST_MOVIES = {
    "tvostest-codecs": ("Nuvio tvOS Codec Tests", "Tears-of-Steel"),
    "tvostest-dropout": ("Nuvio tvOS Dropout Test", "BigBuckBunny_124"),
}
TEST_MOVIE_NOTE = "Synthetic test data: ffmpeg test pattern media, not a film."

BIG_BUCK_BUNNY = f"{ARCHIVE}/BigBuckBunny_124/Content/big_buck_bunny_720p_surround.mp4"
SINTEL = f"{ARCHIVE}/Sintel/sintel-2048-stereo.mp4"
TEARS_OF_STEEL = f"{ARCHIVE}/Tears-of-Steel/tears_of_steel_720p.mp4"

FILMS = {
    "tt1254207": [
        ("720p MP4", "Big Buck Bunny 720p", BIG_BUCK_BUNNY),
    ],
    "tt1727587": [
        ("1080p MP4", "Sintel 2048 stereo", SINTEL),
        ("Torrent", "Sintel via WebTorrent (needs a streaming server)", None),
    ],
    "tt2285752": [
        ("720p MP4", "Tears of Steel 720p", TEARS_OF_STEEL),
        ("MKV HEVC 10-bit", "Test MKV: HEVC 10-bit, AC-3 and DTS audio, ASS subtitles",
         f"{LOCAL_MEDIA}/{MKV_SAMPLE}"),
        ("Broken MP4", "Test file of random bytes, so the player fails",
         f"{LOCAL_MEDIA}/broken.mp4"),
        ("Garbage no extension", "Test file of random bytes without an extension",
         f"{LOCAL_MEDIA}/garbage-no-extension"),
        ("Stalled link", "Test link the server never answers", STALLED_LINK),
    ],
}

SINTEL_INFO_HASH = "08ada5a7a6183aae1e09d831df6748d566095a10"  # webtorrent.io/free-torrents

SERIES_ID = "tvostest-series"
SERIES_NAME = "Movies tvOS Test Series"
SERIES_NOTE = "Synthetic test data: not a real show. Episodes reuse CC Blender films."

EPISODES = [
    (1, "Bunny Day", "BigBuckBunny_124", BIG_BUCK_BUNNY, "2008-05-30T00:00:00.000Z"),
    (2, "Dragon Search", "Sintel", SINTEL, "2010-09-27T00:00:00.000Z"),
    (3, "Steel Rain", "Tears-of-Steel", TEARS_OF_STEEL, "2012-09-26T00:00:00.000Z"),
]

MANIFEST = {
    "id": "dev.nuvio.tvos.testaddon",
    "version": "1.1.0",
    "name": "Nuvio tvOS test films",
    "description": "Local test addon with Creative Commons Blender films and a synthetic "
                   "test series.",
    "resources": [
        {"name": "stream", "types": ["movie", "series"], "idPrefixes": ["tt", "tvostest"]},
        {"name": "meta", "types": ["movie", "series"], "idPrefixes": ["tvostest"]},
        {"name": "subtitles", "types": ["movie"], "idPrefixes": ["tt"]},
    ],
    "types": ["movie", "series"],
    "idPrefixes": ["tt", "tvostest"],
    "catalogs": [],
}


def stream_entry(name, title, url):
    source = LICENSE if url else "CC BY 3.0, Blender Foundation, via webtorrent.io"
    if url and url.startswith(LOCAL_HOST):
        source = "Local test file, not a film"
    entry = {"name": f"Test {name}", "description": f"{title}\n{source}"}
    if url is None:
        entry["infoHash"] = SINTEL_INFO_HASH
    else:
        entry["url"] = url
    return entry


def episode_video(number, title, archive_item, released):
    return {
        "id": f"{SERIES_ID}:1:{number}",
        "title": title,
        "season": 1,
        "episode": number,
        "released": released,
        "thumbnail": f"{ARCHIVE_IMAGE}/{archive_item}",
        "overview": f"{SERIES_NOTE} Episode {number} plays {archive_item}.",
    }


def series_meta():
    return {
        "meta": {
            "id": SERIES_ID,
            "type": "series",
            "name": SERIES_NAME,
            "description": SERIES_NOTE,
            "poster": f"{ARCHIVE_IMAGE}/Sintel",
            "background": f"{ARCHIVE_IMAGE}/Tears-of-Steel",
            "releaseInfo": "2008-2012",
            "genres": ["Test data"],
            "videos": [
                episode_video(number, title, item, released)
                for number, title, item, _, released in EPISODES
            ],
        }
    }


def movie_meta(movie_id, name, archive_item):
    return {
        "meta": {
            "id": movie_id,
            "type": "movie",
            "name": name,
            "description": TEST_MOVIE_NOTE,
            "poster": f"{ARCHIVE_IMAGE}/{archive_item}",
            "background": f"{ARCHIVE_IMAGE}/{archive_item}",
            "releaseInfo": "2026",
            "genres": ["Test data"],
        }
    }


def run_ffmpeg(output, seconds, codec_args):
    command = [
        "ffmpeg", "-nostdin", "-loglevel", "error", "-y",
        "-f", "lavfi", "-i", f"{TEST_PATTERN}:duration={seconds}",
        "-f", "lavfi", "-i", f"{TEST_TONE}:duration={seconds}",
        *codec_args, "-c:a", "aac", "-b:a", "96k", "-f", "mp4", str(output),
    ]
    return subprocess.run(command, capture_output=True, text=True)


def write_codec_samples(media_dir):
    streams, content_types = [], {}
    for name, label, codec_args in CODEC_SAMPLES:
        result = run_ffmpeg(media_dir / name, 10, codec_args)
        if result.returncode != 0:
            (media_dir / name).unlink(missing_ok=True)
            print(f"Skipped {name}: {result.stderr.strip()[:200]}")
            continue
        content_types[name] = "video/mp4"
        streams.append(stream_entry(f"Codec {label}", f"Extensionless MP4 with {label}",
                                    f"{LOCAL_MEDIA}/{name}"))
    return streams, content_types


def write_dropout_sample(media_dir):
    output = media_dir / DROPOUT_FILE
    codec_args = ["-c:v", "libx264", "-pix_fmt", "yuv420p", "-b:v", "900k",
                  "-maxrate", "900k", "-bufsize", "1800k", "-movflags", "+faststart"]
    result = run_ffmpeg(output, DROPOUT_SECONDS, codec_args)
    if result.returncode != 0:
        raise SystemExit(f"ffmpeg failed for {DROPOUT_FILE}: {result.stderr.strip()}")
    size = output.stat().st_size
    cut = int(size * DROPOUT_CUT_FRACTION)
    rate = int(size / DROPOUT_SECONDS * DROPOUT_RATE_FACTOR)
    url = f"{FLAKY_ROUTE}/{cut}/{rate}/media/{DROPOUT_FILE}"
    return [stream_entry("Dropout MP4", "Server closes the connection part way", url)]


def write_test_movies(out_dir):
    if shutil.which("ffmpeg") is None:
        raise SystemExit("ffmpeg is required to generate the test movies")
    media_dir = out_dir / "media"
    codec_streams, content_types = write_codec_samples(media_dir)
    content_types["garbage-no-extension"] = "video/mp4"
    write_json(media_dir / "content-types.json", content_types)
    streams_by_movie = {
        "tvostest-codecs": codec_streams,
        "tvostest-dropout": write_dropout_sample(media_dir),
    }
    for movie_id, (name, archive_item) in TEST_MOVIES.items():
        write_json(out_dir / "meta" / "movie" / f"{movie_id}.json",
                   movie_meta(movie_id, name, archive_item))
        write_json(out_dir / "stream" / "movie" / f"{movie_id}.json",
                   {"streams": streams_by_movie[movie_id]})


def write_mkv_sample(media_dir):
    subtitles = media_dir / "mkv-sample.ass"
    subtitles.write_text(ASS_SUBTITLES)
    command = [
        "ffmpeg", "-nostdin", "-loglevel", "error", "-y",
        "-f", "lavfi", "-i", f"{TEST_PATTERN}:duration={MKV_SECONDS}",
        "-f", "lavfi", "-i", f"{TEST_TONE}:duration={MKV_SECONDS}",
        "-f", "lavfi", "-i", f"{SECOND_TONE}:duration={MKV_SECONDS}",
        "-i", str(subtitles),
        "-map", "0:v", "-map", "1:a", "-map", "2:a", "-map", "3:s",
        "-c:v", "libx265", "-pix_fmt", "yuv420p10le", "-x265-params", "log-level=error",
        "-c:a:0", "ac3", "-c:a:1", "dca", "-strict", "-2", "-ar:a:1", "48000",
        "-c:s", "ass",
        "-metadata:s:a:0", "language=eng", "-metadata:s:a:0", "title=English AC-3",
        "-metadata:s:a:1", "language=vie", "-metadata:s:a:1", "title=Vietnamese DTS",
        "-metadata:s:s:0", "language=vie", "-metadata:s:s:0", "title=Vietnamese ASS",
        str(media_dir / MKV_SAMPLE),
    ]
    result = subprocess.run(command, capture_output=True, text=True)
    subtitles.unlink()
    if result.returncode != 0:
        raise SystemExit(f"ffmpeg failed for {MKV_SAMPLE}: {result.stderr.strip()}")


ONLINE_SUBTITLE_MOVIE = "tt2285752"
ONLINE_SUBTITLES = {
    "vie": "Phụ đề tải về (online subtitle)",
    "eng": "Downloaded subtitle (online)",
}


def write_online_subtitles(out_dir):
    entries = []
    for language, line in ONLINE_SUBTITLES.items():
        name = f"online-{language}.srt"
        (out_dir / "media" / name).write_text(f"1\n00:00:00,000 --> 00:02:00,000\n{line}\n")
        entries.append({"id": f"online-{language}", "url": f"{LOCAL_MEDIA}/{name}",
                        "lang": language})
    write_json(out_dir / "subtitles" / "movie" / f"{ONLINE_SUBTITLE_MOVIE}.json",
               {"subtitles": entries})


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n")


def write_broken_media(out_dir):
    media_dir = out_dir / "media"
    media_dir.mkdir(parents=True, exist_ok=True)
    (media_dir / "broken.mp4").write_bytes(bytes(range(256)) * 64)
    (media_dir / "garbage-no-extension").write_bytes(bytes(range(255, -1, -1)) * 64)


def main():
    out_dir = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/nuvio-test-addon")
    write_json(out_dir / "manifest.json", MANIFEST)
    for imdb_id, streams in FILMS.items():
        payload = {"streams": [stream_entry(*stream) for stream in streams]}
        write_json(out_dir / "stream" / "movie" / f"{imdb_id}.json", payload)
    write_json(out_dir / "meta" / "series" / f"{SERIES_ID}.json", series_meta())
    for number, title, _, url, _ in EPISODES:
        video_id = f"{SERIES_ID}:1:{number}"
        payload = {"streams": [stream_entry("Episode MP4", f"S1 E{number} {title} (test)", url)]}
        write_json(out_dir / "stream" / "series" / f"{video_id}.json", payload)
    write_broken_media(out_dir)
    write_test_movies(out_dir)
    write_mkv_sample(out_dir / "media")
    write_online_subtitles(out_dir)
    print(f"Wrote test addon to {out_dir}")


if __name__ == "__main__":
    main()
