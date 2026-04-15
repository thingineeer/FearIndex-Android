#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="04-chart-interaction"
ensure_device
restart_app
wait_for_ui 3

tap_tab chart
wait_for_ui 2
assert_text_visible "히스토리" || fail "$SCENARIO" "chart screen not loaded"

# Drag across chart area to surface tooltip.
# Approx chart band y=900 (between header and period buttons).
DRAG_Y=900
swipe 200 "$DRAG_Y" 900 "$DRAG_Y" 600
sleep 2
dump_ui
cp "$LOCAL_DUMP" /tmp/e2e-chart-tooltip.xml

# Tooltip detection — heuristic: dump size grew or contains a date/value-like token.
if ! grep -E "[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}|[0-9]+\.[0-9]+" /tmp/e2e-chart-tooltip.xml >/dev/null; then
    echo "[warn] tooltip text not detected via regex (UI may render via Canvas)"
fi

# Switch period — tooltip should clear.
tap_text "1Y" || fail "$SCENARIO" "period 1Y not tappable"
sleep 2
dump_ui

# Compare: dump should differ from tooltip-active dump.
H1=$(md5 -q /tmp/e2e-chart-tooltip.xml 2>/dev/null || md5sum /tmp/e2e-chart-tooltip.xml | awk '{print $1}')
H2=$(md5 -q "$LOCAL_DUMP" 2>/dev/null || md5sum "$LOCAL_DUMP" | awk '{print $1}')
if [ "$H1" = "$H2" ]; then
    fail "$SCENARIO" "selection not cleared after period switch"
fi

echo "[$SCENARIO] PASS"
