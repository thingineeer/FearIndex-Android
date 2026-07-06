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

## 최신 상태 (2026-07-06, v1.4.0 dev 통합 — iOS v1.8.8 parity)

- **현재 dev 기준**: Android `1.4.0` / `versionCode 18` / package `th1ngjin.fearindex`. **dev 머지 완료** (아직 배포 전 — Play Console 업로드/게시 미실행, push도 미실행: 모두 로컬 상태, 명시 요청 대기). 이전 배포본은 v1.3.0(vc17, release 태그).
- **v1.4.0 = iOS v1.8.8 동기화 5건** (worktree 단위 분할, --no-ff 그래프 유지, 총 701 테스트 GREEN):
  1. **알림 온보딩 개선**: `NotificationPermissionSyncPolicy.initialAuthorizationAction`(iOS 1:1). 시스템 프롬프트 실표시 최초 결정에서 허용 → master toggle ON + 서버 동기화. 이미 결정된 기기/사용자 OFF 불가침. MainActivity 시작 시 POST_NOTIFICATIONS 요청. + registerFCMToken 직후 updateNotificationSettings 전체 동기화(서버 즉시체크 트리거 — 33번 서버 공백 클라측 대응).
  2. **BTC 지표 cryptoOfficialIndicatorsV1 전환**: CRYPTO RSI/공매도를 서버 official endpoint(`asia-northeast3-fear-index-a4f4b.cloudfunctions.net/cryptoOfficialIndicatorsV1`)로 전환(rsi.closes 180 → 클라 Wilder RSI, short.ratios 14). CoinGecko/Binance 직접 호출 제거. `IndicatorSourceMetadata` 엔티티 + 카드 출처 라벨("source·basis·asOf") + info 시트 "데이터 기준" 섹션. SPX(Yahoo·비공식)/KOSPI(KIS/KRX)는 클라 하드코딩 메타(locale-neutral 영문, 번역 금지). `indicator_source_section` 45 locale.
  3. **KOSPI 공매도 숨김**: `/api/kospi/short` {available:false} → 빈 배열 → 카드 숨김. KospiShortResponse에 available 필드(default true). unavailable은 미캐시(소스 복구 시 즉시 반영). → 34번 "0.0% 숏커버링" 오신호 해소.
  4. **광고 제거 IAP 신규**(Play Billing): `core/purchases/PurchaseManager.kt`(iOS PurchaseManager 1:1), 상품 ID `remove_ads_lifetime`(one-time non-consumable). isAdFree StateFlow + SharedPreferences 캐시(첫 프레임 깜빡임 방지). 배너/인터스티셜 게이트(AdBanner.kt:70, HomeScreen.kt:181, AdsEntryPoint.purchaseManager()). 설정 프리미엄 카드(구매/복원). 순수 로직 IapEntitlement/IapPurchaseOutcome TDD 13개. 구매 실패 다이얼로그에 "문의: dlaudwls1203@gmail.com" 작은 라벨 + 모든 IAP 로그에 "Error" 토큰. 10키×45 locale.
