#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="01-launch"
ensure_device
restart_app
wait_for_ui 3

# Home gauge should render — check Korean header label.
assert_text_visible "공포 탐욕 지수" || fail "$SCENARIO" "home gauge header missing"

echo "[$SCENARIO] PASS"
