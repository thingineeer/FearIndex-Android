#!/usr/bin/env bash
# 7"/10" 태블릿 스크린샷 자동 캡처 — 45 locale × 4 화면 × 2 size = 360장.
#
# 사전 조건:
#   1. 태블릿 AVD 부팅 (7" 또는 10")
#   2. FearIndex 앱 (debug 또는 production) 설치
#   3. 본 스크립트 사용법:
#      bash scripts/screenshots/capture-tablet-all-locales.sh seven   # 7" only
#      bash scripts/screenshots/capture-tablet-all-locales.sh ten     # 10" only
#
# 산출물 경로:
#   fastlane/metadata/android/<locale>/images/sevenInchScreenshots/{1..4}_*.png
#   fastlane/metadata/android/<locale>/images/tenInchScreenshots/{1..4}_*.png

set -euo pipefail

MODE="${1:-seven}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
ACTIVITY=th1ngjin.fearindex.MainActivity

# PKG 자동 감지: 스토어 캡처는 debug + screenshot mode 우선.
if "$ADB" shell pm list packages 2>/dev/null | grep -q "package:th1ngjin.fearindex.debug$"; then
  PKG="th1ngjin.fearindex.debug"
elif "$ADB" shell pm list packages 2>/dev/null | grep -q "package:th1ngjin.fearindex$"; then
  PKG="th1ngjin.fearindex"
else
  echo "✗ FearIndex 앱이 디바이스에 설치되지 않음. assembleDebug 후 install 하세요."
  exit 1
fi
echo "▶ 감지된 PKG: $PKG"

prime_notification_settings() {
  local prefs_file="/tmp/fearindex-notification-settings.xml"
  cat > "$prefs_file" <<'XML'
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<map>
    <boolean name="notificationEnabled" value="true" />
    <boolean name="globalNotificationEnabled" value="true" />
    <boolean name="kospiNotificationEnabled" value="true" />
    <boolean name="cryptoNotificationEnabled" value="true" />
    <boolean name="weeklyReportNotificationEnabled" value="true" />
    <int name="marketLowerThreshold" value="30" />
    <int name="marketUpperThreshold" value="70" />
    <int name="kospiLowerThreshold" value="30" />
    <int name="kospiUpperThreshold" value="70" />
    <int name="cryptoLowerThreshold" value="25" />
    <int name="cryptoUpperThreshold" value="75" />
</map>
XML
  "$ADB" shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  "$ADB" push "$prefs_file" /data/local/tmp/fearindex-notification-settings.xml >/dev/null
  "$ADB" shell run-as "$PKG" mkdir -p shared_prefs >/dev/null 2>&1 || true
  "$ADB" shell run-as "$PKG" cp /data/local/tmp/fearindex-notification-settings.xml \
    shared_prefs/notification_settings_prefs.xml >/dev/null 2>&1 || true
  "$ADB" shell rm /data/local/tmp/fearindex-notification-settings.xml >/dev/null 2>&1 || true
  rm -f "$prefs_file"
}