- **⚠️ Play Console 사용자 작업 필요** (IAP 실동작 전제): (1) 수익 창출 결제 프로필 연결(이매진 3593-7323-4054 사용 가능), (2) 인앱 상품 `remove_ads_lifetime` 등록(일회성/non-consumable, 권장가 US$4.99 상당), (3) 라이선스 테스터 등록, (4) 데이터 보안/앱 콘텐츠 선언 업데이트. 미등록 시 앱은 가격 fallback "US$4.99" 표시 + 구매 시 실패 다이얼로그.
- **런타임 검증 완료**(에뮬 Medium_Phone_API_36.1 debug): 알림 프롬프트→ON→서버 동기화(App Check debug token `74af5682-8968-4d1f-a9b9-59753d98c5bf` Firebase Console 등록 후 registerFCMToken+updateNotificationSettings 성공 확인), BTC/SPX/KOSPI 출처 라벨 노출, KOSPI 공매도 카드 숨김, 프리미엄 카드(US$4.99 fallback+복원) 표시. IAP 실결제는 실기기 라이선스 테스터로 재검증 권장(에뮬 Play Billing 제약).
- **서버/기존 이슈 (미해결)**: ① KOSPI SimilarEvents에 `insight.kospi.event.tradeWar2018` raw key 노출 (기존 버그, 번역 키 누락 — 이번 범위 밖, fix 대상) ② `/api/kospi/short` 서버측 available:false는 iOS 팀이 처리(클라는 대응 완료).
- **RC 게이트**: `force_update_minimum_version` [Android]=`1.2` 유지 (1.4.0 통과, 조치 불필요). 배포 시 전파 확인 후 상향 검토.
- **v1.4.0에 광고 개선 3건 추가 통합 (2026-07-06, versionCode 18 유지 — 배포 전이라 흡수)**:
  1. **광고 로드 실패 재시도**: `AdRetryPolicy`(core, iOS AdBannerView 스펙 1:1 — retryDelays [5s,15s,45s] 3회 + 최종 300s 1회, INVALID_REQUEST 제외). 배너 onAdFailedToLoad + 인터스티셜 onAdFailedToLoad에서 재요청. TDD 4케이스.
  2. **배너 재생성 방지**: AdView를 `remember(adUnitId, adSize)`로 유지 + DisposableEffect destroy. 기존엔 탭(홈 세그먼트 when 분기)/바텀탭(NavHost dispose) 전환마다 AdView 재생성+요청 재발→직전 요청 낭비였음(사용자 "1~2초 지연" 원인). 이제 재진입 시 로드된 배너 즉시 표시.
  3. **앱오픈 광고 신규**(iOS AppOpenAdCoordinator parity, Android 미구현이던 채널): `AppOpenAdPolicy`(core, 콜드스타트 최초 실행 제외=backgroundEnteredAt 없으면 자격 없음 / 최소 백그라운드 30s / 세션cap 2 / cooldown 600s, TDD 11케이스) + `AppOpenAdManager`(presentation, 4h 만료). FearIndexApp ProcessLifecycleOwner onStop→백그라운드 기록/onStart→복귀 시 노출, Activity 약참조 추적, MobileAds init 콜백 preload. MainActivity 스플래시/강제업데이트 중 isForegroundBlocked. RC 4키 신규(app_open_ads_enabled/session_cap/cooldown_sec/min_background_sec, 기본 OFF). 에뮬 검증: 백그라운드 왕복 복귀 시 앱오픈 노출 확인, 콜드스타트 미노출 확인.
- **⚠️ 광고 정책 판단 (사용자 질문 대응)**: 일치율 95.5%는 정상(fill rate, 코드로 100% 불가). 스크린샷의 eCPM -56%/수입 -53%는 광고 단가 이슈(코드 무관). 인터스티셜 트리거 확대는 **안 함** — iOS도 KOSPI+위젯가이드 2개뿐이고 차트기간/화면전환 트리거는 과거 제거된 정책(bugs-fixed 1번), sessionCap=2는 상한이지 목표 아님(iOS parity 위반). 결제는 에뮬 Play Billing 미지원이라 실결제 불가, 실패 다이얼로그(문의 라벨 포함)+복원 실패 동작만 검증 완료.
- **⚠️ Play Console/AdMob 사용자 작업 (v1.4.0 배포 시)**: (기존 IAP 항목 외) **앱오픈 광고 프로덕션 단위 ID를 AdMob Console에서 신규 발급**해 app/presentation build.gradle.kts release의 `ADMOB_APP_OPEN` 빈 값 교체. **Firebase RC에 app_open_ads_enabled 등 4키 게시** 안 하면 앱오픈 안 뜸(코드 default OFF). 배너/인터스티셜용 기존 ads 키(bugs-fixed 21번)도 게시 확인 필수.
- **다음 세션 (선택)**: v1.4.0 Play Console 배포(AAB+changelog 18, 앱오픈 광고 포함), push(모든 브랜치 로컬), Play Console IAP 상품 등록 + AdMob 앱오픈 단위 발급 + RC 앱오픈 키 게시, release 머지+v1.4.0 태그(배포 통과 후), SimilarEvents raw key fix.

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
