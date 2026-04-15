# Session State — FearIndex-Android

## Date
2026-04-15

## Branch
`dev` (v1.0.0 베이스라인 머지 완료, 베타 출시 진행 중)

## Active Worktrees

| 경로 | 브랜치 | 용도 |
|---|---|---|
| `/Users/imyeongjin/Desktop/side/FearIndex-Android` | `dev` | 본진 |
| `/Users/imyeongjin/Desktop/side/FearIndex-Android-share-and-gauge` | `feature/v1.0.0-share-and-gauge` | 다음 단위 작업 (게이지/공유/암호화폐 5Y) |

## Completed (v1.0.0 베이스라인)

### 🔧 프로젝트 세팅
- [x] 5개 모듈 Clean Architecture 풀 구현 (app/core/data/domain/presentation)
- [x] Kotlin 2.x + Jetpack Compose + Hilt + Retrofit + Firebase + AdMob
- [x] **Package 통일**: `com.thingineer` 오타 → `th1ngjin.fearindex` (+ `.debug`) — iOS `th1ngjin.FearIndex-iOS`와 대칭
- [x] compileSdk 36, JDK 21 toolchain

### 🎨 UI 구현
- [x] HomeScreen + FearGaugeView (270° gauge, 5색 segments, 햅틱 애니메이션)
- [x] ChartScreen (3M~5Y 드래그 상호작용 + 툴팁 + 스냅)
- [x] VoteScreen + StuckCounterCard (Firebase Functions 연동)
- [x] NotificationSettingsScreen (임계값 슬라이더)
- [x] SettingsScreen + 네비게이션
- [x] SegmentedPicker → Material 3 PrimaryTabRow
- [x] Bottom NavigationBar (4탭)
- [x] Light/Dark theme

### 🌍 다국어
- [x] 45개 locale strings.xml 자동 변환 (iOS Localizable → Android)
- [x] rating 5단계 통일 (iOS와 동일 번역)
- [x] share_message_template + share_chooser_title (14개 의미 번역 + 31개 영어 fallback)

### 🔥 Firebase
- [x] `fear-index-a4f4b` 프로젝트 공유 (iOS/macOS와 동일)
- [x] Android 앱 등록: `th1ngjin.fearindex` + `.debug`
- [x] `google-services.json` 배치
- [x] Analytics: AnalyticsEvent (iOS 1:1) + AnalyticsManager + NavHost 자동 트래킹
- [x] ChartScreen/VoteScreen 액션 이벤트 (차트기간선택, 지수타입전환, 투표참여, 투표탭진입)
- [x] Functions 공유 — `submitStuckStatus`, `getStuckCount` (asia-northeast3)

### 📱 AdMob
- [x] 실제 App ID 발급: `ca-app-pub-5283496525222246~1308884877`
- [x] HomeBanner 단위 ID: `ca-app-pub-5283496525222246/3189551565`
- [x] build.gradle.kts manifestPlaceholders + buildConfigField
- [x] `gma_ad_services_config.xml` (Manifest meta-data)

### 🔐 Release 배포 준비
- [x] Release keystore 생성 (`~/fearindex-release.keystore`, PKCS12)
- [x] `~/.gradle/gradle.properties` FEARINDEX_* 설정
- [x] AAB 빌드 + 서명 (`app-release.aab`, 9.5MB)
- [x] SHA-1 / SHA-256 fingerprint 추출

### 🏪 Google Play Console
- [x] FearIndex 앱 신규 생성 (`th1ngjin.fearindex`)
- [x] 내부 테스트 트랙 버전 1 (1.0.0) **업로드 완료**
- [x] 출시 노트 (다국어 en-US 대표)
- [x] "저장 및 출시" 클릭 완료 → Google 처리 중

### 🧪 테스트 & 도구
- [x] E2E 시나리오 9개 sh 스크립트 (adb + uiautomator)
- [x] Fastlane + 45 locale 메타데이터 + 스크린샷 자동화
- [x] `docs/GOOGLE-PLAY-INTERNAL-TEST.md` (335줄 수동 절차)

