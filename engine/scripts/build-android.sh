#!/usr/bin/env bash
# Builds libmovies_engine.so (engine, libtorrent and the JNI shim) for each Android ABI into
# engine/build/android/jniLibs/<abi>, where shared/torrent packages it.
# ABIs: ABIS in shared/local.properties, as the box build reads them; armeabi-v7a by default.
set -euo pipefail
source "$(dirname "$0")/tools.sh"

android_home="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ndk_version="29.0.14206865"
ndk="${ANDROID_NDK_HOME:-$android_home/ndk/$ndk_version}"
# shared/ minSdk (gradle/libs.versions.toml android-minSdk).
min_sdk="24"
output="$engine_root/build/android/jniLibs"
local_properties="$engine_root/../shared/local.properties"

[[ -f "$ndk/build/cmake/android.toolchain.cmake" ]] || {
    echo "build-android: no NDK at $ndk (sdkmanager --install 'ndk;$ndk_version')" >&2
    exit 1
}

abis() {
    local listed=""
    if [[ -f "$local_properties" ]]; then
        listed="$(sed -n 's/^ABIS[[:space:]]*=[[:space:]]*//p' "$local_properties" | tail -1)"
    fi
    echo "${listed:-armeabi-v7a}" | tr ',' '\n' | sed 's/[[:space:]]//g' | sed '/^$/d'
}

build_abi() {
    local abi="$1"
    local build="$engine_root/build/android-$abi"
    # c++_static: this is the only library of the app that uses the C++ runtime across its
    # own code, and nothing C++ crosses its JNI boundary.
    cmake -Wno-author -S "$engine_root" -B "$build" -G Ninja -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$abi" -DANDROID_PLATFORM="android-$min_sdk" -DANDROID_STL=c++_static
    cmake --build "$build" --target movies_engine_jni
    mkdir -p "$output/$abi"
    "$ndk/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-strip" --strip-unneeded \
        -o "$output/$abi/libmovies_engine.so" "$build/libmovies_engine.so"
}

rm -rf "$output"
while IFS= read -r abi; do
    build_abi "$abi"
done < <(abis)
