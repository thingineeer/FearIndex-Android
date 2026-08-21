---
name: Bugs Fixed
description: 세션별로 해결된 버그 이력. 같은 문제 재발 방지용.
type: project
---

## 2026-08-21 세션 (v1.5.3 게시 모니터링 → App Check 401 근본 원인 규명·복구)

### 68. 🚨 Android 프로덕션 App Check 전수 실패(~2개월) — Firebase 인증서 미등록 + Play Integrity API 미연결 (콘솔 수정으로 즉시 복구)
- **발단**: v1.5.3 모니터링 중 Crashlytics 비치명 1위 `FirebaseFunctionsException: Unauthenticated` 1,701건/167명(1.5.2~1.5.3). 로그는 `E/App: FCM token registration failed on startup`. vc24 부터 보인 이유는 **CrashlyticsTree(60번) 신규 계측**이지 신규 문제가 아니었다.
- **규모 실측 (gcloud Cloud Run 요청 로그, 30일)**: Android(okhttp UA) 보호 Callable = **401 매일 ~1,000건 vs 200 ≈ 0**(성공은 공개 endpoint `kospipublicsnapshotv2`/`cryptoofficialindicatorsv1` 뿐, 7/22~30·8/19·8/21 의 1~5건은 내 테스트 기기). 24h 기준 registerfcmtoken 401 905 / getsimilarevents 336 / getstuckcount 258 / submitstuckstatus 70. **Firestore `users`(createdAt≥6/1) android 주간 생성 29/67/53 → 6/22(1.2.0 vc16) 이후 0**(7월 이후 3건은 전부 `-debug`). 즉 **1.3.0~1.5.3 프로덕션 Android 사용자는 단 1명도 푸시 등록·물림 카운터·유사 이벤트 Callable 을 성공한 적이 없었다.** 시점은 서버 "App Check hard mode 복구"(iOS repo 403a40b3a, 6/10 커밋 → 6/22경 S5 리팩터와 실배포)와 일치 — 그 전엔 4/30 soft mode 가 Android 토큰 실패를 가려주고 있었다.
- **원인 2개 (둘 다 콘솔, 앱 코드 무관)**:
  1. **Firebase 프로젝트 설정 → Android 앱 `th1ngjin.fearindex` 등록 지문이 SHA-256 `AD:48:68:DA…` 1개뿐** = `.env` `KEYSTORE_SHA256_V100`(**v1.0.0 폐기 키**, 4/15 앱 재등록 당시). 현재 업로드 키(`91:47:9A…`)도, **Play 설치본에 실제 찍히는 Play App Signing 키(`EF:5D:B8:C8:92:1A:9B:DC:CB:FB:AA:E1:E6:EC:A3:AE:95:22:E9:6C:0A:41:FC:84:00:0B:A2:B5:15:7C:B4:AA`)도 미등록**. Play Integrity App Check 는 verdict 의 인증서 digest 를 등록 지문과 대조 → 항상 403 "App attestation failed" → 토큰 없음 → Functions SDK 는 토큰 없이 요청 → 서버 `enforceAppCheck:true` 가 핸들러 진입 전 401(latency 2ms).
  2. **Play Console → Google Play로 보호됨 → Play Integrity API: Cloud 프로젝트 미연결("서비스 7개 중 0개 활성")**. Firebase App Check Play Integrity 의 필수 선행 조건(Play Console 에서 Firebase 프로젝트 8243517543 연결)을 앱 출시 후 한 번도 한 적이 없었다. 첫 Android 200 이 정확히 **연결 완료 시각 06:05:44Z** 에 터져 나온 걸 보면 이게 결정적 제약이었고, SHA 등록(05:58Z)만으로는 즉시 회복되지 않았다.
  - 부수: App Check Play Integrity 설정 `accountDetails.requireLicensed:true`(Play 라이선스 필수) → `false` 로 완화(정상 사용자 오탈락 방지). `deviceIntegrity.minDeviceRecognitionLevel` 은 NO_INTEGRITY(최대 관용) 그대로.
- **조치 (A, 콘솔·API — 앱 배포 불필요)**: ① Firebase Management API `POST androidApps/{app}/sha` 로 **Play 앱 서명 키 `EF:5D…` + 업로드 키 `91:47…` SHA-256 등록**(`AD:48…` 는 남겨둠, 무해) ② Play Console 앱 무결성 → Play Integrity API 설정 → **Cloud 프로젝트 연결 = fear-index(8243517543)** (응답: 앱 라이선스/애플리케이션 무결성/기기 무결성 사용) ③ App Check `requireLicensed=false`(PATCH `playIntegrityConfig?updateMask=accountDetails`).
- **복구 증거**: 수정 직후 15분 내 Android 200 — registerfcmtoken 06:05:44Z 부터 연속, getsimilarevents/getstuckcount/updatenotificationsettings 동반 성공(스페인·홍콩·멕시코 등 실사용자 IP). **Firestore 오늘 신규 android 5건(1.5.1/1.5.2/1.5.3)** — 6/22 이후 처음. 기존 1.3.0~1.5.3 설치본은 앱이 매 시작마다 등록을 시도하므로 **다음 실행에서 자동 복구**(앱 업데이트 불필요).
- **조치 (B, 1.5.4 클라이언트 보강 — feature/v1.5.4-appcheck-resilience)**: ① `core/appcheck/AppCheckTokenProbe` — 보호 Callable 전 `getAppCheckToken(false)` 선취득, 실패 시 `AppCheckFailureClassifier`(ATTESTATION_REJECTED/PLAY_INTEGRITY_UNAVAILABLE/THROTTLED/NETWORK/UNKNOWN, TDD 5)로 분류해 `Timber.w(AppCheckUnavailableException)` → Crashlytics non-fatal 에 **원인이 남는다**(기존엔 Unauthenticated 만 남아 두 달간 원인을 못 봤음) + 확정 401 호출은 보내지 않음 ② `domain/util/FcmRegistrationPolicy`(TDD 7) — 토큰 해시·설정 해시·빌드 동일 + 24h 이내면 재등록 skip(프로세스 기동마다 Play Integrity 호출·401 노이즈 억제; 시계 역행은 skip) + `NotificationStorage.lastRegistration` 스냅샷(원문 토큰 대신 SHA-256) ③ `app/notification/FcmRegistrationWorker` — 즉시 등록 실패 시 WorkManager 지수 백오프(1분~, 최대 8회, 네트워크 제약, unique KEEP) ④ Firebase BoM 33.7.0→**33.16.0**(ktx 유지 마지막 33.x; 34.x 는 ktx 제거라 별도 마이그레이션). 유닛 **1044 tests / 0 fail**.
- **⚠️ 교훈 / 재발 방지**:
  - **"사이드로드 release 403 = 사이드로드 탓"(67번)은 오진이었다.** Play 설치본도 똑같이 실패 중이었는데 사이드로드로만 검증해 놓쳤다. **App Check 는 반드시 Play 설치본(내부 테스트 트랙)으로 1회 실측**해야 한다 — 서버 로그 `httpRequest.userAgent:"okhttp" status=200` 또는 Firestore android 신규 createdAt 이 유일한 증거.
  - Firebase Android 앱 지문은 **Play 앱 서명 키 SHA-256(Play Console 앱 무결성 → 앱 서명 키)** 이 핵심. 업로드 키·로컬 keystore 지문은 사이드로드에만 해당. 키 재설정/앱 재등록 때마다 `firebase.googleapis.com/v1beta1/projects/fear-index-a4f4b/androidApps/{app}/sha` 로 실등록 값을 확인할 것(`x-goog-user-project` 헤더 필수).
  - Play Integrity API 는 **Play Console 에서 Cloud 프로젝트 연결이 없으면 토큰 자체가 안 나온다** — Firebase 쪽 설정만 보고 "됐다"고 판단 금지.
  - 서버 App Check 모드 전환(soft→hard)은 **플랫폼별 verified 메트릭을 둘 다** 확인하고 배포해야 한다(6/10 커밋 메모는 "App Attest verified 메트릭 확인"만 — iOS 만 봤다). iOS 세션에 전달 필요.
  - Crashlytics 비치명 "+9.8만%" 같은 급증이 **신규 계측 때문인지 신규 결함인지** 먼저 구분. 이번엔 계측 덕에 두 달 된 결함이 드러났다.
- **미해결/관찰**: 남은 401 은 (a) 백오프 중인 기존 기기(다음 시작에 회복) (b) 진짜 미인증 기기(사이드로드/커스텀 ROM/GMS 없음) — 1.5.4 배포 후 `AppCheckUnavailableException` kind 분포로 비율 확정. macOS 1.8.0 의 registerFCMToken 401 소수는 iOS 팀 영역.

## 2026-08-18 세션 후반 (프리미엄 parity 4종 — iOS v1.9.4 이식, ultracode)

