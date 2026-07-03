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
- **DUNS / 사업자 (2026-06-26 발급)**: D-U-N-S Number = **`696610806`**, 사업자 상호(Legal Business Name) = **`ImaJine`(이매진)**. 조직 계정(Apple/Google Play) 전환용. 상세 + 전환 절차: @memory/org-account.md

## 최신 상태 (2026-07-03, v1.3.0 배포)

- **현재 배포 기준**: Android `1.3.0` / `versionCode 17` / package `th1ngjin.fearindex`. **production 게시 완료 ✅** (2026-07-03 당일 심사 통과, 관리형 게시 "게시"는 사용자 수동 클릭). `release` 머지 + `v1.3.0` 태그 완료.
- **v1.3.0 = RSI/공매도 투자 지표 신규**: 3자산(S&P500/KOSPI/BTC) 가격 RSI(14, Wilder) + 공매도 동향 카드 + 설명 시트. KOSPI 종가는 기존 스냅샷 `kospiClose` 재사용(추가 API 0). TDD 신규 45개, 전체 GREEN. 상세: @memory/bugs-fixed.md 34번.
- **FCM 정책 개편 검증 완료 (33번)**: Android는 payload 그대로 표시 — 코드 무수정 확인, 실수신 테스트 통과. ⚠️ **서버 전달 필요**: `updateNotificationSettings`에 즉시체크 훅 없음 → Android 신규 유저 즉시 알림 못 받음(30분 cron 폴백).
- **서버/기존 이슈 (미해결)**: ① `/api/kospi/short` 미집계 당일 `0` → "0.0% 숏커버링" 오신호 (iOS 동일 영향) ② KOSPI SimilarEvents에 `insight.kospi.event.tradeWar2018` raw key 노출 (기존 버그, fix 대상) ③ 에뮬레이터 /data 400MB 부족 — 정리 필요.
- **RC 게이트**: `force_update_minimum_version` [Android]=`1.2` 유지 (1.3.0 통과, 조치 불필요). `minimum_app_version` Android=`1.1.3` 유지.
- **다음 세션 (선택)**: SimilarEvents raw key fix, AdMob 정책센터 재확인, push (모든 브랜치 로컬 상태 — 명시 요청 대기), 서버 팀 전달 2건.
- **세션 저장**: 최신 resume 진입점은 @docs/checkpoints/SESSION-STATE.md.

## 문서 인덱스

### 메모리 (`.claude/memory/`)

- [Bugs Fixed](bugs-fixed.md) — 세션별 해결된 이슈 이력 (차트 기간/인터스티셜/다국어 등)
- [Deployment](deployment.md) — Keystore/AAB/Play Console/AdMob 상수와 절차
- [iOS Parity](ios-parity.md) — iOS와 동기화해야 할 항목 체크리스트
- [Firebase Setup](firebase-setup.md) — Firebase 프로젝트 구조, Functions, Firestore, App Check
- [Secrets Env](secrets-env.md) — `~/thingineeer-env/projects/fearindex-android/.env` 토큰/ID 저장 위치 및 키 목록
- [Org Account](org-account.md) — 조직 계정 전환 + DUNS(`696610806`/ImaJine, Apple 전용). ⚠️ Google Play 14일 테스트 규칙은 조직 전환과 무관 — 재검증 필요

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
