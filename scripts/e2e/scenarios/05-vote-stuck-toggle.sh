#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="05-vote-stuck-toggle"
ensure_device
restart_app
wait_for_ui 3

tap_tab vote
wait_for_ui 2
assert_text_visible "물림 비율" || fail "$SCENARIO" "vote screen not loaded"

# 두 버튼이 클릭 가능한지 확인 (기능 검증)
assert_text_visible "물렸어요" || fail "$SCENARIO" "stuck button not visible"
assert_text_visible "안 물렸어요" 2>/dev/null || assert_text_visible "안물렸어요" || \
    fail "$SCENARIO" "not-stuck button not visible"

# 탭 후 화면이 깨지지 않고 Vote 탭에 남아있는지 확인
tap_text "물렸어요" || fail "$SCENARIO" "stuck button not tappable"
sleep 2
assert_text_visible "물림 비율" || fail "$SCENARIO" "Vote screen lost after toggling stuck"

# 반대 토글
tap_text "안 물렸어요" 2>/dev/null || tap_text "안물렸어요" || \
    fail "$SCENARIO" "not-stuck button not tappable"
sleep 2
assert_text_visible "물림 비율" || fail "$SCENARIO" "Vote screen lost after toggling not-stuck"

echo "[$SCENARIO] PASS"
