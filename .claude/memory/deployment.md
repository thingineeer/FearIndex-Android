---
name: Deployment
description: 릴리즈 빌드, 서명, Play Console 업로드까지의 절차와 상수.
type: reference
---

# Deployment

## 빠른 상수

| 항목 | 값 |
|---|---|
| Package (prod) | `th1ngjin.fearindex` |
| Package (debug) | `th1ngjin.fearindex.debug` |
| versionCode | `app/build.gradle.kts:20` 참고 (매 제출마다 증가) |
| versionName | `app/build.gradle.kts:21` |
| Keystore 파일 (활성) | `~/fearindex-secrets/fearindex-release.keystore` (PKCS12) |
| Keystore 원본 (SSOT) | `~/thingineeer-env/android/fearindex/fearindex-release.keystore` |
| Keystore alias | `upload` (v1.0.3+) |
| Keystore 비밀번호 | `~/.gradle/gradle.properties`의 `FEARINDEX_STORE_PASSWORD` 키 사용 — 코드/문서에 직접 기록 금지 |
| SHA-1 (활성, v1.0.3+) | `CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16` |
| SHA-256 (활성, v1.0.3+) | `91:47:9A:4E:3C:F6:F4:F0:D4:0C:1D:AB:C8:0E:95:94:AE:91:EB:0C:64:1B:A7:ED:3B:D6:79:44:BC:AB:57:A2` |
| AdMob App ID | `ca-app-pub-5283496525222246~1308884877` |
| AdMob HomeBanner | `ca-app-pub-5283496525222246/3189551565` |
| AdMob InsightBanner | `ca-app-pub-5283496525222246/1779867597` |
| AdMob ChartBanner | `ca-app-pub-5283496525222246/1616216062` |
| AdMob VoteBanner | `ca-app-pub-5283496525222246/2417949811` |
| AdMob SettingsBanner | `ca-app-pub-5283496525222246/4627498578` |
| AdMob KospiInterstitial | `ca-app-pub-5283496525222246/1522532479` |
| AdMob AppOpen (2026-08-18 발급) | `ca-app-pub-5283496525222246/6583206280` |

### 키 이력 (폐기됨, Firebase 등록 정리 시 참고)

| 시기 | alias | SHA-1 |
|---|---|---|
| v1.0.0 | `fearindex` | `A1:54:8A:92:C3:AF:A5:0E:BD:31:F6:6B:47:1B:9E:BB:51:5D:23:51` |
| v1.0.1~v1.0.2 | `fearindex` | `81:AD:9D:5D:9A:E1:50:EB:F1:AE:9D:AF:86:CB:03:3D:67:6B:2A:75` |
| **v1.0.3+ (활성)** | **`upload`** | **`CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16`** |

진짜 활성 keystore 는 **`~/thingineeer-env/android/fearindex/`** 의 README 가 SSOT. 새 머신 셋업 시 `bash ~/thingineeer-env/android/fearindex/install.sh` 실행. 자세한 사고 이력은 `@bugs-fixed.md` 17번 참조.

## 출시 이력

