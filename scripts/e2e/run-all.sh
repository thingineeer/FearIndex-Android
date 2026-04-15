#!/bin/bash
# Run all E2E scenarios sequentially.
# Exits 1 if any scenario fails (after running the rest).
set +e
HERE="$(cd "$(dirname "$0")" && pwd)"
source "$HERE/lib/common.sh"

ensure_device

SCENARIOS=(
    "01-launch.sh"
    "02-tab-navigation.sh"
    "03-chart-periods.sh"
    "04-chart-interaction.sh"
    "05-vote-stuck-toggle.sh"
    "06-notification-settings.sh"
    "07-locale-ko.sh"
    "08-locale-ja.sh"
    "09-locale-en.sh"
)

PASS=()
FAIL=()

for s in "${SCENARIOS[@]}"; do
    echo ""
    echo "============================================================"
    echo "  RUN: $s"
    echo "============================================================"
    bash "$HERE/scenarios/$s"
    rc=$?
    if [ $rc -eq 0 ]; then
        PASS+=("$s")
    else
        FAIL+=("$s")
    fi
done

echo ""
echo "============================================================"
echo "  E2E SUMMARY"
echo "============================================================"
echo "PASS (${#PASS[@]}):"
for s in "${PASS[@]}"; do echo "  - $s"; done
echo "FAIL (${#FAIL[@]}):"
for s in "${FAIL[@]}"; do echo "  - $s"; done

if [ ${#FAIL[@]} -gt 0 ]; then
    exit 1
fi
exit 0
