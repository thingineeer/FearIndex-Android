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

## 2026-04-17 세션

### 12. Similar Events 카드 — 암호화폐 탭에서도 "S&P" 노출 버그
- **증상**: 암호화폐 탭의 "지금과 비슷했던 시기" 카드에서 `1년 후 S&P +996.2%` 처럼 S&P 하드코딩 노출. 실제로는 크립토면 BTC를 표시해야 함.
- **원인**: `insight_similar_events_one_year_return` 키 값에 "S&P"가 하드코딩 (`"1년 후 S&P %1$s"`). iOS는 `"1년 후 %@"` 형태로 자산명을 value로 주입하는데 Android는 key 자체에 S&P 박아둠.
- **해결**:
  1. 45 locale strings.xml: `insight_similar_events_one_year_return` 값을 iOS 패턴 `"1년 후 %1$s"` 로 변경
  2. `SimilarEventsCard` 에 `indexType: FearIndexType` 파라미터 추가 → CRYPTO면 "BTC" 아니면 "S&P" 문자열 주입
  3. `HomeScreen` 호출부에 indexType 전달
- **교훈**: iOS 키 값은 Android의 SSOT. 다국어 키에 하드코딩 자산명/단어 넣지 말고 placeholder로 주입.

### 13. 43 locale 설정 메뉴 영어 fallback (번역 누락)
- **증상**: ko 외 모든 locale에서 설정 화면 "Notification Settings / Rate App / Share App / About / Privacy Policy" 영어로 노출. tab 라벨은 locale 번역인데 ListItem만 영어.
- **원인**: `settings_menu_notification/rate/share/about/privacy` + `settings_about_version` 키가 ko를 제외한 **43개 locale에 전부 누락**. base `values/strings.xml` fallback 로 영어 노출.
- **해결**: iOS `Localizable.strings` 의 `settings.notification/review/privacyPolicy/appInfo` 키 값을 Python 스크립트로 45 locale 일괄 주입. `settings_menu_share` 는 iOS에 없어서 locale별 직접 번역 dict 사용.
- **교훈**: 새 키 추가 시 반드시 45 locale 전부 체크. 단일 locale (ko) 에만 추가하고 끝내지 말 것.

### 14. 43 locale comparison 카드 영어 fallback
- **증상**: "Comparison / Prev. Close / 1W ago / 1M ago / 1Y ago" 영어로 노출 (위 13번과 유사).
- **원인**: `comparison_card_title` + `comparison_previous_close/_1w_ago/_1m_ago/_1y_ago` 키 — ko만 있고 43 locale 전부 누락.
- **해결**: Python 스크립트로 45 locale 각자 번역 일괄 주입.
- **교훈**: 13번과 동일.

### 15. 45 locale 스크린샷 촬영 중 ANR 다이얼로그 반복 발생
- **증상**: `adb shell cmd locale set-app-locales` + `am start` 를 빠르게 연속 호출하면 `Application Not Responding: system` / `nexuslauncher` 다이얼로그 누적. "Wait" 버튼 탭해도 새 dialog 계속 쌓임.
- **원인**: 에뮬레이터 OS가 locale 전환 부하를 못 따라감.
- **해결**: `adb shell settings put global hide_error_dialogs 1` 로 **ANR dialog 시스템 차단**. 이후 45 locale 순회 무정전.
- **교훈**: 자동화 스크립트는 ANR 예방 설정 필수. dialog를 tap으로 dismiss 하는 것보다 아예 차단이 효율적.

### 16. Play Console 업로드 키 재설정 대기 중 AAB 업로드 거부
- **증상**: `app-release.aab` 업로드 시 "최근에 재설정되어 아직 유효하지 않은 업로드 인증서로 서명됨. 2026-04-19 04:33:09 UTC 이후 업로드 가능" 에러.
- **원인**: Google에 업로드 키 리셋 요청은 접수됐지만 새 인증서 활성화 대기 기간 (≈48h) 이 있음.
- **해결 대기 중**: 2026-04-19 13:33 KST 이후 현재 AAB 그대로 재업로드.
- **교훈**: 업로드 키 재설정은 즉시 반영 아님. 새 keystore로 변경 시 최소 2일 여유 필요.

## 2026-05-09 세션

