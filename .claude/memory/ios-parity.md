---
name: iOS Parity
description: iOS/macOS 프로젝트와 일관성 유지할 항목 체크리스트. 대시보드/Analytics에서 같이 비교되므로 중요.
type: project
---

# iOS Parity

**핵심 원칙**: Android는 독자 프로덕트가 아니라 **iOS/macOS와 같은 제품의 플랫폼 변종**. 모든 대시보드, Firebase Analytics 이벤트, Crashlytics, 사용자 인사이트에서 일관되게 보여야 함.

## 프리미엄 parity (iOS v1.9.4 → Android, 2026-08-18 이식)

iOS SSOT: `/Users/imyeongjin/Desktop/worktrees/fi-v194-design` (spec `docs/superpowers/specs/2026-08-18-v194-premium-score-explorer-notification-history-design.md`).

- **프리미엄 = 광고 제거 구매**(새 SKU 없음): iOS `PremiumEntitlementProviding.isPremium == isAdFree` ↔ Android `PurchaseManager.isPremium`(=`isAdFree`). `PremiumFeature{SCORE_EXPLORER, NOTIFICATION_HISTORY_UNLIMITED}` + `PremiumFeaturePolicy.canUse` 동일.
- **GA 이벤트 이름 동일**: `premium_lock_tapped{feature}`, `score_explorer_moved{index_type,score,period}`, `notification_history_viewed{count}`; 구매 이벤트(광고제거구매시작/완료/실패/복원)에 `source`(settings|score_explorer|notification_history).
- **점수 탐색기 신뢰 정책 동일**: 정확 버킷만(보간 금지), 범위 = n>0 min..max, n==0 → "기록한 날 없음", horizon 별 n = `horizonCounts`, n<5 저표본 배지, 라벨 평균/비관(p10)/낙관(p90). 번들 fallback 수익률 = iOS `DefaultReturnData.swift` 2026-08-18 재생성분과 동일(같은 JSON에서 `scripts/gen-default-return-data.py`).
- **위치 divergence**: iOS 는 인사이트 피드 아래·현재 점수 카드 위, Android 차트 탭은 현재 점수 카드가 최상단이라 **AdBanner 와 InsightFeed 사이**(배너 위치 불변).
- **알림 내역**: 정책(무료 30일/프리미엄 무제한/하드캡 5000/dedup message_id→kind+초), 채널 판별(`data.type` 접두), 배너 3번째 뒤·7개마다, 하단 잠금 row, 홈 🔔+빨간 점 — 동일. 저장은 iOS App Group JSONL(main+spool) ↔ Android 내부 저장소 JSONL 1파일. **Android 추가 규칙**: 트레이 동기화(activeNotifications)는 data payload 가 없어 kind=OTHER·fallback id 로 기록 → 이후 탭(message id) 시 같은 title/body·±120s 레코드를 승격(`NotificationHistoryPolicy.upsert`).
- **DEBUG 결제 테스트**: iOS `#if DEBUG DebugPremiumOverride` ↔ Android `core/src/debug` + `app/src/debug`(release 심볼 0). 카피는 양쪽 다 개발자용 한글 리터럴.
- **라벨**: `scoreExplorer.*`/`notification.history.*`/`premium.*`/`settings.premium.*` 45키 + 보조 10키를 xcstrings 데이터로 이식(`scripts/i18n/import_xcstrings_keys.py`). Android 재사용 키: `insight_current_score_avg_return/max_drawdown/best_return`, `insight_detail_disclaimer_*`, `market_detail_updated_at`, `period_*`.

## 온보딩 코치마크 투어 (iOS v1.9.3 → Android v1.4.1, 이식 완료)

iOS SSOT: `OnboardingTourView.swift` / `OnboardingEligibility.swift` / `FearIndexView.swift`(startTourIfNeeded/handleTourStep/finishTour/restartTour).

- **GA 이벤트 파리티** (iOS AnalyticsEvent.swift ↔ Android AnalyticsEvent.kt, 이름·파라미터 1:1):
  - `온보딩완료(단계:Int)` → GA `onboarding_done`, param `step`(1-based)
  - `온보딩건너뛰기(단계:Int)` → GA `onboarding_skip`, param `step`
  - (iOS `docs/ios-android-event-parity.md` 은 iOS 레포 소유 — Android는 AnalyticsEvent.kt 가 파리티 아티팩트)
