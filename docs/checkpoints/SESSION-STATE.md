# Session State - FearIndex-Android

## Date
2026-06-15

## Branch
`release` (체크아웃 상태). dev/release 모두 origin과 동기화 완료.

## Release Status
- **현재 배포 기준**: Android `1.1.2` / `versionCode 14` / package `th1ngjin.fearindex`.
- **Play Console production 트랙**: `1.1.2` 177개국 100% rollout, `release_status: completed`. **사용자가 관리형 게시(managed publishing) "변경사항 출시" 클릭 완료** → 출시 처리됨 (Google 심사는 백그라운드 진행).
- **배포 방식**: `bundle exec fastlane production` → HTTP 200 성공 (AAB + 45 locale 메타/changelog/스크린샷 일괄 업로드). service account JSON: `~/fearindex-secrets/play-store-service-account.json`.
- **태그**: `v1.1.2` 생성 + push 완료.

## 이번 세션에서 한 일 (v1.1.2 — 강제 업데이트 / AdMob 정책 대응)
- [x] **AdMob 정책 문제 진단** (Chrome MCP, 사용자 로그인 세션):
  - 정책 센터 URL: `https://admob.google.com/v2/policycenter/issues/details/app/1/th1ngjin.fearindex`
  - **광고 게재 제한됨** (1.7천 요청/7일, 신고 2026-06-09). 문제 유형 `발견된 문제(이전 버전)`, 샘플 버전 `1.0.1`, 정책 문제 `광고 프레임 크기 변경`.
  - AdMob 권장 해결책 = "사용자를 최신 버전으로 업데이트 유도" → 강제 업데이트 구현으로 대응.
- [x] **iOS force-update 패턴 포팅** (iOS가 SSOT, 읽기 전용 준수):
  - `core/.../update/UpdateChecker.kt` + `UpdateStatus.kt`: iOS `RemoteConfigManager.checkForUpdate()` 로직 (major.minor 강제 비교, 전체버전 선택 비교).
  - `core/.../remoteconfig/RemoteConfigManager.kt`: `force_update_minimum_version` / `minimum_app_version` 키 추가 (iOS 동일). 코드 default 빈 문자열 = 차단 안 함.
  - **TDD**: `UpdateCheckerTest.kt` 9개 케이스 (RED→GREEN 확인).
- [x] **Play In-App Update IMMEDIATE**: `app/.../update/InAppUpdateManager.kt` (의존성 `com.google.android.play:app-update(-ktx):2.1.0`). 스토어에 새 버전 없으면 `market://` 폴백.
- [x] **강제 업데이트 UI**: `presentation/.../feature/update/ForceUpdateView.kt` (iOS 레이아웃 대칭, BackHandler dismiss 차단).
- [x] **MainActivity 게이트**: RemoteConfig fetch → 강제 판정 → ForceUpdateView + IMMEDIATE 플로우 (debug `-debug` suffix 제거 후 비교).
- [x] **45 locale 다국어**: `force_update_title/message/button` (iOS `force.update.*` 값 동일).
- [x] **45 locale changelog**: `fastlane/metadata/android/*/changelogs/14.txt` (안정성 개선 + 업데이트 유도 메시지).
- [x] **versionCode 14 / versionName 1.1.2** bump.
- [x] **빌드/배포**: `./gradlew test` 통과, `./gradlew clean :app:bundleRelease` 성공(서명됨), `fastlane production` HTTP 200.
- [x] **Git workflow**: worktree 단위 feature 브랜치 → `feature/v1.1.2` → `dev` → `release` 모두 `--no-ff` 머지 (squash 금지 준수). 분기/합류 그래프 유지. 태그 `v1.1.2`.
- [x] **브랜치 정리**: `feature/v1.1.2`, `feature/v1.1.2-changelog`, `feature/v1.1.2-force-update` 로컬 삭제 (release 머지 확인 후). dev/release/v1.1.2 push 완료.

