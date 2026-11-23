#!/usr/bin/env bash
# Persistence round trip on the box: run one installs the test addon and saves watch progress,
# the app process is force-stopped, run two reads both back. Two `am instrument` runs, so the
# data must come from disk. Installs the debug APK and its test APK (signed with the box key, so
# an installed box build keeps its data); launches no Activity.
# Needs the test addon served on the Mac:
#   python3 -I apps/apple-tv/TestAddon/make_test_addon.py /tmp/nuvio-test-addon
#   python3 -I apps/apple-tv/TestAddon/serve_test_addon.py /tmp/nuvio-test-addon 8765
# Usage: ANDROID_SERIAL=<ip>:5555 [ADDON_URL=<manifest url>] box/roundtrip.sh
set -euo pipefail

usage="usage: ANDROID_SERIAL=<ip>:5555 [ADDON_URL=<manifest url>] box/roundtrip.sh"
: "${ANDROID_SERIAL:?$usage}"
package=com.kien.movies.next
runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
test_class=com.kien.movies.PersistenceRoundTripTest
addon_url="${ADDON_URL:-http://127.0.0.1:8765/manifest.json}"
apk_dir="$(cd "$(dirname "$0")/.." && pwd)/app/build/outputs/apk"
app_apk="$apk_dir/debug/androidTvApp-debug.apk"
test_apk="$apk_dir/androidTest/debug/androidTvApp-debug-androidTest.apk"

fail() {
    echo "roundtrip.sh: $1" >&2
    exit 1
}

run_step() {
    local method="$1" output
    output="$(adb shell am instrument -w -e persistenceRoundTrip true -e addonUrl "$addon_url" \
        -e class "$test_class#$method" "$runner")"
    echo "$output"
    grep -q '^OK (1 test)' <<<"$output" || fail "$method failed"
}

[[ -f "$app_apk" ]] || fail "no debug APK at $app_apk (make box-roundtrip builds it)"
[[ -f "$test_apk" ]] || fail "no test APK at $test_apk (make box-roundtrip builds it)"

adb install -r "$app_apk"
adb install -r "$test_apk"
if [[ "$addon_url" == http://127.0.0.1:* ]]; then
    port="${addon_url#http://127.0.0.1:}"
    port="${port%%/*}"
    adb reverse "tcp:$port" "tcp:$port"
fi
adb shell am force-stop "$package"
run_step run1_installsTestAddonAndSavesProgress
adb shell am force-stop "$package"
run_step run2_readsAddonAndProgressBack
echo "roundtrip.sh: addon and progress survived a force-stop"
