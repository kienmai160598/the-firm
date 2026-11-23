#!/usr/bin/env bash
# Builds MoviesEngine.xcframework (tvOS device and simulator slices, arm64) into engine/build.
set -euo pipefail
source "$(dirname "$0")/tools.sh"

minimum_tvos="17.0"
output="$engine_root/build/MoviesEngine.xcframework"

build_slice() {
    local sdk="$1"
    local build="$engine_root/build/$sdk"
    cmake -S "$engine_root" -B "$build" -G Ninja -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_SYSTEM_NAME=tvOS -DCMAKE_OSX_SYSROOT="$sdk" -DCMAKE_OSX_ARCHITECTURES=arm64 \
        -DCMAKE_OSX_DEPLOYMENT_TARGET="$minimum_tvos" >&2
    cmake --build "$build" >&2
    echo "$build/libmovies_engine.a"
}

device="$(build_slice appletvos)"
simulator="$(build_slice appletvsimulator)"
rm -rf "$output"
xcodebuild -create-xcframework \
    -library "$device" -headers "$engine_root/include" \
    -library "$simulator" -headers "$engine_root/include" \
    -output "$output"
