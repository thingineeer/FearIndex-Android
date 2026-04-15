#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="02-tab-navigation"
ensure_device
restart_app
wait_for_ui 3

# Home
tap_tab home
wait_for_ui 1
assert_text_visible "공포 탐욕 지수" || fail "$SCENARIO" "home tab not visible"

# Chart
tap_tab chart
wait_for_ui 1
assert_text_visible "히스토리" || fail "$SCENARIO" "chart tab not visible"

# Vote
tap_tab vote
wait_for_ui 1
assert_text_visible "물림 비율" || fail "$SCENARIO" "vote tab not visible"

# Settings
tap_tab settings
wait_for_ui 1
assert_text_visible "알림 설정" || fail "$SCENARIO" "settings tab not visible"

echo "[$SCENARIO] PASS"