### 67. v1.5.3(vc25) 배포 + API/푸시 임계치/결제 전수 검증 (2026-08-19)
- **배포**: v1.5.3(vc25) = 배너 콜드스타트 fix(66) + 알림 보관 분리(65) + 어댑터 진단(62). 게이트 전부 통과(1019 tests/0, AAB 19MB, SHA-1 CE:08:B4 일치, DEBUG 심볼 0, versionName 1.5.3, locale 대칭). `fastlane production` 성공, **production=[25]**, 관리형 게시 OFF.
- **✅ API health 전수 GREEN**: KOSPI v2(50.7 neutral)·KOSPI short(available=false 설계대로)·CNN(53.6)·Alternative.me(46)·Yahoo ^GSPC·CoinGecko(BTC 64299)·currency-api·Naver KOSPI·cryptoOfficialIndicators 모두 200+스키마 정상. **⚠️ 함정**: KOSPI 는 `fear-index-a4f4b.web.app/api/kospi/*`(호스팅 rewrite), CNN 은 브라우저 UA+Referer/Origin 헤더 필수 — 아무 URL/UA 로 치면 404/418 이 나와 오진한다.
- **✅ 푸시 임계치 E2E 완전 검증(이상+이하 실수신)**: 에뮬 debug + App Check debug token 신규 등록(`Claude push E2E emulator 2026-08-19`). ①KOSPI 상한 하향 → 즉시체크 발송 → **"KOSPI 51 · selling opportunity"(이상)** 수신 ②crypto 하한 상향 → **"Crypto 46 · buying opportunity"(이하)** 수신. 트레이 표시 + **알림 내역 화면에 채널/점수 정확 기록**(vc25 신규 기능 동시 검증). 서버 Firestore 에 lastCryptoNotifiedScore=46/lower/16:37:31 발송 기록 확정. 서버측 clamp 도 확인(클라 74→서버 50).
- **서버 지식(오진 방지)**: ①`updateNotificationSettings` 는 **기존 기기에도 dispatchInstantCheck 발동**(v1.8.8+, 33번 공백 해소 확인) ②글로벌(시장) 채널은 `isUsMarketPushAllowed`(오늘 ET 거래일 데이터만), KOSPI 는 `isKospiPushAllowed`(장시간/스냅샷) 게이트 — **미국 장외/KOSPI 장외엔 market=null·kospi skip 이 정상**(크론 로그의 market=null 은 버그 아님) ③크론 30분 주기, total_users 2,217, failed=0.
- **⚠️ E2E 함정**: ①사이드로드 release 는 **App Check(Play Integrity) 403** → Callable 전부 거부 — 서버 연동 E2E 는 에뮬 debug+debug token 으로 ②에뮬 화면 잠김 상태면 시스템이 "locked user" 로 알림 표시 보류 — `input keyevent KEYCODE_WAKEUP`+`wm dismiss-keyguard` 선행 ③Firebase 콘솔도 u/0 계정 주의(`?authuser=` 지정).
- **✅ 결제(광고 제거) 검증**: ①TDD — IapEntitlement/IapPurchaseOutcome/IapOfferSelection 유닛 + PremiumQaTest 계측 3종(1019 green 포함) ②S22 release 실기기 — ₩7,500 실가격 조회(Billing 8 쿼리) → 구매 탭 → **launchBillingFlow → ProxyBillingActivity → Google Play 결제 시트 실진입** ③Play 가 "이 버전의 앱에서는 결제 불가" 거부 = **사이드로드 제약**(Play 에 없는 vc25 로컬 빌드, 앱 결함 아님) ④거부 후 앱 실패 처리 정상(다이얼로그+문의 이메일+스피너 해제). 실결제 완주는 vc25 게시 후 Play 설치본으로만 가능 — 참고로 실주문 1건(7/23 HUF 1,999) 존재, vc24(Billing 8) 프로덕션 결제 크래시 0.
- **기기 정리**: S22 임계값 원복(상한 69), 그늘길 re-enable, 에뮬 종료. S22 는 AdMob 테스트 기기(66번) 유지.

### 66. 홈 배너 콜드스타트 미노출 — Next-Gen 동일 AdView 재-loadAd 무산 (실측 규명) + 테스트 기기 검증 체계
- **증상(사용자 보고)**: "광고 안 뜨는 것 같다". S22 재현 — 콜드스타트 후 홈 배너 슬롯이 수 분간 빈 공간(설정/재진입 배너는 정상).
- **진단 방법**: release 에서도 보이는 `android.util.Log("FearIndexAds")` 진단 로그를 심어 게이트 차단 사유/로드/실패코드/재시도를 추적. 실측 타임라인: 게이트(consent→sdkInit) 통과 후 loadAd → `NO_FILL` → 5s 재시도 → **`CANCELLED "Ad request cancelled by publisher action"` + `NO_FILL` 쌍** → 이후 재시도 전부 동일 무산.
- **원인 2건**:
  1. **Next-Gen SDK 는 같은 AdView 에 loadAd 재호출 시 재경매가 무산된다**(CANCELLED+NO_FILL 쌍). 같은 시각 새 AdView(홈_인사이트, 탭 재진입 홈)는 즉시 fill — 동일 뷰 재시도만 실패.
  2. backoff([5,15,45]→300s) 소진/대기 중 홈에 머물면 영구 빈 슬롯(회복 트리거 없음).
- **수정(feature/v1.5.3-banner-first-load → dev a502180)**: ① 재시도마다 **새 AdView 생성** — FrameLayout 컨테이너(remember)에 교체 장착, 콜백은 자기 AdView 캡처로 식별(교체/파기 후 늦은 콜백 무시) ② **ON_RESUME 복귀 재시도** — 미로드+미예약이면 새 사이클(로드됐으면 no-op, 첫 컴포지션 ON_RESUME 스킵) ③ 진단 로그 유지(수익 직결 상시 관측). 수정 후 실측: 재시도 클린 경매(CANCELLED 소멸), 유닛 1019 GREEN.
- **✅ AdMob 테스트 기기 등록(S22)**: 오늘 테스트로 기기 트래픽이 죄여 실광고 no-fill 이 반복되자, iOS AdInspector 대응 표준 절차로 **AdMob 콘솔 → 설정 → 기기 테스트 → S22 등록**(GAID `e8625b30-…14c0`, 광고 검사기 동작=흔들기). 등록 후 홈_상단/홈_인사이트 **onAdLoaded(retryCount=0) 즉시 fill** + "Test Ad" 라벨. GAID 는 GMS 광고 설정 화면(`am start -a com.google.android.gms.settings.ADS_PRIVACY`) uiautomator 덤프로 획득.
- **✅ 앱오픈 광고 정책 실기기 검증**(오늘 RC 로 첫 실가동): ① 콜드스타트+무입력 35s → **미노출**(topResumed=MainActivity, preload 만) ② 백그라운드 35s→복귀 → **노출**(topResumed=AdActivity, "공포지수 | Test Ad" 헤더). 41번 정책(콜드 제외/30s 체류) 실전 확인.
- **⚠️ 함정들**: ① 전면 광고 헤더의 앱 이름은 **퍼블리셔 앱**(같은 계정 타 앱 그늘길의 앱오픈을 우리 것으로 오인 — 같은 AdMob 계정은 테스트 모드 공유) ② 좌표 탭 자동화 중 전면 광고가 뜨면 **광고를 클릭해버림**(그늘길 열림) — 전면 노출 가능 시점엔 uiautomator 로 상태 확인 후 입력 ③ 반복 테스트로 기기 트래픽이 죄이면 실광고 no-fill 만 나옴 — **판단은 테스트 기기 등록 후에** ④ AdMob 콘솔 URL 이 다른 Google 계정으로 열리면 **signup 페이지**가 뜬다 — 절대 진행 말고 `?authuser=<email>` 로 재진입.
- **⚠️ S22 는 이제 테스트 광고만 나옴**(수익 발생 안 함) — 실광고 확인이 필요하면 AdMob 콘솔에서 기기 등록 해제.

### 65. 알림 내역 prune — 표시/영속 분리 (카피 과약속 + 시계 스큐 데이터 손실 동시 해소)
- **발단**: 64번 감사가 "prune 이 기기 시계 기준 물리 삭제"를 지적 → iOS 세션이 같은 구조를 먼저 고치고(dev_1.9.5 6dd603ce5) **"잠금 카피가 과약속"** 이라는 더 큰 함의를 짚어줌.
- **결함 2개(같은 코드 한 줄에서 파생)**: 옛 `fetch` 는 `prune`(기간+하드캡) 결과를 그대로 `replaceAll` 로 영속했다.
  1. **카피 과약속**: `notification_history_lock_title` = "30일 이전 내역은 프리미엄에서"(45 locale 동일)인데 실제로는 이미 물리 삭제라 **결제해도 복원 불가**. 환불 사유가 될 수 있었다.
  2. **시계 스큐 손실**: cutoff 가 기기 시계 기준이라 시계가 미래로 튀면 정상 레코드가 기간 초과로 오판 → 영구 삭제(무료 사용자, 복구 불가).
- **수정(iOS 와 동일 계약)**: `prune(records, isPremium, now)` = **표시용**(기간 숨김+하드캡, 시그니처 유지) / `persistablePrune(records)` = **영속용**(하드캡만, 시계 무관) 신규. `UseCase.fetch` 는 `persistablePrune` 결과만 `replaceAll` 하고 반환은 `prune` 으로 필터 → **저장소 무손실**.
- **회귀 테스트 7건**: 무료 숨김 시 저장소 보존(replaceAll 0), **프리미엄 fetch 즉시 복원**(카피 성립 조건을 테스트로 고정), 1년 시계 스큐 후 복귀 시 자동 복원, 하드캡 초과분만 삭제(replaceAll 1), `persistablePrune` 단위 2건, data/presentation 통합 테스트 갱신.
- **⚠️ 부작용 주의**: `replaceAll` 을 안 하게 되면서 **저장소 레코드 순서가 더 이상 정렬 보장되지 않는다**(원본 삽입 순서 유지). 표시 경로는 `prune` 이 정렬하므로 무관하나, 저장소를 직접 읽는 경로가 생기면 정렬을 가정하지 말 것. 기존 테스트 3건이 순서를 기대하고 있었고 집합 비교로 교체했다.
- **교훈**: 기존 테스트가 **결함을 "정상"으로 고정**하고 있었다 — `NotificationHistoryViewModelTest` 주석에 "무료 fetch 시 prune 된 레코드는 저장소에서도 제거됐으므로 a 만 남는다 (정책상 정상)"이라고 적혀 있었다. 테스트가 녹색이어도 그 기대값이 제품 약속(카피)과 어긋나는지 별도로 봐야 한다.
- **검증**: 전체 **1019 tests / 0 fail** + release 빌드 OK. (`:data` StuckStatusDebouncerImplTest 는 `runBlocking`+실 delay 기반이라 부하 시 flaky — 재실행 통과, 이 변경과 무관.)

