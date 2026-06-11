#!/bin/bash
# E2E common helpers — pure adb + uiautomator (no Appium dependency)
# Usage: source "$(dirname "$0")/../lib/common.sh"

# ─────────────────────────── Config ───────────────────────────
: "${PKG:=th1ngjin.fearindex.debug}"
: "${MAIN_ACTIVITY:=th1ngjin.fearindex.MainActivity}"
: "${ADB:=adb}"
: "${SERIAL:=}"   # set ANDROID_SERIAL externally if multiple devices

# Tab bar coordinates (emulator pixel basis: 1080x2400 class)
TAB_HOME_X=135;    TAB_HOME_Y=2230
TAB_CHART_X=405;   TAB_CHART_Y=2230
TAB_VOTE_X=678;    TAB_VOTE_Y=2230
TAB_SETTINGS_X=953; TAB_SETTINGS_Y=2230

DUMP_PATH="/sdcard/window_dump.xml"
LOCAL_DUMP="/tmp/e2e-dump.xml"

# ─────────────────────────── adb wrapper ───────────────────────────
_adb() {
    if [ -n "$SERIAL" ]; then
        "$ADB" -s "$SERIAL" "$@"
    else
        "$ADB" "$@"
    fi
}

# ─────────────────────────── Lifecycle ───────────────────────────
launch_app() {
    echo "[common] launch_app $PKG"
    _adb shell setprop debug.screenshot_mode 1 >/dev/null 2>&1 || true
    _adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
    sleep 3
}

kill_app() {
    echo "[common] kill_app $PKG"
    _adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
}

restart_app() {
    kill_app
    sleep 1
    launch_app
}

# ─────────────────────────── Input ───────────────────────────
tap() {
    local x="$1" y="$2"
    _adb shell input tap "$x" "$y"
}

tap_tab() {
    case "$1" in
        home)     tap "$TAB_HOME_X" "$TAB_HOME_Y" ;;
        chart)    tap "$TAB_CHART_X" "$TAB_CHART_Y" ;;
        vote)     tap "$TAB_VOTE_X" "$TAB_VOTE_Y" ;;
        settings) tap "$TAB_SETTINGS_X" "$TAB_SETTINGS_Y" ;;
        *) echo "[common] unknown tab: $1"; return 1 ;;
    esac
    sleep 1
}

swipe() {
    local x1="$1" y1="$2" x2="$3" y2="$4" dur="${5:-300}"
    _adb shell input swipe "$x1" "$y1" "$x2" "$y2" "$dur"
}

# ─────────────────────────── UI dump / assertions ───────────────────────────
dump_ui() {
    _adb shell uiautomator dump "$DUMP_PATH" >/dev/null 2>&1
    _adb pull "$DUMP_PATH" "$LOCAL_DUMP" >/dev/null 2>&1
}

wait_for_ui() {
    local seconds="${1:-2}"
    sleep "$seconds"
    dump_ui
}

assert_text_visible() {
    local needle="$1"
    dump_ui
    if grep -q "$needle" "$LOCAL_DUMP"; then
        echo "[ok] visible: $needle"
        return 0
    else
        echo "[fail] not visible: $needle"
        return 1
    fi
}

assert_text_not_visible() {
    local needle="$1"
    dump_ui
    if grep -q "$needle" "$LOCAL_DUMP"; then
        echo "[fail] still visible: $needle"
        return 1
    else
        echo "[ok] hidden: $needle"
        return 0
    fi
}

# Find a node containing $1 and tap its center (best-effort regex parse).
tap_text() {
    local needle="$1"
    dump_ui
    local bounds
    bounds=$(grep -o "text=\"[^\"]*${needle}[^\"]*\"[^/]*bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"" "$LOCAL_DUMP" \
        | head -n1 \
        | grep -o "bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"")
    if [ -z "$bounds" ]; then
        echo "[tap_text] not found: $needle"
        return 1
    fi
    local nums
    nums=$(echo "$bounds" | grep -o '[0-9]\+')
    local x1 y1 x2 y2
    x1=$(echo "$nums" | sed -n '1p')
    y1=$(echo "$nums" | sed -n '2p')
    x2=$(echo "$nums" | sed -n '3p')
    y2=$(echo "$nums" | sed -n '4p')
    local cx=$(( (x1 + x2) / 2 ))
    local cy=$(( (y1 + y2) / 2 ))
    echo "[tap_text] $needle @ ($cx,$cy)"
    tap "$cx" "$cy"
    sleep 1
}

# ─────────────────────────── Screenshot / failure ───────────────────────────
screenshot() {
    local name="$1"
    local out="/tmp/e2e-${name}.png"
    _adb shell screencap -p /sdcard/_e2e.png >/dev/null 2>&1
    _adb pull /sdcard/_e2e.png "$out" >/dev/null 2>&1
    _adb shell rm -f /sdcard/_e2e.png >/dev/null 2>&1
    echo "[screenshot] $out"
}

fail() {
    local scenario="$1" msg="$2"
    echo "[FAIL] $scenario: $msg"
    screenshot "$scenario-fail"
    exit 1
}

# ─────────────────────────── Locale (per-app) ───────────────────────────
# Requires Android 13+ (API 33+) per-app language API.
set_app_locale() {
    local locale="$1"   # ko-KR, ja-JP, en-US ...
    echo "[common] set_app_locale $locale"
    _adb shell cmd locale set-app-locales "$PKG" --locales "$locale" >/dev/null 2>&1 || true
    restart_app
}

reset_app_locale() {
    _adb shell cmd locale set-app-locales "$PKG" --locales "" >/dev/null 2>&1 || true
}

# ─────────────────────────── Sanity ───────────────────────────
ensure_device() {
    if ! _adb get-state >/dev/null 2>&1; then
        echo "[fail] no adb device"
        exit 1
    fi
}