- **게이팅**: iOS `fcm_device_id`(App Group) ↔ Android `stuck_counter_prefs/deviceId`. FCM 초기화 전 판별 → `onboarding_prefs.onboardingTourEligibleV1`. 신규 설치 첫 실행만 자동 노출.
- **8단계·문구**: iOS `Localizable.xcstrings` 의 onboarding.* 20키를 45 locale 그대로 이식(면책 포함). step8 면책 필수.
- **위젯**: iOS는 풀 위젯 스위트, Android는 요청에 따라 2×2 3종 + 4×2 대시보드(Glance)만. 위젯 사용법 가이드는 Android 절차용 자체 문구.
- ⚠️ Android 특이: 알림 권한 프롬프트가 투어를 가리지 않게 `notificationPromptResolved` 후 시작. 투어 중 인터스티셜/앱오픈/배너 억제.

## 미디에이션 어댑터 링크 — 플랫폼별 메커니즘 차이 (2026-08-19 규명)

**같은 어댑터(Unity/Pangle)를 붙여도 "번들에 파일이 있다"가 런타임 로드를 보장하지 않는다.** 양 플랫폼의 보존 메커니즘이 다르다.

| | Android | iOS |
|---|---|---|
| 위험 | R8 minify 가 미참조 클래스 shrink | 링커가 정적 아카이브에서 미참조 오브젝트를 안 뽑음 |
| 보호 | **GMA AAR 이 consumer proguard 룰 내장**(`-keep class * implements ...MediationAdapter`) → 자동 보존 | **대응 메커니즘 없음** → 앱 타겟에 `OTHER_LDFLAGS = "-ObjC"` 필수 |
| 실측 결과 | debug/release(R8) 모두 3/3 **COMPLETE** — 수동 keep 룰 불필요 | 수정 전 "No such adapter"(33KB 스텁), Xcode 26.2 + GMA 13.8 + `-ObjC` 로 3/3 **Ready** |
| 정상 상태값 | `AdapterStatus.InitializationState.**COMPLETE**` (NOT_STARTED/INITIALIZING/COMPLETE/TIMED_OUT/FAILED) | `AdapterStatus.state = **Ready**` |

- **양쪽 공통 교훈**: 정적 증거(AAR 리소스·.so, 프레임워크 임베드, nm/otool)는 근거가 못 된다. **유효한 증거는 런타임 `adapterStatusMap`(Android) / `adapterStatusesByClassName`(iOS) 로그뿐.** 그래서 양 플랫폼 모두 상시 진단 로깅을 심었다(Android `FearIndexAdapters` 태그 + Crashlytics / iOS Logger.error + Crashlytics non-fatal `FearIndex.adMediation`).
- **iOS 전용 함정**: `-force_load`(어댑터만)로는 Unity 는 되지만 Pangle 이 ObjC 카테고리 미로드로 죽는다(`SKStoreProductViewController pagConfig` unrecognized selector) → 광고 SDK 는 `-ObjC` 가 표준.
- **iOS 전용 함정 2**: pbxproj 빌드 설정은 **Debug/Release 블록 각각**에 넣어야 한다. 한쪽만 넣고 다른 스킴으로 검증하면 "수정해도 안 됨"으로 오판한다 → 빌드 로그의 실제 링크 커맨드에서 플래그 존재를 확인할 것.
- 상세: `@bugs-fixed.md` 62·64번.

## Bundle/Package ID 매핑

| 플랫폼 | 식별자 |
|---|---|
| iOS | `th1ngjin.FearIndex-iOS` |
| macOS | `th1ngjin.FearIndex-macOS` |
| **Android** | **`th1ngjin.fearindex`** (+ `.debug`) |

Android는 소문자 관습. 나머지는 `th1ngjin.*` 공유.

## 기능 대칭성 체크리스트

변경할 때마다 iOS 프로젝트에도 같은 개념이 존재/일관되는지 확인:

### UI/UX
- [ ] **탭 구조** — iOS: 홈/차트/투표/설정. Android도 동일 4탭.
- [ ] **상단 세그먼트** — "시장 / 암호화폐". iOS는 SegmentedControl, Android는 Material `PrimaryTabRow`. 라벨 텍스트는 동일.
- [ ] **공포 탐욕 지수 게이지** — 범위 0~100, 5단계 색상 (극단적 공포 ~ 극단적 탐욕). iOS 기준 색상 동일 적용.
- [ ] **차트 기간** — 3M/6M/1Y/2Y/3Y/5Y (시장), 1W/1M/3M/6M/1Y (암호화폐). 완전 동일.
- [ ] **물림 카운터** — 토글 2개 (나도 물렸어요/난 안 물렸어요) + 실시간 % 게이지. iOS와 동일 UX.
- [ ] **알림 설정** — 하한/상한 임계값 슬라이더 (0~100). iOS 동일.

### Rating 문자열 (5단계)

iOS `Localizable.strings`의 45개 locale 모두 동일 번역 사용:

| 점수 범위 | 키 |
|---|---|
| 0~24 | `rating.extremeFear` |
| 25~44 | `rating.fear` |
| 45~55 | `rating.neutral` |
| 56~75 | `rating.greed` |
| 76~100 | `rating.extremeGreed` |

Android: `values{-ko,-ja,...}/strings.xml`의 `rating_extreme_fear` 등 — **iOS와 값이 완전히 동일해야 함**.

### 데이터 API
- **CNN Fear & Greed**: `index/fearandgreed/graphdata/{startDate}` — iOS와 동일.
- **Alternative.me Crypto**: `fng/?limit={days}` — iOS와 동일.
- **Stuck Counter**: Firebase Functions `submitStuckStatus`, `getStuckCount` (asia-northeast3) — **iOS와 같은 엔드포인트 공유**.

### Firebase
- **프로젝트**: `fear-index-a4f4b` — 공유.
- **Firestore**: 동일 컬렉션 구조. Android가 iOS와 동시에 읽기/쓰기.
- **Analytics 이벤트**: 이벤트 이름을 **소문자_스네이크 케이스**로 통일. iOS에서 쓰는 이름 그대로 Android 사용.
- **Crashlytics**: 같은 프로젝트에서 스트림. Android는 ProGuard mapping 업로드.
- **App Check**: iOS는 App Attest, Android는 Play Integrity. 같은 Firebase Console App Check 설정에서 관리.

## "같이 움직여야" 하는 타이밍

iOS에서 다음이 바뀌면 Android도 바로 맞춤:
1. Firestore 스키마 변경 → Android DTO도 수정
2. Firebase Functions 시그니처 변경 → Android `submitStuckStatus` 호출부도 수정
3. 새 화면/피처 추가 → Android에도 동등 구현 (또는 명시적 "Android는 제외" 결정)
4. rating 문자열 추가/변경 → 45개 locale strings.xml도 동기화
5. 버전 번호 정책 — iOS가 v1.7.8이면 Android도 같은 버전 시리즈

## 독립적으로 가도 되는 영역

- **플랫폼 네이티브 UI 패턴** — iOS는 SwiftUI NavigationStack, Android는 Compose Navigation. 동일 화면이어도 구현 디테일은 다름.
- **푸시 토큰** — iOS APNs, Android FCM. Firebase Cloud Messaging 내부에서 추상화되므로 자동 처리.
- **광고** — AdMob 앱 하나 (`ca-app-pub-5283496525222246~1308884877`)지만, Android/iOS가 별도 광고 단위 ID 보유. 광고 표시 빈도 정책만 일관되면 OK.
- **배포 주기** — App Store 심사는 1~3일, Play Console 내부 테스트는 즉시. 릴리즈 타이밍은 맞추려 하되 심사 지연으로 어긋나는 건 허용.

## 검증 스크립트

```bash
# iOS의 공식 rating 문자열 조회
grep -h "rating\." /Users/imyeongjin/Desktop/side/FearIndex-iOS/FearIndex-iOS/Resources/ko.lproj/Localizable.strings

# Android의 동등 rating 문자열 조회
grep "rating_" /Users/imyeongjin/Desktop/side/FearIndex-Android/presentation/src/main/res/values-ko/strings.xml
```

