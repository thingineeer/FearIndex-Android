# Session State — FearIndex-Android

## Date
2026-05-08

## Version (계획)
- 다음 출시: `versionCode 4` / `versionName 1.0.3`
- AAB 아직 빌드 안 함 (현재 Play Console에는 v1.0.2 (versionCode 3) 게시됨)

## Branch / Worktrees

| Worktree | 경로 | 브랜치 |
|---|---|---|
| 본진 (dev) | `~/Desktop/side/FearIndex-Android` | `dev` |
| promo 합성본 | `~/Desktop/side/FearIndex-Android-promo` | `feature/v1.0.3-promo-graphics` |
| v103 raw + 자동화 | `~/Desktop/side/FearIndex-Android-v103` | `feature/v1.0.3-ads-locale-ui` |

세 worktree 모두 dev 동기화 완료. origin push 완료.

## ✅ Completed (이번 세션 / 2026-05-08)

### v1.0.3 promo 자동화 인프라
- `app/src/debug/` source set: `ScreenshotPushReceiver` (BroadcastReceiver, debug-only, release AAB 미포함)
- `AdBanner.isScreenshotMode()`: SystemProperties `debug.screenshot_mode=1` 시 빈 뷰 → 광고 차단
- `scripts/screenshots/capture-all-locales-v103.sh`: 45 locale × 5 화면 자동 촬영
  - push banner peek 0.7s window (cold-start 측정)
  - app cold-start 12s, 탭 전환 4s, 설정 nav 3s+4s
  - debug.screenshot_mode 자동 set/restore (trap)
- `scripts/screenshots/capture-en-push-only.sh`: 영어 push banner 만 별도 보충
- `scripts/screenshots/push_locales.json`: iOS Functions getNotificationMessage 와 동기화 (lower 조건, score=25)

### 합성 promo 이미지 정착 (claude.ai/design 결과)
- `~/Desktop/fastlane/metadata/android/<locale>/images/phoneScreenshots/*.png` (외부 합성본)
  → `fastlane/metadata/android/<locale>/images/phoneScreenshots/` 일괄 복사
- 45 locale × 5장 = **225장 정착** (모든 파일 정상)
- 구 v1.0.2 leftover 4파일 × 45 locale = 180개 정리
- en_US/`1_notification_en.png` raw 백업 제거 (합성본에 영어 banner 이미 포함)
- featureGraphic 은 이번 라운드 제외

### Git 정리 / 동기화
- `feature/v1.0.3-ads-locale-ui` 브랜치: 기존 11 commits + promo 자동화 3 commits + dev sync 머지
- `feature/v1.0.3-promo-graphics` 브랜치: 합성본 일괄 적용 + settings.local.json 추가
- `dev` ← `feature/v1.0.3-ads-locale-ui` (`f844092`, no-ff)
- `dev` ← `feature/v1.0.3-promo-graphics` (`0813ddc`, no-ff)
- `feature/v1.0.3-ads-locale-ui` ← `dev` (`3d4cbdf`, no-ff, 동기화)
- 모든 origin push 완료 (`origin/dev`, `origin/feature/v1.0.3-ads-locale-ui`, `origin/feature/v1.0.3-promo-graphics`)

### 기타
- `.claude/settings.local.json`: Agent Teams 실험 플래그 (`CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`) 추가

## ⏳ Remaining — v1.0.3 출시 작업

### 1. AAB 빌드
```bash
cd ~/Desktop/side/FearIndex-Android-v103
# versionCode 4, versionName 1.0.3 bump (app/build.gradle.kts)
./gradlew clean :app:bundleRelease
# 산출물: app/build/outputs/bundle/release/app-release.aab
```

### 2. Play Console 업로드
- Closed Testing Alpha 트랙 → 새 버전 만들기
- AAB 드래그&드롭
- `fastlane/metadata/android/<locale>/changelogs/4.txt` (45 locale 출시 노트 — ko_KR `4.txt` 만 작성됨, 나머지 44 locale 작성 필요)
- 스크린샷 업로드: `fastlane/metadata/android/<locale>/images/phoneScreenshots/*.png` (45 × 5 = 225장 합성본 사용)

### 3. dev → release 머지 + 태그
Production 통과 시:
```bash
git checkout release
git merge --no-ff dev
git tag v1.0.3
git push origin release v1.0.3
```

### 4. 14일 카운트다운
- 12명 테스터 opt-in 유지
- Production 신청 자격 획득

## Key Files (집에서 이어가기 위해 먼저 읽을 것)

| 파일 | 역할 |
|---|---|
| `CLAUDE.md` | 프로젝트 절대 규칙 |
| `.claude/memory/MEMORY.md` | 메모리 인덱스 |
| `.claude/memory/deployment.md` | AAB / Play Console 절차 |
| `.claude/memory/bugs-fixed.md` | 버그 이력 (15번까지) |
| `app/build.gradle.kts` | versionCode 3 → 4, versionName 1.0.2 → 1.0.3 bump 위치 |
| `scripts/screenshots/capture-all-locales-v103.sh` | 자동 촬영 스크립트 (필요 시 재실행) |
| `fastlane/metadata/android/ko_KR/changelogs/4.txt` | v1.0.3 ko 출시 노트 (다른 44 locale 추가 필요) |
| `app/src/debug/java/th1ngjin/fearindex/screenshot/ScreenshotPushReceiver.kt` | promo push 자동화 receiver |

## Notes

### 집 컴퓨터 셋업 (한 번)
```bash
# 1. clone (이미 했으면 skip)
gh repo clone thingineeer/FearIndex-Android ~/Desktop/side/FearIndex-Android
cd ~/Desktop/side/FearIndex-Android

# 2. fearindex-secrets 셋업 (~/.gradle/gradle.properties 자동 처리)
# AirDrop 으로 ~/fearindex-secrets/ 통째 복사 후
bash ~/fearindex-secrets/install.sh

# 3. thingineeer-env (텍스트 토큰)
gh repo clone thingineeer/thingineeer-env ~/thingineeer-env

# 4. worktree 셋업 (선택)
git worktree add ../FearIndex-Android-v103 feature/v1.0.3-ads-locale-ui
git worktree add ../FearIndex-Android-promo feature/v1.0.3-promo-graphics
```

### 합성 이미지 검증 (집에서)
```bash
# 모든 locale 5장 정상 여부 audit
DST=~/Desktop/side/FearIndex-Android/fastlane/metadata/android
for d in "$DST"/*/; do
  locale=$(basename "$d")
  count=$(ls "$d/images/phoneScreenshots/"*.png 2>/dev/null | wc -l)
  [ "$count" -ne 5 ] && echo "[BAD] $locale: $count files"
done
```

### 알려진 이슈
- iOS-Android Similar Events 카드 점수 불일치 (게이지 vs 마지막 historical) — 의도된 동작, v1.0.4 백로그
- v1.0.3 출시 노트 ko_KR 만 작성됨 (44 locale 추가 필요)

### Chrome MCP / fastlane supply
- Chrome MCP 로 Play Console 이미지 업로드 불가 (macOS 네이티브 다이얼로그)
- fastlane supply 자동화 위해서는 service account JSON 발급 필요 (`~/fearindex-secrets/play-store-service-account.json`)
