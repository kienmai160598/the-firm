#!/usr/bin/env bash
# Runs one fixed remote-control scenario on the box and records frame and memory stats.
# Usage: ANDROID_SERIAL=<ip>:5555 box/perf/measure.sh <label> [runs]
# Results: box/perf/results/<label>/run-<n>.txt, a summary line per segment on stdout.
# The box is muted while it runs and its mute state is restored on any exit.
set -euo pipefail

usage="usage: ANDROID_SERIAL=<ip>:5555 [BOX_PACKAGE=<id>] box/perf/measure.sh <label> [runs]"
label="${1:?$usage}"
runs="${2:-3}"
: "${ANDROID_SERIAL:?$usage}"
package="${BOX_PACKAGE:-com.nuviobox.tv}"
activity="$package/com.nuvio.tv.MainActivity"
out="$(dirname "$0")/results/$label"

fail() {
    echo "measure.sh: $1" >&2
    exit 1
}

validate_arguments() {
    case "$label" in
        */* | *..*) fail "label must not contain '/' or '..': $label" ;;
    esac
    [[ "$runs" =~ ^[1-9][0-9]*$ ]] || fail "runs must be a positive integer: $runs"
}

music_muted() {
    local audio_dump
    audio_dump="$(adb shell dumpsys audio)" || return 1
    awk '
        /^ *- / { in_music = ($2 == "STREAM_MUSIC:" || $2 == "STREAM_MUSIC") }
        in_music && $1 == "Muted:" && state == "" { state = $2 }
        END { if (state == "") exit 1; print state }
    ' <<<"$audio_dump" || {
        echo "measure.sh: no STREAM_MUSIC 'Muted:' line in dumpsys audio" >&2
        return 1
    }
}

set_music_muted() {
    local wanted="$1" current
    current="$(music_muted)" || return 1
    [[ "$current" == "$wanted" ]] && return 0

    press KEYCODE_VOLUME_MUTE 1
    current="$(music_muted)" || return 1
    [[ "$current" == "$wanted" ]] && return 0

    press KEYCODE_VOLUME_MUTE 1
    echo "measure.sh: the mute key did not change STREAM_MUSIC Muted, sent it again" >&2
    return 1
}

restore_music_mute() {
    set_music_muted "$original_muted" ||
        echo "measure.sh: could not restore mute ($original_muted), check the box" >&2
}

mute_for_measuring() {
    original_muted="$(music_muted)"
    trap restore_music_mute EXIT
    set_music_muted true
}

press() {
    local key="$1" count="$2"
    for _ in $(seq "$count"); do
        adb shell input keyevent "$key"
        sleep 0.35
    done
}

matching_lines() {
    local what="$1" pattern="$2" text="$3"
    grep -E "$pattern" <<<"$text" || fail "no $what in the box output for $package"
}

start_app() {
    local file="$1" launch
    adb shell am force-stop "$package"
    sleep 2
    launch="$(adb shell am start -W -n "$activity")"
    matching_lines "TotalTime from am start ($launch)" "TotalTime" "$launch" >>"$file"
}

frames_reset() {
    adb shell dumpsys gfxinfo "$package" reset >/dev/null
}

frames_record() {
    local segment="$1" file="$2" gfxinfo frame_stats
    gfxinfo="$(adb shell dumpsys gfxinfo "$package")"
    frame_stats="Total frames|Janky frames|th percentile:|Number (Missed|Slow UI)"
    echo "## $segment" >>"$file"
    matching_lines "frame stats" "$frame_stats" "$gfxinfo" | grep -v "gpu percentile" >>"$file"
}

memory_record() {
    local file="$1" meminfo
    meminfo="$(adb shell dumpsys meminfo "$package")"
    matching_lines "memory total" "TOTAL PSS|TOTAL:" "$meminfo" | sed -n 1p >>"$file"
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
    start_app "$file"
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

    memory_record "$file"
}

validate_arguments
mkdir -p "$out"
mute_for_measuring
for run in $(seq "$runs"); do
    run_once "$out/run-$run.txt"
    summary "$out/run-$run.txt"
done
adb shell am force-stop "$package"
