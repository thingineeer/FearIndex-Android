#!/bin/bash
# v1.1.0 — 45 locale × 5 화면 = 225 장 Play Store promo 스크린샷 자동 촬영
#
# 산출:
#   fastlane/metadata/android/<supply_locale>/images/phoneScreenshots/
#     ├── 1_notification.png        (Android Launcher 위에 푸시 banner)
#     ├── 2_home.png                (앱 홈 탭, KOSPI 선택)
#     ├── 3_chart.png               (앱 차트 탭, KOSPI 선택)
#     ├── 4_vote.png                (앱 투표 탭, KOSPI 선택)
#     └── 5_notification_settings.png  (앱 알림 설정, KOSPI 선택)
#
# 선행 조건:
#   - debug APK 설치 + POST_NOTIFICATIONS 권한 grant
#   - debug.screenshot_mode=1 (스크립트가 자동 설정)

set -e

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
META=$ROOT/fastlane/metadata/android
PUSH_JSON=$ROOT/scripts/screenshots/push_locales.json
PKG=th1ngjin.fearindex.debug
ACTIVITY=th1ngjin.fearindex.MainActivity
RECEIVER=th1ngjin.fearindex.screenshot.ScreenshotPushReceiver
ACTION=th1ngjin.fearindex.SCREENSHOT_PUSH
KOSPI_TAB_X=540
# 1080x2400 phone emulator 기준 uiautomator bounds:
# chart/vote tabs [42,179][1038,305], home tabs [42,257][1038,383],
# notification tabs [42,609][1038,735].
INDEX_TAB_Y=242
HOME_INDEX_TAB_Y=320
NOTIFICATION_TAB_Y=672

adb shell settings put global hide_error_dialogs 1 < /dev/null > /dev/null
adb shell setprop debug.screenshot_mode 1 < /dev/null > /dev/null

trap 'adb shell setprop debug.screenshot_mode 0 < /dev/null > /dev/null 2>&1 || true' EXIT

# locale 매핑: <supply_locale> <bcp47_for_app> <push_lang_key>
LOCALES=(
  "af af af"
  "ar ar ar"
  "bg bg bg"
  "bn_BD bn-BD bn"
  "ca ca ca"
  "cs_CZ cs-CZ cs"
  "da_DK da-DK da"
  "de_DE de-DE de"
  "el_GR el-GR el"
  "en_US en-US en"
  "es_ES es-ES es"
  "et et et"
  "fa fa fa"
  "fi_FI fi-FI fi"
  "fr_FR fr-FR fr"
  "hi_IN hi-IN hi"
  "hr hr hr"
  "hu_HU hu-HU hu"
  "id id id"
  "it_IT it-IT it"
  "iw_IL iw-IL he"
  "ja_JP ja-JP ja"
  "ko_KR ko-KR ko"
  "lt lt lt"
  "lv lv lv"
  "ms ms ms"
  "nl_NL nl-NL nl"
  "no_NO nb-NO nb"
  "pl_PL pl-PL pl"
  "pt_BR pt-BR pt-BR"
  "pt_PT pt-PT pt-PT"
  "ro ro ro"
  "ru_RU ru-RU ru"
  "sk sk sk"
  "sl sl sl"
  "sr sr sr"
  "sv_SE sv-SE sv"
  "sw sw sw"
  "ta_IN ta-IN ta"
  "th th th"
  "tr_TR tr-TR tr"
  "uk uk uk"
  "vi vi vi"
  "zh_CN zh-CN zh-Hans"
  "zh_TW zh-TW zh-Hant"
)

dismiss_anr() {
  local tries=0
  while [ $tries -lt 3 ]; do
    if ! adb shell dumpsys window windows 2>/dev/null | grep -q "Application Not Responding"; then
      return 0
    fi
    adb shell input tap 300 1555 < /dev/null
    sleep 2
    tries=$((tries+1))
  done
}

wait_for_app_ready() {
  local tries=0
  while [ $tries -lt 40 ]; do
    if adb shell dumpsys window windows 2>/dev/null | grep -q "$PKG/$ACTIVITY"; then
      if adb shell uiautomator dump /sdcard/fearindex-window.xml >/dev/null 2>&1 &&
        adb shell grep -q "KOSPI" /sdcard/fearindex-window.xml 2>/dev/null; then
        sleep 20
        return 0
      fi
    fi
    sleep 2
    tries=$((tries+1))
  done
  echo "  ! app ready wait timed out; using fallback sleep" >&2
  sleep 15
}

clear_notifications() {
  # API 28+ 의 cmd notification clear 미지원 → 알림 ID 단위 cancel 또는 패널 swipe 활용.
  # 우리는 매 broadcast 가 같은 ID(20503)을 덮어쓰므로 사실상 1개만 존재.
  adb shell input keyevent KEYCODE_HOME < /dev/null
}

