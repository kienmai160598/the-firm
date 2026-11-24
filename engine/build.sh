#!/usr/bin/env bash
# Builds NuvioEngine.xcframework (iOS, macOS and tvOS slices) into the-firm/nuvio-engine.
# The engine is NuvioMedia/nuvio-engine (GPL-3) at a pinned commit plus tvos-slices.patch and
# stream-readahead.patch.
set -euo pipefail

engine_repo="https://github.com/NuvioMedia/nuvio-engine.git"
engine_commit="167f8541d2e07212a9cf7034b9ca4a0c3b6be4d3"
here="$(cd "$(dirname "$0")" && pwd)"
checkout="$here/../nuvio-engine"
tools="$here/.tools"

if [[ ! -d "$checkout/.git" ]]; then
    git clone "$engine_repo" "$checkout"
fi
git -C "$checkout" fetch --quiet origin "$engine_commit"
git -C "$checkout" checkout --quiet "$engine_commit"
git -C "$checkout" checkout --quiet -- scripts src
git -C "$checkout" apply "$here/tvos-slices.patch"
# Fetches up to 1 GiB ahead instead of 256 MiB, about 90 s of a 4K remux instead of 20 s.
git -C "$checkout" apply "$here/stream-readahead.patch"

if [[ ! -x "$tools/bin/cmake" || ! -x "$tools/bin/ninja" ]]; then
    python3 -m venv "$tools"
    "$tools/bin/pip" install --quiet cmake ninja
fi

PATH="$tools/bin:$PATH" "$checkout/scripts/build-apple-xcframework.sh"
