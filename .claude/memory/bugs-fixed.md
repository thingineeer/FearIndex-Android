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

## 2026-06-15 세션

### 20. AdMob 광고 게재 제한 (이전 1.0.1 버전 광고 프레임 크기 정책 위반) → 강제 업데이트로 대응

- **증상**: AdMob 정책 센터(`https://admob.google.com/v2/policycenter/issues/details/app/1/th1ngjin.fearindex`)에서 **"광고 게재 제한됨"** (광고 요청 1.7천/7일, 신고일 2026-06-09). 문제 유형: `발견된 문제(이전 버전)` — 문제 샘플 버전 **`1.0.1`**, 정책 문제 = **"수정된 광고 코드: 광고 프레임 크기 변경"**.
- **원인**: 구버전(1.0.x)의 inline adaptive banner 프레임 크기 변경이 AdMob 정책 위반으로 판정됨. **이미 1.1.1(edge-ads-hotfix)에서 배너 높이 constrain으로 수정 완료** (bugs-fixed 19번 + SESSION-STATE 참조). AdMob 안내문 그대로: *"이전 버전에서 광고 재개 불가. 문제를 해결하고 사용자를 최신 버전으로 업데이트하도록 유도하라."*
- **해결** (v1.1.2, versionCode 14, `feature/v1.1.2-force-update`):
  1. **iOS force-update 패턴 포팅** (iOS가 SSOT — `FearIndex-iOS/.../RemoteConfigManager.swift` checkForUpdate(), `AppRoot.swift` ForceUpdateView):
     - `core/.../update/UpdateChecker.kt` + `UpdateStatus.kt`: major.minor 비교(1.0.x < 1.1 → 강제), 전체버전 비교(선택), 수치 비교(1.10>1.9), 비정상 입력 안전 처리. **TDD 9개 케이스** (`UpdateCheckerTest.kt`).
     - `RemoteConfigManager`에 iOS 동일 키 추가: `force_update_minimum_version`(major.minor), `minimum_app_version`(전체). **코드 default는 빈 문자열 = 아무도 차단 안 함** → 실제 트리거는 Firebase Console에서 `force_update_minimum_version=1.1` 설정.
  2. **Play In-App Update IMMEDIATE** (`com.google.android.play:app-update(-ktx):2.1.0`): `app/.../update/InAppUpdateManager.kt`. Store에 새 버전 없으면 `market://details?id=th1ngjin.fearindex` 폴백. onResume에서 진행 중 업데이트 재개.
  3. `presentation/.../feature/update/ForceUpdateView.kt`: iOS 레이아웃 포팅, **BackHandler로 dismiss 차단**.
  4. `MainActivity`: RemoteConfig fetch → `checkForUpdate(BuildConfig.VERSION_NAME.substringBefore("-"))` → 강제 시 ForceUpdateView + IMMEDIATE 플로우. debug suffix(`-debug`) 제거 후 비교.
  5. **force_update_title/message/button 45 locale** (iOS `force.update.*` 값 동일, apostrophe/ampersand escape).
- **배포**: `bundle exec fastlane production` → **HTTP 200 성공**. Play Console production 트랙 = **"활성 · 출시 버전 1.1.2 검토 중 · 국가/지역 177개 · 100% rollout"** (`release_status: completed`). 심사 통과 시 자동 게시.
- **남은 작업 (다음 세션)**:
  1. **Firebase Console Remote Config에서 `force_update_minimum_version=1.1` 설정** ← 이게 안 되면 강제 업데이트가 트리거되지 않음. 1.1.2 심사 통과/게시 후 설정해야 1.0.x 유저에게 강제 업데이트 발동.
  2. 1.1.2 심사 통과 확인 (production 트랙 `검토 중` → `게시 완료`).
  3. AdMob 정책 센터 상태 재확인 — 1.0.x 사용자가 1.1.2+로 빠지면 정책 위반 자연 해소.