### 📚 문서/메모리 체계
- [x] `CLAUDE.md` (Git Workflow, Package 규칙, Firebase, AdMob, 브라우저 자동화 분기)
- [x] `.claude/memory/MEMORY.md` (인덱스 + 상수표 + @ 참조)
- [x] `.claude/memory/bugs-fixed.md` (9번까지 이슈 이력)
- [x] `.claude/memory/deployment.md`, `ios-parity.md`, `firebase-setup.md`
- [x] `.claude/rules/package-convention.md`, `git-workflow.md`, `ios-parity.md`
- [x] `.claude/agents/android-refactor-expert.md`, `compose-ui-reviewer.md`, `firebase-integration.md`
- [x] `.claude/settings.local.json` PreToolUse hook (구 package 차단)
- [x] `.claude/skills/resume-FearIndex-Android/SKILL.md` — 세션 복원용

### 🌿 Git
- [x] `dev` 브랜치 신규 생성 (main 베이스)
- [x] `feature/v1.0.0-baseline`에서 4개 의미 단위 commit
- [x] `dev`에 `--no-ff` merge → 분기/합류 그래프 형성
- [x] worktree `feature/v1.0.0-share-and-gauge` 생성 (다음 단위 대기)

## In Progress

### Google Play Console 내부 테스트 출시
- 출시 완료 클릭했으나 **테스터 미지정 경고** — 테스터 이메일 입력 필요
- 경로: https://play.google.com/console/u/0/developers/5351376807423705889/app/4973920645070208584/tracks/internal-testing → **"테스터" 탭**
- 사용자 결정 대기: 테스터 초대 진행

### `feature/v1.0.0-share-and-gauge` worktree
- FearGaugeView needle/텍스트 겹침 수정 (baseline에 포함됨, 실기기 재검증 필요)
- 공유 버튼 Intent.ACTION_SEND 구현 (baseline 포함, hosting URL 추가 예정)

## Remaining (v1.0.0 ~ v1.0.1)

### 🎯 v1.0.0 (베타 출시 완료 직전)
- [ ] Play Console 테스터 이메일 입력 (사용자 수동)
- [ ] 기존 오타 앱 "공포지수" (`com.thingineeer.fearindex` e3) 삭제 결정

### 🎯 v1.0.1 (iOS parity 포팅)
- [ ] **SkeletonView** — 로딩 스켈레톤 (1순위, 사용자 첫 인상)
- [ ] **MarketIndexTickerView 실제 데이터** — 현재 HomeScreen 코스피/S&P 500 mock 제거
- [ ] **VoteCardView / VoteCountdownView** — 정통 Buy/Hold/Sell 투표
- [ ] **시장/암호화폐 알림 설정 분리** — 통합 1개 화면 → 2개로
- [ ] **iOS hosting 공유 링크** — `https://fear-index-a4f4b.web.app/...` URL 포함
- [ ] **암호화폐 차트 5Y까지** — 현재 1W~1Y만. Alternative.me limit 최대 확인
- [ ] **데이터 정제 함수** — iOS의 `ChartDataFilter`, `ReturnDataInterpolator` 포팅
- [ ] **나머지 화면 Analytics 이벤트** — 공유, 알림, 인사이트 등

### 🎯 사용자 결정 대기
- "투표" 탭 이름 → "심리"로 변경 여부
- AdMob 테스트 디바이스 등록 (베타 테스터 디바이스 ID 수집 후)
- App Check 토큰 설정 (Play Integrity for release)

## Key Files

세션 복원 시 **이 파일들을 먼저 읽어야 전체 컨텍스트 파악 가능**:

### 프로젝트 루트
- `CLAUDE.md` — 프로젝트 규칙 (Git Workflow, Package, Firebase, AdMob)
- `.claude/memory/MEMORY.md` — 전체 인덱스 + 상수표 + @ 참조
- `.claude/memory/bugs-fixed.md` — 세션별 해결 이슈 이력 (9개)
- `.claude/memory/ios-parity.md` — iOS 대칭 + 누락 기능 분석 (v1.0.1 우선순위)
- `.claude/memory/deployment.md` — Keystore/AAB/Play Console 절차
- `.claude/memory/firebase-setup.md` — Functions/Firestore/App Check

### 핵심 코드
- `app/build.gradle.kts` — namespace, applicationId, signingConfigs, manifestPlaceholders
- `app/google-services.json` — Firebase Android 앱 2개 (prod + debug)
- `core/src/main/java/th1ngjin/fearindex/core/analytics/AnalyticsEvent.kt` — iOS 1:1 이벤트 sealed class
- `core/src/main/java/th1ngjin/fearindex/core/analytics/AnalyticsManager.kt` — Firebase Analytics 매니저
- `presentation/src/main/java/th1ngjin/fearindex/presentation/navigation/FearIndexNavHost.kt` — 화면 진입/탭 자동 트래킹
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/home/HomeScreen.kt` — 공유 Intent, TitleBar
- `presentation/src/main/java/th1ngjin/fearindex/presentation/component/FearGaugeView.kt` — 270° gauge

### 배포 가이드
- `docs/GOOGLE-PLAY-INTERNAL-TEST.md` — 335줄 Play Console 수동 절차

## Notes

### Firebase 공유 프로젝트
- Project ID: `fear-index-a4f4b` (iOS/macOS/Android 공유)
- Functions 리전: `asia-northeast3`
- Android 앱 2개 등록됨 (prod + debug) — iOS 2개 (iOS + macOS)와 병렬 관리
- **iOS/macOS 앱 삭제 절대 금지** (iOS 세션 유지 중)

### Release Signing
- Keystore 위치: `~/fearindex-release.keystore`
- 비밀번호: `~/.gradle/gradle.properties`의 `FEARINDEX_STORE_PASSWORD` (plain text로 어디에도 기록 금지)
- Key alias: `fearindex`
- SHA-1: `A1:54:8A:92:C3:AF:A5:0E:BD:31:F6:6B:47:1B:9E:BB:51:5D:23:51`
- SHA-256: `AD:48:68:DA:81:3C:9D:39:65:D0:C8:F9:59:62:61:6F:0A:6D:3A:BF:4E:21:DA:12:C0:DF:D8:2C:11:6A:14:0D`

### Play Console
- 개발자 계정 ID: `5351376807423705889`
- 앱 ID: `4973920645070208584` (FearIndex)
- 내부 테스트 트랙 ID: `4701735377174107144`
- **기존 오타 앱 잔존**: `com.thingineeer.fearindex` (e3) — 사용자 삭제 결정 대기

### AdMob
- App ID: `ca-app-pub-5283496525222246~1308884877`
- HomeBanner Unit ID: `ca-app-pub-5283496525222246/3189551565`

### Known Issues / Blockers
- `.kotlin/` 빌드 임시 디렉토리 gitignore 처리됨
- App Check debug token 미설정 — debug 빌드에서 Firestore 쓰기 실패 가능성 (베타 테스터 영향 없음, 내부 테스트만)
- Firebase 삭제 대기 중 3개 앱 (Skip, com.thingineer 오타 prod+debug) — 30일 후 영구 삭제

### iOS 프로젝트 위치
- `/Users/imyeongjin/Desktop/side/FearIndex-iOS` — Bundle ID `th1ngjin.FearIndex-iOS`
- iOS/Android 같이 작업 시 `@.claude/memory/ios-parity.md` 참조

## 다음 세션 시작 시
1. `/resume-FearIndex-Android` 실행 (위 스킬 자동 호출)
2. 브리핑 확인 후 "Ready to continue?" 응답
3. 우선순위: Play Console 테스터 입력 → v1.0.1 worktree 진행
