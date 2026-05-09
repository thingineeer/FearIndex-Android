#!/usr/bin/env bash
# 7"/10" 태블릿 스크린샷 자동 캡처 — 45 locale × 5 화면 × 2 size = 450장.
#
# 사전 조건:
#   1. 태블릿 AVD 2개 미리 생성:
#      - Pixel_Tablet_API_34_7inch (1024x600 or similar)
#      - Pixel_Tablet_API_34_10inch (1920x1200 or 2560x1600)
#      Android Studio → Device Manager → Create Device → Tablet
#
#   2. 각 AVD 부팅 + ANR dialog 차단:
#      adb shell settings put global hide_error_dialogs 1
#
#   3. 본 스크립트 사용법:
#      bash scripts/screenshots/capture-tablet-all-locales.sh seven   # 7" only
#      bash scripts/screenshots/capture-tablet-all-locales.sh ten     # 10" only
#      bash scripts/screenshots/capture-tablet-all-locales.sh both    # 둘 다
#
# 산출물 경로:
#   fastlane/metadata/android/<locale>/images/sevenInchScreenshots/{1..5}_*.png
#   fastlane/metadata/android/<locale>/images/tenInchScreenshots/{1..5}_*.png

set -euo pipefail

MODE="${1:-both}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PKG="${PKG:-th1ngjin.fearindex}"   # production 기준 (debug 는 PKG=th1ngjin.fearindex.debug)
ADB="$HOME/Library/Android/sdk/platform-tools/adb"

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

# 5 화면 × tap 시퀀스 (홈/차트/투표/설정 + 알림 화면)
declare -a SCREENS=(
  "1_notification:notification_settings"   # 알림 설정 화면 진입 (cold-start 후 settings → notification)
  "2_home:home"                            # 홈 (cold-start 후 default)
  "3_chart:chart"                          # 차트 탭
  "4_vote:vote"                            # 투표 탭
  "5_notification_settings:notification_settings"  # 알림 설정 (위와 동일하나 ko 의 경우 다른 안내)
)

capture_locale_size() {
  local size="$1"   # "seven" or "ten"
  local size_dir
  if [ "$size" = "seven" ]; then size_dir="sevenInchScreenshots"; else size_dir="tenInchScreenshots"; fi

  echo "═══ $size 태블릿 시작 (45 locale × 5 화면) ═══"
  for i in "${!LOCALE_BCP[@]}"; do
    local bcp="${LOCALE_BCP[$i]}"
    local dir="${LOCALE_DIR[$i]}"
    local out_dir="$ROOT/fastlane/metadata/android/$dir/images/$size_dir"
    mkdir -p "$out_dir"

    echo ""
    echo "── [$((i+1))/45] $bcp → $dir/$size_dir ──"

    # 1. locale 전환
    "$ADB" shell cmd locale set-app-locales "$PKG" --locales "$bcp"
    "$ADB" shell am force-stop "$PKG"
    sleep 1

    # 2. cold-start
    "$ADB" shell am start -n "$PKG/th1ngjin.fearindex.MainActivity" >/dev/null 2>&1
    sleep 12   # cold-start + load

    # 3. 화면별 캡처
    local W=$("$ADB" shell wm size | grep -oE "[0-9]+x[0-9]+" | head -1 | cut -dx -f1)
    local H=$("$ADB" shell wm size | grep -oE "[0-9]+x[0-9]+" | head -1 | cut -dx -f2)
    local TABY=$((H - 60))
    local T1X=$((W * 1 / 8))   # 홈
    local T2X=$((W * 3 / 8))   # 차트
    local T3X=$((W * 5 / 8))   # 투표
    local T4X=$((W * 7 / 8))   # 설정

    # 1_notification: 설정 → 알림 설정 진입
    "$ADB" shell input tap $T4X $TABY
    sleep 3
    "$ADB" shell input tap $((W / 2)) $((H * 25 / 100))   # 알림 설정 메뉴 (대략)
    sleep 4
    "$ADB" shell screencap -p "/sdcard/cap_1.png"
    "$ADB" pull -a "/sdcard/cap_1.png" "$out_dir/1_notification.png" >/dev/null 2>&1
    "$ADB" shell rm "/sdcard/cap_1.png" >/dev/null

    # 홈으로 돌아가기
    "$ADB" shell input keyevent KEYCODE_BACK
    sleep 1
    "$ADB" shell input tap $T1X $TABY
    sleep 3

    # 2_home
    "$ADB" shell screencap -p "/sdcard/cap_2.png"
    "$ADB" pull -a "/sdcard/cap_2.png" "$out_dir/2_home.png" >/dev/null 2>&1
    "$ADB" shell rm "/sdcard/cap_2.png" >/dev/null

    # 3_chart
    "$ADB" shell input tap $T2X $TABY
    sleep 4
    "$ADB" shell screencap -p "/sdcard/cap_3.png"
    "$ADB" pull -a "/sdcard/cap_3.png" "$out_dir/3_chart.png" >/dev/null 2>&1
    "$ADB" shell rm "/sdcard/cap_3.png" >/dev/null

    # 4_vote
    "$ADB" shell input tap $T3X $TABY
    sleep 4
    "$ADB" shell screencap -p "/sdcard/cap_4.png"
    "$ADB" pull -a "/sdcard/cap_4.png" "$out_dir/4_vote.png" >/dev/null 2>&1
    "$ADB" shell rm "/sdcard/cap_4.png" >/dev/null

    # 5_notification_settings (다시 설정 → 알림)
    "$ADB" shell input tap $T4X $TABY
    sleep 3
    "$ADB" shell input tap $((W / 2)) $((H * 25 / 100))
    sleep 4
    "$ADB" shell screencap -p "/sdcard/cap_5.png"
    "$ADB" pull -a "/sdcard/cap_5.png" "$out_dir/5_notification_settings.png" >/dev/null 2>&1
    "$ADB" shell rm "/sdcard/cap_5.png" >/dev/null

    echo "  ✓ $bcp: 5/5 captured"
  done

  echo ""
  echo "═══ $size 태블릿 완료 — $((${#LOCALE_BCP[@]} * 5))장 ═══"
}

# device 부팅 확인
if ! "$ADB" shell getprop sys.boot_completed 2>/dev/null | grep -q '1'; then
  echo "✗ 디바이스/에뮬레이터 부팅 안됨. AVD 먼저 실행하세요."
  exit 1
fi

# ANR dialog 차단
"$ADB" shell settings put global hide_error_dialogs 1

case "$MODE" in
  seven) capture_locale_size "seven" ;;
  ten)   capture_locale_size "ten" ;;
  both)
    echo "▶ 7\" 태블릿 AVD 부팅 후 'seven' 모드로 실행하고, 끝나면 10\" AVD 로 부팅 후 'ten' 모드로."
    echo "  자동으로 둘 다 실행하려면 두 개 AVD 가 동시에 실행되어야 하나 본 스크립트는 단일 device 가정."
    capture_locale_size "seven"
    ;;
  *) echo "Usage: $0 [seven|ten|both]"; exit 1 ;;
esac

echo ""
echo "다음:"
echo "  - 24h 후 fastlane supply 권한 전파 끝나면:"
echo "    bash scripts/deploy/upload-metadata-all-locales.sh"
echo "  - 또는 manual: Play Console → 각 locale → 7\"/10\" 태블릿 슬롯 → 5장 업로드"
