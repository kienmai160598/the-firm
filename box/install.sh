#!/usr/bin/env bash
# Installs an APK on the box and compiles it with its baseline profile.
# `adb install` leaves the app at the `verify` filter (interpreted and JIT only), which is
# what Compose looks like at its slowest; Play installs would apply the profile themselves.
# Usage: ANDROID_SERIAL=<ip>:5555 box/install.sh <apk>
set -euo pipefail

apk="${1:?apk}"
package="com.nuviobox.tv"
receiver="$package/androidx.profileinstaller.ProfileInstallReceiver"

adb install -r "$apk"
adb shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE "$receiver"
adb shell am force-stop "$package"
adb shell cmd package compile -f -m speed-profile "$package"
adb shell dumpsys package dexopt | grep -A3 "\[$package\]" | grep -oE "status=[a-z-]+"
