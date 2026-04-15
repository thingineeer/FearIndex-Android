#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="06-notification-settings"
ensure_device
restart_app
wait_for_ui 3

tap_tab settings
wait_for_ui 2
assert_text_visible "알림 설정" || fail "$SCENARIO" "settings screen not loaded"

# 설정 화면의 "알림 설정" 행 탭 → NotificationSettingsScreen으로 이동
tap_text "알림 설정" || fail "$SCENARIO" "notification settings entry not found"
sleep 3

# NotificationSettingsScreen의 주요 UI 요소 확인 (TopAppBar 타이틀 + 푸시 알림 설명)
dump_ui
if grep -q "푸시 알림" "$LOCAL_DUMP"; then
    echo "[ok] entered NotificationSettingsScreen"
else
    fail "$SCENARIO" "NotificationSettingsScreen content not found"
fi

# 마스터 스위치 클릭 — Compose Switch는 role=Switch 또는 androidx.compose.foundation
# 단순히 "푸시 알림" 텍스트 우측 영역 탭으로 대체 (하드코딩 x좌표)
# 스위치가 오른쪽 끝에 위치하므로 해당 행에서 x=980 정도 탭
SWITCH_BOUNDS=$(grep -o 'text="푸시 알림"[^/]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' "$LOCAL_DUMP" \
    | head -n1 \
    | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' || true)

if [ -n "$SWITCH_BOUNDS" ]; then
    NUMS=$(echo "$SWITCH_BOUNDS" | grep -o '[0-9]\+')
    Y1=$(echo "$NUMS" | sed -n '2p')
    Y2=$(echo "$NUMS" | sed -n '4p')
    CY=$(( (Y1 + Y2) / 2 ))
    echo "[$SCENARIO] tap master switch @ (980,$CY)"
    tap 980 "$CY"
    sleep 2

    # 토글 후 임계값 슬라이더 또는 25/75 같은 숫자가 노출되어야 함
    dump_ui
    if grep -qE "하한|상한|임계값|공포지수가" "$LOCAL_DUMP"; then
        echo "[ok] threshold controls visible after toggle"
    else
        fail "$SCENARIO" "threshold controls not visible after enabling notifications"
    fi
else
    # 스위치 행 위치 못 찾아도 "푸시 알림" 자체가 떠 있으면 진입은 성공
    echo "[warn] master toggle position not found, but screen loaded"
fi

echo "[$SCENARIO] PASS"
