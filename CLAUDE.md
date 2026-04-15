# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 세션 시작 규칙

매 세션 시작 시 반드시 `.claude/memory/MEMORY.md`를 읽고 프로젝트 상태를 파악하세요.
사용자가 `/start`를 입력하면 브리핑을 출력합니다.

## Memory (프로젝트 로컬)

메모리는 **반드시 프로젝트 내 `.claude/memory/`에만** 저장합니다.
글로벌 `~/.claude/` 경로는 절대 쓰지 마세요 (여러 머신에서 git으로 동기화되므로).

- **메모리 읽기/쓰기 경로**: `.claude/memory/MEMORY.md` (프로젝트 루트 기준)
- **상세 파일**: `.claude/memory/bugs-fixed.md`, `.claude/memory/deployment.md`, `.claude/memory/ios-parity.md`, `.claude/memory/firebase-setup.md`
- 글로벌 auto-memory (`~/.claude/projects/.../memory/`)에는 **절대 쓰지 마세요**

## 브라우저 자동화 도구 선택

**Playwright 우선** (`mcp__playwright__*` 또는 `playwright` CLI):
- 한 번의 호출로 click/wait/fill/submit 시퀀스 실행 → tool call 수와 토큰 사용량을 1/3~1/5로 절감
- 공개 페이지, 로컬 개발 서버, 반복 가능한 E2E 시나리오, 스크래핑 등 **로그인 세션이 필요 없는 모든 작업**에 사용
- 가능하면 1 페이지 = 1 Playwright 스크립트로 묶어 호출

**Chrome MCP** (`mcp__claude-in-chrome__*`)는 다음 경우에만 사용:
- 사용자가 이미 로그인된 세션이 필요한 페이지 — Firebase Console, Google Play Console, AdMob, App Store Connect 등
- OAuth/SSO 기반 페이지에서 Playwright는 매번 재로그인이 필요하므로 비효율적
- Chrome MCP 사용 시에도 `read_page` → 여러 ref 한 번에 파악 → 가능한 한 묶어서 click 호출

새 세션에서 브라우저 작업 들어오면 위 분기 적용. 어떤 도구를 쓰는지 1줄로 사용자에게 알리고 진행.

**속도 원칙 (절대 규칙)**:
- Chrome MCP 한 동작당 별도 호출은 토큰/시간 낭비 → **반드시 묶을 것**
- 패턴 1: `read_page` 1회로 모든 ref 파악 → 한 텀에 click/type/click/click 연속 호출
- 패턴 2: `javascript_tool`로 여러 동작 한 번에 (DOM 조작, value 설정, dispatchEvent, submit)
- 페이지 로딩 후 첫 read_page만 wait 4초, 이후 연속 click은 wait 1~2초 또는 생략
- 같은 페이지에서 1동작당 1 tool call로 분해되면 → 즉시 멈추고 묶는 방식으로 재시도

## Build Commands

```bash
# Debug APK 빌드
./gradlew :app:assembleDebug

# Release AAB 빌드 (Play Console 업로드용)
./gradlew :app:bundleRelease

# 유닛 테스트
./gradlew test

# Instrumentation 테스트
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# 클린 빌드
./gradlew clean
```

## Project Overview

FearIndex-Android는 시장 공포지수(VIX, Fear & Greed Index 등)를 표시하는 Jetpack Compose 앱입니다.
iOS 프로젝트 `FearIndex-iOS`와 동일 프로덕트이며, **같은 Firebase 프로젝트 (`fear-index-a4f4b`)를 공유**합니다.

## Tech Stack

- **Android**: minSdk 26+ / targetSdk 최신
- **Kotlin**: 2.x
- **UI Framework**: Jetpack Compose + Material 3
- **DI**: Hilt (KSP)
- **Async**: Coroutines + Flow
- **Network**: Retrofit + OkHttp + kotlinx.serialization
- **Firebase**: Auth, Firestore, Functions, Cloud Messaging, Crashlytics
- **Ads**: AdMob (Google Mobile Ads SDK)
- **Architecture**: Clean Architecture (5 modules)

## Architecture

```
app (Android Entry, DI Wiring)
 └──→ presentation (Compose, ViewModels, Navigation)
         └──→ domain (Entities, UseCases, Repository Interfaces)
                 ↑
data (Repository Impl, DataSources, DTOs) ───┘
 └──→ core (Network, Logger, Config, Utils)
```

