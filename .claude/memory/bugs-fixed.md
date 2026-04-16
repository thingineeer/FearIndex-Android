---
name: Bugs Fixed
description: 세션별로 해결된 버그 이력. 같은 문제 재발 방지용.
type: project
---

# Bugs Fixed

## 2026-04-14 세션

### 1. 차트 기간 변경 시 인터스티셜 광고 노출

- **증상**: 차트 탭에서 3M/6M/1Y/2Y/3Y/5Y 기간 버튼 5회째 탭 시 AdMob 인터스티셜 튀어나옴.
- **원인**: AdMob 연동 에이전트가 임의로 `ChartScreen`에 `INTERSTITIAL_TRIGGER_INTERVAL = 5` 로직 삽입.
- **해결**: `ChartScreen.kt`에서 `InterstitialAdManager.loadAd`, `showIfReady`, `findActivity`, `periodClickCount` 관련 코드 전부 제거. HomeScreen의 배너만 유지.
- **재발 방지**: 사용자가 명시적으로 요청하지 않은 광고 트리거 추가 금지.

### 2. 탭 재진입 시 차트 기간 상태 불일치

- **증상**: 5Y 선택 → 홈 탭 이동 → 차트 탭 복귀 시 "3M 버튼 선택" 표시인데 차트는 5년치 데이터.
- **원인**: `ChartLoadedContent` 내부에서 `var selectedMarketPeriod by remember { mutableStateOf(THREE_MONTHS) }` — 탭 이동 시 Composable destroy → state 초기화. ViewModel의 `marketHistoryDays`는 살아있어 데이터 불일치.
- **해결**: `ChartPeriod.fromDays(days)` 역산 헬퍼 추가 → `ChartScreen`에서 `uiState.marketHistoryDays`로부터 현재 기간 계산. 로컬 `remember`는 제거, 파라미터 주입 방식으로 변경.
- **교훈**: UI 상태 중 ViewModel에 SSOT로 있는 값은 `remember`로 중복 보관하지 말 것.

### 3. 한국어 locale인데 "Fear" 등 영어 문자열 노출

- **증상**: 앱이 ko-KR locale인데 차트 현재 지수 라벨에 "Fear" 영어로 표시.
- **원인**: `ChartScreen.kt`, `VoteScreen.kt`, `FearGaugeView.kt` 세 곳 모두 `ratingLabel(score)` 함수가 하드코딩(심지어 `ChartScreen`은 영어, `VoteScreen`은 한국어로 불일치).
- **해결**:
  1. `presentation/res/values{-ko,-ja,...}/strings.xml` 에 45개 locale 모두 `rating_extreme_fear` 등 5개 키 추가 (iOS `Localizable.strings`에서 Python으로 일괄 변환).
  2. `presentation/common/RatingLabels.kt` 공통 Composable 헬퍼 생성 (`@Composable fun ratingLabel(score: Int): String`).
  3. 3개 파일의 하드코딩 함수 제거, 공통 헬퍼 import로 교체.
  4. `ChartCard` 내 Canvas draw 코드는 `@Composable` 외부라 `stringResource` 불가 → `Array<String>` 5개를 상위에서 주입받는 방식(`ratingLabelFromArray`)으로 처리.
- **교훈**: 다국어 문자열은 중복 선언 금지, 반드시 strings.xml로 통일.

### 4. Android package 오타 (`com.thingineer` vs `com.thingineeer`)

- **증상**: Firebase/Play Console에 `com.thingineer.fearindex`(e 2개, 오타)로 앱 등록됨. 다른 Android 앱들(1 Problem, FLIPOP)은 `com.thingineeer`(e 3개). iOS는 `th1ngjin.FearIndex-iOS`.
- **결론**: iOS와 대칭을 맞추기 위해 **`th1ngjin.fearindex`**로 최종 통일. (e 2개 / e 3개 모두 과거 타이핑 오류)
- **해결**:
  1. 전체 소스 `com.thingineer.fearindex` → `th1ngjin.fearindex` 치환 (`/tmp/repackage_android.sh`).
  2. 5개 모듈 `build.gradle.kts`의 `namespace` 수정.
  3. 디렉토리 이동 (`java/com/thingineer/fearindex/` → `java/th1ngjin/fearindex/`).
  4. Firebase 3개 앱 삭제 (Skip, e2개 prod, e2개 debug) + 2개 재등록 (`th1ngjin.fearindex`, `.debug`).
  5. `google-services.json` 재다운로드.
  6. Play Console에서도 재생성 필요 (세션 종료 시점 진행 중).