| 버전 | versionCode | 게시일 | 상태 | 주요 변경 |
|---|---|---|---|---|
| v1.0.0 | 7 | 2026-05-09 | 정식 게시 ✅ | 최초 출시 |
| v1.0.1 | 9 | 2026-05-12 | 정식 게시 ✅ | Android 15 edge-to-edge 호환성 + 안정성 개선 (`fastlane production` 자동화 첫 사용) |
| v1.1.0 | ~12 | 2026-06-11 | 정식 게시 ✅ | KOSPI Fear Index, iOS 1.8.x parity, 실데이터/차트 |
| v1.1.1 | 13 | 2026-06-12 | 정식 게시 ✅ | 실데이터/차트 hotfix + Android 15 edge-to-edge & AdMob 배너 프레임 fix |
| v1.1.2 | 14 | 2026-06-15 | 게시 완료 ✅ | **강제 업데이트(Play In-App Update) + AdMob 정책 대응**. 177개국 100% rollout. Firebase Console Remote Config `force_update_minimum_version` Android=1.1 게시 완료 |
| v1.1.3 | 15 | 2026-06-16 | production 업로드 ✅ (전파 중) | **배너 광고 미표시 버그 fix** (inline adaptive height 0) + Play Core proguard keep. fastlane production 100% rollout. RC 광고키 6개(`ads_enabled` 등) + `minimum_app_version` Android=1.1.3 게시. ⚠️ 전파 후 force_update Android=1.2 상향 검토 |
| v1.2.0 | 16 | 2026-06-16 | **게시·전파 완료 ✅ + 강제 업데이트 1.2 발동 ✅** | **iOS parity 대량**: 차트 peak 고점/저점 마커, 홈 공유→Play 스토어 링크, SimilarEvents 점수 게이지 일치, 현재지수 info 버튼+KOSPI 장상태/업데이트시각, 암호화폐 비교 날짜기반 앵커, **시장 상세 화면(지수/환율/암호화폐 3탭)**. 585 테스트 통과. fastlane production 100% rollout + changelog 16(45 locale "전체적인 성능 및 개선") + 스크린샷 5장. 관리형 게시 검토전송→게시 완료, Play Store 공개 리스팅 1.2.0 / Updated Jun 16 2026 전파 확인. **전파 확인 후 RC `force_update_minimum_version` Android=`1.1`→`1.2` 상향 완료**(1.0.x·1.1.x 강제, 1.2.0 통과). RC default fail-open(iOS 1.6.0/1.8.2) 유지. 상세: @bugs-fixed.md 32번 |
| v1.5.2 | **24** | 2026-08-18 | **production 업로드 ✅ (빠른 검사→자동 검토, 관리형 게시 OFF→승인 즉시 게시)** | **GMA Next-Gen SDK 1.3.1**(레거시 제거, Kotlin 2.2.21/Hilt 2.58) + Pangle 8.2.0.4.0 + AppOpen 유닛 + Billing ensureConnected ANR fix + **프리미엄 parity 4종**(점수 탐색기·알림 내역·프리미엄 게이트·DEBUG 결제 토글, 59번) + **release Timber→Crashlytics 트리**(60번). changelog 24 45 locale. ⚠️ vc23 은 fastlane internal 이 draft(임시)로만 올려 소모됨(재업로드 거부) → vc24 로 production. 트랙 검증 production=[24]/internal=[23,22]. AAB targetSdk 36 (proto manifest 직접 확인) |
| v1.5.3 | **25** | 2026-08-19 16:58 | **게시 완료 ✅** (Play "Google Play에 제공됨", 177개국 100%, 8/21 설치 403회, Crashlytics crash-free 100%, API 36 정책 경고 해소) | 배너 콜드스타트 fix(66) + 알림 보관 표시/영속 분리(65) + 어댑터 진단(62). changelog 25 45 locale. **게시 후 모니터링에서 Android App Check 전수 실패(68번) 발견 → 콘솔 수정(Play 앱 서명 키 SHA-256 Firebase 등록 + Play Integrity API Cloud 프로젝트 연결)으로 8/21 06:05Z 복구.** |
| v1.6.0 | **26** | 2026-08-21 (업로드 15:35 KST → 공개 리스팅 1.6.0 전파 07:58Z) | **게시·전파 완료 ✅ + RC 강제 업데이트 1.6 발동 ✅(08:0xZ)** | **App Check 보강**(토큰 선취득+실패 사유 Crashlytics, FCM 등록 정책/재시도) + BoM 33.16.0 + **강제 업데이트 patch 단위 비교**. changelog 26 45 locale "알림 등록 안정성과 보안 검증을 개선하였습니다." 1054 tests GREEN, SHA-1 `CE:08:B4` 일치. **전파 확인 후 RC `force_update_minimum_version`[Android] `1.2`→`1.6`, `minimum_app_version`[Android] `1.2.0`→`1.6.0` 게시 완료(iOS default 1.9.0/1.9.2 불변, 14 params)** → 1.0.x~1.5.x 전원 강제. E2E: v1.5.3 release 를 에뮬에 설치·실행 → PlayCore `requestUpdateInfo` 발동 + 스토어 폴백 확인. 상세: @bugs-fixed.md 69번. 다음 배포 vc27 |
| v1.3.0 | 17 | 2026-07-03 | **게시 완료 ✅** (당일 심사 통과, 사용자 수동 게시) | **RSI/공매도 투자 지표 신규**: 3자산(S&P500/KOSPI/BTC) 가격 RSI(14, Wilder) 카드 + 공매도 동향 카드 + 설명 시트. 데이터: Yahoo ^GSPC 6mo/CoinGecko 180d/FINRA RegSHO(3거래일 병렬)/Binance 롱숏/서버 /api/kospi/short. KOSPI 종가는 기존 스냅샷 kospiClose 재사용. TTL 캐시(12h/30m/1h)+8s timeout. indicator.* 35키×45 locale. 신규 테스트 45개, 전체 GREEN. changelog 17(45 locale "투자 지표를 추가하였습니다."), 스크린샷/메타 미변경(AAB+changelog만 업로드). 업로드 후 빠른 검사 통과 → 자동 검토 전송(수동 클릭 불필요) → 당일 게시. 상세: @bugs-fixed.md 33·34번 |