**의존성 방향** (Clean Architecture 엄수):
- `presentation → domain` (프로토콜만)
- `data → domain` (구현체)
- `app → data + presentation + domain` (DI 조립)
- `core`는 누구나 사용 가능, 어디에도 의존하지 않음

## Directory Structure

```
FearIndex-Android/
├── app/                    # Android 앱 진입점, Hilt DI 조립
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/th1ngjin/fearindex/   # Application, MainActivity, DI Module
│       └── res/                       # drawable, values (다국어 45개)
├── domain/                 # Entities, UseCase, Repository 인터페이스 (Pure Kotlin)
├── core/                   # Network, Logger, Config, Utils (Pure Kotlin/Android)
├── data/                   # Repository 구현, Retrofit, Firebase, DTO → Entity 매핑
├── presentation/           # Compose UI, ViewModel, Navigation, UI State
├── docs/                   # 프로젝트 가이드 (Google Play 배포 등)
├── fastlane/               # (예정) Play Console 배포 자동화
└── scripts/                # 빌드/배포 스크립트
```

## Package Convention (절대 규칙)

**모든 Kotlin 패키지는 반드시 `th1ngjin.fearindex.*`를 사용합니다.**

| 타겟 | 값 |
|------|---|
| `namespace` (R/BuildConfig) | `th1ngjin.fearindex` |
| `applicationId` (release) | `th1ngjin.fearindex` |
| `applicationId` (debug) | `th1ngjin.fearindex.debug` |
| 소스 경로 | `java/th1ngjin/fearindex/...` |

**금지 사항** (자주 발생하는 오타):
- `com.thingineer.*` — 구 이름, 사용 금지
- `com.thingineeer.*` — 오타, 사용 금지
- `th1ngjin.FearIndex` (대문자 혼용) — Android 패키지 관습상 소문자만

iOS/macOS와의 대칭:
- iOS: `th1ngjin.FearIndex-iOS`
- macOS: `th1ngjin.FearIndex-macOS`
- **Android: `th1ngjin.fearindex`** (Android 소문자 관습 준수)

자세한 이력과 배경은 `@.claude/rules/package-convention.md` 참조.

## Version Management

### 버전 형식

- `versionName`: Semantic Versioning (`major.minor.patch`), 현재 `1.0.0`
- `versionCode`: 정수, Play Console 업로드마다 증가 (현재 `1`)
- debug 빌드: `versionNameSuffix = "-debug"`

### 타겟별 버전

| 타겟 | ID | 위치 |
|------|-----|------|
| App (release) | `th1ngjin.fearindex` | `app/build.gradle.kts` |
| App (debug) | `th1ngjin.fearindex.debug` | `app/build.gradle.kts` (suffix) |

### 배포 체크리스트

```bash
# 1. versionCode, versionName bump (app/build.gradle.kts 수동 편집)
# 2. AAB 빌드
./gradlew clean :app:bundleRelease
# 3. 산출물: app/build/outputs/bundle/release/app-release.aab
# 4. Play Console 내부 테스트 트랙 업로드
```

## 다국어 지원 (45개 locale)

iOS와 **동일한 locale 코드**를 사용해야 메타데이터 동기화가 가능합니다.

| 그룹 | 디렉토리 (Android) |
|------|--------------------|
| 기본 | `res/values/strings.xml` (en) |
| 주요 7 | `values-ko`, `values-ja`, `values-de`, `values-zh-rCN`, `values-fr`, `values-es` |
| 유럽 | `values-af`, `values-bg`, `values-ca`, `values-cs`, `values-da`, `values-el`, `values-et`, `values-fi`, `values-hr`, `values-hu`, `values-it`, `values-lt`, `values-lv`, `values-nb`, `values-nl`, `values-pl`, `values-pt-rBR`, `values-pt-rPT`, `values-ro`, `values-ru`, `values-sk`, `values-sl`, `values-sr`, `values-sv`, `values-uk` |
| 아시아 | `values-ar`, `values-bn`, `values-fa`, `values-he`, `values-hi`, `values-id`, `values-ms`, `values-sw`, `values-ta`, `values-th`, `values-tr`, `values-vi`, `values-zh-rTW` |

**iOS와 locale 매핑 주의**:
- iOS `zh-Hans` → Android `values-zh-rCN`
- iOS `zh-Hant` → Android `values-zh-rTW`
- iOS `pt-BR` → Android `values-pt-rBR`
- iOS `nb` (Norwegian Bokmål) → Android `values-nb`

## Git Workflow (절대 규칙 — 모든 작업에 적용)

**단일 커밋이라도 worktree 단위로 피처 브랜치를 만들어 머지합니다. squash 머지 절대 금지.**