### 17. Keystore 두 키 혼동 — 머신별 sync 누락으로 v1.0.3 AAB 업로드 거부
- **증상**: v1.0.3 AAB 빌드 후 Play Console 업로드 시 "잘못된 업로드 인증서. 등록된 인증서와 다름" 에러. 새 머신 (이번 작업 컴퓨터) 의 `~/fearindex-secrets/fearindex-release.keystore` 로 서명됐는데 Play Console 등록 인증서와 SHA1 불일치.
- **원인** (3겹):
  1. **두 keystore 가 평행 존재**: 이전 머신의 `~/fearindex-secrets/` 안 keystore 는 alias=`fearindex` / SHA1=`81:AD:9D:5D:9A:E1:50:EB:F1:AE:9D:AF:86:CB:03:3D:67:6B:2A:75` (v1.0.1~v1.0.2 시기 활성). 한편 Play Console 은 그 이후 업로드 키 재설정 결과로 alias=`upload` / SHA1=`CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16` 등록 중.
  2. **진짜 활성 keystore 는 thingineeer-env repo 안**: `~/thingineeer-env/android/fearindex/fearindex-release.keystore` (alias=upload, SHA1=CE:08:...). 새 머신은 이걸 install 해야 했는데 옛 `~/fearindex-secrets/` 만 가지고 있었음.
  3. **메모리/`.env` 가 옛 SHA1 (`81:AD:...`) 명시**: 메모리 파일과 `.env` 모두 v1.0.1 시기 SHA1 만 적혀있어 의심 없이 진행 → 빌드 → 업로드 실패 후에야 진짜 활성 키 발견.
- **해결**:
  1. 옛 secrets 폴더 백업: `~/fearindex-secrets.bak.20260509_163426/` (옛 keystore 보존)
  2. `bash ~/thingineeer-env/android/fearindex/install.sh` 실행 → 진짜 활성 keystore + gradle.properties 를 `~/fearindex-secrets/` 에 install
  3. **google-services.json 누락 발견**: install.sh 가 google-services.json 처리 안 해서 빌드 실패 → 백업 폴더에서 `cp ~/fearindex-secrets.bak.20260509_163426/google-services.json ~/fearindex-secrets/` 로 복원 → app/google-services.json 심볼릭 링크 복구
  4. `./gradlew clean :app:bundleRelease` 재빌드 → `keytool` 로 SHA1 검증 = `CE:08:B4:...` 일치
  5. Play Console 업로드 통과 확인 (v1.0.3 versionCode 4)
- **교훈**:
  - **단일 진실 출처는 thingineeer-env repo**. `~/fearindex-secrets/` 는 install 결과물이지 원본 아님.
  - 새 머신 셋업 시 **반드시 `bash ~/thingineeer-env/android/fearindex/install.sh` 부터** — AirDrop 으로 옛 secrets 폴더 가져오는 절차는 이미 deprecated.
  - 메모리/`.env` 의 SHA1 갱신은 keystore 변경과 동시에 이루어져야 함 (지연 시 같은 사고 재발).
- **예방**:
  1. `.claude/rules/secrets.md` + `.claude/memory/deployment.md` + `~/thingineeer-env/projects/fearindex-android/.env` 모두 진짜 활성 SHA1 (`CE:08:B4:...`) 로 갱신.
  2. 옛 SHA1 들은 `KEYSTORE_SHA1_V101` 등 suffix 붙여 이력만 보존.
  3. `install.sh` 에 google-services.json 처리 추가 (안 되어있으면 빌드 깨짐).
  4. CLAUDE.md Release Signing 섹션에 thingineeer-env 가 single source of truth 라고 명시.

## 2026-05-12 세션

### 18. (×) "Firebase Analytics 가 한글 이벤트 이름을 drop 한다" — Claude 의 잘못된 진단, revert 함

- **상황**: v1.0.1 작업 중 Claude 가 `AnalyticsEvent.kt` 의 이벤트 이름이 한글(`앱시작`, `탭선택` 등)인 걸 발견하고 "Firebase Analytics 규격 `^[A-Za-z][A-Za-z0-9_]{0,39}$` 위반이라 SDK 가 drop 한다" 고 단언. 영문 snake_case 마이그레이션 worktree(`feature/v1.0.1-analytics-en-names`) 만들고 dev 까지 머지 완료한 상태에서 사용자가 Firebase Console Analytics 대시보드 스크린샷 제시.
- **실측 (사용자 제공 스크린샷, iOS 측 632,127 이벤트)**:
  - `차트상호작용` 103,953 / `배너광고노출` 72,556 / `공포지수조회` 60,867 / `자동새로고침` 44,980 / `앱시작` 40,109 / `인터스티셜광고실패` 24,518 ...
  - **한글 이벤트 이름이 모두 정상 수집되고 있음**. drop 발생하지 않음.
