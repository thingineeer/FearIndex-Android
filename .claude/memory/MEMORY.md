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

## 최신 상태 (2026-07-31 새벽, v1.5.1 vc22 — Billing 8 마이그레이션 + 배포)

- **v1.5.1(vc22) production 업로드** — 1.5.0(vc21)이 심사 중인 상태에서 **대체 업로드**. 내용 = 1.5.0 전부(코스피 신호 분해 + targetSdk 36) + **Play Billing 7.1.1→8.3.0**(정책 기한 8/31, 미준수 시 업데이트 거부). changelog 22 45 locale. 관리형 게시 OFF → 승인 즉시 자동 게시.
- **⚠️ Billing 9.x 는 Kotlin 2.3 메타데이터 요구로 불가** (현재 2.1.0). 8.3.0 채택. 다음에 9.x 가려면 Kotlin/Compose/KSP 동반 업그레이드 필요. 상세: @memory/bugs-fixed.md 50번.
- **API/푸시 전수 실측 정상** (11종 200, 푸시 경로 정상, targetSdk 36 영향 없음). BTC 공매도 복구됨. 상세: 52번. **오늘 시장 계획: @docs/checkpoints/MARKET-READINESS-20260731.md**
- **다음**: 심사 승인·게시 확인 → release 머지 + v1.5.1 태그. 실기기에서 결제(₩7,500 표시 + 구매 시트) 1회 확인 — Billing 8 첫 배포라 필수. push 미실행(로컬 dev만).
- **미해결(낮음)**: `KospiFearIndexApi.history` boolean 지뢰, Yahoo spark 死코드, 미사용 BOOT_COMPLETED 권한, 결제 프로필 지급 보류(신원 확인 — 사용자 직접 처리).

## 최신 상태 (2026-07-30 밤, v1.5.0 vc21 production 업로드 — 검토 중)

- **v1.5.0(vc21) production 업로드 완료** (fastlane "Successfully finished the upload", Play Console 프로덕션 "활성 · 출시 버전 1.5.0 검토 중 · 177개국"). 내용 = 코스피 신호 분해 카드/산출 방식 시트 + **targetSdk 36**(Google Play 8/31 요건 대응, compileSdk는 기존 36) + Unity Ads SDK 본체 의존성 fix(48번, release R8 실패 해소). changelog 21 45 locale. **관리형 게시 OFF → 승인 즉시 자동 게시.**
- **다음 세션**: 게시·전파 확인 → release 머지 + v1.5.0 태그, 대시보드 API 36 경고 자동 해제 확인. push 미실행(로컬 dev만).
- **7/31 확인 (Chrome MCP)**: 게시 개요 = 빠른 검사 통과 → "변경사항을 검토 중"(프로덕션 1.5.0 + 스토어 등록정보 45 locale, 승인 시 자동 게시). **결제 계정 긴급 문제(7/24) 정체 = Google payments 신원 확인 미완료 → 지급 보류**(결제 프로필 페이지: "지급을 받으려면 본인 확인을 완료하세요" + "Google 계정 관련 중요 정보" 제목 이메일 확인 안내). 신분증 제출이라 **사용자 본인만 가능**. 부가: 싱가포르 세금 정보 제공 경고(낮은 우선순위).
- **⚠️ 신규 발견 (심각, 2026-08-31 기한)**: Play Console 알림(7/22) "곧 지원 중단될 Google Play 결제 라이브러리 사용 중 — 8/31까지 최신 버전으로 업데이트 안 하면 앱 업데이트 거부". 현재 billing-ktx **7.1.1** → **Billing Library 8.x 업그레이드 필요** (다음 릴리즈 필수 작업, PurchaseManager 마이그레이션 확인). 정책 이슈 ID 4989139547398182305.
- v1.4.2(vc20)는 7/18 게시 완료 상태였음(트랙에서 확인) — release 머지+v1.4.2 태그도 미처리 상태라 v1.5.0 태깅 시 함께 정리.

## 최신 상태 (2026-07-30, 코스피 신호 분해 카드 + 산출 방식 시트)

- **feature/kospi-signal-breakdown → dev 머지(--no-ff) 완료, push 미실행.** iOS parity: 홈 KOSPI 탭에 "코스피 신호 분해" 카드(신호별 점수/가중치/클러스터 + USD/KRW 환율 행) + ⓘ→"코스피 산출 방식" 시트 7섹션. strings 46키×45 locale, `KospiSignalText` TDD, `HomeUiState.usdKrwRate`(GetUsdKrwRateUseCase 재사용). 781 테스트 GREEN, 에뮬(Ddalggak_Play_API_34) en/ko 실데이터 검증.
- **⚠️ 배치 divergence (사용자 결정)**: iOS는 티저 아래, Android는 **상단 배너 바로 아래** — "산출 근거를 상단에 노출해 신뢰도 먼저" 지시. 상세: @memory/bugs-fixed.md 47번.
- 데이터 계층은 원래 완비 상태였음(KospiLatestDTO signals/clusters 파싱) — UI만 없었음. 미배포(버전 bump 없음, dev 로컬).