### 64. v1.5.2(vc24) 배포 후 검증 — 실기기 GREEN + 적대적 감사 2건(높음 0) + KOSPI fallback 밀도 함정
- **동기**: vc24 는 224파일 9,511줄이 나간 대형 릴리스인데 Crashlytics 활성 사용자가 2명뿐이라 실사용 데이터로는 검증 불가 → 실기기 + 코드 적대적 감사 병행.
- **Crashlytics(7일)**: 크래시 **0건**, crash-free 사용자/세션 **100%**. 단 표본 2명이라 "문제 없음"의 근거로는 약함(그래서 아래 검증 수행).
- **✅ S22(SM-S901N, Android 13) 실기기 — 1.5.1 위에 vc24 업데이트 설치 경로로 검증**(clean install 아님 = 실사용자 시나리오): 콜드스타트 크래시 0, `FATAL EXCEPTION`/ANR **0건**, 프로세스 생존. 홈 실데이터(54 중립, 비교카드 전일/1주/1개월/1년 정상), **알림 내역 진입 정상**(빈 상태 "아직 받은 알림이 없어요"), **점수 탐색기 정상 렌더**(시장/코스피 양쪽, 프리미엄 잠금 카드 + 실가격 **₩7,500** + 구매 복원), **프로덕션 실광고 노출 확인**("제우스/언리얼 엔진5" 사전등록 광고).
- **⚠️ KOSPI 점수 탐색기 fallback 밀도 함정 (감사 발견 → 실측으로 영향 범위 확정)**: `DefaultReturnData.kospiVerifiedDataPoints()` 는 검증 버킷이 **11개뿐**(8/10/13/17/18/24/25/29/32/50/64)이라 fallback 사용 시 범위 8..64 중 **46개(80.7%)가 n=0 빈 화면**. **그러나 실서버 데이터는 건전**(Firestore `returnData/kospi` = 범위 8..88, 빈칸 **1개(1%)**; market 0..97 빈칸 1개, crypto 5..95 빈칸 1개) → **이 함정은 Firestore 실패/미도달 시에만 발현**. 프리미엄 결제 후 KOSPI 빈 화면 = CS 리스크이므로 1.5.3 에서 KOSPI 번들 fallback 보강 권장.
- **감사 결과 요약(심각도 높음 0건)**: ①알림내역 — 동시성 안전(@Singleton + Mutex 이중, 프로세스 분리 없음), HARD_CAP 최신순 take 정상, temp→ATOMIC_MOVE 로 rewrite 실패 시 원본 보존, 크래시 경로 0. **유일 위험 = prune 시계 의존**(기기 시계가 미래로 튀면 30일 초과 판정으로 **정상 레코드 물리 삭제**, 무료 사용자 한정, 복구 불가) → 조회 시점 필터링으로 바꾸는 것 검토. ②프리미엄/탐색기 — `entitlementOverride` release 격리 코드로 확정(호출자 debug/androidTest 뿐, release VariantHooks no-op), 결제 종료 경로 9종 전부 터미널 이벤트 보장(timeout 3곳 포함), 보간 금지·n/라벨 정합·클램프 경계 정상, **M3 Slider steps 축퇴 크래시 부재를 Material3 1.3.1 바이트코드로 실증**(calcFraction 이 b-a==0 시 0f 반환, stepsToTickFractions 가 steps=0 시 빈 배열).
- **낮음 2건**: `purchaseEvents`(replay=0 SharedFlow)를 3개 VM 이 source 구분 없이 소비 — 현재 VM 생명주기상 영구 행 재현 불가하나 진입점 증가 시 부채. KOSPI fallback 은 `HistoricalSampleCounts.same(count)` 라 1Y 표본이 1M 과 같게 과대 표시(임계 5 근처만 영향).
- **교훈**: 표본이 작은 배포 직후엔 Crashlytics 100% 를 근거로 삼지 말 것. **실기기 업데이트 경로 + 서버 실데이터 대조 + 코드 감사** 삼중으로 봐야 fallback 전용 함정(KOSPI 80.7%)처럼 실사용자 데이터엔 안 잡히는 결함이 드러난다.

### 63. Unity 대시보드 실사(Android) + FCM 플랫폼별 notification 제거 불가 확인
- **Unity 대시보드 Android 실측**(org 14569783411652): 공포지수 Android **Game ID `800107232`**(iOS 는 800107231 — 다른 값), Store ID `th1ngjin.fearindex` 정확 일치, Google Designed for Families=Disabled, child-directed=general audience. **경고 0건** — iOS 에서 나온 "Missing SKAdNetwork IDs" 는 Android 엔 해당 없음(SKAdNetwork 는 Apple 전용 항목이라 설정 화면에 존재조차 안 함).
- **Placements 4개 전부 Active(초록)**: `Banner_iOS`/`Interstitial_iOS`(800107231), `Banner_Android`/`Interstitial_Android`(800107232). AdMob 유닛 매핑(61번)의 Placement ID 와 1:1 일치.
- **⚠️ Unity payout profile 미설정 배너 있으나 우리 수익엔 무관**: Payments 페이지 원문 "Payments for impressions served by Unity with **Bidding Placement Type in AdMob** and Google Ad Manager are **handled by Google**". 우리는 전부 입찰(bidding) 방식 → 수익은 AdMob 정산. payout profile 은 Unity 직접 폭포식 때만 필요.
- **Unity 7일 실적**: 공포지수 Android $0.17/노출 14, iOS $0.00, 딸깍 iOS/Android 둘 다 $0.00. → **Unity 자체가 계정 전반에서 fill 이 거의 없다**(우리 앱만의 문제가 아님). 낙찰률 6.12% 는 설정 결함이 아니라 Unity 인벤토리 문제로 결론.
- **FCM 플랫폼별 notification 제거 — 공식 문서상 불가**: cross-platform 문서 원문 "All app instances, **regardless of platform**, can interpret the following common fields: message.notification.title, message.notification.body" + "Whenever you want to send values only to particular platforms, use platform-specific fields"(추가용이지 최상위 제거용 아님). `AndroidConfig` 에 notification 억제/데이터 전용 전환 필드 **없음**. → **iOS 는 notification 유지, Android 만 data-only** 로 만들려면 **같은 payload 를 플랫폼별로 두 번 발송**(iOS 토큰엔 notification+data, Android 토큰엔 data-only)하는 방법뿐. 서버 분기 비용이 있으므로 A 항목(트레이 직행 누락) 해결은 이 트레이드오프를 감안해 결정해야 한다.

### 62. 미디에이션 어댑터 "파일 존재 ≠ 런타임 로드" — Android 는 정상(3/3 COMPLETE), iOS 는 미로드
- **계기**: iOS 세션이 실기기 로그에서 `GADMediationAdapterUnity/Pangle = Not Ready; No such adapter in the application` 확인(프레임워크는 번들에 임베드됨). 같은 층위의 내 "AAB 에 Pangle 리소스·Unity .so 존재" 증거도 **런타임 로드를 보장하지 않는다**는 지적을 받아 Android 도 런타임 실측.
- **진단 코드**(`FearIndexApp.logAdapterStatuses`): `MobileAds.initialize(ctx, config) { initializationStatus -> ... }` 3-arg 오버로드로 `initializationStatus.adapterStatusMap` 기록. **release 는 Timber tree 가 Crashlytics 전용(60번)이라 logcat 에 안 남으므로 `android.util.Log("FearIndexAdapters")` 로도 남긴다** — 배포 후 logcat/Firebase 양쪽 확인 가능.
- **⚠️ Next-Gen enum 이름이 레거시와 다름**: 정상 상태는 `READY` 가 아니라 **`AdapterStatus.InitializationState.COMPLETE`**(NOT_STARTED/INITIALIZING/COMPLETE/TIMED_OUT/FAILED). READY 로 쓰면 컴파일 에러.
- **실측 결과 (헤드리스 에뮬 API 36)**: debug = Pangle COMPLETE(262ms)/Unity COMPLETE(1156ms)/GMA COMPLETE(5659ms). **release(R8 minify) = Unity COMPLETE(690ms)/Pangle COMPLETE(196ms)/GMA COMPLETE(2983ms)** → **Android 는 어댑터 로드 정상, R8 shrink 도 없음.**
- **수동 proguard keep 룰 불필요**: 우리 proguard-rules.pro 에 어댑터 keep 0건이지만 **GMA Next-Gen AAR 이 consumer proguard 룰 내장**(`-keep class * implements com.google.android.gms.ads.mediation.MediationAdapter`, `-keep class * extends ...Adapter`) → 어댑터가 자동 보존. release 실측이 이를 확인.
- **결론**: Unity fill 6.12%(eCPM $14.30) 의 원인은 어댑터 미로드도, 콘솔 매핑도 아니다(61번에서 콘솔 정상 확인). 남은 후보는 Unity 대시보드 쪽 Game ID 상태/인벤토리. **iOS 는 별개 문제**(어댑터 자체가 런타임 미로드 — `-ObjC` linker flag 부재 의심, iOS 세션 수정 중).
- **교훈**: 정적 증거(AAR 리소스/so, 프레임워크 임베드, nm/otool)는 런타임 로드의 근거가 못 된다. **유효한 증거는 `adapterStatusMap` 런타임 로그 하나뿐.** iOS 세션은 대조군(GoogleMobileAds) 없이 nm 으로 판정했다가 오판할 뻔했다.

