#!/usr/bin/env bash
# Runs one fixed remote-control scenario on the box and records frame and memory stats.
# Usage: ANDROID_SERIAL=<ip>:5555 box/perf/measure.sh <label> [runs]
# Results: box/perf/results/<label>/run-<n>.txt, a summary line per segment on stdout.
set -euo pipefail

label="${1:?label}"
runs="${2:-3}"
package="com.nuviobox.tv"
activity="$package/com.nuvio.tv.MainActivity"
out="$(dirname "$0")/results/$label"
mkdir -p "$out"

press() {
    local key="$1" count="$2"
    for _ in $(seq "$count"); do
        adb shell input keyevent "$key"
        sleep 0.35
    done
}

frames_reset() {
    adb shell dumpsys gfxinfo "$package" reset >/dev/null
}

frames_record() {
    local segment="$1" file="$2"
    echo "## $segment" >>"$file"
    adb shell dumpsys gfxinfo "$package" |
        grep -E "Total frames|Janky frames|th percentile:|Number (Missed|Slow UI)" |
        grep -v "gpu percentile" >>"$file"
}

summary() {
    local file="$1"
    awk -v run="$(basename "$file")" '
        /^## / { segment = $2 }
        /^Janky frames:/ { jank[segment] = $4 }
        /^90th percentile/ { p90[segment] = $3 }
        /^Number Slow UI/ { slow[segment] = $5 }
        END { for (s in jank) printf "%s %-12s jank %s p90 %s slowUI %s\n",
            run, s, jank[s], p90[s], slow[s] }
    ' "$file"
}

run_once() {
    local file="$1"
    : >"$file"
    adb shell am force-stop "$package"
    sleep 2
    adb shell am start -W -n "$activity" | grep -E "TotalTime" >>"$file"
    sleep 10

    frames_reset
    press KEYCODE_DPAD_DOWN 1
    press KEYCODE_DPAD_RIGHT 20
    press KEYCODE_DPAD_DOWN 5
    press KEYCODE_DPAD_LEFT 12
    press KEYCODE_DPAD_UP 4
    frames_record home "$file"

    frames_reset
    press KEYCODE_DPAD_CENTER 1
    sleep 5
    press KEYCODE_DPAD_DOWN 4
    press KEYCODE_DPAD_UP 4
    frames_record details "$file"
    press KEYCODE_BACK 1
    sleep 2

    adb shell dumpsys meminfo "$package" | grep -E "TOTAL PSS|TOTAL:" | head -1 >>"$file"
}

adb shell input keyevent KEYCODE_VOLUME_MUTE
for run in $(seq "$runs"); do
    run_once "$out/run-$run.txt"
    summary "$out/run-$run.txt"
done
adb shell input keyevent KEYCODE_VOLUME_MUTE
adb shell am force-stop "$package"
