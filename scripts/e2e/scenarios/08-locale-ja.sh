#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="08-locale-ja"
ensure_device

set_app_locale "ja-JP"
wait_for_ui 3

tap_tab home
wait_for_ui 2

# 점수 구간에 따라 恐怖/貪欲 중 하나만 보임. 하나라도 있으면 ja 번역 OK.
dump_ui
if grep -q "恐怖\|貪欲\|中立" "$LOCAL_DUMP"; then
    echo "[ok] Japanese translation applied"
    echo "[$SCENARIO] PASS"
else
    fail "$SCENARIO" "ja-JP rating text not found in UI dump"
fi