- **교훈**:
  - AdMob "발견된 문제(이전 버전)"는 **이미 수정된 버전이 있어도** 구버전 사용자가 남아있으면 제한 유지 → **강제 업데이트로 구버전 사용자를 0으로** 만드는 게 정공법.
  - 강제 업데이트 게이트는 코드만 배포하면 끝이 아니라 **Remote Config 트리거 값 설정이 필수**. 코드 default를 빈 값으로 두면 배포해도 아무도 차단 안 되므로 안전.

## 2026-06-16 세션 (광고 미노출 + 배너 버그 + 정책 심화)

### 21. 광고가 배너·인터스티셜 전부 미노출 → Remote Config에 광고 키 자체가 없었음

- **증상**: release/debug 모두 배너·인터스티셜 광고가 하나도 안 뜸.
- **진단**: firebase CLI(`firebase remoteconfig:get --project fear-index-a4f4b`)로 확인 → **`ads_enabled` 등 광고 관련 키가 Remote Config에 통째로 없음**. 파라미터는 force_update/minimum_app/review/storefronts 4개뿐.
- **원인**: `RemoteConfigManager.defaultAdsConfig()`가 `adsEnabled=false`. Console에 키가 없으면 앱이 코드 default `false`를 사용 → `AdBanner.kt:63` 게이트 `!adsConfig.adsEnabled`에서 `return` → 빈 뷰.
- **해결**: firebase CLI로 광고 키 6개 게시 (RC 버전 38). `firebase deploy --only remoteconfig`는 firebase.json 필요 → `/tmp/rc_deploy/`에 임시 firebase.json+.firebaserc+rc_template.json 구성 후 deploy.
  - `ads_enabled=true`, `interstitial_ads_enabled=true`, `interstitial_session_cap=2`, `interstitial_cooldown_sec=180`, `kospi_interstitial_enabled=true`, `vote_enabled=true`
- **교훈**: 코드에 RemoteConfig 키를 쓰면 **Firebase Console/CLI에도 반드시 키를 게시**해야 함. 키 없으면 코드 default(여기선 광고 off)로 조용히 동작. CLI(`remoteconfig:get`)가 Console 클릭보다 빠른 검증 수단.

### 22. 배너 광고 화면 미표시 (onAdLoaded는 뜨는데 안 보임) → inline adaptive height 0 버그

- **증상**: 21번 해결 후 logcat에 `배너광고노출`(onAdLoaded) 이벤트는 정상 발사 + 실패 0건인데 **화면에는 배너가 안 보임**.
- **진단**: dynamic workflow(11 agents, 5축 검증+적대적 반박)가 `AdBanner.kt:77,84`로 정확히 지목.
- **원인**: inline adaptive 배너는 **로드 전 `adSize.height`가 0**인데, 컨테이너 높이를 `.height(adSize.height.dp)`(line 77) + `getHeightInPixels`(line 84)로 **로드 전에 고정**하고 `onAdLoaded`에서 실제 높이로 갱신 안 함 → 광고 수신돼도 0높이/클립.
- **해결** (v1.1.3 vc15, `feature/v1.1.3-ad-banner-height`):
  1. `AdBannerLayout.kt`에 `resolveBannerHeightDp(estimatedHeightDp, loadedHeightDp)` 순수 함수 추가: 로드 전 추정/0이면 fallback(50dp), 로드 후 실제 높이 우선, MAX(120dp) 클램프. **TDD 5개 케이스**.
  2. `AdBanner.kt`: `onAdLoaded`에서 실제 AdView 높이를 `mutableStateOf`로 끌어올려 컨테이너 height 갱신. layoutParams를 WRAP_CONTENT로.
  3. `proguard-rules.pro`에 `com.google.android.play.core.*` keep 추가 (In-App Update release minify 안정성).
