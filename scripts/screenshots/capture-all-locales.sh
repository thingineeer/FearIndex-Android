#!/usr/bin/env bash
#
# capture-all-locales.sh
#
# 45개 locale에 대해 FearIndex Android 앱 스크린샷을 자동 촬영한다.
# - per-app locale: `cmd locale set-app-locales` (Android 13+ / API 33+)
# - 탭 좌표는 Pixel 6 1080x2400 기준 (홈/차트/투표/설정 4탭)
# - 산출물: fastlane/screenshots/android/{locale}/0X-*.png
#
# 사전 조건:
#   - emulator 또는 device가 1대 연결되어 있을 것
#   - app debug 빌드(com.thingineer.fearindex.debug)가 설치되어 있을 것
#   - Android 13+ (per-app locale API 사용)

set -euo pipefail

PACKAGE="com.thingineer.fearindex.debug"
LAUNCH_INTENT="${PACKAGE}/com.thingineer.fearindex.MainActivity"

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_BASE="${PROJECT_ROOT}/fastlane/screenshots/android"
WAIT_AFTER_LAUNCH=3
WAIT_AFTER_TAP=2

# 탭 좌표 (Pixel 6 1080x2400 기준)
TAB_HOME_X=135;     TAB_HOME_Y=2230
TAB_CHART_X=405;    TAB_CHART_Y=2230
TAB_VOTE_X=678;     TAB_VOTE_Y=2230
TAB_SETTINGS_X=953; TAB_SETTINGS_Y=2230

# 설정 화면 안에서 "알림 설정"으로 진입하는 좌표 (실측 후 보정 필요)
NOTIF_ROW_X=540
NOTIF_ROW_Y=600

# 45개 locale (Android resource qualifier 형식)
LOCALES=(
  af ar bg bn ca cs da de el en es et fa fi fr iw hi hr hu in it
  ja ko lt lv ms nb nl pl pt-rBR pt-rPT ro ru sk sl sr sv sw ta th
  tr uk vi zh-rCN zh-rTW
)

log() { printf "[%s] %s\n" "$(date +%H:%M:%S)" "$*"; }

require_adb() {
  if ! command -v adb >/dev/null 2>&1; then
    echo "adb not found in PATH" >&2
    exit 1
  fi

  local device_count
  device_count="$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')"
  if [[ "${device_count}" -lt 1 ]]; then
    echo "No connected adb device. Start an emulator or attach a device." >&2
    exit 1
  fi
  log "adb device(s) connected: ${device_count}"
}

set_locale() {
  local locale="$1"
  # Android는 'pt-rBR' 같은 resource qualifier 대신 BCP47('pt-BR')을 받는다.
  local bcp47="${locale/-r/-}"
  adb shell cmd locale set-app-locales "${PACKAGE}" --locales "${bcp47}" >/dev/null 2>&1 || {
    log "set-app-locales failed for ${bcp47} (Android 13+ required)"
  }
}

restart_app() {
  adb shell am force-stop "${PACKAGE}" >/dev/null 2>&1 || true
  adb shell monkey -p "${PACKAGE}" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || \
    adb shell am start -n "${LAUNCH_INTENT}" >/dev/null 2>&1
  sleep "${WAIT_AFTER_LAUNCH}"
}

tap() {
  local x="$1" y="$2"
  adb shell input tap "${x}" "${y}" >/dev/null
  sleep "${WAIT_AFTER_TAP}"
}

capture() {
  local locale="$1" name="$2"
  local out_dir="${OUT_BASE}/${locale}"
  mkdir -p "${out_dir}"
  local device_path="/sdcard/_fastlane_${locale}_${name}.png"
  adb shell screencap -p "${device_path}" >/dev/null
  adb pull "${device_path}" "${out_dir}/${name}.png" >/dev/null
  adb shell rm -f "${device_path}" >/dev/null
  log "  -> ${out_dir}/${name}.png"
}

capture_locale() {
  local locale="$1"
  log "==> ${locale}"
  set_locale "${locale}"
  restart_app

  # 1) Home
  tap "${TAB_HOME_X}" "${TAB_HOME_Y}"
  capture "${locale}" "01-home"

  # 2) Chart
  tap "${TAB_CHART_X}" "${TAB_CHART_Y}"
  capture "${locale}" "02-chart"

  # 3) Vote
  tap "${TAB_VOTE_X}" "${TAB_VOTE_Y}"
  capture "${locale}" "03-vote"

  # 4) Settings
  tap "${TAB_SETTINGS_X}" "${TAB_SETTINGS_Y}"
  capture "${locale}" "04-settings"

  # 5) Notification settings (설정 → 알림)
  tap "${NOTIF_ROW_X}" "${NOTIF_ROW_Y}"
  capture "${locale}" "05-notification-settings"
}

main() {
  require_adb
  mkdir -p "${OUT_BASE}"

  local started_at
  started_at="$(date +%s)"
  log "Total locales: ${#LOCALES[@]}"
  log "Output base: ${OUT_BASE}"

  for locale in "${LOCALES[@]}"; do
    capture_locale "${locale}" || log "FAILED: ${locale} (계속 진행)"
  done

  local ended_at elapsed mins secs
  ended_at="$(date +%s)"
  elapsed=$((ended_at - started_at))
  mins=$((elapsed / 60))
  secs=$((elapsed % 60))
  log "Done. ${#LOCALES[@]} locales in ${mins}m ${secs}s"
}

main "$@"
