---
name: Bugs Fixed
description: 세션별로 해결된 버그 이력. 같은 문제 재발 방지용.
type: project
---

## 2026-07-07 세션 (v1.4.0 배포 차단 원인 규명 + IAP 제외 + 설정/알림 UX 정리)

브랜치: dev ← feature/v1.4.0-no-iap, feature/v1.4.0-settings-ux (모두 --no-ff, 분기/합류 그래프). vc18/1.4.0 유지. **배포 전(로컬 dev만), push 미실행.** 703 테스트 GREEN, 실기기(Galaxy S23) 릴리즈 검증 완료.

### 43. ⚠️ fastlane "Korean law / Account Details" 에러 = 계정 레벨 게이트 (코드 무관 확정)
- **증상**: `bundle exec fastlane internal` 이 마지막 커밋 단계에서 `Google Api Error: Invalid request - To comply with Korean law, developers in Korea must provide additional information on the Account Details page.` 로 실패. 메타/스크린샷 업로드는 성공("Uploading all changes to Google Play..." 직후 실패) → **AAB 내용이 아니라 edit commit 단계의 계정 검증에서 차단**.
- **원인 (확정)**: 어제(7/6) IAP(광고 제거) 위해 **결제 프로필("이매진" 3593-7323-4054)을 개발자 계정에 연결** → 계정이 "유료/수익 발생 개발자"로 분류 → 한국 전자상거래법상 개발자 계정 Account Details의 **"비즈니스 연락처 세부정보"에 통신판매업 신고번호(E-commerce license number) + 신고기관 입력 요구**. 이게 비어 있어 **모든 트랙 업로드(internal 포함) 차단**. Google 공식 문서(support.google.com/googleplay/android-developer/answer/3255733): 유료앱/IAP 있는 한국 사업자는 사업자등록번호+통신판매업신고번호+신고기관 필수, **무료·IAP 없는 앱은 불필요**.
- **실증**: IAP를 코드에서 완전 제거(44번) 후 fastlane 재실행 → **동일 에러**. → 코드로 못 푼다 확정. iOS(Apple)는 통신판매업 안 물어봐서 문제없었음(스토어 정책 차이). 통신판매업 법적 면제(직전연도 50건 미만/간이과세자)가 있어도 **Google은 결제 프로필 연결 시 필드를 요구**. 사용자는 통신판매업을 발급받았다 환불(폐업)한 상태라 현재 신고번호 없음.
- **해법(사용자 결정 대기)**: (A) 통신판매업 재신고(등록면허세 40,500원/년, 정부24) → Account Details 입력 → 통과, (B) 결제 프로필 연결 해제(IAP 영구 포기 근접). 세금정보(대한민국)는 이미 "수락됨"(7/6 제출)이라 무관 — 문제는 통신판매업 필드.
- **교훈**: fastlane 업로드 실패 시 **어느 단계(빌드/업로드/commit)에서 나는지 로그로 구분**. "Uploading all changes" 이후 실패면 계정 레벨. AAB 코드 뜯기 전에 계정 상태부터 확인. Google Play는 결제 프로필 연결만으로 한국 통신판매업 게이트가 켜짐(앱 무료·IAP 미등록이어도).
- **재확인 (같은 세션 후반)**: 사용자 "fastlane production으로 릴리즈 트랙 올려라" 지시 → `bundle exec fastlane production`(track production/rollout 1.0/completed) 실행 → **동일하게 "Uploading all changes" 직후 Korean law 에러**. IAP 제거 + changelog 정정분까지 다 올라갔으나 edit commit에서 차단. **게이트 해제 전엔 internal/production 어느 트랙도 불가 재확인.** 사용자 결정: **배포 보류, changelog(46번 아래 참조)만 dev 커밋.** 게이트 해제(통신판매업 입력 or 결제프로필 해제)는 사용자가 나중에 직접.
- **changelog 정정 (배포 준비)**: 18.txt(vc18) 45 locale이 "광고 제거 옵션 추가"를 언급했으나 IAP 제거로 사실 불일치 → "알림 설정 개선 + 앱 안정성 향상"으로 교체(IAP 문구 완전 제거). 게이트 해제 후 이 changelog로 배포.

