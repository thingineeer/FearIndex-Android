#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="07-locale-ko"
ensure_device

set_app_locale "ko-KR"
wait_for_ui 3

tap_tab home
wait_for_ui 2

assert_text_visible "공포" || fail "$SCENARIO" "Korean '공포' not visible"
assert_text_visible "탐욕" || fail "$SCENARIO" "Korean '탐욕' not visible"

echo "[$SCENARIO] PASS"