- **검증**: 에뮬레이터 debug 빌드에서 "AdMob Adaptive Banner / Test Ad" **배너 시각적 노출 확인** (수정 전엔 같은 로그인데 안 보였음).
- **교훈**: `onAdLoaded` = "광고 수신"이지 "화면 표시"가 아님. inline adaptive 배너는 반드시 로드 후 실제 높이를 컨테이너에 반영해야 함. logcat 노출 이벤트만 믿지 말고 실제 화면 캡처로 검증.

### 23. AdMob 배너 "적용 불가" — 새 광고단위 추가는 무의미, 항소도 불가

- **상황**: AdMob 광고단위 6개 중 **배너 5개 "적용 불가"**, KospiInterstitial(전면)만 "제한 없음". 사용자가 "새 광고단위 추가해야 하나?" 질문.
- **진단**: 정책센터 = "광고 게재 제한됨", 정책 문제 "수정된 광고 코드: 광고 프레임 크기 변경", 샘플 버전 1.0.1.
- **결론**:
  1. **새 광고단위 추가 무의미** — 제한이 *광고단위*가 아니라 **앱 전체 배너 게재**에 걸림. 새로 만들어도 똑같이 "적용 불가". (전면광고가 "제한 없음"인 이유 = "프레임 크기" 정책은 배너에만 적용)
  2. **항소 버튼 없음** — "발견된 문제(이전 버전)" 타입은 정책센터에 검토요청/항소 UI 없음. AdMob 안내문 자체가 "구버전 광고 재개 불가, 유저를 최신으로 유도하라"고 명시.
  3. **유일 해법 = 1.0.x 트래픽 0 수렴** → 강제 업데이트(이미 v1.1.2부터 적용). 1.0.x 유저가 빠지면 AdMob 자동 해제 (수일).
- **점검 완료 (광고 차단 요인 전부 클리어)**: UMP/GDPR consent form "활성화된 메시지 1개" ✅ (canRequestAds 정상) / 결제계정 애드센스(대한민국) ✅ / release 배너 광고단위 ID 5개 콘솔 일치 ✅.
- **교훈**: AdMob "적용 불가"는 광고단위 문제가 아니라 앱/계정 레벨 신호. "발견된 문제(이전버전)"는 트래픽 전환만이 답. 새 단위 생성·항소로 시간 낭비 금지.

### (참고) 강제 업데이트 기준 정책

- **정책 해소만**: `force_update_minimum_version` Android=`1.1` 로 충분 (1.0.x만 강제, 위반 트래픽 제거).
- **배너버그(1.1.3) 빠른 전파**: 1.1.3 Play 전파 확인 후 Android=`1.2`로 상향 → 1.1.0~1.1.2도 강제. (사용자 선택, 2026-06-16)
- **순서 중요**: 1.1.3이 Play Store에 전파되기 전에 강제 기준을 올리면 In-App Update가 받을 새 버전이 없어 스토어 폴백됨.
- **RC default는 fail-open 유지**: `force_update_minimum_version` default=`""`. default를 `1.1`로 올리는 fail-closed 처방은 **채택 금지** (미래 minor 출시 후 최신 유저를 영구 게이트에 가둘 위험, iOS parity 깨짐).

## 2026-06-16 세션 (v1.2.0 — peak 마커 + 공유 링크 + SimilarEvents 점수)

브랜치: `feature/v1.2.0-banner-clip-fix` (v1.2.0 / versionCode 16). 4개 커밋. 모두 iOS parity 성격이라 현재 브랜치에 함께 커밋 (사용자 선택).

### 24. 차트 고점/저점(peak) 마커 추가 — iOS parity, TDD

