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

### 18. Firebase Analytics 이벤트 이름이 한글이라 SDK 가 모두 drop — Console 에 아무 데이터도 도달하지 않음

- **증상**: 사용자가 Firebase Console (`fear-index-a4f4b`) Analytics 대시보드에서 이벤트가 보이는지 의심. 코드 점검 결과 `AnalyticsEvent.앱시작 → name="앱시작"`, `탭선택 → name="탭선택"` 처럼 **거의 모든 이벤트 이름이 한글**.
- **원인**: Firebase Analytics 이벤트 이름 규격은 `^[A-Za-z][A-Za-z0-9_]{0,39}$`. 한글 등 비-ASCII 가 포함되면 SDK 가 `Invalid event name` 경고를 찍고 이벤트 자체를 drop. 즉 빌드/런타임 에러는 없지만 **데이터가 Console 에 단 한 건도 들어가지 않음**. `AnalyticsScreen.screenName` 도 한글이라 `screen_view` 의 `screen_name` 파라미터도 sanitize 됨.
- **해결** (v1.0.1, `feature/v1.0.1-analytics-en-names`):
  - `core/analytics/AnalyticsEvent.kt` 의 모든 case 의 `name` 인자를 영문 snake_case 로 변경. Kotlin 식별자(class/case 이름)는 한글 유지 (코드 가독성).
  - 파라미터 키도 영문 raw 로 통일 (`탭이름→tab_name`, `현재점수→current_score`, `에러메시지→error_message` 등).
  - `AnalyticsScreen.screenName` 도 영문화 (`홈→home`, `차트→chart`, `투표→vote`, `설정→settings`, `알림설정→notification_settings`).
  - 기존부터 영문 raw 였던 `insight_*` 시리즈는 그대로 유지.
- **iOS 동기화 필요 (읽기 전용 정책으로 본 저장소에서는 수정 금지)**:
  - iOS `LocalPackages/Core/Sources/Core/Analytics/AnalyticsEvent.swift` 도 동일한 문제를 가짐 (case 가 한글 enum). iOS Firebase SDK 도 같은 규격 검증을 함.
  - iOS 팀에서 Android 와 동일한 영문 raw 이름으로 변경해야 대시보드에서 플랫폼 비교 가능. 매핑 표는 본 커밋의 `AnalyticsEvent.kt` 가 SSOT.
  - 매핑 (Android 기준 → iOS 도 동일하게):
    - `앱시작=app_start`, `앱백그라운드=app_background`, `앱포그라운드=app_foreground`
    - `탭선택=tab_selected` (param `tab_name`)
    - `수동새로고침=manual_refresh` (param `screen`), `자동새로고침=auto_refresh`
    - `차트기간선택=chart_period_selected` (param `period`)
    - `시장지수조회=market_index_viewed` (param `index_name`)
    - `API에러=api_error` (params `error_type`, `error_message`)
    - `네트워크에러=network_error` (param `error_message`)
    - `배너광고노출=banner_ad_impression` / `배너광고클릭=banner_ad_clicked` / `배너광고실패=banner_ad_failed`
    - `설정변경=setting_changed` (params `setting_key`, `setting_value`)
    - `위젯업데이트=widget_updated` / `워치동기화=watch_synced`(iOS 전용)
    - `공포지수조회=fear_index_viewed` (params `current_score`, `rating`) / `비교데이터조회=comparison_viewed`
    - `알림설정화면진입=notification_settings_opened` / `알림설정변경=notification_toggled` (param `enabled`)
    - `알림임계값변경=notification_threshold_changed` (params `lower`, `upper`)
    - `푸시알림수신=push_received` / `푸시알림탭=push_tapped` / `푸시알림후체류시간=push_engagement_duration`
    - `지수타입전환=index_type_switched` (params `type`, `screen`, optional `previous_type`)
    - `암호화폐공포지수조회=crypto_fear_index_viewed` / `암호화폐차트조회=crypto_chart_viewed`
    - `암호화폐알림설정변경=crypto_notification_toggled` / `암호화폐알림임계값변경=crypto_notification_threshold_changed`
    - `투표참여=vote_cast` (params `choice`, `index_type`, `current_score`)
    - `투표결과조회=vote_results_viewed` / `투표중복시도=vote_duplicate_attempt` / `투표탭진입=vote_tab_opened`
    - `투표세그먼트전환=vote_segment_switched` / `투표제출실패=vote_submit_failed`
    - `공유버튼탭=share_button_tapped` / `공유카드생성=share_card_generated` / `공유완료=share_completed` / `공유취소=share_cancelled`
    - `인터스티셜광고노출=interstitial_ad_impression` / `인터스티셜광고닫기=interstitial_ad_closed` / `인터스티셜광고실패=interstitial_ad_failed`
    - `화면체류시간=screen_dwell_time` / `차트상호작용=chart_interaction` / `알림설정진입경로=notification_settings_source` / `세션종료=session_ended`
  - Screen mapping: `홈=home`, `차트=chart`, `설정=settings`, `투표=vote`, `알림설정=notification_settings`.
- **검증**: `./gradlew :app:assembleDebug` + `:app:bundleRelease` 통과. logcat 에서 `Invalid event name` 경고 사라지는지는 사용자가 디바이스에서 직접 확인.
- **교훈**: Analytics 식별자는 처음부터 ASCII snake_case 로. 한글 그대로 쓰면 빌드는 성공하지만 dashboard 에서 데이터 0 건이라 운영이 깨짐. UI 라벨 ≠ Analytics key.

## 주의사항

버그는 **해결 후 반드시 이곳에 추가**. 같은 문제 반복 방지가 목적.
