# FearIndex-Android Memory Index

이 프로젝트의 **세션 간 공유되는 기록**을 모아두는 인덱스입니다. 세션 시작 시 반드시 먼저 읽습니다.

## 핵심 규칙 (절대 규칙)

- **Package name**: `th1ngjin.fearindex` (+`.debug`). `com.thingineer` / `com.thingineeer` 오타 **절대 금지**. 자세한 이력은 @rules/package-convention.md
- **iOS 대칭성**: 모든 기능은 iOS/macOS 프로젝트(`th1ngjin.FearIndex-iOS`, `th1ngjin.FearIndex-macOS`)와 **대시보드/Analytics/Crashlytics에서 일관**되어야 함. 상세: @memory/ios-parity.md
- **메모리 경로**: `.claude/memory/` 안에만. 글로벌 `~/.claude/projects/...` 절대 사용 금지.
- **Git**: 피처 브랜치 → 버전 브랜치 → main 머지. cherry-pick/force push 금지. 상세: @rules/git-workflow.md
- **Secrets**: 두 저장소 분리 보관
  - 파일(keystore/gradle/google-services): `~/fearindex-secrets/` + install.sh
  - 텍스트 토큰(Firebase/AdMob/AppCheck): `~/thingineeer-env/projects/fearindex-android/.env` (GitHub private repo, 다른 머신 공유)
  - 상세: @rules/secrets.md + @memory/secrets-env.md

## 최신 상태 (2026-06-16, v1.2.0 배포)

- **현재 배포 기준**: Android `1.2.0` / `versionCode 16` / package `th1ngjin.fearindex`. **production 검토 전송 완료 (심사 대기)**.
- **v1.2.0 = iOS parity 대량 + 시장 상세 신규**: 차트 peak 고점/저점 마커, 홈 공유→Play 스토어 링크, SimilarEvents 점수 게이지 일치, 현재지수 info 버튼+KOSPI 장상태/업데이트시각, 암호화폐 비교 날짜기반 앵커, **시장 상세 화면(지수/환율/암호화폐 3탭)**. 상세: @memory/bugs-fixed.md 24~30번. TDD 위주 **585 테스트 통과**.
- **배포 절차 (2026-06-16)**: changelog 16(45 locale "전체적인 성능 및 개선을 하였습니다.") + 기존 광고없는 스크린샷 5장. `bundleRelease` 서명 SHA-1 `CE:08:B4:...`(UPLOAD.RSA) 일치. `fastlane production` HTTP 200 → Chrome MCP로 "검토 전송 136개" 클릭 완료. 상세: @memory/bugs-fixed.md 31번.
- **⚠️ 관리형 게시(Managed Publishing) ON**: fastlane은 "업로드+검토전송 대기"까지만 자동. ① 검토 전송 = Console "게시 개요"에서 수동 클릭(이번엔 MCP로 완료) ② **심사 통과 후에도 자동 게시 안 됨 → "게시" 버튼 1회 더 수동 클릭** 필요.
- **강제 업데이트 RC 타이밍 (사용자 승인)**: 현재 RC `force_update_minimum_version` [Android]=`1.1`(1.0.x만 강제), `minimum_app_version` Android=`1.1.3`, default fail-open(iOS용 1.6.0/1.8.2). **1.2.0 Play 전파 전엔 1.2로 안 올림**(전파 전 상향 시 받을 1.2.0 없어 In-App Update 폴백). **전파 확인 후** Android force_update=`1.1`→`1.2` 상향.
- **⚠️ 다음 세션 최우선** (순서대로):
  1. Play Console 1.2.0 **심사 통과** 확인 ("검토 중인 변경사항" → 승인).
  2. **관리형 게시이므로 "게시" 버튼 수동 클릭** → 실제 production 출시.
  3. Play Store **전파 확인** 후 **Firebase Console RC `force_update_minimum_version` [Android app users]=`1.2`** 설정 → 1.0.x/1.1.x 강제 업데이트 발동.
- **브랜치**: 모든 v1.2.0 작업은 `feature/v1.2.0-banner-clip-fix`에 커밋됨 (push/dev머지는 명시적 요청 대기). app ID `4973920645070208584`.
- **세션 저장**: 최신 resume 진입점은 @docs/checkpoints/SESSION-STATE.md.

## 문서 인덱스

### 메모리 (`.claude/memory/`)

- [Bugs Fixed](bugs-fixed.md) — 세션별 해결된 이슈 이력 (차트 기간/인터스티셜/다국어 등)
- [Deployment](deployment.md) — Keystore/AAB/Play Console/AdMob 상수와 절차
- [iOS Parity](ios-parity.md) — iOS와 동기화해야 할 항목 체크리스트
- [Firebase Setup](firebase-setup.md) — Firebase 프로젝트 구조, Functions, Firestore, App Check
- [Secrets Env](secrets-env.md) — `~/thingineeer-env/projects/fearindex-android/.env` 토큰/ID 저장 위치 및 키 목록

### 규칙 (`.claude/rules/`)

