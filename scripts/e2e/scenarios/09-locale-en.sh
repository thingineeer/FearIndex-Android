#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="09-locale-en"
ensure_device

set_app_locale "en-US"
wait_for_ui 3

tap_tab home
wait_for_ui 2

# 점수 구간에 따라 Fear/Greed/Neutral 중 하나만 화면에 표시됨.
dump_ui
if grep -qE "Fear|Greed|Neutral" "$LOCAL_DUMP"; then
    echo "[ok] English translation applied"
    reset_app_locale
    echo "[$SCENARIO] PASS"
else
    reset_app_locale
    fail "$SCENARIO" "en-US rating text not found in UI dump"
fi