- **요청**: "그 시기의 고점과 저점" — iOS 차트 peak 마커를 Android 포팅.
- **iOS SSOT**: `Domain/Entities/FearIndexPeak.swift` + `Domain/UseCases/ComputeFearIndexPeaks.swift` + `Presentation/.../SwiftUIChartView.swift`(peakMarks). Android엔 셋 다 없었음.
- **구현** (TDD):
  1. `domain/.../entity/FearIndexPeak.kt`: kind(HIGH/LOW)/score/date/index.
  2. `domain/.../usecase/ComputeFearIndexPeaks.kt`: history 1회 순회 min/max, **동점 시 `>=`/`<=`로 최근(뒤) 채택**, 빈 배열 null, 단일 포인트 high==low. `fun interface FearIndexPeaksComputing` + `operator fun invoke` → `Pair<high,low>?`. iOS 11개 테스트 케이스 포팅 (`ComputeFearIndexPeaksTest`).
  3. `ChartScreen.kt` Canvas: `drawPeakMarkers` — 빨간 점(`PeakMarkerColor=0xFFFF3B30`) + 점수 라벨. high 위/low 아래, score>85(high)/score<10(low) 시 좌우 배치. 단일 포인트는 high만. `peakScoreText`: 정수 "82" / 소수 "2.9".