dismiss_heads_up_banner() {
  adb shell cmd statusbar collapse < /dev/null > /dev/null 2>&1 || true
  adb shell input swipe 540 280 540 50 250 < /dev/null
  sleep 1
  adb shell cmd statusbar collapse < /dev/null > /dev/null 2>&1 || true
}

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
  adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS < /dev/null > /dev/null 2>&1 || true
  adb push "$prefs_file" /data/local/tmp/fearindex-notification-settings.xml < /dev/null > /dev/null
  adb shell run-as $PKG mkdir -p shared_prefs < /dev/null > /dev/null 2>&1 || true
  adb shell run-as $PKG cp /data/local/tmp/fearindex-notification-settings.xml \
    shared_prefs/notification_settings_prefs.xml < /dev/null > /dev/null 2>&1 || true
  adb shell rm /data/local/tmp/fearindex-notification-settings.xml < /dev/null > /dev/null 2>&1 || true
  rm -f "$prefs_file"
}

tap_kospi_index_tab() {
  adb shell input tap $KOSPI_TAB_X $INDEX_TAB_Y < /dev/null
  sleep 1
  adb shell input tap $KOSPI_TAB_X $HOME_INDEX_TAB_Y < /dev/null
  sleep 2
  dismiss_anr
}

tap_kospi_notification_tab() {
  adb shell input tap $KOSPI_TAB_X $NOTIFICATION_TAB_Y < /dev/null
  sleep 2
  dismiss_anr
}

# 푸시 banner peek 캡처 (launcher 배경)
capture_push_banner() {
  local push_lang=$1
  local out=$2
  local title=$(jq -r --arg k "$push_lang" '.[$k].title // .en.title' "$PUSH_JSON")
  local body=$(jq -r --arg k "$push_lang" '.[$k].body  // .en.body'  "$PUSH_JSON")

  # 앱 백그라운드로
  adb shell am force-stop $PKG < /dev/null
  sleep 1
  adb shell input keyevent KEYCODE_HOME < /dev/null
  sleep 1
  # 알림 발사 (single-string 으로 한국어/공백 escape)
  adb shell "am broadcast -n $PKG/$RECEIVER -a $ACTION --es title '$title' --es body '$body'" \
    < /dev/null > /dev/null
  # peek banner 가 안정적으로 올라온 뒤 캡처
  # 실측 (app 화면 캡처 후 scenario): 0.7s = banner 미표시, 1.5s = 깨끗.
  python3 -c "import time; time.sleep(1.5)" < /dev/null
  adb exec-out screencap -p > "$out"
  dismiss_heads_up_banner
}

# 화면 진입 후 캡처 (앱 내부)
capture_app_screen() {
  local out=$1
  adb exec-out screencap -p > "$out"
}

TOTAL=${#LOCALES[@]}
count=0
prime_notification_settings

for triplet in "${LOCALES[@]}"; do
  count=$((count+1))
  supply=$(echo $triplet | awk '{print $1}')
  bcp=$(echo $triplet | awk '{print $2}')
  push_lang=$(echo $triplet | awk '{print $3}')
  dir="$META/$supply/images/phoneScreenshots"
  mkdir -p "$dir"
  rm -f "$dir/1_notification_en.png"

  echo "=== ($count/$TOTAL) supply=$supply bcp=$bcp push=$push_lang ==="

  # 0. locale 적용 (다음 cold-start에 반영)
  adb shell am force-stop $PKG < /dev/null
  sleep 1
  adb shell cmd locale set-app-locales $PKG --locales $bcp < /dev/null > /dev/null 2>&1 || true
  sleep 1

  # ────────────────────────────────────────────────────────────
  # 2. 앱 cold start → 홈(KOSPI)
  # ────────────────────────────────────────────────────────────
  adb shell am start -n $PKG/$ACTIVITY < /dev/null > /dev/null
  wait_for_app_ready
  dismiss_anr
  tap_kospi_index_tab
  capture_app_screen "$dir/2_home.png"

  # ────────────────────────────────────────────────────────────
  # 3. 차트 탭(KOSPI, bottom nav 두 번째 = x ≈ 405)
  # ────────────────────────────────────────────────────────────
  adb shell input tap 405 2250 < /dev/null
  sleep 4
  dismiss_anr
  tap_kospi_index_tab
  capture_app_screen "$dir/3_chart.png"

  # ────────────────────────────────────────────────────────────
  # 4. 투표 탭(KOSPI, bottom nav 세 번째 = x ≈ 675)
  # ────────────────────────────────────────────────────────────
  adb shell input tap 675 2250 < /dev/null
  sleep 4
  dismiss_anr
  tap_kospi_index_tab
  capture_app_screen "$dir/4_vote.png"

  # ────────────────────────────────────────────────────────────
  # 5. 설정 탭 → 알림 설정 (첫 번째 ListItem)
  # ────────────────────────────────────────────────────────────
  adb shell input tap 945 2250 < /dev/null
  sleep 3
  dismiss_anr
  adb shell input tap 540 390 < /dev/null
  sleep 4
  dismiss_anr
  tap_kospi_notification_tab
  capture_app_screen "$dir/5_notification_settings.png"

  # ────────────────────────────────────────────────────────────
  # 1. Launcher + 푸시 banner peek (네이티브 언어)
  #    앱 내부 화면을 먼저 찍고 마지막에 캡처해야 heads-up banner가 섞이지 않는다.
  # ────────────────────────────────────────────────────────────
  capture_push_banner "$push_lang" "$dir/1_notification.png"

  echo "  [$supply] 5 screenshots saved -> $dir"
done

echo "=== 완료 (locales=$TOTAL, screens=5, total=$((TOTAL*5))) ==="
