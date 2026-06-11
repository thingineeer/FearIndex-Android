#!/bin/bash
# v4 — ANR 발생 시 uiautomator로 Wait 버튼 클릭
set -e
ROOT=/Users/imyeongjin/Desktop/side/FearIndex-Android
META=$ROOT/fastlane/metadata/android
PKG=th1ngjin.fearindex.debug
ACTIVITY=th1ngjin.fearindex.MainActivity

adb shell settings put global hide_error_dialogs 1 < /dev/null > /dev/null || true
adb shell setprop debug.screenshot_mode 1 < /dev/null > /dev/null
if [ "$(adb shell getprop debug.screenshot_mode | tr -d '\r')" != "1" ]; then
  echo "debug.screenshot_mode=1 설정 실패" >&2
  exit 1
fi
trap 'adb shell setprop debug.screenshot_mode 0 < /dev/null > /dev/null 2>&1 || true' EXIT

dismiss_anr() {
  local tries=0
  while [ $tries -lt 3 ]; do
    if ! adb shell dumpsys window windows 2>/dev/null | grep -q "Application Not Responding"; then
      return 0
    fi
    echo "    (ANR detected #$tries → dismiss)"
    # Wait 버튼은 2-row dialog에서 두 번째 (y ≈ 1550 on 1080x2400)
    adb shell input tap 300 1555 < /dev/null
    sleep 3
    tries=$((tries+1))
  done
}

LOCALES=(
  "af af" "ar ar" "bg bg" "bn_BD bn-BD" "ca ca"
  "cs_CZ cs-CZ" "da_DK da-DK" "de_DE de-DE" "el_GR el-GR" "en_US en-US"
  "es_ES es-ES" "et et" "fa fa" "fi_FI fi-FI" "fr_FR fr-FR"
  "hi_IN hi-IN" "hr hr" "hu_HU hu-HU" "id id" "it_IT it-IT"
  "iw_IL iw-IL" "ja_JP ja-JP" "ko_KR ko-KR" "lt lt" "lv lv"
  "ms ms" "nl_NL nl-NL" "no_NO nb-NO" "pl_PL pl-PL" "pt_BR pt-BR"
  "pt_PT pt-PT" "ro ro" "ru_RU ru-RU" "sk sk" "sl sl"
  "sr sr" "sv_SE sv-SE" "sw sw" "ta_IN ta-IN" "th th"
  "tr_TR tr-TR" "uk uk" "vi vi" "zh_CN zh-CN" "zh_TW zh-TW"
)

TOTAL=${#LOCALES[@]}
count=0
for pair in "${LOCALES[@]}"; do
    count=$((count+1))
    supply=$(echo $pair | awk '{print $1}')
    bcp=$(echo $pair | awk '{print $2}')
    dir="$META/$supply/images/phoneScreenshots"
    mkdir -p "$dir"
    echo "=== ($count/$TOTAL) $supply / $bcp ==="

    adb shell am force-stop $PKG < /dev/null
    sleep 3
    dismiss_anr
    adb shell cmd locale set-app-locales $PKG --locales $bcp < /dev/null >/dev/null 2>&1 || true
    sleep 2
    dismiss_anr
    adb shell am start -n $PKG/$ACTIVITY < /dev/null >/dev/null
    sleep 15
    dismiss_anr

    adb exec-out screencap -p > "$dir/1_home.png"
    sleep 1
    adb shell input tap 405 2250 < /dev/null
    sleep 5
    dismiss_anr
    adb exec-out screencap -p > "$dir/2_chart.png"
    sleep 1
    adb shell input tap 675 2250 < /dev/null
    sleep 5
    dismiss_anr
    adb exec-out screencap -p > "$dir/3_vote.png"
    sleep 1
    adb shell input tap 945 2250 < /dev/null
    sleep 3
    dismiss_anr
    adb shell input tap 540 390 < /dev/null
    sleep 4
    dismiss_anr
    adb exec-out screencap -p > "$dir/4_notification_settings.png"
    echo "  [$supply] done."
done
echo "=== 완료 (total=$TOTAL) ==="