## AAB 빌드 (Release)

```bash
cd /Users/imyeongjin/Desktop/side/FearIndex-Android
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

- `~/.gradle/gradle.properties`에 `FEARINDEX_STORE_FILE` / `FEARINDEX_STORE_PASSWORD` / `FEARINDEX_KEY_ALIAS` / `FEARINDEX_KEY_PASSWORD` 가 설정되어 있어야 자동 서명.
- CI 환경에서는 같은 이름의 환경변수로 대체 가능 (`app/build.gradle.kts`의 `signingConfigs` 로직).

## Play Console 업로드 (수동)

절차는 @../../docs/GOOGLE-PLAY-INTERNAL-TEST.md 참조.

요약:
1. https://play.google.com/console → FearIndex 앱 선택
2. 테스트 및 출시 → **내부 테스트** 트랙 → "새 버전 만들기"
3. AAB 드래그&드롭 업로드
4. 출시명 + 출시 노트 입력
5. "검토 시작" → "내부 테스트로 출시"

## 자동 업로드 (fastlane)

`fastlane/` 구조 존재. `supply` 기반.

```bash
cd /Users/imyeongjin/Desktop/side/FearIndex-Android
bundle exec fastlane internal
```

선행 조건: Play Console에서 **service account JSON 발급** → `fastlane/Appfile`의 `json_key_file` 경로 설정.

## 광고 정책 (배포 전 체크)

**내부 테스트 트랙**: 광고 제외 가능 (디버그 빌드는 테스트 광고 ID만). QA 편의상 일부 배너 숨김 허용.

**Production 배포 전 필수**:
- `app/build.gradle.kts`와 `presentation/build.gradle.kts`의 release 빌드 `buildConfigField`가 위 프로덕션 광고 단위 ID로 세팅되어 있는지 재확인.
- Remote Config에서 광고를 켤 때 `ads_enabled=true`, `interstitial_ads_enabled=true`, `interstitial_session_cap=2`, `interstitial_cooldown_sec=180`, `kospi_interstitial_enabled=true`를 Android 조건에 맞게 설정.
- 코스피 홈 진입 인터스티셜은 사용자가 KOSPI 탭으로 전환한 뒤 5초 후에만 노출. 앱 시작/종료/백그라운드 전환 시 노출 금지.
- 광고 사이에 Insight/SimilarEvents 카드가 자연스럽게 배치되는지 확인.
- 광고 로딩 실패 시 크래시 없이 빈 공간으로 처리되는지 확인.

## 프로모션 / Staged Rollout

- **Alpha → Beta → Production**: Play Console에서 "프로모션" 버튼.
- **Staged rollout**: Production 배포 시 비율 선택 (예: 10% → 50% → 100%).

## dSYM 해당사항 없음

- **iOS 대응**: Android는 ProGuard mapping 파일 필요. `app/build.gradle.kts`에서 `isMinifyEnabled = true` 일 때 자동 생성.
- **Crashlytics 심볼 업로드**: `google-services` 플러그인이 자동으로 처리 (`uploadCrashlyticsMappingFileRelease` 태스크).

## 관련 자동화

- `scripts/screenshots/capture-all-locales.sh` — 45개 locale 스크린샷 자동 촬영
- `scripts/e2e/run-all.sh` — E2E 테스트 스위트 실행

## 관련 문서

- @../../docs/GOOGLE-PLAY-INTERNAL-TEST.md — Play Console 수동 배포 8단계
- @firebase-setup.md — Firebase Functions 배포
- @../rules/package-convention.md — package name 엄수
