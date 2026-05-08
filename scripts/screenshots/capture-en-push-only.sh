#!/bin/bash
# v1.0.3 — 45 locale 각각에 영어 푸시 banner 만 추가 캡처 (1_notification_en.png)
#
# 이미 capture-all-locales-v103.sh 가 다 끝난 다음 영어 banner 만 보충하고 싶을 때 사용.
# locale 별로 OS locale 은 그대로 두고 (이미 set-app-locales 로 설정되어 있음) push_lang 만 en 로 broadcast.

set -e

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
META=$ROOT/fastlane/metadata/android
PUSH_JSON=$ROOT/scripts/screenshots/push_locales.json
PKG=th1ngjin.fearindex.debug
ACTIVITY=th1ngjin.fearindex.MainActivity
RECEIVER=th1ngjin.fearindex.screenshot.ScreenshotPushReceiver
ACTION=th1ngjin.fearindex.SCREENSHOT_PUSH

adb shell settings put global hide_error_dialogs 1 < /dev/null > /dev/null
adb shell setprop debug.screenshot_mode 1 < /dev/null > /dev/null

trap 'adb shell setprop debug.screenshot_mode 0 < /dev/null > /dev/null 2>&1 || true' EXIT

# locale 매핑: <supply_locale> <bcp47_for_app>
LOCALES=(
  "af af"
  "ar ar"
  "bg bg"
  "bn_BD bn-BD"
  "ca ca"
  "cs_CZ cs-CZ"
  "da_DK da-DK"
  "de_DE de-DE"
  "el_GR el-GR"
  "en_US en-US"
  "es_ES es-ES"
  "et et"
  "fa fa"
  "fi_FI fi-FI"
  "fr_FR fr-FR"
  "hi_IN hi-IN"
  "hr hr"
  "hu_HU hu-HU"
  "id id"
  "it_IT it-IT"
  "iw_IL iw-IL"
  "ja_JP ja-JP"
  "ko_KR ko-KR"
  "lt lt"
  "lv lv"
  "ms ms"
  "nl_NL nl-NL"
  "no_NO nb-NO"
  "pl_PL pl-PL"
  "pt_BR pt-BR"
  "pt_PT pt-PT"
  "ro ro"
  "ru_RU ru-RU"
  "sk sk"
  "sl sl"
  "sr sr"
  "sv_SE sv-SE"
  "sw sw"
  "ta_IN ta-IN"
  "th th"
  "tr_TR tr-TR"
  "uk uk"
  "vi vi"
  "zh_CN zh-CN"
  "zh_TW zh-TW"
)

EN_TITLE=$(jq -r '.en.title' "$PUSH_JSON")
EN_BODY=$(jq -r '.en.body' "$PUSH_JSON")

TOTAL=${#LOCALES[@]}
count=0

for pair in "${LOCALES[@]}"; do
  count=$((count+1))
  supply=$(echo $pair | awk '{print $1}')
  bcp=$(echo $pair | awk '{print $2}')
  dir="$META/$supply/images/phoneScreenshots"
  mkdir -p "$dir"

  echo "=== ($count/$TOTAL) supply=$supply bcp=$bcp (en push) ==="

  # locale 적용 (앱 white statusbar 텍스트 등이 locale 영향 받을 수 있어 동일 유지)
  adb shell am force-stop $PKG < /dev/null
  sleep 1
  adb shell cmd locale set-app-locales $PKG --locales $bcp < /dev/null > /dev/null 2>&1 || true
  sleep 1

  adb shell input keyevent KEYCODE_HOME < /dev/null
  sleep 1

  # 영어 push 발사
  adb shell "am broadcast -n $PKG/$RECEIVER -a $ACTION --es title '$EN_TITLE' --es body '$EN_BODY'" \
    < /dev/null > /dev/null

  python3 -c "import time; time.sleep(0.7)" < /dev/null
  adb exec-out screencap -p > "$dir/1_notification_en.png"

  echo "  [$supply] 1_notification_en.png saved"

  # banner fade-out 대기
  sleep 4
done

echo "=== 완료 (locales=$TOTAL, en push captured) ==="