### 61. 콘솔 실측 2건 — Pangle 수익 출처 오인 정정 + RC 앱오픈 키 게시 + iOS Unity 매핑 결론
- **Pangle 수익 출처 = 딸깍(Android), 공포지수 아님** (AdMob 미디에이션 보고서 30일, 측정기준 앱×광고 소스). 25행 중 Pangle 행은 `딸깍 - 키보드 소리 ASMR(Android) | Pangle ROW SDK(입찰) | US$4.03 | eCPM US$1.38 | 요청 13,811 | 일치율 39.38%` **단 1개**. 공포지수 Android 에 Pangle 행이 없는 건 정상 — **vc24 가 Pangle 어댑터 최초 포함본**(같은 날 게시).
- **공포지수 30일 실측**: Android AdMob Network US$21.21/eCPM 4.09/요청 11,330/일치율 86.66%, Android **Unity Ads US$0.67/eCPM 14.30/요청 5,389/일치율 6.12%**(eCPM 최고인데 fill 6% = 사실상 미가동 → Pangle 합류로 개선 기대). iOS AdMob US$44.89/eCPM 1.94/요청 154,876/일치율 96.61%(요청은 Android 13배인데 eCPM 절반). 계정 전체 US$363.84 중 미디에이션 파트너 기여 US$4.70(1.3%).
- **✅ RC 앱오픈 키 4개 게시**(get→최소수정→deploy, 32번 절차): `app_open_ads_enabled` default=false + **조건 "Android app users"=true**(iOS 는 자체 서버 config 사용하므로 default 는 fail-safe OFF 유지), `app_open_session_cap=2`, `app_open_cooldown_sec=600`, `app_open_min_background_sec=30` — 전부 코드 default(`AppOpenAdPolicy` sessionCap 2/cooldown 600_000ms/minBackground 30_000ms)와 일치. 파라미터 10→14, 기존 10개 무변경 assert 통과. 라이브 재조회로 반영 확인. **vc24 유저부터 앱오픈 광고 실가동.**
- **⚠️ iOS Unity 미노출은 콘솔 갭이 아님 (가설 반증)**: "공포지수 iOS 배너"(그룹 5570621994) 입찰 소스 = AdMob/Pangle ROW SDK/**Unity Ads 3개 전부 활성**, 광고 단위 매핑도 배너 6개 전부 `Game ID 800107231 / Placement Banner_iOS` 로 채워져 있음. "공포지수 iOS 전면"(1619665507)도 3소스 활성. → **콘솔 설정은 정상, 수정 불필요**. iOS Unity row 부재 원인은 콘솔 밖(앱 측 어댑터 초기화/버전 또는 단순 fill 0)에서 찾아야 함.
- **iOS 앱오프닝(7993500901)은 Unity 추가 자체가 불가**: 입찰 소스 2개(AdMob/Pangle)뿐이고, "입찰 광고 소스 추가" 다이얼로그의 선택지가 **Liftoff Monetize / Mintegral / Pangle ROW SDK 3개뿐 — Unity Ads 없음**. Unity 가 앱오프닝 형식 입찰을 지원하지 않는 구조적 제약. Android 앱오프닝 그룹도 동일 제약 예상(현재 미생성).

### 60. v1.5.2 production 배포 (vc24) — fastlane internal 은 draft 로만 올린다 + release Crashlytics 트리
- **⚠️ `fastlane internal` lane 은 `release_status` 미지정 → Play 에 "임시(draft) 버전"으로만 업로드**되고 테스터에게 안 나감. 콘솔 내부 테스트 트랙에 "비활성 · 임시 버전 1.5.2 / 버전 수정" 으로 표시됨. 그리고 **이미 업로드된 versionCode 는 다른 트랙에 재업로드 불가**("Version code 23 has already been used") → 승격은 `track:internal track_promote_to:production` 조합(54번)이거나, 새 vc 로 올려야 함. 이번엔 CrashlyticsTree 포함을 위해 **vc24 로 production 직접 업로드**(vc23 draft 는 그대로 방치, 무해).
- **교훈**: 내부 테스트를 실제 테스터에게 내보내려면 Fastfile internal lane 에 `release_status: "completed"` 추가 필요(현재 미수정 — 다음에 internal 쓸 때 고칠 것). fastlane "Successfully" 만 믿지 말고 **콘솔 트랙 페이지의 상태 문구(임시/검토 중/제공됨)** 까지 볼 것(37번 교훈 재확인).
- **API 36 경고 재확인**: vc23/vc24 AAB 의 proto manifest 에서 `targetSdkVersion "36"` 직접 파싱 확인. 1.5.1 도 36 이었으나 Google 안내문("새 버전을 프로덕션에 게시하면 해제")대로 스캐너가 프로덕션 게시 이벤트를 기다림 → vc24 게시 후 자동 해소 예상.
- **release Timber → Crashlytics 트리 (사용자 지시 "안 되면 로그 남겨 Firebase 로 나중에 알 수 있게")**: release 에 Timber tree 가 전혀 없어 `Timber.w(t, ...)`(수익률 Firestore fallback, 알림내역 read/rewrite 실패 등)가 아무 데도 안 남던 갭 발견. `app/src/release/.../variant/CrashlyticsTree.kt`(WARN+ → Crashlytics log, Throwable → non-fatal recordException) + `VariantHooks.plantLogging(crashReporter)`(release 식재/debug no-op) + FearIndexApp initFirebase 직후 호출. mapping 에 `CrashlyticsTree -> K7.a` 로 포함 확인, DEBUG 결제 심볼은 여전히 0.
- **게시 개요 실측**: "관리형 게시가 사용 중지됨" + "검토 중인 변경사항 · 빠른 검사 실행 중(최대 13분)" → 검사 통과 시 자동 검토 전송·승인 즉시 게시. production=[24].

### 59. 프리미엄 parity 4종 (iOS v1.9.4 → Android) — 점수 탐색기·알림 내역·프리미엄 게이트·DEBUG 결제 토글
- **goal 문서**: `/Users/imyeongjin/Desktop/worktrees/fi-v194-design/docs/handoff/premium-parity-android.md` (iOS SSOT). 브랜치 `feature/v1.5.2-premium-parity`(통합) ← 4 sub-worktree(returndata/history/explorer-ui/history-ui) 전부 `--no-ff` → **dev 머지 완료(beebbe8)**. push 미실행.
- **프리미엄 게이트**: 새 SKU 없음. `PurchaseManager.isPremium` = `isAdFree` 별칭. 게시값 = `entitlementOverride ?: realAdFree` — `setEntitlementOverride(Boolean?)`(QA/디버그 seam). `PremiumFeature{SCORE_EXPLORER, NOTIFICATION_HISTORY_UNLIMITED}` + `PremiumFeaturePolicy.canUse`(domain 순수). 공용 `PremiumLockRow`(잠금 제목/본문·"해제 CTA · 가격"·복원, testTag premium-lock-row/-cta) + `PremiumBadge`/`LowSampleWarningBadge`. 구매 이벤트 `source`(settings|score_explorer|notification_history) + GA `premium_lock_tapped{feature}`/`score_explorer_moved{index_type,score,period}`/`notification_history_viewed{count}` (iOS 이름 1:1).
- **점수 탐색기(차트 탭, AdBanner↔InsightFeed 사이)**: domain `ReturnHorizon`/`HistoricalSampleCounts`/`ReturnDataPoint.horizonSampleCounts`/`ReturnDataTable.sourceRange`/`ScoreExplorerStats`(정확 버킷만·보간 금지, LOW_SAMPLE_THRESHOLD=5). `DefaultReturnData` market/crypto 를 iOS 2026-08-18 집계에서 재생성(`scripts/gen-default-return-data.py`, 점수 20/48/75 iOS 값 일치 확인). UI `ScoreExplorerCard`/`InfoSheet`/`ScoreExplorerViewModel` + 순수 `ScoreExplorerSelection`(자산별 선택 보존·클램프·앵커 복귀=리셋, iOS Interactor 1:1).
- **알림 내역(홈 🔔)**: `NotificationRecord/Kind/Mapper` + `NotificationHistoryPolicy`(무료 30일/프리미엄 무제한, HARD_CAP 5000, upsert: fallback id→±120s 동일 title/body message-id 승격) + UseCase. data `JsonlFileStore`(Mutex, temp→ATOMIC_MOVE) + Codec + RepositoryImpl(filesDir/notification_history/*.jsonl). 기록 3경로(onMessageReceived/알림 탭 인텐트/activeNotifications 동기화). **서버 변경 0, Firestore 0**. 무료 리스트 배너는 홈 유닛 fallback(전용 유닛 미발급 — 사용자 결정 대기).
- **⚠️ 실결함 fix (클럭 스큐 무한루프)**: 알림내역 VM 이 updates 수신→markSeen→setLastSeenAt→updates 재발행 루프 — 레코드 receivedAt(FCM sentTime)이 markSeen 시각보다 미래면 영원히 unread. `lastMarkedNewest` 가드로 수정. **이 루프가 유닛테스트에서 test scheduler 큐 무한 증식 → -Xmx512m 힙 고갈 → GC livelock** — gradle 테스트 워커가 1시간 hang(잔존 고아 워커, jstack 으로 진단: 테스트 프레임 없음 + executor stop 대기 + GC 스레드 CPU 17분+). **교훈**: 워커 hang 은 jstack 부터 — "테스트 로직 hang"과 "종료 단계 GC 스래싱" 구분. TaskStop 은 gradle 런처만 죽이고 워커는 고아로 남는다.
- **DEBUG 결제 테스트 토글**: `core/src/debug` `DebugPremiumOverride{REAL,PURCHASED,NOT_PURCHASED}`+`DebugPremiumOverrideStore`(iap_debug_prefs 영속), `app/src/debug` `DebugPurchaseTestCard`(설정 하단, testTag debug-iap-seg-*)+`VariantHooks`(release no-op). **release dex+mapping grep 0** (DebugPremiumOverride/DebugPurchaseTestCard/debug-iap/iap_debug_prefs/entitlementOverride).
- **i18n**: `scripts/i18n/import_xcstrings_keys.py`+`check_locale_symmetry.py`, 55키×45 locale 이식, 대칭 495키 통과.
- **검증**: 유닛 **1014 tests / 0 fail**(domain 202 포함). 계측 QA 3/3 GREEN(헤드리스 에뮬 API 36, `ANDROID_SERIAL=emulator-5554`): T1 잠금 7.1s / T2 해제·슬라이더 3.5s / T3 전환(구매 안 함→구매함→즉시 해제→재잠금) 5.1s. release 빌드 OK.
- **⚠️ 계측 QA 함정 3개 (재발 방지)**:
  1. **API 36 에뮬 + Espresso 3.6.1 = 전 테스트 즉사** `NoSuchMethodException: InputManager.getInstance` — API 36 에서 제거된 리플렉션. **espresso 3.7.0 + ext-junit 1.3.0 + rules/core 1.7.0** 으로 해결.
  2. **INSTALL_FAILED_INSUFFICIENT_STORAGE**: 에뮬 /data 92%(496MB 여유)에서 100MB debug APK 설치 거부. 에뮬의 타 프로젝트 debug 앱(shadewalk·bamfiresurvive) 제거로 644MB 확보 후 통과. 에뮬 공유 시 주기 정리 필요.
  3. **M3 Slider 는 `performTouchInput{swipeLeft()}` 가 flaky**(클립/터치슬롭) — `performSemanticsAction(SemanticsActions.SetProgress){it(target)}` 가 표준(onValueChange→VM→state 전체 체인 검증 + 목표값 assert 가능).
- **남은 사용자 결정 2건**: ① 알림 내역 전용 AdMob 배너 유닛 발급(현재 홈 유닛 fallback) ② 스토어 IAP 표시명("광고 제거"→프리미엄 혜택 3종 반영 여부).

## 2026-08-18 세션 (Play 계정 이전 후속 — SA 403 / API 36 경고 원인 / GMA Next-Gen 판단)

### 53. fastlane 서비스 계정 403 — 앱이 다른 개발자 계정으로 이전됐기 때문 (SA 재초대 필요)
- **증상**: `bundle exec fastlane run google_play_track_version_codes` / `upload_to_play_store validate_only` 모두 `Google Api Error: Invalid request - The caller does not have permission`. `validate_play_store_json_key`는 성공(인증 OK, 권한만 없음).
- **원인 (Play Console 실측)**: 8/4~8/6에 FearIndex·딸깍·그늘길 3개 앱이 조직 계정 **"Myeongjin Lee"(ID 5351376807423705889)** → 개인 계정 **"이명진"(ID 5573450681823453997)** 으로 **앱 이전 완료**. 새 계정 "사용자 및 권한"에는 `fastlane-deploy@fear-index-a4f4b.iam.gserviceaccount.com`이 **없음**(dlaudwls1203 / mjplist / shadewalk SA / compute SA 4명뿐). SA 권한은 계정에 귀속되므로 이전과 함께 사라짐.
- **해결(사용자 작업)**: 새 계정 → 사용자 및 권한 → 신규 사용자 초대 → 이메일 `fastlane-deploy@fear-index-a4f4b.iam.gserviceaccount.com` → 앱 권한에서 Fear & Greed Index 추가 → 출시(프로덕션/테스트 트랙) + 스토어 등록정보 관리 권한 → 초대. (SA는 수락 절차 없이 즉시 활성.) Claude의 폼 입력은 harness 분류기가 차단(계정 권한 변경) → 사용자가 직접.
- **접근 경로**: Play Console은 `mjplist@gmail.com`(또는 dlaudwls1203)으로 로그인 → 개발자 계정 선택에서 **"이명진"** 선택. "Myeongjin Lee" 조직 계정은 이제 앱 0개(껍데기). Chrome 프로필에 따라 `/u/N` 인덱스가 다름(오늘은 `/u/6`이 이명진 계정으로 리다이렉트).
- **교훈**: fastlane 403이면 SA 키가 아니라 **앱이 어느 개발자 계정에 있는지**부터 확인. 앱 이전 시 SA/사용자 권한은 자동으로 따라오지 않는다.
- **✅ 해결(2026-08-18 11:01)**: 사용자가 이명진 계정에 SA 초대 완료 → `google_play_track_version_codes` production=[22]/internal=[8,3]/alpha=[3] 정상 조회 = 권한 복구 확인.

### 54. Play "API 36 타겟" 경고가 1.5.1 게시 후에도 남는 이유 = 내부/비공개 알파 트랙에 vc3(1.0.2) 활성
- **실측** (최신 버전 및 번들): 프로덕션 1.5.1/vc22(7/31, targetSdk 36) 외에 **비공개 테스트 Alpha `3 (1.0.2)` vc3 "Google Play에서 테스터에게 제공"** + **내부 테스트 `3 (1.0.2)` vc3 "내부 테스터에게 제공됨"**(둘 다 2026-04-21) + 내부 테스트 1.0.1/vc8 임시. Play는 **모든 활성 트랙**의 targetSdk를 검사하므로 vc3(targetSdk 35 이하)이 경고를 유지시킴. 대시보드 카드 "8월 31일까지 조치"의 알림 날짜는 7/22(1.5.0 이전).
- **해결(코드 무관)**: vc22를 내부 테스트 + 비공개 알파에 **라이브러리에서 새 버전 만들기**로 승격(재빌드/재업로드 불필요) 또는 두 트랙 일시중지. SA 복구 후 fastlane으로도 가능: `upload_to_play_store(track:"internal", version_code:22, skip_upload_aab:true, skip_upload_apk:true, skip_upload_metadata:true, skip_upload_changelogs:true, skip_upload_images:true, skip_upload_screenshots:true)` (알파는 track 이름 확인 필요).
- **✅ 실행(2026-08-18 11:03)**: `upload_to_play_store track:production track_promote_to:internal version_code:22 track_promote_release_status:completed skip_upload_*:true` (alpha도 동일) → internal=[22], alpha=[22], production=[22]. 콘솔: 내부 테스트 1.5.1 "내부 테스터에게 제공됨", 비공개 알파 1.5.1 "검토 중"(비공개 트랙은 심사 후 옛 vc3 대체). ⚠️ **첫 시도 `track:internal version_code:22 skip_upload_aab:true`는 "Successfully"라고 뜨지만 아무 변화 없음** — supply는 업로드가 없으면 update_track을 건너뛰므로 기존 번들 승격은 반드시 `track:<원본> track_promote_to:<대상>` 조합으로.
- **교훈**: 정책 경고는 프로덕션만이 아니라 **테스트 트랙의 옛 번들**도 본다. 배포 후 경고가 안 사라지면 "최신 버전 및 번들" 표부터 볼 것. 알파 심사 통과 + 스캐너 갱신 후 대시보드 "8월 31일까지 조치" 카드 자동 소멸 예상.

### 58. Crashlytics/서버 점검 (2026-08-18) — Android 미해결 5건 정리
- **전체 상태**: Android crash-free 사용자/세션 **100%**(30일, 크래시 7건/사용자 4명). iOS도 100%. **Cloud Functions 서버 오류 0**(최근 로그의 유일한 경고는 오늘 에뮬 debug 테스트의 App Check 토큰 거부 — 환경 문제).
- **미해결 이슈(30일, 전체 유형)**:
  1. **Glance 위젯 크래시** `ActionTrampolineKt.launchTrampolineAction` IllegalArgumentException "List adapter activity trampoline invoked without specifying target intent" — 3건/2명, 1.4.2~1.5.1. **실사용자 위젯 탭 크래시로 추정 — v1.5.2 fix 후보 1순위**(Glance 1.1.1 트램폴린 인텐트 소실, Glance 버전업 or actionStartActivity 인텐트 명시 확인).
  2. **Billing 8.3.0 `ProxyBillingActivity.onCreate` NPE**(PendingIntent.getIntentSender null) — 3건/1명, **1.5.1 신규**. RevenueCat 공식 문서: 자동화 테스트(Play 사전 출시 보고서 등)가 ProxyBillingActivity 를 인자 없이 기동해 발생, "실사용자 프로덕션 발생 근거 없음", **개발자가 못 고침 — crash 리포트 음소거 권장**. 1.5.1 게시(7/31) 직후 1명/3건 패턴도 봇 정황. 관찰 유지, 실사용자 발생 증거 나오면 재평가.
  3. ANR `art::ConditionVariable::WaitHoldingLocks` — 2건/1명, 1.5.0. 시스템/아트 내부, 관찰.
  4. ANR `PurchaseManager.kt:345 ensureConnected` binder 대기 — 1건/1명, **1.4.1**(Billing 7 시절 구버전). 8.3.0 마이그레이션에서 경로 변경됨 — 재발 시 재평가.
  5. WebView 이중 프로세스(crbug/558377) — 1건/1명, 1.4.2. chromium 내부, 관찰.
- **✅ 처리(같은 날 후속, 사용자 지시 "5건 해결")**:
  - **Glance 트램폴린(1) + Billing Proxy(2) = 동일 봇 세션 확정**: 두 이슈 모두 OnePlus8Pro/Android 11, 8/11 21:39:04 → 21:39:55 연속 발생 + LGE/Android 15(1 만). `ActionTrampolineActivity`/`ProxyBillingActivity` 둘 다 **`exported=false`** 라 다른 앱이 못 띄움 → 루트/계측 봇(Play 사전 출시 보고서식 액티비티 fuzzing)이 extras 없이 강제 기동한 것. Glance 트램폴린은 **lazy 리스트 fill-in intent 전용 경로**(ApplyActionKt `isLazyCollectionDescendant`)인데 우리 위젯은 Lazy 를 쓴 적 없음(git 이력 0) — 즉 우리 코드가 만든 인텐트가 아님. Glance 1.2.0-rc01 도 동일 코드(버전업 무효). **앱 코드 fix 불가/불필요 → Crashlytics 종료.**
  - **ensureConnected ANR(4) → 코드 fix**: `startConnection` 내부 bindService 바인더 호출을 메인에서 하던 것을 **IO 스레드로 이동**(+CompletableDeferred 멱등 완료, timeout 10s 유지). S22 release 에서 가격 조회 정상 재확인. feature/v1.5.2-billing-connect-offmain → dev. Crashlytics 종료.
  - ART ANR(3, Google/Android 14 = Pixel 봇 정황, "기본 잠금 경합" 통계만) / WebView 이중 프로세스(5, 1.4.2 1건) → 조치 불가, 종료(재발 시 회귀로 자동 재오픈).
  - **추가 발견(비치명, 미종료)**: `PurchaseManager.reportPurchaseFailure` "[IAP] 구매 실패 -1 상품 정보를 불러오지 못함" 3건/2명(Samsung SM-A107F·Honor, 1.4.2~1.5.1) — **실사용자**. 구매 탭 시 상품 로드 재시도까지 하고도 실패(Play 결제 미지원 계정/지역, GMS 없는 Honor 추정). 의도된 진단 로그라 열어 둠. 반복 시 실패 다이얼로그 문구에 "Play 스토어 결제 가능 계정 필요" 안내 추가 검토.
- **결론**: 배포 차단 이슈 없음, 미해결 5건 전부 종료. Next-Gen #96(MotionEvent) 크래시는 현재 0건(배포 전 — 배포 후 감시).

### 57. GMA Next-Gen SDK 1.3.1 마이그레이션 (사용자 지시 "지금 착수") — 레거시 SDK 완전 제거
- **범위**: `play-services-ads` 24.8.0 → **`ads-mobile-sdk` 1.3.1**. 광고 4파일(AdBanner/InterstitialAdManager/AppOpenAdManager/FearIndexApp) 재작성. feature/v1.5.2-gma-next-gen 3커밋 → dev --no-ff.
- **선행 툴체인**: Kotlin 2.1.0→**2.2.21** + KSP **2.2.21-2.0.5**(KSP2) + Hilt 2.53.1→**2.58**. 함정 2개: ① Hilt 2.53.1 은 KSP2 에서 `Expected @AndroidEntryPoint to have a value` 로 전멸 → 2.55+ 필요 ② **Hilt 2.59+ 는 AGP 9 필수**(우리 AGP 8.7.3) → 2.58 이 상한.
- **어댑터**: Pangle 8.2.0.4.0(NG 1.3.0 검증)/Unity 4.19.0.1+unity-ads 4.19.0(NG 1.3.1 검증). 어댑터 POM 이 레거시 SDK 를 끌어오므로 app/build.gradle.kts 에 `configurations.configureEach { exclude(play-services-ads, -lite) }` **전역 exclude 필수**. UMP 3.0.0→4.0.0(NG transitive와 정렬). runtime classpath 에 kotlin-stdlib 2.3.0 승격 확인(Kotlin 2.2 컴파일러가 소화).
- **API 변경 요점**: `MobileAds.initialize(ctx, InitializationConfig.Builder(APP_ID).build()) {}` — **background thread 필수**(ANR), App ID 는 Manifest meta-data 에서 읽음(UMP 도 그걸 요구해 meta-data 유지). 요청에 단위 ID: `AdRequest.Builder(adUnitId)` / 배너 `BannerAdRequest.Builder(id, adSize)` + `AdView.loadAd(request, AdLoadCallback<BannerAd>)`. 이벤트는 로드된 ad 객체의 `adEventCallback`(BannerAdEventCallback/InterstitialAdEventCallback/AppOpenAdEventCallback, 전부 open이라 필요한 것만 override). `FullScreenContentCallback`→각 EventCallback, `AdError`→`FullScreenContentError`. 인라인 adaptive 실제 높이는 `onAdLoaded(ad)` 의 **`ad.getAdSize()`**(22번 높이 정책 유지). `LoadAdError.code` 가 Int→**enum ErrorCode**.
- **⚠️ 콜백 전부 백그라운드 스레드**: 상태/Analytics/재시도/Compose state 는 모두 메인 Handler 로 디스패치(3파일 공통 패턴). AdBanner 는 `BannerAdSlot(adView, @Volatile disposed)` 로 destroy 후 늦은 콜백이 죽은 뷰에 재시도 거는 것 차단.
- **AdRetryPolicy SDK 비의존화**: `isRetryable(errorCode: Int)` → `isRetryable(errorCodeName: String)`. 비재시도 = INVALID_REQUEST/APP_ID_MISSING/**CANCELLED**(AdView.destroy 시 SDK 가 CANCELLED 로 콜백 — 우리가 끊은 요청). TDD 7케이스.
- **초기화 게이트 신규(AdSdkState)**: NG 는 initialize 전 load 시 `UninitializedPropertyAccessException` 위험(공식 가이드 명시) → core `AdSdkState.isInitialized`(StateFlow, TDD 3) + init 콜백 `markInitialized()`. AdBanner 는 초기화 후에만 AdView 생성, 인터스티셜/앱오픈 preload 는 미초기화 시 스킵. **부수 효과: 게이트 전 로드→destroy 로 나던 "Ad request cancelled by publisher action" 배너 실패 로그 소멸.**
- **검증**: 803 테스트 GREEN + release AAB(R8, consumer rules 에 protobuf keep 내장 — 구 #69 이슈 해결 확인) + 에뮬(Medium_Phone_API_36.1) debug: 배너 홈_상단/홈_인사이트/설정 노출, 인터스티셜 KOSPI 진입 노출→닫기→재로드, 앱오픈 백그라운드 복귀 노출→닫기→재로드(RC 기본값 TEMP-VERIFY 로 켰다 원복). afma-sdk-a-v1.3.1 UA 확인.
- **✅ 실기기(Galaxy S22, Android 13) release E2E (8/18 오후)**: vc22 release APK(upload key 서명) 설치·콜드스타트 크래시 없음. **프로덕션 실광고 fill 확인** — 홈 배너(Saxonawyer)·차트/투표 배너(TikTok Lite — Pangle 계열 소재)·설정 배너(Pocket Option), Test Ad 라벨 없음, 인라인 adaptive 높이 정상. 4탭+코스피 신호 분해 카드+KOSPI RSI 정상. **설정 Premium ₩7,500 실가격 표시 = Billing 8.3.0 실기기 가격 조회 검증**(남은 건 구매 시트 진입만). 인터스티셜은 이 세션 미노출(프로덕션 fill/타이밍 — 에뮬 debug 에선 노출·닫기·재로드 완전 검증됨). 기존 이슈 재확인: SimilarEvents `insight.kospi.event.tradeWar2018/rateHikeBear2022` raw key 노출(34번, 이번 범위 밖).
- **미검증/주의**: ① 실기기 프로덕션 인터스티셜 실노출(배포 후 AdMob 리포트로 확인) ② **1.3.x 알려진 이슈**: #96 MotionEvent recycled twice 랜덤 크래시(광고 밖 스크롤 중에도), #85 App Open show NPE — 배포 후 Crashlytics 감시, 재현 시 1.2.1 다운그레이드/1.3.2 대기 판단 ③ 배포 후 AdMob 배너 match rate/eCPM 을 마이그레이션 전 기준선과 비교(#62 fill 하락 보고) ④ NG lifecycle: v1.x Deprecation Q1 2028.

### 56. AdMob 미디에이션/광고 단위 실사 (2026-08-18, Chrome MCP) — 공포지수 Android 현황 스냅샷
- **앱**: `ca-app-pub-5283496525222246~1308884877` **준비됨 · 광고 게재 사용 설정됨**(6월 "배너 적용 불가/게재 제한" 상태는 해소된 상태로 표시). 광고 단위 6개 = HomeBanner/InsightBanner/ChartBanner/VoteBanner/SettingsBanner(배너 5) + KospiInterstitial(전면). ID 전부 `app/build.gradle.kts` release buildConfigField와 일치. **앱오프닝 단위 없음**(release `ADMOB_APP_OPEN=""` 그대로, RC `app_open_ads_enabled` 기본 OFF → 앱오픈 광고는 코드만 있고 미가동. iOS는 "공포지수 iOS 앱오프닝" 그룹 있음).
- **미디에이션 그룹**(Android 2개): "공포지수 Android 배너"(ID 6680418598, 배너 5단위, Google 최적화 전체) / "공포지수 Android 전면"(ID 4054253992, KospiInterstitial). 둘 다 **입찰 소스 = AdMob Network + Pangle ROW SDK + Unity Ads(모두 활성)**, **폭포식 소스 0**.
- **입찰 소스(계정)**: Pangle ROW SDK(직접적인 관계·활성 파트너 관계) — 공포지수 Android 매핑 6개(배너 5 → Placement 983442697, KospiInterstitial → 983442708), Unity Ads(직접적인 관계·활성) — 매핑 6개, AdMob 네트워크(승인된 구매자 407). **AppLovin은 입찰/폭포식 어디에도 없음(미연결)** — 앱 코드에도 AppLovin 어댑터 없음(Unity·Pangle만).
- **7일 성과**(8/11~17, Android): 요청 2.35천 / 노출 1.01천 / 일치율 82.75% / eCPM $3.21 / 수입 $3.23. Pangle 배너 노출 887(eCPM $0.33, 계정 전체 기준).
- **코드↔콘솔 정합**: 어댑터(Unity 4.13.1.0 + Pangle 7.8.0.8.0) ↔ 콘솔 입찰 소스(Unity, Pangle) 일치. Pangle은 로컬 머지 커밋에 이미 포함(1.5.1 배포본엔 Unity만) — 콘솔 매핑은 이미 되어 있으므로 Pangle 입찰은 **Pangle 어댑터 포함 빌드(v1.5.2) 배포 시 실가동**.
- 부수 관찰: 그늘길 Android(구, `~6115131561`) "검토 필요·게재 제한(스토어 추가)" — 별도 레포 이슈.

### 55. GMA Next-Gen SDK 이전 — 지금은 보류, v1.6.0에서 계획적으로 (근거 기록)
- **AdMob 메일(8/18)**: "GMA Next-Gen SDK가 Android 기본·권장, 레거시는 maintenance mode". 조사(공식 문서·POM·GitHub 이슈, 2026-08-18): Next-Gen **1.0.0 GA 2026-04-14 → 최신 1.3.1(7/29)**. **하드 데드라인 없음** — 레거시 v24/25는 2027-06-30 deprecated / 2028-06-30 sunset. v23.x는 이미 deprecated(2026-02-17, sunset 2027-06-30) → 이번 머지로 24.8.0.
- **보류 근거**: ① 1.3.x에 `MotionEvent recycled twice` 랜덤 크래시(#96, 광고 밖 스크롤 중에도), App Open show() NPE(#85), 배너 fill 30~50% 하락 보고(#62) ② 최신 어댑터(Pangle 8.1.0.3.0+/Unity 4.18.1.0+)가 kotlin-stdlib 2.3.0 의존 → **Kotlin 2.1.0에선 빌드 실패 가능(Kotlin ≥2.2 bump 선행)** — Billing 9.x와 같은 벽 ③ Billing 8 첫 배포(1.5.1) 결제 실기기 검증도 아직인데 광고 SDK까지 갈면 변수 겹침.
- **옮길 때 체크리스트**: Kotlin ≥2.2 → Pangle ≥8.0.0.5.0(권장 8.2.0.4.0)/Unity ≥4.18.0.0(권장 4.19.0.1+unity-ads 4.19.0) → 앱 전역 `exclude(group="com.google.android.gms", module="play-services-ads")`+`-lite`(어댑터 POM이 레거시를 끌어옴) → UMP 3.0.0→4.0.0(transitive) → `MobileAds.initialize(ctx, InitializationConfig.Builder(APP_ID))` **background thread 필수** → 모든 로드/이벤트 콜백이 background thread(메인 디스패치) → `AdRequest.Builder(adUnitId)`/`BannerAdRequest.Builder(id, adSize)`, `AdListener`→`BannerAdEventCallback`, `FullScreenContentCallback`→`InterstitialAdEventCallback`/`AppOpenAdEventCallback` → 인라인 adaptive는 `onAdLoaded`에서 `BannerAd.getAdSize()`로 실제 높이(22번 정책 유지) → 자체 `AdRetryPolicy`와 SDK 자동 재시도 중복 정리 → AdMob 배너 match rate/eCPM 기준선 A/B. 광고 코드 표면적: AdBanner/InterstitialAdManager/AppOpenAdManager/FearIndexApp 4파일 ~550줄. 공식 Claude Code skill: `npx skills add google/skills --skill google-mobile-ads-android-migrate-to-next-gen`.
- **머지 결과**: dev = origin/dev(1.5.1) + Pangle 어댑터. libs: GMA **24.8.0**, Unity 어댑터 4.13.1.0(23.6.0 빌드지만 24.0.0 breaking change에 mediation API 없음, runtime classpath에서 23.6.0→24.8.0 승격 확인), Pangle 7.8.0.8.0. 791 테스트 GREEN + release AAB(R8) 빌드 성공(vc22 그대로, 미배포).

## 2026-07-31 새벽 (v1.5.1 vc22 — Play Billing 8 마이그레이션 + API/푸시 전수 검증)

### 50. Play Billing 7.1.1 → 8.3.0 마이그레이션 (정책 기한 2026-08-31)
- **요구사항 (정책센터 원문)**: "앱에서 Google Play 결제 라이브러리 버전 8.0.0 이상을 사용해야 합니다. 2026년 8월 31일부터 모든 앱은 8.0.0 버전 이상을 사용해야 합니다." 미준수 시 **앱 업데이트 자체가 거부**. 이슈 ID `4989139547398182305`.
- **⚠️ 9.x 불가 (중요)**: `billing-ktx 9.1.0`은 **Kotlin 메타데이터 2.3.0**을 요구 → 현재 Kotlin 2.1.0에서 `Module was compiled with an incompatible version of Kotlin` 컴파일 실패. 9.x로 가려면 Kotlin 2.3 + Compose 컴파일러 + KSP 동반 업그레이드 필요. **8.3.0(8.x 최신)은 Kotlin 2.1과 호환** → 정책 요건 충족하면서 최소 변경으로 채택.
- **breaking change 2건 (우리 코드 기준 전부)**:
  1. `queryProductDetailsAsync` 콜백 2번째 인자가 `List<ProductDetails>` → **`QueryProductDetailsResult`** (`.productDetailsList` 로 접근).
  2. **일회성(INAPP) 상품도 `launchBillingFlow` 에 `offerToken` 필수.** `ProductDetails.oneTimePurchaseOfferDetails`(단수)는 Kotlin에서 접근 불가(unresolved) → **`oneTimePurchaseOfferDetailsList`(복수)** 사용. 오퍼마다 `offerToken`/`formattedPrice` 보유. (공식 integrate 문서에 "For one-time products, call getOneTimePurchaseOfferDetailsList()" 명시)
- **설계**: `IapOfferSelection`(core, 순수) 신규 — 토큰이 비지 않은 첫 오퍼 선택. **가격 표시와 결제에 같은 오퍼**를 쓰도록 `RemoveAdsOffering(product, offerToken)` 한 객체로 묶어 보관(둘이 어긋날 여지 제거). TDD 5케이스.
- **검증**: 791 테스트 GREEN, release R8 빌드 성공, **merged manifest `com.google.android.play.billingclient.version = 8.3.0`** 확인(Play가 정책 판정에 읽는 바로 그 메타데이터). 에뮬 release 실행 — 크래시 0, 설정 Premium 카드 정상(가격은 `US$4.99` fallback = Play 서명 앱이 아니라 상품 조회 실패, v1.4.0 때와 동일 정상 동작). **실결제는 게시 후 실기기(라이선스 테스터)로만 검증 가능.**
- **교훈**: 라이브러리 메이저 업그레이드는 **최신(9.x)이 항상 답이 아니다** — Kotlin 메타데이터 호환성을 먼저 확인하고 정책 요건을 만족하는 최소 버전을 고를 것. 마이그레이션 문서보다 **javap 로 api.jar 시그니처를 직접 확인**하는 게 빠르고 정확했다.

### 51. 에뮬레이터 ANR = 환경 문제 (코드 무관, 오진 방지)
- **증상**: v1.5.1 release 스모크 중 "Fear & Greed Index isn't responding" ANR.
- **원인 (증거)**: ANR Reason = `Input dispatching timed out ... Waited 5005ms for MotionEvent`, **Load: 47.86 / 18.81 / 6.94**(이후 95.31까지 상승). 같은 시각 `com.google.android.gms.persistent` ANR 후 `am_kill bg anr`로 강제 종료, `gms.icing.AppIndexingService`/`android.process.media`도 ANR. `-wipe-data` 콜드부팅 직후 Play/GMS 동기화 폭주.
- **판정 근거**: 부하 진정 후 재실행 시 앱이 포커스 보유(`mCurrentFocus=th1ngjin.fearindex/.MainActivity`)하고 설정 화면 정상 렌더. 최근 ANR은 전부 시스템 프로세스.
- **교훈**: 에뮬 ANR은 **`uptime` load average + `am_anr` 이벤트의 동반 ANR 여부**를 먼저 볼 것. Play 이미지는 root 불가라 `/data/anr/` 트레이스를 못 읽으므로 `logcat -b events|system` 이 유일한 근거. 부하 40+ 에서의 스모크 결과는 신뢰하지 말 것.

### 52. API/푸시 전수 실측 — 전 채널 정상 (2026-07-31 00:51 KST)
- **API 11종 전부 HTTP 200 + 스키마 일치**: KOSPI v2(dataDate 2026-07-30, intScore 17, isFinal true, kospiClose 1374/1374), cryptoOfficialIndicatorsV1(**BTC 공매도 available:true 로 복구** — 37번 당시 미제공이었음), CNN 37.54, Alternative.me 28, Yahoo chart v8 8심볼, CoinGecko 5종, currency-api 1444.789, Naver KOSPI/KOSDAQ, FINRA 5일.
- **KOSPI 공매도는 여전히 `available:false`** → 카드 숨김(37번 설계대로, 정상).
- **푸시 경로 정상**: 채널 `fear_index_alerts` 일치, payload 중첩 구조 서버 기대치와 일치, deviceId UUID v4 정규식 통과. **33번 "Android 신규 유저 즉시체크 공백"은 해소됨** — registerFCMToken payload에 임계값이 실려 서버 신규 분기 조건을 충족하고, 이어지는 updateSettings가 별도 훅도 발동. 서버 크론 30분 주기.
- **targetSdk 36 영향 없음**: notification trampoline/exact alarm/foreground service/BOOT_COMPLETED 수신자 전부 코드에 없음. POST_NOTIFICATIONS 런타임 요청 이미 구현.
- **⚠️ 새로 발견한 지뢰**: `KospiFearIndexApi.history` 파라미터가 개수처럼 보이지만 **서버는 boolean 취급** — `history=1`/`true`는 1374건, **`history=365`는 history 필드 자체가 없는 응답**. 현재 코드는 `if (includeHistory) 1 else null`이라 정상이나, 누가 "기간 늘리자"고 숫자를 넣으면 **KOSPI 차트/RSI가 에러 없이 빈 화면**이 된다. Boolean 타입으로 교체 권장.
- **정리 대상**: Yahoo **spark** API는 실제로 429 반환 중이나 참조 0건인 死코드(`MarketIndexApi`/`DataSource`/`RepositoryImpl` + DI 3곳). `RECEIVE_BOOT_COMPLETED` 권한도 수신자 0건.

## 2026-07-30 세션 후반 (v1.5.0 vc21 production 업로드 — targetSdk 36 + 코스피 신호 분해)

### 48. Unity 미디에이션 어댑터만 있고 SDK 본체 누락 — release R8 빌드 실패
- **증상**: `:app:minifyReleaseWithR8` FAILED — `Missing class com.unity3d.ads.*` (IUnityAdsInitializationListener 등, com.google.ads.mediation.unity 참조). debug는 minify off라 통과 → **7/26 Unity 어댑터 커밋(67410b3) 이후 release 빌드가 한 번도 안 돌았던 것**.
- **원인**: `com.google.ads.mediation:unity:4.13.1.0` 어댑터 POM이 `com.unity3d.ads:unity-ads`를 transitively 안 끌어옴 (`:app:dependencies`로 확인 — 어댑터 노드에 자식 없음). Google 미디에이션 가이드는 SDK+어댑터 둘 다 명시가 표준.
- **해결**: `unity-ads = 4.13.1` (어댑터 4.13.1.0과 짝) 명시 추가. R8 통과.
- **교훈**: 미디에이션 어댑터 추가 시 파트너 SDK 본체 동반 여부를 dependencies 트리로 확인하고, **의존성 변경 후엔 release 빌드까지 돌려볼 것** (debug만 돌리면 R8 실패가 배포 직전에 터짐).

### 49. v1.5.0(vc21) production 업로드 — targetSdk 36 (Google Play 2026-08-31 요건)
- **요건**: Play Console 경고 "앱이 Android 16(API 36) 이상을 타겟팅해야 함, 8/31부터 업데이트 불가". compileSdk는 이미 36, **targetSdk만 35→36** (libs.versions.toml 한 줄). 매니페스트에 API 36 차단 요소 없음(orientation 고정/엣지투엣지 opt-out/back opt-out 전무 — 19번에서 이미 대응).
- **배포**: v1.5.0/vc21 (신규 기능 minor bump, 47번 코스피 신호 분해 포함). changelog 21 45 locale "코스피 지수의 산출 근거를 확인할 수 있습니다. 앱 안정성을 개선하였습니다." `bundle exec fastlane production` 성공("Successfully finished the upload to Google Play"). 검증: AAB SHA-1 `CE:08:B4:...` 일치 + merged manifest targetSdkVersion=36 + 에뮬 release 스모크(카드 상단 노출).
- **Play Console 확인 (Chrome MCP)**: 프로덕션 트랙 "활성 · 출시 버전 1.5.0 검토 중 · 177개국". **관리형 게시 사용 중지 상태** → 빠른 검사 후 자동 검토 전송, 승인 즉시 자동 게시(수동 클릭 불필요). API 36 경고는 1.5.0 게시 후 자동 해제 예정.
- **⚠️ 관찰**: 대시보드에 "결제 계정에 주의가 필요한 긴급한 문제"(7/24 알림) 존재 — 업로드는 차단 안 함(Korean law 게이트는 43번에서 종결). 사용자 확인 필요.
- **⚠️ 에뮬 스모크 함정**: `adb install` 실패가 체인 중간에 묻혀 **7/18에 깔린 구버전 1.4.1을 신버전으로 착각**하고 "카드 안 보임" 오진할 뻔 — `dumpsys package | grep versionName`으로 설치본 버전 확인 후 재설치로 해소. 설치 검증은 반드시 버전 확인까지.

## 2026-07-30 세션 (코스피 신호 분해 카드 + 산출 방식 시트)

### 47. 코스피 신호 분해/산출 방식 UI — iOS parity 이식 (feature/kospi-signal-breakdown)
- **요청**: "코스피가 어떻게 산출되었는지 iOS처럼 안드로이드도 보여달라". 조사 결과 **데이터는 이미 완비** — `KospiLatestDTO`가 signals/clusterScores/confidence/missingSignals를 파싱해 `HomeUiState.kospiSnapshot`까지 올라와 있었으나 **UI가 안 쓰고 있었음** (설정 메뉴의 요약 텍스트만 존재).
- **구현** (iOS SSOT: `FearIndexView.swift` kospiSignalBreakdownSection + KospiMethodInfoSheet):
  - `KospiSignalBreakdownCard`: 신호별 [이름 + 점수(fearScoreColor) + 프로그레스바 + "클러스터 · 가중치 N%" 캡션] 최대 8행, USD/KRW 환율 행(갱신일 + 등락 배지, 한국식 상승빨강/하락파랑), 빈 상태/결측 신호 캡션.
  - `KospiMethodInfoSheet`(ModalBottomSheet): 산출 방식(원천/종가확정/252일 백분위/분리 저장) + 데이터 품질(carry-forward 3일/가중치 재분배/원천 저장) + 현재 계산 정보(기준일/계산 시각 KST/신뢰도) + 환율 보조 지표 + 신호 분해 + 클러스터 점수(가격/시장 폭/심리/신용, 소수 1자리) + 결측 처리 7섹션.
  - `KospiSignalText`(presentation/common): 서버 신호 이름 8종/클러스터/신뢰도 → 리소스 매핑, unknown 폴백. TDD 4케이스 (EventLocalizer 패턴).
  - `HomeUiState.usdKrwRate` + `GetUsdKrwRateUseCase` 주입(기존 MarketDetail용 재사용, 실패 시 행만 숨김). KOSPI 탭 refresh 시 forceRefresh.
  - strings 46키 × 45 locale — iOS xcstrings 스크립트 추출(%@→%n$s, ar 등 일부 locale은 iOS 원본이 영어값 그대로라 verbatim 유지).
- **⚠️ 배치는 iOS와 다름 (사용자 결정)**: iOS는 인사이트 티저 아래지만, "산출 근거를 상단에 노출해 지수 신뢰도를 먼저 보여달라"는 사용자 지시로 **상단 배너 바로 아래** 배치. 배너 위치(매출 핵심)는 유지. ios-parity 체크 시 이 divergence 인지할 것.
- **검증**: 781 테스트 GREEN. 에뮬(Ddalggak_Play_API_34) en/ko 실데이터 육안 — 신호 7개(서버가 현재 7개 게시: momentum 0/priceStrength 0/volatility 57/junkBond 1/safeHaven 2/foreignerFlow 13/marginBalance 12), 클러스터 점수(가격 0.2/폭 0.0/심리 27.2/신용 1.6), 신뢰도 높음, USD/KRW 1,444.8 ▼0.50%, 결측 "현재 제외된 신호가 없습니다".
- **비고**: KOSPI 탭 진입 5초 후 인터스티셜 정상 발동 확인(기존 기능). priceBreadth 신호는 서버 응답에 현재 미포함(결측 목록에도 없음) — 클라 정상, 서버가 7개만 게시 중.

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
- **✅ 해결 진행 (2026-07-18)**: 사용자가 (A)안 실행 — 통신판매업 재신고 완료(등록면허세 40,500원, 면허번호 `2026-서울영등포-1656`, 서울특별시 영등포구청, 상호 이매진, 3종). Play Console 계정 세부정보(`developer-details?tab=aboutYou`) "한국 개발자의 경우 추가 정보 필요"에 사업자등록번호 `1263501870`+라이선스 번호+대행사(서울특별시 영등포구) **저장 상태 확인**(PDF 대조 + 새로고침 유지 검증). 값은 `~/thingineeer-env/.../fearindex-android/.env`의 `ECOMMERCE_LICENSE_*`에도 기록. 게이트 실해제는 fastlane 업로드 재시도로 검증해야 함(승인 게이트). 참고: 계정이 조직 계정(이매진/DUNS)으로 전환 완료돼 있음.

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

## 2026-07-18 세션 후반 (v1.4.1 vc19 production 업로드 — Korean law 게이트 해제 확정)

### 37. v1.4.1(vc19) production 배포 — 게이트 해제 + IAP 재도입 + 관리형 게시 OFF

- **Korean law 게이트 해제 확정**: 통신판매업(2026-서울영등포-1656) 계정 세부정보 저장 후 `bundle exec fastlane production` → **업로드+커밋 성공** (43번에서 막히던 "Uploading all changes" 지점 통과). 프로덕션 트랙 "활성 · 출시 버전 1.4.1 검토 중 · 177개국". ⚠️ 첫 실행 출력이 bundler 경고에 묻혀 실패로 오인 → 재실행에서 "Version code 19 has already been used" = 첫 실행이 성공했던 것. fastlane 성공 여부는 **Play Console 트랙 상태로 확인**할 것.
- **관리형 게시 OFF**: 게시 개요에서 "관리형 게시 사용 중지" 전환 → 이후 승인 즉시 자동 게시. 전환 시 게시 준비돼 있던 데이터 보안 변경이 즉시 게시됨(7/18).
- **데이터 보안 정책 위반(7/27 기한) 종결**: 위반="기기 또는 기타 ID 미선언"(vc17). 설문은 이미 수정돼 있었으나 **관리형 게시 때문에 검토 제출이 안 된 채 방치**가 진짜 원인. "검토를 위해 변경사항 전송" → 당일 승인 → 게시. 교훈: 관리형 게시 ON이면 앱 콘텐츠 선언 수정도 게시 개요에서 전송해야 반영.
- **IAP 재도입**: `git revert 17e69432`(no-iap 커밋)로 복원 + 충돌 47파일 해소(온보딩/위젯/설정UX 보존, findActivity 수동 재추가). AdBanner 게이트 순서: inspection→screenshot→투어→isAdFree→canRequestAds. 전체 테스트 GREEN(771 실행).
- **인앱 상품 등록**(Chrome MCP): `remove_ads_lifetime` / Remove Ads / 구매옵션 lifetime(구입·비소비성·디지털 콘텐츠) / ₩7,500 기준 173개국 환산 / **활성**. ⚠️ 상품 등록은 **BILLING 권한 포함 AAB 업로드가 선행 조건** ("새 APK 업로드" 안내가 뜨면 AAB부터).
- **Crashlytics 점검**: crash-free 100%. 30일 미해결 2건 — ① SplashView.kt:69 Resources$NotFoundException(1건, 1.0.1~1.2.0, 앱 업데이트 중 리소스 교체) → **Drawable 직접 로드+실패 시 아이콘 생략 방어 적용**(painterResource는 try/catch 불가) ② FearIndexApp.initAdMob ANR(2건, SDK 내부, 이미 백그라운드 init 권장 패턴) → 관찰 유지.
- **온보딩 투어 UX fix 2건**(유저 피드백): ① 대형 앵커(4단계 인사이트, 화면 55%+ 덮음)는 카드를 탭바 쪽 하단 배치(TDD 5케이스) ② 인터스티셜 5초 지연 중 투어 시작 엣지 방어 1줄.
- **인터스티셜 실기+적대적 검증**: 정상 노출(KOSPI 진입 5s 후, 시각+이벤트) / 세션 1회 제한 / 투어 중 억제·탭 흡수 / 투어 후 기회 미소모 — 모두 확인. 코드 검증 3에이전트 확정 이슈 low 3건뿐.
- **이 머신 최초 셋업**: `~/.gradle/gradle.properties` 부재로 signReleaseBundle NPE → `install.sh` 실행으로 해결. bundler 2.6.9 재설치 필요했음(`gem install bundler -v 2.6.9`).
- **미검증**: IAP 실결제(라이선스 테스터 실기기 필요), 에뮬 최종 스모크(에뮬 불안정 — 프리미엄 카드 렌더는 v1.4.0 시절 동일 코드로 검증된 이력 44번).
- **남은 것**: 심사 승인 → 자동 게시 확인 → release 머지+v1.4.1 태그. RC force_update=1.2 유지(조치 불필요). 앱인토스 "지금 공포지수 23" 배너 건은 별도 레포(유저 A/B 선택 대기).

## 2026-07-18 세션 (v1.4.1 — 온보딩 코치마크 투어 + 위젯)

### 35. 온보딩 투어가 알림 권한 다이얼로그에 가려 안 뜸 (첫 실행)

- **증상**: 신규 설치/QA 강제 모두 투어 카드가 화면에 안 나타남. E2E 12 FAIL. 하지만 `onboarding_prefs` 는 `onboardingTourEligibleV1=true` + `hasSeenOnboardingTourV1=true` — **투어는 정상 시작(자격 판별·마킹 정상)됐으나 보이지 않음**.
- **근본원인 (로그로 확인, 추측수정 금지)**: `dumpsys window mCurrentFocus` = `GrantPermissionsActivity`. MainActivity 가 첫 실행에 POST_NOTIFICATIONS 시스템 다이얼로그(`maybeRunInitialNotificationAuthorization`, v1.4.0 알림 온보딩)를 띄우는데, 이게 투어 오버레이 **위에** 떠서 가림. 투어는 그 밑에서 활성 상태.
- **해결**: `MainActivity.notificationPromptResolved: MutableState<Boolean>` 추가 → `readyForTour = !showSplash && !forceUpdate && notificationPromptResolved`. 권한 프롬프트 콜백/불필요(이미 결정·스크린샷모드) 시 true. 즉 **다이얼로그가 걷힌 뒤에만 투어 시작**.
- **교훈**: 첫 실행 오버레이(투어)는 첫 실행 시스템 프롬프트(알림 권한)와 충돌한다. "안 뜬다" 진단 시 **prefs 상태 + mCurrentFocus 로 '시작됐지만 가려짐'과 '아예 안 시작됨' 구분**. E2E 에선 `pm grant POST_NOTIFICATIONS` 로 다이얼로그 회피.

### 36. 온보딩 투어 + 위젯 구현·검증 (iOS v1.9.3 parity)

- **범위**: 8단계 코치마크 투어(딤+컷아웃+마칭앤츠 링+카드, Compose 재작성) + Glance 위젯 4종(2×2 3 + 4×2 대시보드) + 위젯 사용법 가이드 + 설정 "앱 사용법"(재생)/"위젯 사용법" 행. iOS 라벨 20키 45 locale verbatim.
- **게이팅**: `stuck_counter_prefs/deviceId`(v1.0.0부터 불변) raw 프로브(FCM 전) → 신규설치만 자동 노출. iOS `OnboardingEligibility`(fcm_device_id) 미러. `OnboardingStorage`(data) + `OnboardingEligibility`(domain, TDD).
- **배선**: `OnboardingTourViewModel`(activity-scoped, NavHost 생성) + `LocalOnboardingTour` CompositionLocal. 앵커=`Modifier.onGloballyPositioned{boundsInWindow}`. NavHost LaunchedEffect 로 단계별 탭/세그먼트/스크롤 구동, 종료 시 홈 market 최상단. 투어 중 인터스티셜(HomeScreen effect)/앱오픈(MainActivity 전달)/배너(AdBanner LocalOnboardingTour 게이트) 억제. 스켈레톤은 HomeVM init eager load 로 미노출.
- **위젯**: `WidgetEntryPoint`(Get(Fear/Kospi/Crypto)FearIndexUseCase), Glance 1.1.1, `actionStartActivity<MainActivity>`, `FearWidgetUpdateWorker`(3h)+포그라운드 updateAll. providers 4개 등록·픽커 노출 확인. **실기기 배치 시각 최종확인은 남음**(런처 자동화 flaky).
- **검증**: E2E(`scripts/e2e/onboarding_tour_check.py`) 15/15 — 8단계 워크스루 스크린샷, 2단계 KOSPI 자동 세그먼트, 3시나리오(신규 노출/기존 미노출/강제종료 미노출), 설정 재생, GA `onboarding_done{8}`/`onboarding_skip{5}`, 광고/스켈레톤 미노출. 일본어 로케일 렌더 확인. 20키 감수 패널 must-fix 0.
- **브랜치**: `feature/v1.4.1-onboarding-tour` (dev 기준, 5커밋). **미푸시·미머지·미배포** — 유저 승인 게이트. app ID `4973920645070208584`... (버전 확정/dev 머지는 다음).

## 주의사항

버그는 **해결 후 반드시 이곳에 추가**. 같은 문제 반복 방지가 목적.
