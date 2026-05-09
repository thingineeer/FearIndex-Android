#!/usr/bin/env bash
# 45 locale 메타데이터 + changelog + phoneScreenshots 를 Play Console 에 일괄 sync.
# AAB 와 icon/featureGraphic 은 별도 (Play Console 측 자산 그대로 유지).
#
# 사전 조건: bash scripts/deploy/check-fastlane-ready.sh 가 ✓ 통과해야 함.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JSON_KEY="$HOME/fearindex-secrets/play-store-service-account.json"
PACKAGE="th1ngjin.fearindex"

echo "═══ 45 locale 일괄 sync — Play Console ═══"
echo ""
echo "json_key: $JSON_KEY"
echo "package: $PACKAGE"
echo ""

cd "$ROOT"

# Internal Testing 트랙으로. AAB 는 별도 업로드 (skip_upload_aab=true).
fastlane run upload_to_play_store \
  json_key:"$JSON_KEY" \
  package_name:"$PACKAGE" \
  track:internal \
  validate_only:false \
  skip_upload_apk:true \
  skip_upload_aab:true \
  skip_upload_metadata:false \
  skip_upload_changelogs:false \
  skip_upload_images:true \
  skip_upload_screenshots:false \
  changes_not_sent_for_review:false

echo ""
echo "═══ sync 완료. Play Console 에서 변경사항 검토 후 출시 ═══"