### 브랜치 구조 (FearIndex 표준)

```
release       ◄── (Play Store 배포 통과 시 dev 머지 + 태그)
  ↑
dev           ◄── (개발 기준선, 테스터 빌드 모음)
  ↑
feature/v1.x.x (버전 브랜치, dev에서 분기)
  ↑
feature/v1.x.x-기능A   (worktree 1, n개 커밋)  ──git merge──┐
feature/v1.x.x-기능B   (worktree 2, n개 커밋)  ──git merge──┼─→ feature/v1.x.x
feature/v1.x.x-기능C   (worktree 3, n개 커밋)  ──git merge──┘            │
                                                                        │
                                              모든 피처 머지 후:         │
                                                       dev ←─git merge──┘
                                                       │
                                          Play Store 배포 통과:
                                              release ←─git merge──┘ + tag v1.x.x
```

### 핵심 규칙

1. **`dev`가 개발 기준선** (`main` 아님). iOS와 동일하게 `dev`로 통일.
2. 새 버전 시작 시 **`dev` → `feature/v1.x.x` 버전 브랜치 분기**.
3. **모든 작업은 worktree 단위 피처 브랜치**에서. 한 worktree에 **n개 커밋** 가능 (의미 단위 묶음).
4. 작업 완료 → 버전 브랜치로 **`git merge --no-ff`** (`--squash` **절대 금지**). 반드시 분기/합류 그래프 유지.
5. 모든 피처 머지 → `dev`에 `git merge --no-ff`.
6. Play Store 배포 통과 → `release`에 `git merge --no-ff` + 태그.
7. **테스터 빌드는 `dev`에서** — Closed Testing 12명 14일 opt-in → Production 신청.
8. **각 worktree = 하나의 기능 단위**. worktree 안에서 n개 커밋 OK. 관련 없는 작업은 별도 worktree 분리.

### 절대 금지

| 금지 | 이유 |
|---|---|
| `git merge --squash` | n개 커밋 의미 손실, 부분 revert 불가 |
| `git cherry-pick` | 일자 히스토리 → 그래프 깨짐 |
| `gh pr merge` (squash 옵션) | 위와 동일 |
| `dev`/`release`/버전 브랜치 직접 커밋 | 반드시 worktree 거쳐야 함 |
| `git push --force` | 명시적 요청 없이 금지 |
| `--no-verify` | hook 건너뛰기 금지 |

### Worktree 사용 패턴

```bash
# 버전 브랜치에서 worktree 생성
git worktree add ../FearIndex-Android-share feature/v1.0.1-share-feature
git worktree add ../FearIndex-Android-charts feature/v1.0.1-crypto-5y

# 각 worktree에서 독립 작업 (n개 커밋)
cd ../FearIndex-Android-share
# ... commit, commit, commit
git push -u origin feature/v1.0.1-share-feature

# 작업 완료 → 본진으로 돌아와서 머지 (squash 절대 금지!)
cd ../FearIndex-Android
git checkout feature/v1.0.1
git merge feature/v1.0.1-share-feature
git merge feature/v1.0.1-crypto-5y

# worktree 정리
git worktree remove ../FearIndex-Android-share

# 모든 피처 합류 → dev 머지
git checkout dev
git merge feature/v1.0.1
```

### 작업 단위 분할 원칙

- **기능별** (예: `share-feature`, `crypto-5y`, `skeleton-loading`)
- **iOS 포팅 단위별** (한 iOS 컴포넌트 → 한 worktree)
- **버그 fix 별** (관련 없는 fix는 절대 한 브랜치에 묶지 말 것)
- 같은 worktree 안 커밋들은 **같은 머지 단위**여야 함

자세한 규칙: @.claude/rules/git-workflow.md

## Commit Rules

- **Author**: `thingineeer <dlaudwls1203@gmail.com>` (예외 없음)
- **Co-Authored-By 금지**: AI 관련 문구 (Claude, Copilot 등) 절대 포함 금지
- **메시지**: 한글 또는 conventional commits (`feat:`, `fix:`, `chore:`, `refactor:`)
- **단위**: 논리적 단위로 분리, 몰아 커밋 금지
- **push**: 명시적 요청 시에만
- HEREDOC 방식으로 작성

## Firebase

### 프로젝트 정보

- **프로젝트 ID**: `fear-index-a4f4b` (iOS와 공유)
- **리전**: `asia-northeast3` (서울)
- **Android 앱 등록**: `th1ngjin.fearindex` + `th1ngjin.fearindex.debug`
- **google-services.json**: `app/google-services.json`