### 44. 광고 제거 IAP(Play Billing) 전체 제거 — v1.4.0 무료 배포용 (feature/v1.4.0-no-iap)
- **요청**: 사용자 "인앱결제 뺀 코드로 dev 1.4.0 배포", "광고제거는 나중에 넣자". 43번(통신판매업 미보유)로 IAP 활성화 불가 → v1.4.0에서 IAP 제외.
- **제거**: core PurchaseManager/IapEntitlement/IapPurchaseOutcome + 테스트, billing-ktx 의존성 + libs.versions.toml billing 정의, AppOpenAdPolicy.canShowOnForeground의 isAdFree 파라미터, AdsEntryPoint.purchaseManager(), AdBanner/HomeScreen 게이트의 isAdFree(→canRequestAds+adsEnabled만), AppOpenAdManager isAdFree 파라미터, SettingsScreen 프리미엄 카드, SettingsViewModel(구매/복원 책임 소멸로 파일 삭제), FearIndexApp/MainActivity 배선, strings IAP 10키×45locale. **광고 개선 3건(AdRetryPolicy/배너 remember·DisposableEffect/AppOpen*)과 resolveBannerHeightDp는 보존.** grep 잔존 0, 703 테스트 GREEN. 실기기 릴리즈: 설정 프리미엄 카드 사라짐·배너 정상·크래시 없음.
- **교훈**: IAP는 12파일에 얽혀 있으나 순수 로직(AppOpenAdPolicy isAdFree)까지 파라미터만 빼면 광고 게이트가 canRequestAds만으로 정상 동작. 나중에 IAP 재도입 시 이 커밋 revert 참고.

### 45. 설정 '개인정보 선택' 메뉴 제거 — UMP 폼 미설정으로 죽은 버튼
- **증상/원인**: 설정 '개인정보 선택' 탭 시 UMP `showPrivacyOptionsForm` 호출하나 실기기 로그 `Publisher misconfiguration: no form(s) configured for the input app ID (ca-app-pub-5283496525222246~1308884877)` → 항상 실패, 아무 동작 안 함. AdMob Console에 이 앱용 개인정보 동의 폼 미설정. 한국 사용자 대상이라 GDPR UMP 필수도 아님.
- **해결**: SettingsScreen에서 항목+openAdPrivacyOptions+findActivity+UMP/ScreenshotMode import 제거, settings_menu_ad_privacy 45 locale 제거. (UMP 폼을 AdMob에 정식 설정하면 재도입 가능.)

### 46. 알림 설정 UI 재설계 — 탭/토글 중복 제거, iOS parity 허브+상세 (feature/v1.4.0-settings-ux)
- **문제**: 기존 알림설정이 탭바(시장/코스피/암호화폐)와 그 아래 자산 토글 3개가 이름 겹쳐 혼란(탭=슬라이더만 분기, 토글=자산 on/off, 항상 다 보임). 사용자 "iOS처럼 깔끔하게, 사용자가 이해할 UI로. Material Design 잘 맞게".
- **재설계 (iOS NotificationSettingsView 대칭, 사용자가 '허브+상세화면' 방식 선택)**: 탭 제거. **허브**(NotificationSettingsScreen) = 마스터 토글 + 자산별 [아이콘+이름+Switch+화살표] 4행(글로벌🌐/코스피📈/암호화폐₿는 탭 시 상세 이동, 주간📅는 토글만) + Info footer. **상세**(NotificationDetailScreen 신규) = 자산별 하한(매수 기회,빨강 0xFFE53935)/상한(과열 경고,초록 0xFF00897B) 슬라이더 2섹션 + TopAppBar. ViewModel clamp+debounce 로직 그대로 재사용. NavHost에 notification_detail/{category} route.
- **strings**: iOS xcstrings에서 notification_global_title/kospi_title/lower_header/upper_header + 자산별 lower/upper description 추출(%d→%1$d, zh-Hans→zh-rCN 등 매핑), 45 locale. 미사용 notification_market_title/tab_market/tab_crypto 제거.
- **검증(실기기 릴리즈)**: 허브 4행 노출 → 글로벌 행 탭 → 상세(매수기회 30 빨강 / 과열경고 70 초록) → 하한 슬라이더 드래그 30→41 + 문구 실시간 갱신 → 뒤로 허브 복귀, 크래시 없음. 703 테스트 GREEN.
- **교훈**: Android도 iOS Form+NavigationLink 구조를 Material 3(Card+ListItem+TopAppBar+Slider)로 자연 번역 가능. 탭이 슬라이더만 바꾸는데 토글과 이름 겹치면 UX 혼란 — 자산별 [토글+상세이동]을 한 행으로 합쳐 해소.