- **재발 방지**: @rules/package-convention.md 참고. PreToolUse hook으로 오타 경로 차단 시도.

### 5. 알림 설정 화면 진입 불가

- **증상**: 설정 탭에서 "알림 설정" 클릭해도 아무 일 없음.
- **원인**: 화면 자체가 존재하지 않았고, NavHost에도 route 없었음.
- **해결**: `presentation/feature/notification/NotificationSettingsScreen.kt` 신규 생성 (마스터 토글 + 시장/암호화폐 임계값 슬라이더). `FearIndexNavHost`에 `notification_settings` route 추가, `SettingsScreen`이 `onNotificationSettingsClick` 파라미터 받도록 수정.

### 6. SegmentedPicker 상단 탭 — Material 가이드라인 어긋남

- **증상**: 상단 "시장/암호화폐" 세그먼트가 iOS 스타일(둥근 박스 내부 inditcator)인데 Android 사용자에게 어색.
- **해결**: Material 3 `PrimaryTabRow` + `Tab`으로 교체. 인디케이터(underline), ripple, role=Tab 자동 적용.

### 7. 다른 이슈

- **테스트 툴바 광고 단위** — 과거에 `ca-app-pub-3940256099942544/...` (Google 공식 테스트 ID) 사용. 실 프로덕션 AdMob 단위 ID로 교체 완료 (2026-04-14): `ca-app-pub-5283496525222246/3189551565`.
- **Stuck Counter Firebase Functions** — 이미 배포되어 있음 (`submitStuckStatus`, `getStuckCount`, `asia-northeast3`). 재배포 불필요.

## 2026-04-15 세션 (/loop iteration)

### 8. Firebase Analytics 미연동

- **증상**: Android 앱이 화면 진입/탭 클릭 등에 대해 어떤 Analytics 이벤트도 발송하지 않음. 베타 출시 후 사용 데이터 수집 불가.
- **해결**:
  1. `core` 모듈에 `firebase-analytics-ktx` 의존성 추가 (BOM 사용).
  2. `core/analytics/AnalyticsEvent.kt` — iOS `AnalyticsEvent.swift`와 1:1 매핑된 sealed class 신규. **이벤트 이름 한국어 그대로 유지** (Firebase Console 대시보드 공유 목적).
  3. `core/analytics/AnalyticsManager.kt` — `@Singleton` Hilt 컴포넌트. `log(event)`, `logScreen(screen)` 메서드.
  4. `presentation/di/AnalyticsEntryPoint.kt` — Hilt EntryPoint. NavHost 같은 ViewModel 외부에서 접근.
  5. `presentation/navigation/FearIndexNavHost.kt` — 화면 진입 시 `logScreen`, 탭 클릭 시 `AnalyticsEvent.탭선택` 자동 발송.
- **iOS Parity**: `AnalyticsEvent.수동새로고침`, `차트기간선택`, `투표참여` 등 모든 케이스 동일 이름. 화면 이름도 `홈/차트/투표/설정/알림설정` iOS와 동일.
- **남은 작업**: ChartScreen에 `차트기간선택`, VoteScreen에 `투표참여`, 광고 컴포넌트에 `배너광고노출/클릭` 등 화면별 액션 이벤트 박기.

### 9. Git 브랜치 워크플로우 누적 변경 71 파일 — worktree 단위 분할 커밋

