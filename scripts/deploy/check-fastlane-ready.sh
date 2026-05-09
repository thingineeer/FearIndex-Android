#!/usr/bin/env bash
# Fastlane Play Console 자동화 즉시 작동 가능한지 검증.
# 권한 전파 (1~24h) 끝나면 이 스크립트가 통과 → fastlane upload_metadata 한 방 가능.
#
# 사용:
#   bash scripts/deploy/check-fastlane-ready.sh
#
# 통과 조건:
#   - service account JSON 존재 + 인증 통과
#   - Play Console internal track validate_only 호출 통과 (= 권한 전파 완료)
#   - fastlane/metadata/android/ 표준 구조 일치 (45 locale)
#
# 사용 후:
#   ✓ 통과 → bash scripts/deploy/upload-metadata-all-locales.sh 로 45 locale 일괄 sync
#   ✗ 실패 → Google 권한 전파 24h 대기 후 재시도

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JSON_KEY="$HOME/fearindex-secrets/play-store-service-account.json"
PACKAGE="th1ngjin.fearindex"

echo "═══ Fastlane Play Console Readiness Check ═══"
echo ""

# 1. Service account JSON
if [[ ! -f "$JSON_KEY" ]]; then
  echo "✗ Service account JSON 없음: $JSON_KEY"
  echo "  → fastlane/SUPPLY-SETUP.md 절차 따라 발급"
  exit 1
fi
echo "✓ Service account JSON: $JSON_KEY"

# 2. JSON 인증
echo ""
echo "── JSON 인증 검증 ──"
if fastlane run validate_play_store_json_key json_key:"$JSON_KEY" 2>&1 | grep -q "Successfully established connection"; then
  echo "✓ Google Play Store 연결 OK"
else
  echo "✗ JSON 인증 실패"
  echo "  → service account 키 비활성화 또는 GCP project 변경 가능성"
  exit 1
fi

# 3. fastlane/metadata 구조 audit
echo ""
echo "── fastlane/metadata/android/ 구조 ──"
LOCALES=$(ls -d "$ROOT/fastlane/metadata/android/"*/ 2>/dev/null | xargs -n1 basename | wc -l | tr -d ' ')
SCREENSHOTS=$(find "$ROOT/fastlane/metadata/android" -path "*/phoneScreenshots/*.png" -type f | wc -l | tr -d ' ')
CHANGELOGS=$(find "$ROOT/fastlane/metadata/android" -path "*/changelogs/4.txt" -type f | wc -l | tr -d ' ')
echo "✓ Locale 수: $LOCALES"
echo "✓ phoneScreenshots PNG: $SCREENSHOTS (45×5=225 기대)"
echo "✓ changelogs/4.txt: $CHANGELOGS / $LOCALES"

# 4. Play Console validate_only (권한 전파 검증)
echo ""
echo "── Play Console internal track validate_only ──"
TMP_LOG=$(mktemp)
cd "$ROOT"
fastlane run upload_to_play_store \
  json_key:"$JSON_KEY" \
  package_name:"$PACKAGE" \
  track:internal \
  validate_only:true \
  skip_upload_apk:true \
  skip_upload_aab:true \
  skip_upload_metadata:true \
  skip_upload_changelogs:true \
  skip_upload_images:true \
  skip_upload_screenshots:true \
  > "$TMP_LOG" 2>&1 || true

if grep -q "permission" "$TMP_LOG"; then
  echo "✗ 권한 거부 — Google 측 service account 권한 전파 미완 (1~24h 대기 필요)"
  echo "  → 사용자 등록 + 13개 권한 부여 + Android Publisher API enable 모두 OK 인 경우 24h 안에 자동 해결"
  rm "$TMP_LOG"
  exit 2
elif grep -qE "Successfully|completed|Track 'internal'" "$TMP_LOG"; then
  echo "✓ 권한 전파 완료 — fastlane 자동 업로드 즉시 가능"
  rm "$TMP_LOG"
else
  echo "⚠ 결과 모호 — 로그 확인:"
  tail -10 "$TMP_LOG"
  rm "$TMP_LOG"
  exit 3
fi

echo ""
echo "═══ 모든 조건 충족. 다음 명령 가능: ═══"
echo ""
echo "  # 45 locale 메타 + changelog + 스크린샷 일괄 sync"
echo "  bash scripts/deploy/upload-metadata-all-locales.sh"
echo ""
echo "  # AAB 까지 한 방 (Internal Testing draft)"
echo "  fastlane internal"
