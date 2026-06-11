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

adb() {
  python3 - "$@" <<'PY'
import os
import subprocess
import sys

timeout = int(os.environ.get("ADB_TIMEOUT_SECONDS", "20"))
cmd = ["adb", *sys.argv[1:]]
try:
    completed = subprocess.run(cmd, timeout=timeout)
    raise SystemExit(completed.returncode)
except subprocess.TimeoutExpired:
    print(f"adb timeout after {timeout}s: {' '.join(cmd)}", file=sys.stderr)
    raise SystemExit(124)
PY
}

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

is_app_foreground() {
  adb shell dumpsys window 2>/dev/null |
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

wait_until_app_not_foreground() {
  local tries=0
  while [ $tries -lt 8 ]; do
    if ! is_app_foreground; then
      return 0
    fi
    adb shell input keyevent KEYCODE_HOME < /dev/null > /dev/null 2>&1 || true
    sleep 1
    tries=$((tries+1))
  done
  echo "  ! app still foreground before launcher capture" >&2
  return 1
}

clear_notifications() {
  # API 28+ 의 cmd notification clear 미지원 → 알림 ID 단위 cancel 또는 패널 swipe 활용.
  # 우리는 매 broadcast 가 같은 ID(20503)을 덮어쓰므로 사실상 1개만 존재.
  adb shell input keyevent KEYCODE_HOME < /dev/null
}

stop_app_process() {
  adb shell am force-stop "$PKG" < /dev/null > /dev/null 2>&1 || true
  adb shell pkill -9 "$PKG" < /dev/null > /dev/null 2>&1 || true
  sleep 1
}

dismiss_system_overlays() {
  adb shell input keyevent KEYCODE_BACK < /dev/null > /dev/null 2>&1 || true
  sleep 1
  adb shell input keyevent KEYCODE_BACK < /dev/null > /dev/null 2>&1 || true
  sleep 1
  adb shell input keyevent KEYCODE_HOME < /dev/null > /dev/null 2>&1 || true
  sleep 1
}

set_app_locale() {
  local bcp=$1
  local expected=$bcp
  case "$bcp" in
    iw-IL) expected="(iw-IL|he-IL)" ;;
    nb-NO) expected="(nb-NO|no-NO|nb)" ;;
  esac

  if ! adb shell cmd locale set-app-locales "$PKG" --locales "$bcp" \
    < /dev/null > /dev/null; then
    echo "  ! locale set failed: $bcp" >&2
    return 1
  fi
  sleep 1

  local actual
  actual=$(adb shell cmd locale get-app-locales "$PKG" 2>/dev/null || true)
  if ! echo "$actual" | grep -Eq "$expected"; then
    echo "  ! locale verification failed: expected=$bcp actual=$actual" >&2
    return 1
  fi
}

start_app_for_capture() {
  dismiss_system_overlays
  if ! adb shell am start -n $PKG/$ACTIVITY < /dev/null > /dev/null; then
    echo "  ! app start command failed; retrying" >&2
    stop_app_process
    dismiss_system_overlays
    adb shell am start -n $PKG/$ACTIVITY < /dev/null > /dev/null
  fi
  if wait_for_app_foreground; then
    sleep 8
    return 0
  fi

  echo "  ! retrying app cold start" >&2
  stop_app_process
  dismiss_system_overlays
  adb shell am start -n $PKG/$ACTIVITY < /dev/null > /dev/null
  wait_for_app_foreground
  sleep 8
}

open_notification_settings_for_capture() {
  local attempt=1
  while [ $attempt -le 2 ]; do
    adb shell input tap 945 2250 < /dev/null
    sleep 2
    dismiss_anr
    if ! wait_for_app_foreground; then
      echo "  ! settings tab did not keep app foreground" >&2
      start_app_for_capture
      attempt=$((attempt+1))
      continue
    fi
    adb shell input tap 250 330 < /dev/null
    sleep 3
    dismiss_anr
    tap_kospi_notification_tab

    if wait_for_app_foreground; then
      sleep 1
      return 0
    fi

    echo "  ! retrying notification settings flow" >&2
    start_app_for_capture
    attempt=$((attempt+1))
  done
  return 1
}