## In Progress
- 없음 (source code 작업 완료).
- Play Console production `1.1.2` 심사 진행 중 — 다음 세션에서 게시 완료 여부 재확인.

## Remaining (다음 세션 — 중요도 순)
1. **⚠️ 가장 중요**: 1.1.2 심사 통과/게시 후 **Firebase Console Remote Config에서 `force_update_minimum_version=1.1` 설정**. 이걸 해야 1.0.x 유저에게 강제 업데이트가 실제 발동한다. (코드 default는 빈 값이라 지금은 아무도 차단 안 됨 = 안전.)
   - 동시에 `minimum_app_version=1.1.2`도 설정 권장 (선택 업데이트 안내용).
   - Remote Config 조건: `platform == android` (iOS와 키 공유, 조건부 값으로 분기).
2. 1.1.2 심사 통과 확인 (production 트랙 `검토 중` → 게시 완료).
3. **AdMob 정책 센터 재확인** — 1.0.x 사용자가 1.1.2+로 전환되면 광고 게재 제한 자연 해소. AdMob Privacy & messaging form 설정(`ca-app-pub-5283496525222246~1308884877`)도 별건으로 남아있음 (배너 미노출 이슈, SESSION 이전 항목).
4. Firebase Remote Config 광고 gate 확인: `ads_enabled`, interstitial flags.
5. Local 미추적 파일(`.agents/`, `.codex/`, `AGENTS.md`)은 로컬 agent/config로 판단해 스테이징 안 함 (사용자 명시 요청 시에만).

## Key Files (이번 세션 추가/변경 — @ 참조)
- @CLAUDE.md - 프로젝트 규칙, Android-only scope, 메모리 경로, git workflow.
- @docs/checkpoints/SESSION-STATE.md - 이 save point (resume 진입점).
- @.claude/memory/MEMORY.md - 메모리 인덱스 + 상수표.
- @.claude/memory/bugs-fixed.md - **20번 항목**: AdMob 정책 + 강제 업데이트 전체 이력.
- @.claude/memory/deployment.md - 출시 이력에 v1.1.0~v1.1.2 추가됨, fastlane 절차.
- @.claude/memory/ios-parity.md - iOS 대칭성 체크리스트 (force-update도 iOS가 SSOT).
- @.claude/memory/firebase-setup.md - Firebase / Remote Config 설정.
- @core/src/main/java/th1ngjin/fearindex/core/update/UpdateChecker.kt - 버전 판정 순수 로직 (iOS checkForUpdate 포팅).
- @core/src/main/java/th1ngjin/fearindex/core/update/UpdateStatus.kt - UP_TO_DATE / UPDATE_AVAILABLE / FORCE_UPDATE_REQUIRED.
- @core/src/test/java/th1ngjin/fearindex/core/update/UpdateCheckerTest.kt - TDD 9개 케이스.
- @core/src/main/java/th1ngjin/fearindex/core/remoteconfig/RemoteConfigManager.kt - force_update_minimum_version / minimum_app_version 키 + checkForUpdate().
- @app/src/main/java/th1ngjin/fearindex/update/InAppUpdateManager.kt - Play In-App Update IMMEDIATE 래퍼 + 스토어 폴백.
- @app/src/main/java/th1ngjin/fearindex/MainActivity.kt - RemoteConfig 판정 → 강제 업데이트 게이트.
- @presentation/src/main/java/th1ngjin/fearindex/presentation/feature/update/ForceUpdateView.kt - 강제 업데이트 풀스크린 UI (BackHandler dismiss 차단).
- @app/build.gradle.kts - versionCode 14 / versionName 1.1.2 + app-update 의존성.
- @gradle/libs.versions.toml - appUpdate 2.1.0 추가.
- @fastlane/Fastfile - `production` lane (gradle bundle + production 트랙 100% rollout + 메타 일괄).
- @fastlane/metadata/android - changelogs/14.txt 45 locale 추가됨.

