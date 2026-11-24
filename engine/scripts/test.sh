#!/usr/bin/env bash
# Builds and runs the engine unit tests on this Mac.
set -euo pipefail
source "$(dirname "$0")/tools.sh"

build="$engine_root/build/host-tests"
cmake -Wno-author -S "$engine_root" -B "$build" -G Ninja -DCMAKE_BUILD_TYPE=Debug -DMOVIES_ENGINE_TESTS=ON
cmake --build "$build"
ctest --test-dir "$build" --output-on-failure
