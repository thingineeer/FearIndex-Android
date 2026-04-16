# FearIndex-Android Memory Index

이 프로젝트의 **세션 간 공유되는 기록**을 모아두는 인덱스입니다. 세션 시작 시 반드시 먼저 읽습니다.

## 핵심 규칙 (절대 규칙)

- **Package name**: `th1ngjin.fearindex` (+`.debug`). `com.thingineer` / `com.thingineeer` 오타 **절대 금지**. 자세한 이력은 @rules/package-convention.md
- **iOS 대칭성**: 모든 기능은 iOS/macOS 프로젝트(`th1ngjin.FearIndex-iOS`, `th1ngjin.FearIndex-macOS`)와 **대시보드/Analytics/Crashlytics에서 일관**되어야 함. 상세: @memory/ios-parity.md
- **메모리 경로**: `.claude/memory/` 안에만. 글로벌 `~/.claude/projects/...` 절대 사용 금지.
- **Git**: 피처 브랜치 → 버전 브랜치 → main 머지. cherry-pick/force push 금지. 상세: @rules/git-workflow.md
- **Secrets**: `~/fearindex-secrets/` 폴더에만 (keystore / gradle.properties / google-services.json). 새 맥 셋업은 `bash ~/fearindex-secrets/install.sh`. 상세: @rules/secrets.md

## 문서 인덱스

### 메모리 (`.claude/memory/`)

- [Bugs Fixed](bugs-fixed.md) — 세션별 해결된 이슈 이력 (차트 기간/인터스티셜/다국어 등)
- [Deployment](deployment.md) — Keystore/AAB/Play Console/AdMob 상수와 절차
- [iOS Parity](ios-parity.md) — iOS와 동기화해야 할 항목 체크리스트
- [Firebase Setup](firebase-setup.md) — Firebase 프로젝트 구조, Functions, Firestore, App Check

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

## 세션 체크리스트

- [ ] `@memory/MEMORY.md` 읽음 (이 파일)
- [ ] Working branch 확인 (`git branch`)
- [ ] Recent commits 확인 (`git log --oneline -10`)
- [ ] 필요 시 `@memory/bugs-fixed.md`, `@memory/deployment.md`