dismiss_heads_up_banner() {
  adb shell input swipe 540 280 540 50 250 < /dev/null
  sleep 2
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
  sleep 2
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
  local base
  base=$(mktemp /tmp/fearindex-launcher-base.XXXXXX)

  # 앱 백그라운드로 보낸 뒤 launcher 배경을 캡처하고, localized notification card 를 합성한다.
  # 실제 heads-up 은 emulator 상태/notification assistant 에 따라 suppressed 될 수 있어
  # 스크린샷 산출물 안정성을 위해 시각적으로 동일한 promo card 를 생성한다.
  stop_app_process
  dismiss_system_overlays
  adb shell input keyevent KEYCODE_HOME < /dev/null > /dev/null 2>&1 || true
  sleep 2
  wait_until_app_not_foreground
  capture_app_screen "$base" 0
  TITLE="$title" BODY="$body" python3 - "$base" "$out" <<'PY'
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os
import sys
import textwrap

base_path, out_path = sys.argv[1:3]
title = os.environ["TITLE"]
body = os.environ["BODY"]

img = Image.open(base_path).convert("RGBA")
w, _ = img.size
overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
draw = ImageDraw.Draw(overlay)

font_path = "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
try:
    title_font = ImageFont.truetype(font_path, 36)
    body_font = ImageFont.truetype(font_path, 30)
    time_font = ImageFont.truetype(font_path, 24)
except OSError:
    title_font = body_font = time_font = ImageFont.load_default()

card_x = 42
card_y = 150
card_w = w - (card_x * 2)
card_h = 184
radius = 34

shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
shadow_draw = ImageDraw.Draw(shadow)
shadow_draw.rounded_rectangle(
    [card_x, card_y + 10, card_x + card_w, card_y + card_h + 10],
    radius=radius,
    fill=(0, 0, 0, 46),
)
overlay = Image.alpha_composite(overlay, shadow.filter(ImageFilter.GaussianBlur(12)))
draw = ImageDraw.Draw(overlay)
draw.rounded_rectangle(
    [card_x, card_y, card_x + card_w, card_y + card_h],
    radius=radius,
    fill=(248, 249, 253, 242),
)

icon_x = card_x + 34
icon_y = card_y + 50
draw.ellipse([icon_x, icon_y, icon_x + 72, icon_y + 72], fill=(239, 59, 59, 255))
draw.pieslice([icon_x + 18, icon_y + 13, icon_x + 55, icon_y + 61], 235, 55, fill=(255, 255, 255, 255))
draw.rectangle([icon_x + 32, icon_y + 44, icon_x + 40, icon_y + 62], fill=(255, 255, 255, 255))

text_x = icon_x + 100
text_w = card_x + card_w - text_x - 110
draw.text((text_x, card_y + 30), "FearIndex", font=time_font, fill=(109, 113, 121, 255))
draw.text((card_x + card_w - 72, card_y + 30), "now", font=time_font, fill=(109, 113, 121, 255), anchor="ra")
draw.text((text_x, card_y + 66), title, font=title_font, fill=(21, 24, 30, 255))

avg_char_px = max(12, int(body_font.size * 0.56))
wrap_width = max(16, text_w // avg_char_px)
body_lines = textwrap.wrap(body, width=wrap_width, max_lines=2, placeholder="...")
draw.multiline_text(
    (text_x, card_y + 112),
    "\n".join(body_lines),
    font=body_font,
    fill=(89, 94, 104, 255),
    spacing=6,
)

Image.alpha_composite(img, overlay).convert("RGB").save(out_path)
PY
  rm -f "$base"
}

# 화면 진입 후 캡처 (앱 내부)
capture_app_screen() {
  local out=$1
  local require_app=${2:-1}
  local remote="/data/local/tmp/fearindex-screenshot.png"
  local attempt=1
  while [ $attempt -le 2 ]; do
    if [ "$require_app" = "1" ] && ! wait_for_app_foreground; then
      echo "  ! app is not foreground before screencap" >&2
      return 1
    fi
    if adb shell screencap -p "$remote" < /dev/null > /dev/null &&
      adb pull -a "$remote" "$out" < /dev/null > /dev/null; then
      adb shell rm "$remote" < /dev/null > /dev/null 2>&1 || true
      return 0
    fi
    echo "  ! screencap retry $attempt" >&2
    adb shell rm "$remote" < /dev/null > /dev/null 2>&1 || true
    sleep 2
    attempt=$((attempt+1))
  done
  return 1
}

TOTAL=${#LOCALES[@]}
START_AT=${START_AT:-1}
END_AT=${END_AT:-$TOTAL}
count=0
prime_notification_settings

for triplet in "${LOCALES[@]}"; do
  count=$((count+1))
  if [ "$count" -lt "$START_AT" ]; then
    continue
  fi
  if [ "$count" -gt "$END_AT" ]; then
    break
  fi
  supply=$(echo $triplet | awk '{print $1}')
  bcp=$(echo $triplet | awk '{print $2}')
  push_lang=$(echo $triplet | awk '{print $3}')
  dir="$META/$supply/images/phoneScreenshots"
  mkdir -p "$dir"
  rm -f "$dir/1_notification_en.png"

  echo "=== ($count/$TOTAL) supply=$supply bcp=$bcp push=$push_lang ==="

  # 0. locale 적용 (다음 cold-start에 반영)
  stop_app_process
  set_app_locale "$bcp"

  # ────────────────────────────────────────────────────────────
  # 2. 앱 cold start → 홈(KOSPI)
  # ────────────────────────────────────────────────────────────
  start_app_for_capture
  dismiss_anr
  tap_kospi_index_tab
  capture_app_screen "$dir/2_home.png"

  # ────────────────────────────────────────────────────────────
  # 3. 차트 탭(KOSPI, bottom nav 두 번째 = x ≈ 405)
  # ────────────────────────────────────────────────────────────
  adb shell input tap 405 2250 < /dev/null
  sleep 2
  dismiss_anr
  tap_kospi_index_tab
  capture_app_screen "$dir/3_chart.png"

  # ────────────────────────────────────────────────────────────
  # 4. 투표 탭(KOSPI, bottom nav 세 번째 = x ≈ 675)
  # ────────────────────────────────────────────────────────────
  adb shell input tap 675 2250 < /dev/null
  sleep 1
  dismiss_anr
  tap_kospi_index_tab
  capture_app_screen "$dir/4_vote.png"

  # ────────────────────────────────────────────────────────────
  # 5. 설정 탭 → 알림 설정 (첫 번째 ListItem)
  # ────────────────────────────────────────────────────────────
  open_notification_settings_for_capture
  capture_app_screen "$dir/5_notification_settings.png"

  # ────────────────────────────────────────────────────────────
  # 1. Launcher + 푸시 banner peek (네이티브 언어)
  #    앱 내부 화면을 먼저 찍고 마지막에 캡처해야 heads-up banner가 섞이지 않는다.
  # ────────────────────────────────────────────────────────────
  capture_push_banner "$push_lang" "$dir/1_notification.png"

  echo "  [$supply] 5 screenshots saved -> $dir"
done

echo "=== 완료 (locales=$START_AT-$END_AT/$TOTAL, screens=5) ==="
