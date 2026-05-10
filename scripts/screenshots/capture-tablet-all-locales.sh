#!/usr/bin/env bash
# 7"/10" 태블릿 스크린샷 자동 캡처 — 45 locale × 5 화면 × 2 size = 450장.
#
# 사전 조건:
#   1. 태블릿 AVD 부팅 (7" 또는 10")
#   2. FearIndex 앱 (debug 또는 production) 설치
#   3. 본 스크립트 사용법:
#      bash scripts/screenshots/capture-tablet-all-locales.sh seven   # 7" only
#      bash scripts/screenshots/capture-tablet-all-locales.sh ten     # 10" only
#
# 산출물 경로:
#   fastlane/metadata/android/<locale>/images/sevenInchScreenshots/{1..5}_*.png
#   fastlane/metadata/android/<locale>/images/tenInchScreenshots/{1..5}_*.png

set -euo pipefail

MODE="${1:-seven}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"

# PKG 자동 감지: production 우선, 없으면 debug
if "$ADB" shell pm list packages 2>/dev/null | grep -q "package:th1ngjin.fearindex$"; then
  PKG="th1ngjin.fearindex"
elif "$ADB" shell pm list packages 2>/dev/null | grep -q "package:th1ngjin.fearindex.debug$"; then
  PKG="th1ngjin.fearindex.debug"
else
  echo "✗ FearIndex 앱이 디바이스에 설치되지 않음. assembleDebug 후 install 하세요."
  exit 1
fi
echo "▶ 감지된 PKG: $PKG"

# 45 locale (BCP-47, fastlane underscore → ICU hyphen 매핑)
declare -a LOCALE_BCP=(
  "af" "ar" "bg" "bn-BD" "ca" "cs-CZ" "da-DK" "de-DE" "el-GR" "en-US"
  "es-ES" "et" "fa" "fi-FI" "fr-FR" "hi-IN" "hr" "hu-HU" "in" "it-IT"
  "iw-IL" "ja-JP" "ko-KR" "lt" "lv" "ms" "nl-NL" "nb-NO" "pl-PL" "pt-BR"
  "pt-PT" "ro" "ru-RU" "sk" "sl" "sr" "sv-SE" "sw" "ta-IN" "th"
  "tr-TR" "uk" "vi" "zh-CN" "zh-TW"
)

# fastlane 디렉토리 매핑
declare -a LOCALE_DIR=(
  "af" "ar" "bg" "bn_BD" "ca" "cs_CZ" "da_DK" "de_DE" "el_GR" "en_US"
  "es_ES" "et" "fa" "fi_FI" "fr_FR" "hi_IN" "hr" "hu_HU" "id" "it_IT"
  "iw_IL" "ja_JP" "ko_KR" "lt" "lv" "ms" "nl_NL" "no_NO" "pl_PL" "pt_BR"
  "pt_PT" "ro" "ru_RU" "sk" "sl" "sr" "sv_SE" "sw" "ta_IN" "th"
  "tr_TR" "uk" "vi" "zh_CN" "zh_TW"
)

# cold-start — 태블릿 taskbar 모드에서는 dumpsys 가 launcher 를 top 으로 잘못 보고하므로
# fg 검증 대신 충분한 sleep 으로 해결.
cold_start() {
  local pkg="$1"
  "$ADB" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
  "$ADB" shell wm dismiss-keyguard >/dev/null 2>&1 || true
  "$ADB" shell am force-stop "$pkg" 2>/dev/null
  sleep 1
  "$ADB" shell am start -n "$pkg/th1ngjin.fearindex.MainActivity" >/dev/null 2>&1
  # splash → onboarding/메인 까지 약 15~25초 (locale 첫 init 시 더 길음)
  sleep 25
  return 0
}