- **결론**: Claude 의 진단이 틀렸음. Firebase Analytics 가 비-ASCII 이벤트 이름을 정확히 어떻게 처리하는지에 대한 일반화된 단언을 했지만 실측과 다름. (공식 docs 가 영문 권장한다는 점은 사실이나, SDK 가 실제로 drop 하는지는 별개 — 적어도 현재 iOS 가 잘 보내고 있음.)
- **조치 (2026-05-12)**:
  - dev 의 머지 커밋 `6d2d126` 을 `git revert -m 1` 로 취소 → `4cf1591` "Revert 'merge: v1.0.1 analytics 영문화'" 생성.
  - 그래프 보존: 잘못된 변경이 원래 머지된 흔적 + revert 이력 모두 남음.
  - 잘못된 작업물(`docs/checkpoints/IOS-ANALYTICS-SYNC-PROMPT.md`) 삭제.
  - `feature/v1.0.1-analytics-en-names` 브랜치 자체는 남겨둠 (히스토리 추적용).
- **교훈 (Claude 작동 방식)**:
  1. **단언 전 실측**. "이러이러한 규격이라 동작 안 함" 같은 주장은 공식 docs 한 줄로 단언하지 말고 실측(WebSearch, 실제 콘솔, logcat) 으로 확인 후 발언.
  2. **이미 운영 중인 시스템을 "고장났다" 진단할 때는 더 보수적으로**. iOS 가 같은 코드로 production 운영 중이면 이미 동작 검증된 것. 사용자에게 "실제로 dashboard 에 데이터 들어오는지 먼저 확인해보자" 라고 묻고 시작했어야 함.
  3. 코드 컨벤션 메모(`AnalyticsEvent.kt` 의 "한국어 키값으로 Firebase Console에서 쉽게 확인 가능" 주석) 는 보통 검증 끝난 의도다. 무시하고 뒤집으면 안 됨.
- **재발 방지**: 향후 Analytics 관련 의문 생기면 → (1) Firebase Console DebugView 또는 실제 events 화면 → (2) 사용자에게 "현재 수집되는지 본 결과" 공유 받고 → (3) 그 후 코드 변경 여부 결정.

### 19. Android 15 (targetSdk 35) edge-to-edge — Play Console 경고 2건

- **증상**: Play Console 사전 출시 보고서에 두 가지 경고.
  1. "일부 사용자에게는 더 넓은 화면이 표시되지 않을 수 있습니다" — Android 15 부터 SDK 35 타겟 앱은 기본 edge-to-edge 동작, 미준비 시 inset 영역에 콘텐츠 잘림.
  2. "지원 중단된 API 사용" — `Window.setStatusBarColor`, `Window.setNavigationBarColor`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_*` 사용 감지.
- **원인**:
  - `themes.xml` 의 `android:statusBarColor` + `android:navigationBarColor` 두 속성이 deprecated 상태 + Android 15 edge-to-edge 모드에서 무시됨.
  - MainActivity 의 `enableEdgeToEdge()` 호출은 이미 있었고 Material3 Scaffold 가 자동으로 system bar inset 처리 중 → 코드 자체는 정상이고 themes 의 deprecated 속성만 잔재.
  - `LAYOUT_IN_DISPLAY_CUTOUT_MODE_*` 직접 사용은 앱 코드에 없음 — Play Console 경고의 시작 위치 (`J5.b.invoke`, `c.r.a`, `D2.S.onApplyWindowInsets` 등) 는 ProGuard 난독화된 라이브러리 코드 (AdMob/Compose 내부). 라이브러리 업데이트로만 해결 가능.
- **해결** (v1.0.1 vc 9, 2026-05-12, `feature/v1.0.1-edge-to-edge`):
  - `app/src/main/res/values/themes.xml` 에서 `android:statusBarColor` + `android:navigationBarColor` 두 줄 제거 (windowBackground 만 유지).
  - MainActivity 의 `enableEdgeToEdge()` 가 런타임에 system bar 를 transparent 로 설정하므로 themes 속성과 중복이었음.
  - Material3 Scaffold 4 곳 (NavHost / Settings / Privacy / NotificationSettings) 모두 `innerPadding` 적용 검증 완료.
- **검증**:
  - 에뮬레이터 `Medium_Phone_API_36.1` 에 debug APK 설치 후 모든 탭 정상 동작.
  - status bar / navigation bar 영역과 콘텐츠 가시적으로 분리됨.
  - `./gradlew bundleRelease` + `fastlane production` push 성공 (versionCode 9, 100% rollout).
- **교훈**:
  - targetSdk 올릴 때마다 deprecated API 경고 체크 + themes.xml 의 시스템 bar 관련 속성 정리.
  - `LAYOUT_IN_DISPLAY_CUTOUT_MODE_*` 같은 라이브러리 내부 호출 경고는 우리 코드 변경으로 못 고침 — 라이브러리 BoM 업그레이드 시 자연 해결.

## 주의사항

버그는 **해결 후 반드시 이곳에 추가**. 같은 문제 반복 방지가 목적.