- [Package Convention](../rules/package-convention.md) — `th1ngjin.fearindex` 엄수, 오타 방지 원칙
- [Git Workflow](../rules/git-workflow.md) — 브랜치/머지/머지 방식
- [iOS Parity](../rules/ios-parity.md) — 변경 시 iOS 프로젝트와 일관성 유지 규칙
- [Secrets](../rules/secrets.md) — `~/fearindex-secrets/` 로컬 시크릿 폴더 규약, 새 맥 셋업 install.sh

### 에이전트 (`.claude/agents/`)

- [Android Refactor Expert](../agents/android-refactor-expert.md) — 모듈 구조/패키지 이동/Clean Architecture 리팩터링 전담
- [Compose UI Reviewer](../agents/compose-ui-reviewer.md) — Compose UI 코드 리뷰 + Material 3 가이드라인 점검
- [Firebase Integration](../agents/firebase-integration.md) — Firebase/Functions/Firestore/App Check 작업 전담

### 루트 문서

- [CLAUDE.md](../../CLAUDE.md) — 프로젝트 지침 (세션 시작 규칙 포함)
- [docs/GOOGLE-PLAY-INTERNAL-TEST.md](../../docs/GOOGLE-PLAY-INTERNAL-TEST.md) — Play Console 내부 테스트 배포 절차
- [docs/checkpoints/SESSION-STATE.md](../../docs/checkpoints/SESSION-STATE.md) — 최신 세션 상태 (resume 진입점)

## Fastlane Supply Locale 규격 (절대 규칙)

**Android `values-XX` (리소스 규격) ≠ Supply `XX_YY` (Play Console 메타 규격)**

| 용도 | 규격 | 예시 |
|---|---|---|
| Android strings.xml | `values-<lang>[-r<REGION>]` 하이픈 + `r` prefix | `values-ko`, `values-pt-rBR`, `values-zh-rCN`, `values-nb`, `values-iw`, `values-in` |
| fastlane metadata | `<lang>_<REGION>` 언더바 | `ko_KR`, `pt_BR`, `zh_CN`, `no_NO`, `iw_IL`, `id` |

**주요 매핑**:
- `values-nb` (Norwegian Bokmål) ↔ `no_NO`
- `values-iw` (Hebrew legacy) ↔ `iw_IL`
- `values-in` (Indonesian legacy) ↔ `id`
- `values-pt-rBR` ↔ `pt_BR`
- `values-zh-rCN` ↔ `zh_CN`
- `values-zh-rTW` ↔ `zh_TW`

**경로**:
- Play Store 메타: `fastlane/metadata/android/<supply_locale>/{title,short_description,full_description}.txt`
- 스크린샷: `fastlane/metadata/android/<supply_locale>/images/phoneScreenshots/{1_home,2_chart,3_vote,4_notification_settings}.png`
- 출시 노트: `fastlane/metadata/android/<supply_locale>/changelogs/<versionCode>.txt`

**글자수 제한 (Play Console 절대)**:
- title: 30자
- short_description: 80자
- full_description: 4000자
- changelog: 500자

## 45 Locale 자동 촬영

- **스크립트**: `scripts/screenshots/capture-all-locales.sh`
- **ANR 방지 필수**: `adb shell settings put global hide_error_dialogs 1` 먼저 실행 (연속 locale 전환 시 ANR dialog 자동 차단)
- **로직**: `adb shell cmd locale set-app-locales <PKG> --locales <BCP-47>` → 재시작 → `input tap` → `screencap`
- **소요**: 45 locale × ~30초 = 약 22분

## 간단 상수표

| 항목 | 값 |
|---|---|
| Firebase Project ID | `fear-index-a4f4b` |
| Android Production | `th1ngjin.fearindex` |
| Android Debug | `th1ngjin.fearindex.debug` |
| iOS | `th1ngjin.FearIndex-iOS` |
| macOS | `th1ngjin.FearIndex-macOS` |
| Functions 리전 | `asia-northeast3` |
| Functions 엔드포인트 | `submitStuckStatus`, `getStuckCount` |
| Keystore 위치 | `~/fearindex-secrets/fearindex-release.keystore` |
| Keystore 비밀번호 저장소 | `~/fearindex-secrets/gradle.properties` → `~/.gradle/gradle.properties` (install.sh 복사) |
| google-services.json 원본 | `~/fearindex-secrets/google-services.json` → `app/google-services.json` 심볼릭 링크 |
| AdMob App ID | `ca-app-pub-5283496525222246~1308884877` |
| AdMob HomeBanner | `ca-app-pub-5283496525222246/3189551565` |
| AdMob KospiInterstitial | `ca-app-pub-5283496525222246/1522532479` |

## 세션 체크리스트

- [ ] `@memory/MEMORY.md` 읽음 (이 파일)
- [ ] Working branch 확인 (`git branch`)
- [ ] Recent commits 확인 (`git log --oneline -10`)
- [ ] 필요 시 `@memory/bugs-fixed.md`, `@memory/deployment.md`