### (참고) Play Console 사전출시 경고 3건(출시 1.3.0 기준) — 전부 라이브러리 내부, 코드 무관
- **비트맵/BaseEncoding 이미지 최적화**: 시작위치 `com.google.common.io.BaseEncoding.<clinit>`(Guava 내부, 이미지 무관 오탐). 우리 코드에 BaseEncoding/수동 이미지 다운로드 0건.
- **edge-to-edge 미처리**: MainActivity.kt:79 enableEdgeToEdge() 이미 호출 + themes.xml statusBarColor/navigationBarColor 이미 제거(19번) + EdgeToEdgePolicyTest 회귀테스트 존재 → 이미 해결됨.
- **지원중단 API(setStatusBarColor 등)**: 시작위치 `com.google.android.gms.ads.OutOfContextTestingActivity`, 난독화 D2.S/c.q.b = AdMob/Compose 내부. 우리 코드 직접 호출 0건. AdMob SDK 업데이트로만 해결.


## 2026-07-06 세션 (v1.4.0 광고 개선 3건 + 결제 검증)

사용자 질문: "인터스티셜 광고 평소 잘 뜨나? 일치율 100% 도달하게 해줘. 결제 테스트도." + "탭 진입 1~2초 뒤 배너 떠서 일치율 낮은가?"

### 39. 광고 진단 — 일치율 오해 정정 + 실제 노출 손실 3개 발견
- **일치율 개념 정정**: 일치율(match rate) 95.5%는 AdMob이 요청에 광고를 채워준 fill rate로 **코드로 100% 불가**(인벤토리/지역/타겟팅이 결정, 억지 상향은 정책 위반). 배너가 1~2초 늦게 떠도 요청→응답 오면 일치로 집계돼 일치율과 무관. 스크린샷의 진짜 문제는 eCPM -56%/수입 -53%(광고 단가 이슈, 코드 무관).
- **실제 코드 손실 3개**(explore 조사): ① 배너/인터스티셜 로드 실패 시 재시도 전무 → 첫 요청 실패=빈 슬롯 영구 ② 배너 AdView가 remember 안 됨 → 탭/세그먼트 전환마다 재생성+요청 재발, 직전 요청 낭비(사용자 "1~2초 지연" 원인) ③ 앱오픈 광고 미구현(iOS엔 있음).
- **인터스티셜 트리거 확대는 하지 않기로 결정**: iOS도 트리거 KOSPI+위젯가이드 2개뿐, 차트기간/화면전환 트리거는 과거 제거된 정책(1번), sessionCap=2는 상한이지 목표 아님. Android가 트리거 늘리는 건 iOS parity 위반 + 과거 결정 되돌리기 → 스킵.

### 40. 광고 로드 실패 재시도 + 배너 재생성 방지 (feature/v1.4.1-ad-retry)
- **AdRetryPolicy**(core, 순수 로직, iOS AdBannerView 스펙 1:1): retryDelays [5s,15s,45s] 3회 → 최종 300s 1회 → 중단. `isRetryable(errorCode)`=INVALID_REQUEST(1) 제외. **처음 exponential(2/4/8)로 만들었다가 iOS 조사 후 [5,15,45]+300s로 정정**(iOS parity). TDD 4케이스.
- **배너**: onAdFailedToLoad에서 no-fill/네트워크 실패 시 Handler.postDelayed 재요청. **AdView를 `remember(adUnitId, adSize)`로 유지 + DisposableEffect { onDispose { retryHandler 정리; adView.destroy() } }** → 리컴포지션/remount마다 재생성·요청 재발 방지. AndroidView(factory={remembered}). onAdLoaded 시 retryCount=0.
  - ⚠️ **주의**: bugs-fixed 20/23번 프레임 크기 정책 재발 방지 위해 `resolveBannerHeightDp` 로직 유지(remember 리팩터에도 그대로).
- **인터스티셜**(InterstitialAdManager): onAdFailedToLoad에서 재시도 스케줄(Handler). 로드 성공 시 retryCount 리셋. 새 로드 사이클(isRetry=false) 시작 시 이전 스케줄/카운터 정리.
- **검증**: 에뮬 설정 화면 배너 정상 노출(재생성 리팩터 회귀 없음).