## 최신 상태 (2026-07-18 밤, v1.4.2 vc20 업로드 — IAP 경화 hotfix)

- **v1.4.2(vc20) production 업로드 완료** (fastlane exit 0, "Successfully finished the upload"). 내용 = IAP 플로우 경화 2건(AlreadyOwned 플래그 누수 fix + 조회/로드 10s timeout, 적대적 검증 발견) + TDD(AlreadyOwned 순수화). changelog 20 "결제 및 앱 안정성을 개선하였습니다."(45 locale). 관리형 게시 OFF → 승인 즉시 자동 게시. **다음 세션: 게시 확인 후 release 머지 + v1.4.2 태그.**
- **푸시 검증**: FCM 토큰→서버등록(App Check 토큰 0bc9f220... 등록)→설정동기화 실측 성공, Firebase Console 테스트 푸시 발송 완료(수신 화면은 에뮬 adb 불안정으로 유저 육안 확인 요청 상태). 수신 경로 코드는 v1.4.x 무변경 — 7/03 실수신 검증 유효.
- **결제 실검증(실기기)**: 여전히 대기 — 폰에서 1.4.1+ 업데이트 후 ₩7,500 표시+테스트 결제(라이선스 테스터 등록 완료).

## 최신 상태 (2026-07-18 후반, v1.4.1 vc19 production 업로드 — 검토 중)

- **✅ v1.4.1(vc19) 게시 완료 (2026-07-18 당일 심사·자동 게시, 공개 리스팅 1.4.1/Updated Jul 18 확인)** + **release 머지 + v1.4.1 태그 push 완료**. (이전 기록: (fastlane production 성공 = **Korean law 게이트 해제 확정**, 43번 종결). **관리형 게시 OFF** → 승인 즉시 자동 게시. changelog 19 "홈 화면 위젯과 앱 사용법 안내, 광고 제거 옵션을 추가하고 알림 설정을 개선하였습니다."(45 locale). 상세: @memory/bugs-fixed.md 37번.)
- **결제 실검증 대기**: 라이선스 테스터 등록(Play Console 설정→라이선스 테스트, Chrome 연결 끊겨 미완) → 실기기에서 1.4.1 업데이트 후 설정→Premium→Remove Ads(₩7,500) 테스트 결제. 미등록 상태 결제는 실청구되니 주의.
- **v1.4.1 내용물**: 온보딩 코치마크 투어 8단계 + Glance 위젯 4종 + **광고 제거 IAP 재도입**(revert 17e69432, 상품 `remove_ads_lifetime` Play Console 등록·활성 ₩7,500) + 알림 허브/상세 UX + 스플래시 크래시 방어 + 광고 개선(v1.4.0분 흡수).
- **데이터 보안 정책 위반(7/27 기한) 종결**: "기기 또는 기타 ID" 선언 검토 승인·게시(7/18). 원인은 관리형 게시 미전송 방치.
- **push 완료**(dev). **남은 것**: 심사 승인·게시 확인 → release 머지 + v1.4.1 태그, IAP 실결제 실기기 검증(라이선스 테스터), 위젯 실기기 배치 확인. RC force_update=1.2 유지. 앱인토스 배너(글로벌 탭에 코스피 점수 노출) 건은 별도 레포 — 유저 A/B 선택 대기.

## 이전 상태 (2026-07-18, v1.4.1 — 온보딩 코치마크 투어 + 위젯, iOS v1.9.3 parity)

- **브랜치 `feature/v1.4.1-onboarding-tour`** (dev 기준, 5커밋, **미푸시·미머지·미배포** — 유저 승인 게이트). 상세: @memory/bugs-fixed.md 35·36번, @docs/checkpoints/ONBOARDING-WIDGET-PLAN.md.
- **완료**: 8단계 첫실행 코치마크 투어(딤+컷아웃+마칭앤츠 링, Compose) + 신규설치 게이팅(`stuck_counter_prefs/deviceId` FCM 전 프로브 → `onboarding_prefs`) + Glance 위젯 4종(2×2 글로벌/코스피/암호화폐 + 4×2 대시보드) + 위젯 사용법 가이드 + 설정 "앱 사용법"(재생)/"위젯 사용법" 행. GA `onboarding_done/skip(step)`.
- **검증**: build+테스트 GREEN, E2E 15/15(스크린샷 8단계·2단계 KOSPI 자동·3시나리오·재생·GA·광고 미노출), 일본어 로케일 렌더 확인. 20키 45 locale iOS-verbatim + 감수 패널 must-fix 0. 위젯 43 locale 번역.
- **버그 fix**: 알림 권한 다이얼로그가 투어를 가리던 문제 → `notificationPromptResolved` 게이팅(bugs-fixed 35).
- **남은 것 (유저 승인 후)**: 버전 확정(1.4.1/vc?), dev 머지 + push, 위젯 실기기 배치 시각 최종확인, 배포는 승인 게이트. **v1.4.0 자체가 아직 미배포**(아래 참조)라 버전/머지 순서 유저와 조율 필요.