- **증상**: package 재정비 + 신규 화면 + Analytics + 다국어 + 문서까지 71개 변경이 단일 워킹 트리에 섞여 있음. 사용자가 squash merge 절대 금지 + worktree 단위 작업 요청.
- **해결 (2026-04-15)**:
  1. `dev` 브랜치 신규 (main 베이스).
  2. `feature/v1.0.0-baseline` 피처 브랜치에서 **의미 단위 4개 커밋**:
     - feat: package 통일 + Clean Architecture 풀 구현
     - i18n: 45 locale strings.xml + share 다국어
     - docs: 프로젝트 문서/메모리/도구
     - chore: .kotlin/ gitignore
  3. `dev`에 `--no-ff` merge → 분기/합류 그래프 형성.
  4. 다음 작업용 worktree 생성: `feature/v1.0.0-share-and-gauge`.
- **재발 방지**: 이후 모든 작업은 worktree 단위로 시작. 한 worktree = 하나의 의미 단위 (n개 커밋 OK, 관련 없는 작업은 별도 worktree).

## 2026-04-16 세션

### 10. 알림 설정 서버 동기화 실패 "INTERNAL"

- **증상**: 알림 설정 화면에서 슬라이더 드래그 → Snackbar "서버 동기화 실패: INTERNAL" 노출. FCM 토큰 등록도 language 누락.
- **원인**: `NotificationDataSource.updateSettings`가 **평탄 payload** (`{ deviceId, lowerThreshold, ... }`) 전송. Functions 서버는 **중첩 payload** (`{ deviceId, settings: { lowerThreshold, ... } }`) 기대. `settings.lowerThreshold`가 `undefined` → `validateThresholds` 내부 에러 → `INTERNAL` 반환.
- **해결**:
  1. `NotificationDataSource.kt`를 iOS `FCMService.swift` payload 구조와 동일하게 변경.
  2. `settings` 중첩 객체 안에 `lowerThreshold`, `upperThreshold`, `cryptoLowerThreshold`, `cryptoUpperThreshold`, `notificationEnabled`, `cryptoNotificationEnabled`, `language` 포함.
  3. 클라이언트 클램핑 추가: lower 0..50, upper 50..100 (iOS 동일).
  4. `Locale.getDefault().language` 2자리 ISO 639-1 코드 사용 (iOS `Locale.current.language.languageCode?.identifier`와 매칭).
- **검증**: 에뮬레이터에서 슬라이더 드래그 → `Notification settings updated` 로그만 출력, 에러 사라짐.
- **교훈**: Firebase Callable Function 시그니처는 **iOS 클라이언트가 원본**. Android는 payload 구조를 iOS와 1:1 동기화해야 함.

### 11. Vote Firestore 스냅샷 스트림 PERMISSION_DENIED (iOS+Android 공통)

- **증상**: 투표 탭 진입 시 `W Firestore: Listen for Query(target=Query(votes/2026-04-16/results/market) failed: PERMISSION_DENIED`. VoteViewModel에서 vote stream error.
- **원인**: `firestore.rules`의 `match /{document=**} { allow read, write: if false; }`가 `votes/**`를 차단. iOS `VoteDataSource.swift:72`도 같은 path (`votes/{date}/results/{indexType}`)를 addSnapshotListener로 읽고 있어 **iOS도 같은 에러 발생 중**.
- **검증**:
  - `node scripts/admin.js` (admin SDK로 직접 조회) → 오늘 날짜 문서 존재 여부 못 가져옴.
  - 배포된 rules (Firebase Rules API 직접 조회)와 로컬 `firestore.rules` 완전 일치.
- **해결** (2026-04-16):
  - iOS `firebase-functions/firestore.rules`에 다음 블록 추가 후 `firebase deploy --only firestore:rules`:
    ```
    match /votes/{date}/results/{indexType} {
      allow read: if true;
      allow write: if false;
    }
    ```
  - 쓰기는 여전히 차단 → `submitVote` Callable Function 경유 유지.
  - Android 에뮬레이터에서 투표 탭 재진입 → PERMISSION_DENIED 완전 사라짐.
- **관찰**: `stuckStatus/global_*` + `insights/similarEvents_*`은 이미 정상. 모든 Callable Functions 정상. 이제 **vote snapshot 리스너**도 복구.
- **교훈**: 새 Firestore 경로 추가 시 rules도 함께 추가할 것. catch-all `{document=**}`가 우선순위 낮다는 착각 금지.

## 주의사항

버그는 **해결 후 반드시 이곳에 추가**. 같은 문제 반복 방지가 목적.