### 41. 앱오픈 광고 신규 구현 (feature/v1.4.1-app-open-ad, iOS AppOpenAdCoordinator parity)
- **AppOpenAdPolicy**(core, 순수 로직): iOS canAttemptForegroundShow 1:1. **콜드스타트 최초 실행 절대 제외** — `backgroundEnteredAt`(recordBackgroundEntry에서만 set)이 nil이면 자격 없음. 최소 백그라운드 체류(30s)/세션cap(2)/cooldown(600s). recordImpression 시 backgroundEnteredAt 소비(같은 포그라운드 중복 방지). 게이트 순서: isAdFree→enabled/canRequestAds→isReady→cap→cooldown→backgroundEnteredAt존재→체류시간. TDD 11케이스.
- **AppOpenAdManager**(presentation): AppOpenAd SDK 로드/표시 + 4h 만료(iOS maxAdAge). 로드 실패 시 **자동 backoff 없음**(iOS 동일 — 다음 preloadIfNeeded 때 재시도). isForegroundBlocked 외부 가드. 정책은 AppOpenAdPolicy 위임.
- **FearIndexApp lifecycle**: companion `appOpenAdManager` 단일 인스턴스. ProcessLifecycleOwner onStop→recordBackgroundEntry, onStart→showOnForegroundIfEligible. ActivityLifecycleCallbacks로 present 대상 Activity WeakReference 추적. MobileAds.initialize 콜백에서 preload.
- **MainActivity**: `LaunchedEffect(showSplash, forceUpdate) { isForegroundBlocked = showSplash || forceUpdate }` — 스플래시/강제업데이트 중 앱오픈 겹침 방지(콜드스타트 제외로 원천 배타되지만 엣지 방어).
- **RC 4키 신규**: `app_open_ads_enabled`/`app_open_session_cap`/`app_open_cooldown_sec`/`app_open_min_background_sec`. AdsRemoteConfig에 필드+`appOpenAdConfig()` 변환(enabled = ads_enabled && app_open_ads_enabled). **기본 OFF** — Firebase RC 게시로 활성화(iOS는 자체 서버 config app_open_*이지만 Android는 RC로 흡수).
- **광고 단위 ID**: debug=Google 테스트 앱오픈(`ca-app-pub-3940256099942544/9257395921`), release=**빈 값**(AdMob Console 신규 발급 후 교체 필요, 빈 값이면 게이트 자동 차단). app+presentation 양쪽 buildConfigField.
- **검증**(에뮬, RC 임시 켬 후 원복): `AppOpenAd loaded` preload 성공 → 홈으로 백그라운드 5초 후 복귀 시 **Google Ads 전체화면 앱오픈 노출 확인** → force-stop 콜드스타트 재실행 시 **미노출(스플래시만) 확인**. 노출 후 자동 재로드도 확인.
- **교훈**: 앱오픈 콜드스타트 제외 = "백그라운드 진입 기록 nil이면 자격 없음" 플래그가 핵심(iOS backgroundEnteredAt). Android엔 scenePhase 대신 ProcessLifecycleOwner onStop/onStart. RC 임시 검증 시 반드시 원복(TEMP-VERIFY 마커로 추적).

### 42. 결제 실패 다이얼로그 동작 검증 (에뮬레이터)
- **제약**: 에뮬레이터 Play Store가 `In-app billing API version < 3` 미지원 + 상품 `remove_ads_lifetime` Play Console 미등록 → **실결제 테스트 불가**. 코드 동작만 검증 가능.
- **검증 완료**: 설정→프리미엄 카드→Remove Ads 탭 → `[IAP] 연결 Error: 3` + `[IAP] 구매 실패 Error: -1 — 상품 정보를 불러오지 못함` 로그 + 화면에 **"Purchase failed. Please try again." + "Contact: dlaudwls1203@gmail.com"(작은 글씨) + Confirm"** 다이얼로그 정상. Restore Purchases 탭 → `[IAP] 복원 Error(연결 실패)` + Analytics `광고제거복원 성공여부=false` + "No purchases to restore." 다이얼로그 정상. 모든 IAP 로그에 "Error" 토큰 포함 확인.
- **실결제 재검증**: Play Console 상품 등록 + AAB 트랙 업로드 + 라이선스 테스터 설정 후 실기기 필요.

## 2026-07-06 세션 (v1.4.0 — iOS v1.8.8 parity 5건, dev 통합)

