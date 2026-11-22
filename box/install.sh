#!/usr/bin/env bash
# Installs an APK on the box and compiles it with its baseline profile.
# `adb install` leaves the app at the `verify` filter (interpreted and JIT only), which is
# what Compose looks like at its slowest; Play installs would apply the profile themselves.
# Usage: ANDROID_SERIAL=<ip>:5555 [BOX_PACKAGE=<id>] box/install.sh <apk>
set -euo pipefail

usage="usage: ANDROID_SERIAL=<ip>:5555 [BOX_PACKAGE=<id>] box/install.sh <apk>"
apk="${1:?$usage}"
: "${ANDROID_SERIAL:?$usage}"
package="${BOX_PACKAGE:-com.kien.movies}"
receiver="$package/androidx.profileinstaller.ProfileInstallReceiver"

fail() {
    echo "install.sh: $1" >&2
    exit 1
}

dexopt_status() {
    local dexopt status
    dexopt="$(adb shell dumpsys package dexopt)"
    status="$(grep -A3 "\[$package\]" <<<"$dexopt" | grep -oE "status=[a-z-]+")" ||
        fail "no dexopt status for $package in dumpsys package dexopt"
    echo "$status"
}

[[ -f "$apk" ]] || fail "no such APK file: $apk"

adb install -r "$apk"
adb shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE "$receiver"
adb shell am force-stop "$package"
adb shell cmd package compile -f -m speed-profile "$package"
dexopt_status
