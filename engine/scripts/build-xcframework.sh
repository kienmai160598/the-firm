#!/usr/bin/env bash
# Builds MoviesEngine.xcframework (tvOS device and simulator slices, arm64) into engine/build.
set -euo pipefail
source "$(dirname "$0")/tools.sh"

minimum_tvos="17.0"
output="$engine_root/build/MoviesEngine.xcframework"

slice_library() {
    echo "$engine_root/build/$1/xcframework-input/libmovies_engine.a"
}

build_slice() {
    local sdk="$1"
    local build="$engine_root/build/$sdk"
    cmake -Wno-author -S "$engine_root" -B "$build" -G Ninja -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_SYSTEM_NAME=tvOS -DCMAKE_OSX_SYSROOT="$sdk" -DCMAKE_OSX_ARCHITECTURES=arm64 \
        -DCMAKE_OSX_DEPLOYMENT_TARGET="$minimum_tvos"
    cmake --build "$build"
    # One archive per slice: the engine and the libtorrent it links, so a host links one library.
    mkdir -p "$(dirname "$(slice_library "$sdk")")"
    # ZERO_AR_DATE: no member timestamps, so an unchanged engine gives the same bytes.
    ZERO_AR_DATE=1 libtool -static -o "$(slice_library "$sdk")" \
        "$build/libmovies_engine.a" "$build/_deps/libtorrent-build/libtorrent-rasterbar.a"
}

# True when the xcframework already holds these slice libraries and headers.
is_current() {
    cmp -s "$(slice_library appletvos)" "$output/tvos-arm64/libmovies_engine.a" &&
        cmp -s "$(slice_library appletvsimulator)" "$output/tvos-arm64-simulator/libmovies_engine.a" &&
        diff -rq "$engine_root/include" "$output/tvos-arm64/Headers" >/dev/null &&
        diff -rq "$engine_root/include" "$output/tvos-arm64-simulator/Headers" >/dev/null
}

build_slice appletvos
build_slice appletvsimulator
# Rewriting an unchanged xcframework changes its bytes, and Gradle then reruns the engine
# cinterop and every Kotlin/Native compile and link after it (525 s for the Release framework).
if is_current; then
    echo "MoviesEngine.xcframework is up to date"
    exit 0
fi
rm -rf "$output"
xcodebuild -create-xcframework \
    -library "$(slice_library appletvos)" -headers "$engine_root/include" \
    -library "$(slice_library appletvsimulator)" -headers "$engine_root/include" \
    -output "$output"