브랜치: dev←feature/v1.4.0←4개 worktree 피처 브랜치(kospi-short-availability / notification-sync / crypto-official-endpoint / iap) + version-bump. 모두 --no-ff, 분기/합류 그래프 유지. 총 701 테스트 GREEN. **아직 배포 전(로컬 dev만), push 미실행.**

### 35. 알림 온보딩 개선 — 최초 권한 허용 시 master toggle 디폴트 ON (리텐션) + FCM 등록 후 설정 동기화
- **요청**: iOS v1.8.8 — 시스템 알림 프롬프트가 실제로 표시된 최초 결정에서 허용 시 앱 내 master toggle을 ON 저장+서버 동기화. 이미 결정된 기기(즉시 granted)는 저장값 불가침(사용자 OFF 존중).
- **구현**: `domain/.../entity/NotificationPermissionSyncPolicy.kt`에 `initialAuthorizationAction(systemAuthorized, hasStoredPreference, isFirstAuthorizationDecision)` 순수 함수 추가 (iOS `NotificationAuthorizationSyncPolicy` 1:1). sealed `InitialAuthorizationAction`: NoChange / InitializeLocalOnly(enabled) / InitializeAndSyncServer(enabled). 로직: 최초결정+허용→ON+서버동기화(저장값 무시), hasStored→NoChange(불가침), 저장값X+허용→로컬만 ON, 저장값X+미허용→OFF+동기화. TDD 5케이스.
  - **iOS "stale 저장값" 문제(App Group/iCloud 상속)는 Android에 없음** → `isFirstDecision` = "런타임 POST_NOTIFICATIONS를 지금 처음 요청해 허용됨"으로 매핑. `NotificationStorage`에 `hasStoredPreference()`(KEY_ENABLED 존재)/`hasRequestedPermission()`(신규 플래그 KEY_PERMISSION_REQUESTED) 추가.
  - MainActivity: 시작 시(33+, 미허용, 미요청) `notificationPermissionLauncher.launch(POST_NOTIFICATIONS)` — launch 시점에 `markPermissionRequested()`로 회전/킬 재프롬프트 방지. 콜백/pre-33/기결정은 `applyInitialNotificationAuthorization(granted, isFirstDecision)` → 정책 액션대로 saveSettingsLocal 또는 updateSettings(서버).
- **FCM 등록 직후 동기화**(같은 머지 단위): `NotificationRepositoryImpl.registerFCMToken`이 registerFCMToken 호출 **직후** `dataSource.updateSettings(deviceId, storage.load())` 추가. 서버 registerFCMToken이 payload에 임계값 없으면 신규 기기 즉시체크 보류 → updateNotificationSettings가 즉시체크 대신 트리거(33번 서버 공백 클라측 대응). 앱 시작+onNewToken 두 경로 모두 커버(repository 단일 지점). 등록 왕복 중 toggle 변경 반영 위해 최신 storage.load() 재조회.
- **검증**(에뮬 debug): 최초 실행 → "Allow" → prefs `notificationEnabled=true` + `NotificationDataSource: Notification settings updated` 로그. 재실행 → 프롬프트 없음 + 값 불변(NoChange). **App Check debug token 미등록 시 서버 Callable이 Unauthenticated 거부**(코드 정상, 환경 문제) → Firebase Console App Check "공포지수 Android Debug"에 debug token 등록 후 registerFCMToken+updateNotificationSettings 서버 성공 확인.
- **교훈**: 알림 초기화 정책은 순수 함수로 분리해 TDD. Android엔 iOS notDetermined 대응이 없으므로 "권한 요청 이력 플래그"로 최초 결정을 추적.