capture_locale_size() {
  local size="$1"
  local size_dir
  if [ "$size" = "seven" ]; then size_dir="sevenInchScreenshots"; else size_dir="tenInchScreenshots"; fi

  echo "═══ $size 태블릿 시작 (45 locale × 5 화면) ═══"

  # 화면 크기 1회 측정
  local W H
  W=$("$ADB" shell wm size | grep -oE "[0-9]+x[0-9]+" | head -1 | cut -dx -f1)
  H=$("$ADB" shell wm size | grep -oE "[0-9]+x[0-9]+" | head -1 | cut -dx -f2)
  echo "▶ 화면: ${W}x${H}"

  # 태블릿 taskbar (launcher dock) 가 화면 하단 ~60px 차지 → 우리 앱 nav bar 는 그 위에.
  # 1280x800 기준 nav bar center y ≈ H * 89 / 100 (~1139)
  local TABY=$((H * 89 / 100))
  local T1X=$((W * 1 / 8))
  local T2X=$((W * 3 / 8))
  local T3X=$((W * 5 / 8))
  local T4X=$((W * 7 / 8))
  # 설정 화면 첫 ListItem (알림 설정) center y
  local NOTIF_Y=$((H * 12 / 100))
  local NOTIF_X=$((W / 2))

  for i in "${!LOCALE_BCP[@]}"; do
    local bcp="${LOCALE_BCP[$i]}"
    local dir="${LOCALE_DIR[$i]}"
    local out_dir="$ROOT/fastlane/metadata/android/$dir/images/$size_dir"
    mkdir -p "$out_dir"

    echo ""
    echo "── [$((i+1))/45] $bcp → $dir/$size_dir ──"

    # 1. locale 전환
    "$ADB" shell cmd locale set-app-locales "$PKG" --locales "$bcp" 2>/dev/null

    cold_start "$PKG"

    # 2_home
    "$ADB" shell input tap "$T1X" "$TABY"
    sleep 3
    "$ADB" shell screencap -p "/sdcard/cap.png"
    "$ADB" pull -a "/sdcard/cap.png" "$out_dir/2_home.png" >/dev/null 2>&1

    # 3_chart
    "$ADB" shell input tap "$T2X" "$TABY"
    sleep 4
    "$ADB" shell screencap -p "/sdcard/cap.png"
    "$ADB" pull -a "/sdcard/cap.png" "$out_dir/3_chart.png" >/dev/null 2>&1

    # 4_vote
    "$ADB" shell input tap "$T3X" "$TABY"
    sleep 4
    "$ADB" shell screencap -p "/sdcard/cap.png"
    "$ADB" pull -a "/sdcard/cap.png" "$out_dir/4_vote.png" >/dev/null 2>&1

    # 1_notification: 설정 → 알림 메뉴 (첫 항목). 태블릿은 4장만 사용 (5_notification_settings 안 만듦)
    "$ADB" shell input tap "$T4X" "$TABY"
    sleep 3
    "$ADB" shell input tap "$NOTIF_X" "$NOTIF_Y"
    sleep 4
    "$ADB" shell screencap -p "/sdcard/cap.png"
    "$ADB" pull -a "/sdcard/cap.png" "$out_dir/1_notification.png" >/dev/null 2>&1

    "$ADB" shell rm "/sdcard/cap.png" >/dev/null 2>&1
    echo "  ✓ $bcp: 4/4"
  done

  echo ""
  echo "═══ $size 태블릿 완료 — $((${#LOCALE_BCP[@]} * 5))장 ═══"
}

# device 부팅 확인
if ! "$ADB" shell getprop sys.boot_completed 2>/dev/null | grep -q '1'; then
  echo "✗ 디바이스/에뮬레이터 부팅 안됨."
  exit 1
fi

# ANR dialog 차단 + Chrome 비활성화 (이전 사고 방지)
"$ADB" shell settings put global hide_error_dialogs 1
"$ADB" shell pm disable-user com.android.chrome 2>/dev/null || true

# 광고 hide 모드 활성화 — AdBanner.kt 의 isScreenshotMode() 가 이 프로퍼티를 읽음
# 잊으면 테스트 광고가 캡처되어 AdMob 정책 위반 + 스토어 메타 commit 사고 발생
"$ADB" shell setprop debug.screenshot_mode 1
echo "▶ debug.screenshot_mode=1 (AdBanner hide)"

# 화면 wake + keyguard dismiss (이전 cold-start 실패 원인)
"$ADB" shell input keyevent KEYCODE_WAKEUP
"$ADB" shell wm dismiss-keyguard 2>/dev/null || true
sleep 1

case "$MODE" in
  seven) capture_locale_size "seven" ;;
  ten)   capture_locale_size "ten" ;;
  *) echo "Usage: $0 [seven|ten]"; exit 1 ;;
esac

echo ""
echo "▶ 다음:"
echo "  - 산출물: fastlane/metadata/android/<locale>/images/{seven,ten}InchScreenshots/{1..5}_*.png"
echo "  - 검수: ls fastlane/metadata/android/ar/images/sevenInchScreenshots/"