- **핵심 함정 (iOS #19 회귀와 동일)**: Android `ChartDataFilter.sample`이 **고정 step 샘플링**이라 원본 min/max index가 누락되면 peak 마커가 라인 위 허공에 뜸. 2Y/3Y/5Y(maxSamplePoints 260/390/520)만 샘플링, 3M/6M/1Y는 null이라 무관.
  - **해결**: `ChartDataFilter.samplePeakPreserving`로 교체 — 시작/끝/min/max 4 anchor + 각 extremum 좌우 1포인트 보존 후 균등 step으로 채움. iOS `sample`(#19 fix)과 1:1. **TDD 10개 테스트**(`SamplePeakPreservingTest`): min/max 포함, 동점 최근 보존, 정확히 maxPoints개, 정렬, 중복 없음.
  - **1px 일치 강제**: `ChartCoordinates`(core/util) 순수 함수로 x=`width*index/(count-1)`, y=`height*(1-score/100)` 추출 → 라인/peak/선택점이 **동일 수식 공유**. **TDD 11개**(`ChartCoordinatesTest`).
- **검증**: 에뮬레이터+실기기(Galaxy S23) release 빌드에서 시각 확인 — 고점 71.2(위)/저점 5.8(아래) 빨간 점이 라인 위 정확히, 암호화폐는 정수 50/8.
- **교훈**: 차트 마커는 반드시 라인과 **같은 좌표 변환 함수** + **peak-preserving 샘플링**을 써야 1px 어긋남 없음. `onAdLoaded`처럼 "값은 맞는데 화면 어긋남"은 실제 캡처로만 검증 가능.

### 25. 홈 공유 버튼 → Play 스토어 링크 (TDD)

- **요청**: 공포탐욕지수 우측 상단 공유버튼이 Play 스토어 링크 공유 (앱 있으면 앱으로, 없으면 설치).
- **이전**: `ShareUrlBuilder.build()`가 웹앱 URL(`fear-index-a4f4b.web.app/?score=...`) 공유.
- **변경**: `ShareUrlBuilder.playStoreUrl()` = `https://play.google.com/store/apps/details?id=th1ngjin.fearindex`. **항상 production 패키지**(debug suffix 미포함) — debug 빌드에서 공유해도 스토어엔 prod만 존재. Android 표준 동작으로 앱 있으면 앱/없으면 설치 페이지. 안 쓰이게 된 `shareType` 파라미터 제거. **TDD 4개 테스트**.
- **검증**: 에뮬레이터 공유 시트 — "Today's Fear & Greed Index: 41 (Fear)\n\n...app.\nhttps://play.google.com/store/apps/details?id=th1ngjin.fearindex" 정상.
- **주의**: ShareUrlBuilder.build 사용처는 HomeScreen 한 곳뿐이라 안전하게 교체. 공유 본문(`share_message_template`)은 점수/등급 그대로 유지.

### 26. SimilarEvents 헤더 점수를 게이지와 일치 (iOS parity)

- **증상**: SimilarEvents 카드 헤더 점수가 서버 raw(`result.currentScore`, 갱신 지연)라 게이지/비교카드(`fearIndex.roundedScore`)와 어긋남.
- **해결**: `SimilarEventsCard(displayedScore: Int)` 파라미터 추가 → HomeScreen에서 `score`(=roundedScore) 주입. iOS `SimilarEventsCardView.displayedScore` (`fearIndex.score.roundedScore`) 대칭.

### (참고) 실기기/에뮬레이터 광고 차이

- **에뮬레이터**: release 빌드(프로덕션 광고단위)여도 AdMob이 "Test Ad" 라벨 광고 표시 — 정상.
- **실기기**: 진짜 AdMob 광고 노출(예: 대출 광고). release 빌드 = minify+프로덕션 서명(SHA-1 `CE:08:B4:...` 일치 확인)+프로덕션 광고.
- ⚠️ 실기기엔 release(`th1ngjin.fearindex`)와 debug(`.debug`)가 동시 설치 가능 — recents에서 섞임. release 검증 시 `monkey -p th1ngjin.fearindex`로 명시 실행할 것.

## 2026-06-16 세션 (v1.2.0 — info 버튼 + 데이터 정합성 + 시장 상세)

브랜치: `feature/v1.2.0-banner-clip-fix`. 모두 iOS parity. TDD 위주.

### 27. 현재 지수 info 버튼 + KOSPI 장 상태/업데이트 시각 (iOS parity)

- **요청**: iOS는 현재 지수에 ⓘ버튼·업데이트시각·장마감여부를 알려주는데 Android는 없음. "iOS 로직 그대로만".
- **iOS 동작 (이것만 구현)**: ⓘ버튼→대표기준 시트(글로벌=S&P500/한국=KOSPI/암호화폐=BTC + KOSPI 업데이트정책), 업데이트시각(timestampView: KOSPI="지수 업데이트" 그외="업데이트", indexType별 타임존 NY/Seoul/UTC, `yyyy.MM.dd HH:mm`), KOSPI 장상태(kospiCurrentStatusLine: isFinal 기반 "장마감 확정/장중 추정"). **홈탭엔 currentScoreHeader 없음(timestampView만)**, 차트·투표탭에만 ⓘ버튼+상태줄.
- **구현**: `FearIndexDateContext`(타임존 매핑, domain) + `IndexTimestampFormatter`(core) + `RepresentativeIndexInfoSheet`(ModalBottomSheet) 신규. ChartScreen CurrentScoreCard / VoteScreen CurrentScoreHeader에 ⓘ버튼+KOSPI상태줄. HomeScreen timestamp를 iOS 로직으로 교체. `KospiFearIndex.isFinal`이 이미 uiState.kospiSnapshot에 있어 데이터 장애 0. TDD: FearIndexDateContext 6 + IndexTimestampFormatter 5.
- **strings**: 11키 × 45 locale (iOS xcstrings). 검증: 에뮬+실기기 release에서 KOSPI "장마감 확정 · 지수 업데이트: 2026.06.16 15:30"(KST), 대표기준 시트 4항목 시각 확인.
- **검증 Workflow**: iOS parity/locale/compose 3축 적대적 검증 → confirmed 0건(전부 정확).

### 28. 코스피 36 vs 37 — 반올림/staleness 검증, 코드 정상 (수정 없음)

- **증상**: 코스피 현재지수 Android=36 iOS=37.
- **검증 결과**: 서버 API(`/api/kospi/v2`) score=37(정수), Android도 37 정확 표시(차트 KOSPI 직접 확인). 반올림 로직도 iOS와 동일(Kotlin `roundToInt`=Swift `Int(rounded())`=half-up, **둘 다 banker's 아님**). `KospiStalenessResolver`(정규장 09:00~15:20, intraday 2h stall, dataDate≠오늘+장열림→stale)도 iOS와 1:1. → **재현 안 됨, 데이터 갱신 타이밍 차**(애프터장 갱신 전후). 코드 정상.
- **사용자 확인**: "애프터장 ~20시 갱신 시 iOS와 동일하게 갱신되는지" → staleness 로직 동일하므로 같은 시점 같은 데이터 받으면 동일. close 스냅샷 isFinal=true면 isStale=false로 그대로 표시.

### 29. 암호화폐 비교 수치(1개월/1년) iOS와 다름 — 배열 인덱스 → 날짜 기반 (실제 버그)

- **증상**: 암호화폐 비교카드 [전일/1주/1개월/1년] iOS=20/10/31/61, Android=20/10/27/68. 1개월·1년 다름.
- **원인**: `CryptoFearIndexRepositoryImpl`이 `data[30]`(1개월)/`data[365]`(1년) **배열 인덱스**로 앵커 선택. iOS는 `HistoricalAnchorResolver`로 **날짜 기반**(정확히 N개월/년 전 날짜 이하 최신). 윤달/누락일 때문에 인덱스≠날짜.
- **해결**: `HistoricalAnchorResolver`(domain) 신규 — iOS 1:1 포팅(previousClose=기준일 이전 최신, 1주/월/년=날짜-오프셋 이하 최신, 1년 못찾으면 null). crypto repo를 367일 단일 fetch + 날짜기반 enrich(UTC)로 교체. **실 API 검증**: 전일20/1주10/1개월31/1년61 = iOS 정확 일치. TDD: resolver 5 + crypto repo 9.
- **주의**: KOSPI repo는 **이미 날짜 기반**(`scoreOnOrBefore`)이라 정상. crypto만 인덱스 버그였음.

### 30. 시장 상세 화면 신규 구현 (iOS MarketDetailView parity)

- **요청**: iOS만 있는 "시장 상세" feature(지수/환율/암호화폐 3탭) Material Design 포팅.
- **데이터소스 4개 (iOS 전략 1:1)**:
  - **Yahoo chart v8** `query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d` UA=Mozilla/5.0 — 글로벌 7(^IXIC,NQ=F,^GSPC,RTY=F,^DJI,^SOX,^VIX)+DXY(DX-Y.NYB). prevClose=`chartPreviousClose ?? previousClose ?? price`.
  - **Naver** `m.stock.naver.com/api/index/{KOSPI|KOSDAQ}/basic` — 응답 전부 String, 콤마제거 파싱. symbol은 ^KS11이지만 URL은 KOSPI.
  - **CoinGecko** `api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,ripple,solana,binancecoin&vs_currencies=usd&include_24hr_change=true` — 동적 키 맵 → `Map<String,CoinGeckoCoinPrice>`.
  - **currency-api** `latest.currency-api.pages.dev/v1/currencies/usd.json` + 이전일 `{date}.currency-api.pages.dev/...` — usd[krw], 이전일=`LocalDate.parse(date).minusDays(1)`.
- **도메인**: MarketIndex(change/timestamp 추가, isPositive computed로 변경 → 기존 호출처 수정), MarketIndexType(10종 enum), CryptoPrice/CryptoCoinType, ExchangeRateQuote 신규. MarketDetailRepository + UseCase 3개.
- **data**: MarketDetailMapper(순수 변환, TDD 11) + MarketDetailRepositoryImpl(병렬 fetch+부분실패 허용). DI에 @Named("market") UA 클라이언트 + 4 API + repo binding.
- **presentation**: MarketDetailScreen(3탭 SegmentedPicker, 행=좌title+subtitle/우price+change, RowGroup) + MarketDetailViewModel(병렬로드, 10분 쿨다운). MarketQuoteFormat(core, 가격대별 소수/▲▼색, TDD 10).
- **색상 (절대 규칙)**: 지수·환율=상승빨강(0xFFE53935)/하락파랑(0xFF1E88E5) **한국식**, 암호화폐=상승초록(0xFF43A047)/하락빨강 **서양식**.
- **진입점**: 홈 TickerView 탭 → `market_detail` route. ⚠️ **기존 홈 티커가 쓰던 Yahoo spark API(`v8/finance/spark`)가 응답 0건**(833바이트 빈 body) → 홈 `marketIndices`를 새 `GetMarketIndicesDetailUseCase`(chart API)로 교체해 티커 복구 + 진입점 확보.
- **DXY**: 환율 탭이지만 Yahoo에서 옴(currency-api 아님). repo가 indices+DXY 같이 fetch, 화면 indices탭은 DXY 필터링, FX탭만 노출.
- **검증**: 에뮬+실기기 release(R8 minify 후 JSON 파싱 정상). 3탭 전부 iOS 스크린샷과 1:1: NASDAQ 26,683.94 ▲3.07% / S&P 7,554.29 ▲1.65% / KOSPI 8,726.60 ▲2.11% / BTC $66,283 ▲0.94% / USD/KRW 1,512.6 / DXY 99.66. strings 23키 × 45 locale.
- **교훈**: 외부 시세 API는 응답 형식이 바뀔 수 있음(Yahoo spark 0건). chart API가 더 안정적. R8 release에서 kotlinx.serialization DTO는 @Serializable이면 keep됨(별도 proguard 불필요, 실기기 검증 완료).

## 2026-06-16 세션 (v1.2.0 production 배포)

### 31. v1.2.0 production 배포 — 관리형 게시(Managed Publishing) 발견

- **작업**: v1.2.0(versionCode 16)을 production 100% rollout으로 배포. 강제 업데이트 1.2.0 적용 준비. changelog "전체적인 성능 및 개선을 하였습니다." (45 locale).
- **절차**:
  1. `UpdateChecker` 강제 업데이트 v1.2.0 시나리오 TDD 3개 추가 (force=1.2: 1.0.x/1.1.x 강제, 1.2.0 제외). `compareMajorMinor([1,2,0],[1,2])=0` 검증.
  2. changelog 45 locale `16.txt` 생성 (한국어="전체적인 성능 및 개선을 하였습니다.", 나머지 44 동일 의미 번역). 500자 이하 검증.
  3. 스크린샷: 어제(6/16 14:16) 광고 없이 촬영된 기존 5장(home/chart/vote/notification_settings/notification) 그대로 사용. 45 locale 전부 보유. 시장 상세는 미포함(사용자 선택).
  4. `./gradlew test` = **585 테스트 통과**(failures=0). `bundleRelease` AAB 서명 SHA-1 `CE:08:B4:...`(활성 업로드키 UPLOAD.RSA) 일치.
  5. `bundle exec fastlane production` → **HTTP 200 성공** (AAB+메타+changelog 16+스크린샷 phone/7inch/10inch, 100% rollout, release_status completed).
- **⚠️ 핵심 발견 — 관리형 게시 ON**: fastlane 업로드 후 Play Console "게시 개요"에 **"관리형 게시가 사용 설정됨"** + **"변경사항이 아직 검토를 위해 전송되지 않음"**(136개 대기). fastlane이 올려도 **자동 검토/게시 안 됨**. Chrome MCP로 "검토를 위해 변경사항 136개 전송" 버튼 클릭 → 확인 다이얼로그 "검토를 위해 변경사항 전송" → **"검토 중인 변경사항"** 전환 확인. 변경 항목 = 프로덕션 1.2.0(전체 출시 시작) + 스토어 등록정보(45 locale × phone/7in/10in 스크린샷).
- **관리형 게시 후속**: 심사 통과 후에도 **자동 게시 안 됨** → Play Console에서 **"게시" 버튼 1회 더** 눌러야 실제 출시. (Managed Publishing 특성)
- **강제 업데이트 RC 타이밍 (사용자 승인)**: 현재 RC `force_update_minimum_version` [Android app users]=`1.1`(1.0.x만 강제), `minimum_app_version` Android=`1.1.3`. **1.2.0이 Play 전파되기 전엔 RC를 1.2로 올리지 않음**(전파 전 상향 시 1.0~1.1 유저가 받을 1.2.0이 스토어에 없어 In-App Update 폴백/막힘). **1.2.0 게시·전파 확인 후** Android force_update=`1.1`→`1.2` 상향. RC default는 fail-open(`""`/iOS용 1.6.0/1.8.2) 유지.
- **다음 세션 최우선**:
  1. 1.2.0 심사 통과 확인 (Play Console "검토 중인 변경사항" → 승인).
  2. **관리형 게시이므로 승인 후 "게시" 버튼 수동 클릭** → 실제 production 출시.
  3. Play Store 전파 확인 후 **Firebase Console RC `force_update_minimum_version` [Android app users]=`1.2`** 설정 → 1.0.x/1.1.x 강제 업데이트 발동.
- **교훈**: 이 앱은 **관리형 게시 ON**. fastlane production은 "업로드+검토전송 대기"까지만 자동, 실제 검토전송·게시는 Console 수동(또는 fastlane `changes_not_sent_for_review`/별도 publish 호출). 이전 v1.1.x "100% rollout completed" 기록은 검토전송까지 자동 처리된 것으로 보였으나, 이번엔 관리형 게시 대기가 명시적으로 잡힘 — 매 배포 시 Console "게시 개요"에서 대기 변경사항 확인 필수.

## 2026-06-16 세션 (v1.2.0 강제 업데이트 발동)

### 32. RC `force_update_minimum_version` [Android]=`1.1`→`1.2` 상향 — 1.0.x/1.1.x 강제 업데이트 발동

- **맥락**: v1.2.0(vc16)이 Play Store에 게시·전파 완료. 31번에서 보류했던 "전파 후 RC 1.2 상향"을 실행.
- **전파 검증 (선행 필수)**: 공개 Play Store 리스팅(`https://play.google.com/store/apps/details?id=th1ngjin.fearindex`) HTML 파싱 → semver 토큰 `1.2.0` + "Updated on Jun 16, 2026"(당일) 확인. **전파 전 RC 상향 시 1.0~1.1 유저가 받을 1.2.0이 없어 강제창에 막힘**(23번 원칙)이라, 반드시 전파를 먼저 확인하고 진행.
- **변경**: firebase CLI `deploy --only remoteconfig`. `firebase remoteconfig:get -o`로 현재 템플릿(conditions+parameters 10개) 추출 → `force_update_minimum_version`.conditionalValues['Android app users'].value 만 `1.1`→`1.2` 수정 → diff 정확히 한 줄 확인 → `/tmp/rc_deploy/`(firebase.json+.firebaserc) 에서 deploy. **나머지 9개 파라미터/조건/default 전부 보존** (default 1.6.0 iOS, `minimum_app_version` Android 1.1.3, ads 키 6개).
- **결과 (라이브 재확인)**: `force_update_minimum_version` default=1.6.0 / [Android]=`1.2`. `UpdateChecker` major.minor 비교로 **1.0.x·1.1.x 강제 / 1.2.0 통과**(`compareMajorMinor([1,2,0],[1,2])=0`).
- **기대 효과**: AdMob 배너 "적용 불가"(1.0.1 정책위반, 23번)는 구버전 트래픽이 0으로 수렴하며 자연 해소. AdMob 정책센터 상태는 수일 후 재확인.
- **교훈**: RC 강제 게이트 상향 순서 = (1) **새 버전 Play 전파를 공개 리스팅 등으로 먼저 검증** → (2) **한 줄만 변경하는 diff 확인** → (3) deploy 후 **라이브 값 재조회**. firebase CLI deploy는 전체 템플릿을 publish하므로 get→최소수정→deploy로 기존 파라미터 보존.

## 주의사항

버그는 **해결 후 반드시 이곳에 추가**. 같은 문제 반복 방지가 목적.