# 45 locale (BCP-47, fastlane underscore → ICU hyphen 매핑)
declare -a LOCALE_BCP=(
  "af" "ar" "bg" "bn-BD" "ca" "cs-CZ" "da-DK" "de-DE" "el-GR" "en-US"
  "es-ES" "et" "fa" "fi-FI" "fr-FR" "hi-IN" "hr" "hu-HU" "id" "it-IT"
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

START_AT=${START_AT:-1}
END_AT=${END_AT:-${#LOCALE_BCP[@]}}

is_app_foreground() {
  "$ADB" shell dumpsys window 2>/dev/null |
    grep -Eq "mCurrentFocus=.*$PKG/$ACTIVITY|mFocusedApp=.*$PKG/$ACTIVITY"
}

wait_for_app_foreground() {
  local tries=0
  while [ $tries -lt 20 ]; do
    if is_app_foreground; then
      return 0
    fi
    sleep 1
    tries=$((tries+1))
  done
  echo "  ! app foreground wait timed out" >&2
  return 1
}

set_app_locale() {
  local bcp="$1"
  local expected="$bcp"
  case "$bcp" in
    iw-IL) expected="(iw-IL|he-IL)" ;;
    nb-NO) expected="(nb-NO|no-NO|nb)" ;;
  esac

  if ! "$ADB" shell cmd locale set-app-locales "$PKG" --locales "$bcp" >/dev/null; then
    echo "  ! locale set failed: $bcp" >&2
    return 1
  fi
  sleep 1

  local actual
  actual=$("$ADB" shell cmd locale get-app-locales "$PKG" 2>/dev/null || true)
  if ! echo "$actual" | grep -Eq "$expected"; then
    echo "  ! locale verification failed: expected=$bcp actual=$actual" >&2
    return 1
  fi
}

screencap_pull() {
  local out="$1"
  local remote="/data/local/tmp/fearindex-tablet-screenshot.png"
  local attempt=1
  while [ $attempt -le 2 ]; do
    if "$ADB" shell screencap -p "$remote" >/dev/null 2>&1 &&
      "$ADB" pull -a "$remote" "$out" >/dev/null 2>&1; then
      "$ADB" shell rm "$remote" >/dev/null 2>&1 || true
      return 0
    fi
    echo "  ! screencap retry $attempt" >&2
    "$ADB" shell rm "$remote" >/dev/null 2>&1 || true
    sleep 2
    attempt=$((attempt+1))
  done
  return 1
}

cold_start() {
  local pkg="$1"
  "$ADB" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
  "$ADB" shell wm dismiss-keyguard >/dev/null 2>&1 || true
  "$ADB" shell am force-stop "$pkg" 2>/dev/null
  sleep 1
  "$ADB" shell am start -n "$pkg/$ACTIVITY" >/dev/null 2>&1
  wait_for_app_foreground
  # splash → 메인까지 약 15~25초. screenshot fixture 첫 init 여유 포함.
  sleep 30
  return 0
}

capture_locale_size() {
  local size="$1"
  local size_dir
  if [ "$size" = "seven" ]; then size_dir="sevenInchScreenshots"; else size_dir="tenInchScreenshots"; fi

  echo "═══ $size 태블릿 시작 (45 locale × 4 화면) ═══"

  # 화면 크기 1회 측정
  local W H
  W=$("$ADB" shell wm size | grep -oE "[0-9]+x[0-9]+" | head -1 | cut -dx -f1)
  H=$("$ADB" shell wm size | grep -oE "[0-9]+x[0-9]+" | head -1 | cut -dx -f2)
  echo "▶ 화면: ${W}x${H}"

  # 1200x1920 7" AVD 기준 bottom nav center y ≈ 1810.
  # 1600x2560 10" AVD는 top tab/table row가 더 위에 배치되어 별도 보정.
  local TABY=$((H * 94 / 100))
  local T1X=$((W * 1 / 8))
  local T2X=$((W * 3 / 8))
  local T3X=$((W * 5 / 8))
  local T4X=$((W * 7 / 8))
  local KOSPI_X=$((W / 2))
  local HOME_INDEX_TAB_Y=$((H * 15 / 100))
  local INDEX_TAB_Y=$((H * 12 / 100))
  local NOTIFICATION_TAB_Y=$((H * 29 / 100))
  # 설정 화면 첫 ListItem (알림 설정) center y
  local NOTIF_Y=$((H * 20 / 100))
  local NOTIF_X=$((W / 2))
  if [ "$size" = "ten" ]; then
    HOME_INDEX_TAB_Y=$((H * 9 / 100))
    INDEX_TAB_Y=$((H * 7 / 100))
    NOTIFICATION_TAB_Y=$((H * 18 / 100))
    NOTIF_Y=$((H * 9 / 100))
  fi

  for i in "${!LOCALE_BCP[@]}"; do
    local ordinal=$((i+1))
    if [ "$ordinal" -lt "$START_AT" ] || [ "$ordinal" -gt "$END_AT" ]; then
      continue
    fi
    local bcp="${LOCALE_BCP[$i]}"
    local dir="${LOCALE_DIR[$i]}"
    local out_dir="$ROOT/fastlane/metadata/android/$dir/images/$size_dir"
    mkdir -p "$out_dir"
    rm -f "$out_dir"/*.png

    echo ""
    echo "── [$ordinal/45] $bcp → $dir/$size_dir ──"

    # 1. locale 전환
    set_app_locale "$bcp"

    cold_start "$PKG"

    # 2_home
    "$ADB" shell input tap "$T1X" "$TABY"
    sleep 3
    "$ADB" shell input tap "$KOSPI_X" "$HOME_INDEX_TAB_Y"
    sleep 2
    wait_for_app_foreground
    screencap_pull "$out_dir/2_home.png"

    # 3_chart
    "$ADB" shell input tap "$T2X" "$TABY"
    sleep 4
    "$ADB" shell input tap "$KOSPI_X" "$INDEX_TAB_Y"
    sleep 2
    wait_for_app_foreground
    screencap_pull "$out_dir/3_chart.png"

    # 4_vote
    "$ADB" shell input tap "$T3X" "$TABY"
    sleep 4
    "$ADB" shell input tap "$KOSPI_X" "$INDEX_TAB_Y"
    sleep 2
    wait_for_app_foreground
    screencap_pull "$out_dir/4_vote.png"

    # 1_notification: 설정 → 알림 메뉴 (첫 항목) → KOSPI 알림 탭.
    # 태블릿은 4장만 사용 (5_notification_settings 안 만듦)
    "$ADB" shell input tap "$T4X" "$TABY"
    sleep 3
    "$ADB" shell input tap "$NOTIF_X" "$NOTIF_Y"
    sleep 4
    "$ADB" shell input tap "$KOSPI_X" "$NOTIFICATION_TAB_Y"
    sleep 2
    wait_for_app_foreground
    screencap_pull "$out_dir/1_notification.png"
    echo "  ✓ $bcp: 4/4"
  done

  echo ""
  echo "═══ $size 태블릿 완료 — $((${#LOCALE_BCP[@]} * 4))장 ═══"
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
trap '"$ADB" shell setprop debug.screenshot_mode 0 >/dev/null 2>&1 || true' EXIT
prime_notification_settings

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