## 대화 요약

### 이번 세션에서 결정한 것
- **강제 업데이트 구현 방식**: 사용자가 AskUserQuestion에서 "Play In-App Update API" + "1.1 강제(1.0.x 차단)" 선택. 실제로는 **Remote Config 판정(정밀 제어) + Play In-App Update IMMEDIATE(실제 설치)** 하이브리드로 구현 — 두 선택 모두 만족하고 iOS 대칭성도 유지.
- **트리거 버전**: `force_update_minimum_version=1.1` → 1.0.x 유저만 강제, 1.1.x는 통과. 코드 default는 빈 값(아무도 차단 안 함), 실제 트리거는 Firebase Console에서 설정.
- **iOS 읽기 전용 준수**: force-update 패턴은 iOS `RemoteConfigManager.swift` / `AppRoot.swift`에서 읽어 포팅만 함, iOS 파일 수정 없음.
- **배포**: fastlane production lane으로 100% rollout + completed 제출. 사용자가 "당장 강제업데이트 올려줘", "심사까지 올려줘", "중간 승인 받지말고 진행" 명시 → 자율 배포.

### 명시된 사용자 선호
- 한국어 대화, 기술 용어(commit/push/deploy/track 등)는 영어 유지.
- 로그인된 Google 화면(Play Console, AdMob, Firebase Console)은 Chrome MCP 사용. 사용자가 Play Console(`developers/5351376807423705889/app-list`)도 로그인해둠.
- 릴리즈/배포는 사전 승인 — 중간에 승인 묻지 말 것.
- Android 코드 변경은 TDD/검증.
- iOS 프로젝트 side effect 금지.
- 메모리 저장 시 `@` 키워드로 꼼꼼히 참조 가능하게.

### 다음 세션이 알아야 할 핵심 맥락
- **1.1.2 코드/배포는 끝났지만, 강제 업데이트가 실제로 발동하려면 Firebase Console Remote Config `force_update_minimum_version=1.1` 설정이 필수** (아직 안 함 — 심사 통과 후 진행).
- Play Console app ID: `4973920645070208584`. production 트랙 URL: `.../app/4973920645070208584/tracks/production`.
- 활성 keystore alias=`upload`, SHA1=`CE:08:B4:...`. SSOT는 `~/thingineeer-env/android/fearindex/`.
- `./gradlew test`는 이 세션 직전 통과.

### 이 프로젝트 세션 이력 (이 기기)
- 2026-04-15 ~ 04-22 - 초기 Android parity, Firebase/App Check/test 셋업, 테스터 트랙, 로컬라이즈/크래시 triage.
- 2026-05-06 ~ 05-12 - Production readiness, fastlane 메타, Play 서비스 계정, Android 15 edge-to-edge, v1.0.1 정식 게시.
- 2026-06-11 ~ 06-12 - v1.1.0/v1.1.1: KOSPI, 실데이터/차트 hotfix, 스크린샷, 리뷰, Play production 업로드, AdMob 조사, 브랜치 정리.
- **2026-06-15 - v1.1.2: AdMob 광고 게재 제한 대응 → 강제 업데이트(Play In-App Update + Remote Config) 구현, 45 locale, production 100% rollout 제출, 브랜치 정리/push.**

## Notes
- Test result before this save: `./gradlew test` 통과.
- Branches after cleanup:
  - local: `main`, `dev`, `release` (모두 origin 동기화)
  - remote: `origin/main`, `origin/dev`, `origin/release` + 태그 `v1.0.1`, `v1.1.0`, `v1.1.2`
- AdMob IDs: App `ca-app-pub-5283496525222246~1308884877`, HomeBanner `.../3189551565`, KospiInterstitial `.../1522532479`.
- Release signing / Play service account secrets는 repo 외부 private workflow 관리. 커밋 금지.