## 최신 상태 (2026-07-06, v1.4.0 dev 통합 — iOS v1.8.8 parity)

### ⚠️ 2026-07-07 업데이트 — v1.4.0에서 IAP 제외 + 설정/알림 UX 정리 (배포 차단 원인 규명)

- **v1.4.0 배포 차단 원인 확정 (bugs-fixed 43번)**: fastlane internal이 `To comply with Korean law ... Account Details` 로 차단. **결제 프로필("이매진") 연결로 계정이 유료 개발자 분류 → 한국 통신판매업 신고번호(Account Details) 필수**가 원인. **코드 무관 (IAP 제거해도 동일 에러)**. 세금정보(대한민국)는 이미 수락됨(무관). 사용자는 통신판매업 발급→환불 상태라 신고번호 없음. 해법: (A) 통신판매업 재신고 40,500원/년, (B) 결제 프로필 연결 해제. **사용자 결정 대기.**
- **IAP 제외 (bugs-fixed 44번)**: 사용자 지시로 광고 제거 IAP를 v1.4.0에서 **완전 제거**("광고제거는 나중에"). PurchaseManager/billing/프리미엄카드/isAdFree 게이트 전부 제거, 광고 개선 3건 보존. → 아래 "v1.4.0 = IAP 신규" 항목은 **철회됨**(IAP 다시 넣을 때 bugs-fixed 44 커밋 revert 참고).
- **설정 '개인정보 선택' 제거 (bugs-fixed 45번)**: UMP 폼 AdMob 미설정으로 항상 실패하는 죽은 버튼 → 제거.
- **알림 설정 UI 재설계 (bugs-fixed 46번)**: 탭/토글 중복 혼란 → iOS parity 허브+상세 구조로. 허브(마스터+자산별[아이콘+이름+Switch+화살표]4행) + 상세(NotificationDetailScreen 신규, 하한 빨강/상한 초록 슬라이더). Material 3. 실기기 릴리즈 검증 완료.
- **현재 dev**: `1.4.0`/`vc18`. dev 머지 완료(feature/v1.4.0-no-iap + feature/v1.4.0-settings-ux, --no-ff). **703 테스트 GREEN, 실기기 릴리즈 검증 완료.** changelog 18(45 locale, IAP 문구 제거) 준비 완료. **push 완료(dev/release/v1.3.0 태그).**
- **배포 대기 상태 (7/7 저녁 재확인)**: fastlane production 재시도(2회)도 동일 Korean law 게이트 차단 → 사용자 지시로 배포 보류. **다음 세션에서 배포하려면: 사용자가 통신판매업 신고 → Play Console 결제 프로필 "비즈니스 연락처 세부정보"에 신고번호 입력 → `bundle exec fastlane production` 재실행 → 게시·전파 확인 → RC로 Android 선택 업데이트(minimum_app_version) 1.4.0 게시.**
- **⚠️ 2026-07-18 게이트 해제 요건 충족**: 통신판매업 재발급 완료(등록면허세 40,500원 납부, 면허번호 **`2026-서울영등포-1656`**, 신고기관 서울특별시 영등포구, 상호 이매진, 3종). Play Console 계정 세부정보 "한국 개발자의 경우 추가 정보 필요"에 사업자등록번호 `1263501870` + 전자상거래 라이선스 번호 + 대행사 **저장 확인**(새로고침 후 유지 검증, Chrome MCP). `.env`에 `ECOMMERCE_LICENSE_*` 키 추가·푸시. **남은 것: fastlane 재실행으로 게이트 실해제 검증(업로드=배포 행위라 유저 승인 게이트).** 계정은 조직 계정(이매진, DUNS 696610806) 전환 완료 상태 확인.
- **브랜치 정리 완료 (7/7)**: dev에 머지된 로컬 피처 브랜치 11개(feature/v1.4.0*, v1.4.1*) 삭제. 남은 브랜치 = dev/main/release (로컬·리모트 동일).

---

- **현재 dev 기준**: Android `1.4.0` / `versionCode 18` / package `th1ngjin.fearindex`. **dev 머지 완료** (아직 배포 전 — Play Console 업로드/게시 미실행, push도 미실행: 모두 로컬 상태, 명시 요청 대기). 이전 배포본은 v1.3.0(vc17, release 태그). **⚠️ 아래 "v1.4.0 = iOS v1.8.8 5건" 중 4번 IAP는 7/7에 철회됨(위 참조).**
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
