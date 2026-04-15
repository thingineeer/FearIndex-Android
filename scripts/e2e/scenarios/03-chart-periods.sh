#!/bin/bash
set -e
source "$(dirname "$0")/../lib/common.sh"

SCENARIO="03-chart-periods"
ensure_device
restart_app
wait_for_ui 3

tap_tab chart
wait_for_ui 2
assert_text_visible "히스토리" || fail "$SCENARIO" "chart screen not loaded"

# 3개 기간만 테스트 (5회 탭 시 AdMob 인터스티셜이 뜨는 것을 피하기 위해)
# 기간별 데이터 로딩 검증이 목적이므로 3M/1Y/5Y 샘플만으로 충분
PERIODS=("3M" "1Y" "5Y")
PREV_HASH=""

for P in "${PERIODS[@]}"; do
    echo "[$SCENARIO] tap period $P"
    tap_text "$P" || fail "$SCENARIO" "period button $P not tappable"
    sleep 3  # chart re-fetch + animation
    dump_ui

    # 해당 period 텍스트가 dump에 존재해야 함
    grep -q "$P" "$LOCAL_DUMP" || fail "$SCENARIO" "period $P not reflected in UI"

    # 기간 전환 시 X축 레이블이 바뀌어 dump hash가 달라져야 함
    HASH=$(md5 -q "$LOCAL_DUMP" 2>/dev/null || md5sum "$LOCAL_DUMP" | awk '{print $1}')
    if [ -n "$PREV_HASH" ] && [ "$PREV_HASH" = "$HASH" ]; then
        fail "$SCENARIO" "UI did not change after switching to $P"
    fi
    PREV_HASH="$HASH"
done

echo "[$SCENARIO] PASS"