### 36. BTC 지표 cryptoOfficialIndicatorsV1 서버 endpoint 전환 + 지표 카드 출처 라벨
- **요청**: iOS v1.8.8 — CRYPTO RSI/공매도를 서버 official endpoint 경유로 전환(Binance 공식 소스), 지표 카드에 출처 라벨("source · basis · asOf") 작은 글씨 표시. SPX는 서버 endpoint 없음(FINRA/FRED가 GCP IP 차단) → 기기 직접 호출 유지.
- **구현**: `GET https://asia-northeast3-fear-index-a4f4b.cloudfunctions.net/cryptoOfficialIndicatorsV1` → `OfficialIndicatorsResponse{rsi, short}`, 각 `OfficialIndicatorSeriesDTO{available, values, closes, ratios, dates, source, basis, asOf, official, methodology}`. 클라: `closes ?? values`(RSI), `ratios ?? values`(short), available=false면 빈 시계열(카드 숨김). Wilder RSI/ShortPressure 계산은 클라 유지(변경 없음).
  - `domain/.../entity/IndicatorSourceMetadata.kt`(cardLabel="source·basis·asOf", sheetBody="source·asOf·methodology", 빈 값 제외) + `AssetCloseSeries`/`AssetShortRatioSeries`(closes/ratios + sourceMetadata). FearRSI/ShortPressure에 sourceMetadata 부착, UseCase가 `.copy(sourceMetadata=...)`.
  - Repository: CRYPTO=officialApi, MARKET=Yahoo ^GSPC(하드코딩 메타 sourceName="Yahoo Finance ^GSPC"/basis="S&P 500"/**isOfficial=false**경고아이콘), MARKET 공매도=FINRA(하드코딩 "FINRA Daily Short Sale Volume"/"SPY ETF"/asOf=마지막 파일일), KOSPI RSI=KIS/KRX(asOf=마지막 종가일), KOSPI 공매도=서버 응답 메타. **CoinGecko market_chart/Binance globalLongShortAccountRatio 직접 호출·파서 제거**(DataModule에서 BinanceFuturesApi→OfficialIndicatorsApi 교체).
  - UI: `IndicatorCards.kt` sourceLabel(labelSmall/onSurfaceVariant), `IndicatorInfoSheets.kt` SourceSection("데이터 기준" 헤더, 공식=Verified/비공식=WarningAmber 아이콘). HomeScreen이 uiState.currentRsi/currentShortPressure.sourceMetadata 주입.
- **⚠️ 절대 규칙**: source/basis/methodology는 45 locale에 노출되지만 **번역 금지 — locale-neutral 영문 하드코딩**(서버 official endpoint + 클라 하드코딩 동일). l10n 키는 `indicator_source_section`("데이터 기준"/"Data basis") 하나만 45 locale.
- **검증**(에뮬 debug 라이브): BTC RSI "Binance USD-M Futures · BTC · 2026-07-06", S&P500 RSI "Yahoo Finance ^GSPC · S&P 500", S&P500 공매도 "FINRA Daily Short Sale Volume · SPY ETF · 2026-07-02", KOSPI RSI "KIS/KRX KOSPI · KOSPI · 2026-06-30".

### 37. KOSPI 공매도 available:false 처리 — 카드 숨김 (34번 오신호 해소)
- **요청/원인**: 서버 `/api/kospi/short`가 KRX 공식 소스 확정 전까지 `{available:false, shortRatios:[]}` 반환(iOS 팀 변경). 기존 Android는 미집계 당일 `0` → "0.0% 숏커버링" 오신호(34번).
- **구현**: `KospiShortResponse`에 `available: Boolean = true`(구버전 응답 default true) 필드 추가. repo에서 `getKospiShort().takeIf { it.available }?.shortRatios.orEmpty()` → available=false면 빈 배열 → ShortPressureCalculator `count>=3` 가드에서 null → 카드 숨김(기존 null 경로 재사용, 별도 hidden 분기 없음). unavailable 응답은 미캐시(소스 복구 시 즉시 반영). TDD 4개(DTO decode 2 + repo 2).
- **검증**: 에뮬 KOSPI 탭에서 공매도 카드 미노출(RSI 카드만), 오신호 사라짐.

### 38. Android IAP(광고 제거) 신규 구현 — Play Billing (iOS PurchaseManager 1:1)
- **요청**: 사용자 지시 "안드로이드도 인앱결제 넣어". iOS는 광고 제거 non-consumable IAP(v1.6.0~). Android엔 billing 의존성 0건이었음(신규).
- **구현**: `core/purchases/PurchaseManager.kt`(@Singleton, billing-ktx 7.1.1). 상품 ID `remove_ads_lifetime`(one-time INAPP non-consumable). `isAdFree: StateFlow`(SharedPreferences "iap_prefs"/"iap.adFree.cached" 캐시로 init 동기 복원 — 첫 프레임 깜빡임 방지), `priceText`(oneTimePurchaseOfferDetails.formattedPrice, 미로드 null→UI fallback "US$4.99"), `purchaseEvents: SharedFlow`(Completed/Failed/Cancelled).
  - start()=FearIndexApp.onCreate 1회(screenshotMode 스킵): connect→queryPurchasesAsync(INAPP) entitlement 재평가→queryProductDetailsAsync 가격 로드. onResume=refreshEntitlements(외부 구매/환불 반영). purchaseRemoveAds=launchBillingFlow, 성공 PURCHASED→acknowledgePurchase(**consume 금지**, non-consumable). restorePurchases=queryPurchasesAsync 재평가.
  - **순수 로직 분리 TDD 13개**: `IapEntitlement.evaluate`(구매목록→isAdFree+미ack 토큰), `IapPurchaseOutcome.evaluate`(responseCode→Completed/Cancelled/Failed). BillingClient 글루는 얇게.
  - **코드리뷰(xhigh) 보강 5건**: purchaseInFlight AtomicBoolean 이벤트 1회 발행(launchBillingFlow 실패+onPurchasesUpdated 이중 방지), ITEM_ALREADY_OWNED→entitlement 재평가 grant(오류 다이얼로그 방지), ensureConnected 10s timeout(스피너 무한대기 방지), 상품 미로드 구매 경로 실패 시 반드시 실패 이벤트(스피너 해제), grantAdFree 중복 디스크 쓰기 제거.
  - 광고 게이트: `AdBanner.kt:70`(`isAdFree || !canRequestAds || ...`→return), 인터스티셜 `HomeScreen.kt:181`(`interstitialAdPolicyConfig(canRequestAds && !isAdFree)`), 둘 다 `AdsEntryPoint.purchaseManager()` 경유. Analytics 한글 이벤트(광고제거구매시작/완료/실패/복원) parity.
  - 설정 프리미엄 카드: SettingsScreen 최상단(Premium 헤더 + Remove Ads/가격 + Restore Purchases, 구매완료 시 초록 체크+"광고 제거됨"). SettingsViewModel(purchase/restore + isPurchasing/isRestoring + dialog). **구매 실패 다이얼로그에 "문의: dlaudwls1203@gmail.com" 작은 글씨(bodySmall)** + 복원 성공/실패 다이얼로그. **모든 IAP 로그에 "Error" 토큰**(콘솔 필터). 10키×45 locale(settings_premium_header 등, %@→%1$s).
- **검증**(에뮬 debug): 설정 프리미엄 카드 정상(Premium/Remove Ads/US$4.99 fallback/Restore Purchases), 광고 게이트 배선 확인. **실결제는 에뮬 Play Billing 제약 → 실기기 라이선스 테스터로 재검증 필요**.
- **⚠️ Play Console 사용자 작업**: 결제 프로필 연결(이매진), 상품 `remove_ads_lifetime` 등록(일회성/non-consumable/US$4.99 상당), 라이선스 테스터, 데이터 보안/앱 콘텐츠 선언.
- **교훈**: Play Billing non-consumable은 consume 금지 acknowledge만. SharedPreferences 캐시로 async 쿼리 전 첫 프레임 광고 깜빡임 방지. 이벤트 1회 발행은 AtomicBoolean 게이트로 이중 콜백 방어.


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

## 2026-07-03 세션 (FCM 정책 개편 검증 + v1.3.0 RSI/공매도 지표 배포)

### 33. FCM 알림 정책 개편 검증 — Android 코드 무수정 확인 + 서버 즉시체크 공백 발견

- **검증 3항목**: ① title/body 그대로 표시 (`FearIndexMessagingService.kt:37-38`, 가공/파싱 없음, Manifest 기본 채널 `fear_index_alerts` HIGH 일치) ② 클릭은 extras 없는 MainActivity Intent — 문구 파싱 의존 없음 ③ registerFCMToken 매 실행 + onNewToken 호출 확인.
- **실수신 테스트**: admin SDK로 서버 `sendBatch`와 동일 payload(notification+data) 발송 → 새 포맷("글로벌 시장 지수가 25. 매수 기회가 될 수 있습니다.") 백그라운드/포그라운드/탭 진입 모두 정상 (에뮬레이터). App Check debug token(MacBook emulator, 2026-07-03) Firebase Console 등록.
- **⚠️ 서버 공백 (iOS 팀 전달 필요)**: `dispatchInstantCheck`가 `registerFCMToken` **신규 유저 분기에만** 존재 (`device-callables.ts:245`). Android 신규 유저는 최초 실행 시 `notificationEnabled=false`로 등록 → 즉시체크 no-op. 알림 켜는 순간(권한 허용)은 `updateNotificationSettings` 호출인데 **여기엔 훅 없음** → Android 유저는 즉시 알림 혜택을 못 받고 30분 cron 폴백. `updateNotificationSettings`의 enabled false→true 전환 시 훅 추가 권장. instant-check.ts 주석은 양쪽 훅을 전제하나 실제 코드 미구현.
- **비고**: 임계치 실발동(서버 cron) E2E는 서버 배포 후 재검증 필요.

### 34. v1.3.0 — RSI(14)/공매도 동향 투자 지표 (iOS SSOT 포팅, TDD 45개)

- **Domain**: `RSICalculator`(Wilder's smoothing, avgLoss 0→RSI 100, history 변형은 JVM 소거 충돌로 `calculateFromHistory`), `ShortPressureCalculator`(최신값 vs 직전 5개 평균 상대변화율 ±15%, scale-invariant, baseline 0→중립), `FearRSI`/`ShortPressure` 엔티티, `GetAssetRSIUseCase`/`GetAssetShortPressureUseCase`.
- **Data**: MARKET=Yahoo `^GSPC` 6mo(기존 YahooChartApi에 getCloseChart 추가) / CRYPTO=CoinGecko market_chart 180d(getMarketChart 추가, ohlc 무료티어는 4일봉이라 부적합) / KOSPI 종가=**기존 스냅샷 `chartHistoryForDisplay[].kospiClose` 재사용**(KospiHistoryDTO에 nullable 필드 추가, 추가 API 0). 공매도: FINRA RegSHO(신규 FinraShortVolumeApi, NY 기준 주말 제외 후보 5일 병렬 fetch·부분실패 허용·suffix 3) / Binance globalLongShortAccountRatio(shortAccount×100) / 서버 `/api/kospi/short`. In-memory TTL 캐시(MARKET 12h/CRYPTO 30m/KOSPI 1h, **3개 미만 미캐시**) + `withTimeout(8s)`.
- **Presentation**: 홈 비교카드/광고 다음에 RSI 카드+공매도 카드(iOS 순서), ⓘ→ModalBottomSheet 설명 시트(RSI 눈금 막대 0/30/70/100). 선택 탭만 로드(iOS 정책), 실패/부족 시 해당 카드만 숨김(null). `indicator.*` 35키 × 45 locale (iOS xcstrings 스크립트 추출, %@→%%1$s).
- **검증**: 실기기(Galaxy S23) 3탭 육안 확인 — 시장 RSI 54.7 중립/공매도 50.2% 중립, KOSPI RSI 52.9(서버 kospiClose 1352/1355행 존재), BTC RSI 44.0/37.3% 공매도 증가(빨강). 에뮬레이터는 /data 400MB 부족으로 설치 실패(정리 필요).
- **⚠️ 발견 이슈 (서버/기존)**: ① `/api/kospi/short` 미집계 당일 값이 `0` → latest=0 → "-100%" → **"0.0%% 숏커버링" 오신호** (iOS 동일 영향, 서버 kospi-market-short-flow에서 미집계일 제외 권장) ② KOSPI SimilarEvents 카드에 `insight.kospi.event.tradeWar2018` **raw key 노출** (기존 버그, 번역 키 누락 — 별도 fix 대상).
- **배포**: versionCode 17 / 1.3.0. 버전 선택 근거 = 신규 기능(minor) + 강제 게이트가 major.minor 비교라 1.2.1은 1.2.0과 식별 불가. changelog 17 "투자 지표를 추가하였습니다."(45 locale). **AAB+changelog만 업로드**(fastlane run upload_to_play_store, 스크린샷/메타 skip). 업로드 후 **빠른 검사(pre-review scan) 통과 시 자동 검토 전송** 확인(이번엔 수동 "검토 전송" 불필요 — v1.2.0의 136개 대기와 달리 변경이 트랙+changelog뿐). 당일 심사 통과, 사용자가 관리형 게시 "게시" 클릭 → **2026-07-03 게시 완료**. release 머지 + v1.3.0 태그.
- **Git**: dev → feature/v1.3.0 → worktree feature/v1.3.0-rsi-short-indicators(5커밋: domain/파서/data·DI/presentation/버전bump). --no-ff 머지 그래프 유지.

## 주의사항

버그는 **해결 후 반드시 이곳에 추가**. 같은 문제 반복 방지가 목적.