양쪽 값이 같은지 육안 비교. 다르면 iOS 기준으로 Android 수정.

## 누락 기능 분석 (2026-04-15 v1.0.0 베타 기준)

iOS는 풀 스펙(33개 Presentation 컴포넌트)인데 Android는 MVP (7개)만 보유. 베타 출시 후 점진적 포팅 필요.

### Domain Entities — Android 미구현 15개

| iOS Entity | 용도 | Android 우선순위 |
|---|---|---|
| `CryptoPrice` | 암호화폐 가격 데이터 | v1.0.1 |
| `FearAnalytics` | 분석 모델 | v1.1 |
| `FearRSI` | RSI 지표 | v1.1 |
| `FearVelocity` | 속도 지표 | v1.1 |
| `FearZScore` | Z-점수 지표 | v1.1 |
| `MarketIndex` (+Mock) | 시장 지수 (S&P 500 등) | **v1.0.1 — 홈 티커 mock 사용 중** |
| `MarketInsight` | 인사이트 카드 데이터 | v1.1 |
| `PatternMatch` | 패턴 매칭 | v1.2 |
| `ReturnDataTable` | 수익률 표 | v1.1 |
| `VoteChoice` | 투표 선택지 (Buy/Hold/Sell) | v1.0.1 |
| `VoteResult` | 투표 결과 집계 | v1.0.1 |
| `WeeklyReport` | 주간 리포트 | v1.1 |

### Presentation Features — Android 미구현 26개

핵심:
- **Insights 시리즈 (4개)**: `InsightFeedView`, `InsightListView`, `InsightTeaserCard`, `InsightDetailSheet` — v1.1
- **Vote 정통**: `VoteCardView` (Buy/Hold/Sell) + `VoteCountdownView` — v1.0.1 (현재 Stuck Counter만 있음)
- **Real Tickers**: `MarketIndexTickerView`, `CryptoTickerView` — v1.0.1 (Mock 제거 필요)
- **Skeleton/Loading**: `SkeletonView` — **v1.0.1 1순위**
- **Share Sheet**: `ShareSheetView` → Android Intent.ACTION_SEND 변환 — v1.0.1
- **세분화된 알림 설정**: `MarketNotificationSettingsView` + `CryptoNotificationSettingsView` — v1.0.1 (현재 통합 1개 화면)
- **Weekly Report**: `WeeklyReportCardView` — v1.1
- **Insight Share Card**: `InsightShareCardView` — v1.1

### Utils — Android 처리 상태

| iOS Util | Android |
|---|---|
| `ChartDataFilter` | `ChartScreen.kt`의 `nearestIndex` inline ✅ |
| `ChartDateFormatter` | `xAxisFormatter()` inline ✅ |
| `ChartPeriod` | `ChartScreen.kt`의 enum ✅ |
| `ChartStyle` | inline 색상 상수 ✅ |
| `FearScoreColor` | `theme/Color.kt:fearScoreColor()` ✅ |
| `ReturnDataInterpolator` | v1.1 |

### v1.0.1 우선 순위 (베타 후 반영)

1. **SkeletonView** — 로딩 스켈레톤 (사용자 첫 인상 영향 최대)
2. **MarketIndexTickerView 실제 데이터** — `HomeScreen.kt` 코스피/S&P 500 mock 제거
3. **VoteCardView / VoteCountdownView** — 정통 Buy/Hold/Sell 투표
4. **시장/암호화폐 알림 설정 분리** — `NotificationSettingsScreen`을 2개로 분기
5. **ShareSheetView (Intent.ACTION_SEND)** — 공유 기능

## 관련 문서

- @../rules/ios-parity.md — 강제 규칙 (변경 시 iOS와 동기화)
- @firebase-setup.md — Firebase 프로젝트 공유 구조
- @bugs-fixed.md 4번 — package 오타 이슈와 교훈
- @bugs-fixed.md 8번 — Analytics 이벤트 동기화 (화면 진입 + 탭 자동 트래킹 완료)