### 공유 Cloud Functions (iOS와 동일)

| 함수 | 설명 |
|------|------|
| `checkFearIndexAndNotify` | 15분마다 공포지수 체크 및 푸시 발송 |
| `submitStuckStatus` | 물림 상태 제출 |
| `getStuckCount` | 물림 카운터 조회 |
| `registerFCMToken` | FCM 토큰 등록 |
| `updateNotificationSettings` | 알림 설정 업데이트 |
| `unregisterDevice` | 디바이스 등록 해제 |

### Firestore 구조 (iOS와 공유)

```
users/{deviceId}
├── fcmToken: string
├── notificationEnabled: boolean
├── lowerThreshold: number (기본 25)
├── upperThreshold: number (기본 75)
├── platform: "ios" | "android"  ← Android 추가 필드
└── language: string

stuckStatus/global_{indexType}   # 읽기 허용
stuckStatus/user_{deviceId}      # Admin SDK 전용
```

자세한 설정은 `@.claude/memory/firebase-setup.md` 참조.

## AdMob

| 용도 | ID |
|------|-----|
| App ID (production) | `ca-app-pub-5283496525222246~1308884877` |
| HomeBanner (production) | `ca-app-pub-5283496525222246/3189551565` |
| Test Banner (debug) | `ca-app-pub-3940256099942544/9214589741` |

**규칙**:
- debug 빌드는 **반드시 테스트 광고 단위**를 사용 (AdMob 정책 위반 방지)
- `buildConfigField`로 빌드 타입 분기 (`app/build.gradle.kts`에 선언됨)
- release AAB 빌드 전 반드시 App ID 확인

## Release Signing

- **Keystore 위치**: `~/fearindex-release.keystore` (프로젝트 외부, 절대 커밋 금지)
- **비밀번호**: `~/.gradle/gradle.properties` 내 `FEARINDEX_STORE_PASSWORD` 참조
- **Key Alias**: `fearindex`
- **SHA-1 / SHA-256**: Firebase Console에 등록 완료

자세한 배포 절차는 `@.claude/memory/deployment.md` 및 `@docs/GOOGLE-PLAY-INTERNAL-TEST.md` 참조.

## Coding Standards

- 함수 20줄 이하 (iOS는 10줄, Android는 Compose 특성상 완화)
- SOLID 원칙 준수
- 인터페이스 지향 설계, 모든 의존성은 생성자 주입 (Hilt)
- Coroutines + Flow 사용 (RxJava 금지)
- Compose에서 `remember`/`LaunchedEffect`/`collectAsStateWithLifecycle` 올바른 사용
- ViewModel → UseCase → Repository 레이어 엄수
- DTO와 Entity 분리 (Data 레이어 내부에서만 DTO 사용)

(향후 `@.claude/skills/android-dev-standards/` 스킬 추가 예정)

## iOS와의 대칭성

**모든 기능 변경은 iOS 프로젝트와 일관성을 유지해야 합니다.**

동기화 대상:
- 인사이트 카드 종류/문구 (Fear Velocity, Historical Return 등)
- 평점 문자열, 공포 레벨 임계값
- 차트 기간 라벨 (1W / 1M / 3M / 1Y)
- Firebase Analytics 이벤트 이름
- 다국어 키 이름 (iOS `Localizable.strings` ↔ Android `strings.xml`)
- 물림 카운터 (Stuck Status) 스텝/UX

변경 시 체크리스트는 `@.claude/memory/ios-parity.md` 참조.

## 참조

- @CLAUDE.md — 이 파일
- @.claude/memory/MEMORY.md — 메모리 인덱스
- @.claude/memory/bugs-fixed.md — 버그 이력
- @.claude/memory/deployment.md — 배포 가이드
- @.claude/memory/ios-parity.md — iOS 대칭성 체크리스트
- @.claude/memory/firebase-setup.md — Firebase 설정 가이드
- @.claude/agents/android-refactor-expert.md — 리팩터링 에이전트
- @.claude/agents/compose-ui-reviewer.md — Compose UI 리뷰 에이전트
- @.claude/agents/firebase-integration.md — Firebase 연동 에이전트
- @.claude/rules/package-convention.md — 패키지 명명 규칙
- @.claude/rules/git-workflow.md — Git 워크플로우 규칙
- @.claude/rules/ios-parity.md — iOS 대칭성 규칙
- @docs/GOOGLE-PLAY-INTERNAL-TEST.md — Play Console 배포 절차
