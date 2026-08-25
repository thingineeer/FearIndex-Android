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

## 최신 상태 (2026-08-25, 마케팅 급증 대비 점검 + 레이트리밋 협업 + '줍줍' ASO) ← 최신 진입점: @memory/resume-FearIndex-Android.md

- **점검 GREEN(71번)**: FCM 등록 24h 200=656/401=4, 신규 android 123건(8/23~) **임계값 123/123 포함**(즉시체크 게이트 OK), crypto 임계치 발송 실적 확인. 스토어 production=[26]=1.6.0 — **다음 작업 = v1.6.1(vc27) 게시**(@docs/checkpoints/HANDOFF-v1.6.1.md, ASO '줍줍' 포함됨).
- **서버 공유 레이트리밋 60→300/min**(메인 세션 TDD 배포, CGNAT 대비) 전후 대조 종결 — 공유 IP 429 소멸·무부수효과. 잔여 429 = getSimilarEvents per-device 리밋(설계 동작). ⚠️ **공유 모듈 상수는 재배포한 함수만 반영**.
- **ASO '줍줍'**: ko_KR short/full 설명문 3회 자연 삽입(dev 3bbd530b) — v1.6.1 업로드 시 스토어 반영.

## 최신 상태 (2026-08-24, 두 맥 dev 통합 — 1.6.0 소스 합류)

- **8/24 머지로 "1.6.0 소스 부재" 해소**: 로컬(이 맥) 1.6.0 소스 16커밋(빌드 커밋 `7ca2711` 포함, App Check 보강+patch 단위 강제 업데이트) ⟷ origin/dev v1.6.1 준비 26커밋(인터스티셜 세션 리셋 10분, vc27 bump, changelog 27)을 merge. 버전은 **1.6.1(vc27)** 채택. `release` 에 1.6.0 머지 + **v1.6.0 태그** 완료. bugs-fixed 번호 재정리: 광고 세션 항목 68→**70**(68=App Check, 69=1.6.0 배포).
- **다음: v1.6.1(vc27) 게시** — @docs/checkpoints/HANDOFF-v1.6.1.md 절차 (게시 승인됨).

## 최신 상태 (2026-08-23, 광고 미노출 제보 분석 + 요금 점검) 

- **🚨 저장소 ≠ 배포본**: Play production = **v1.6.0(vc26)**, 8/21 게시, 100% completed(Play API 직접 조회). 이 레포 최대는 v1.5.3(vc25, 8/19). **1.6.0 소스가 어디에도 없음**(origin/dev·release·태그·worktree 전부) → 다른 맥에서 빌드·업로드 후 push 누락 추정. 핫픽스/롤백 불가 상태. **최우선: 그 맥에서 push.** (✅ 8/24 두 맥 dev 머지로 해소 — 1.6.0 소스 `7ca2711` 합류) RC `force_update_minimum_version`[Android]=`1.6`/`minimum_app_version`=`1.6.0`(v49, 8/21) 은 1.6.0 게시와 정합 — 락아웃 아님.
- **✅ 광고 "안 뜬다" 재현 안 됨(GA 실측)**: 28일 배너노출 58,529/2,001명. 8/18(1.5.x) vs 8/22(1.6.0): 사용자당 배너 8.18→**8.43**, 배너실패 481→**80**(66번 fix 실효 증거). 약점은 **인터스티셜 fill ~17%**(노출 27/실패 136) — 인벤토리 문제. **AdMob 콘솔 실측(7일)**: 수입 US$5.74(+80%)/노출 1.42천(+40%)/일치율 84.7%/eCPM 4.05 — 전 지표 상승, 앱오픈 단위 첫 가동(13 노출, eCPM 28.55). 정책 제한은 구버전(1.0.1) 잔존 284요청(0%)뿐.
- **Crashlytics 1.6.0**: FATAL 0. 신규 non-fatal `AppCheckTokenProbe.ensureToken` PLAY_INTEGRITY_UNAVAILABLE(-9) 42건/12명 — 1.6.0 신규 코드, Callable 거부 가능성 관찰. RC fetch DNS 실패 40건(네트워크) — 첫 실행 오프라인이면 `ads_enabled` 캐시 없음 → 배너 fail-safe OFF.
- **서버 정상**: Functions 3일 ERROR 1건(crypto FNG 502 일시), 크론 failed=0, total_users 2,365.
- **💰 요금(8월 MTD, 4계정 합 ≈₩8.4k, 과다 아님)**: 공포지수푸시알림(fear-index) ₩2,009(Firestore Read ₩1,179 최대) / 비트코인매매 ₩4,360(7월 ddalggak Cloud Run min-instance ₩43k 제거로 -100%, App Engine ₩3,811 신규 증가) / Firebase결제 ₩2,005(Hosting ₩1,512+Vertex AI ₩986 신규) / 세번째 ₩0. 결제 목록 "30일 ₩49k"는 7월 잔여분.
- **✅ 제보 원인 확정(아버지 Galaxy A35, 화면 크기 기본)**: 증상 = 코스피 진입 **인터스티셜** 안 뜸. 원인 = `InterstitialAdPolicy` 싱글턴 **세션 리셋 호출처 0곳**(KOSPI 진입 광고 프로세스당 1회) + 로드 실패 후 재로드 없음. **iOS v1.9.2(7/13) 가 고친 결함의 Android 미포팅.** **✅ A안(iOS parity) TDD 수정 완료 → dev ba21b53**(30분 백그라운드 후 세션 리셋 + 복귀 preload + 미준비 시 재로드 + cap 도달 시 preload 생략). 이후 **10분으로 단축**(사용자 결정) + **v1.6.1(vc27) bump·changelog 27 준비 완료(dev ab1e31a, 미게시)**. **1.6.0 빌드 커밋 = `7ca2711`**(Crashlytics buildStamp) — 다른 맥에서 push 되면 v1.6.0 태그 → 머지 → `fastlane production`(게시 승인됨). Play 보고서 백업은 thingineeer-env 로 이관. 배너 결함(320dp 하한·AndroidView stale)은 별건, 아버지 기기 비해당. 상세: @memory/bugs-fixed.md 70번(재번호).
- **⚠️ 도구 함정**: Chrome MCP 로 `apps.admob.com` 진입 시 `admob.google.com` 리다이렉트 직후 첫 액션만 Permission denied — 권한 문제 아님, 재시도하면 됨. `/login` 으로 계정 바꾸면 확장 연결 끊김(확장 팝업에서 같은 계정 재로그인 + `/chrome` 으로 브라우저 재선택). GCP 결제는 `console.cloud.google.com/billing/<ACCOUNT_ID>/reports?authuser=0` + get_page_text 로 읽힘. Play 트랙 상태는 SA JSON 으로 androidpublisher API 직접 호출이 fastlane 보다 정확(status/userFraction).


## 최신 상태 (2026-08-21, v1.5.3 게시 확인 + 🚨 Android App Check 전수 실패 복구) ← 최신 진입점: @memory/resume-FearIndex-Android.md

- **✅ v1.5.3(vc25) 게시 완료(8/19 16:58)** — 설치 403, crash-free 100%, ANR 0, API 36 경고 해소, AdMob 정책 신규 이슈 0·노출 +52%/eCPM +34%(배너 fix 효과).
- **🚨 68번: Android 프로덕션 App Check 가 6/22 hard mode 배포 이후 ~100% 실패**(401 매일 ~1,000건, Firestore android 신규 등록 6/22 이후 0건 = 1.3.0~1.5.3 사용자 전원 푸시 미등록). 원인 = Firebase 등록 지문이 v1.0.0 폐기 키 1개뿐(Play 앱 서명 키 `EF:5D…` 미등록) + **Play Console Play Integrity API Cloud 프로젝트 미연결**. **콘솔 수정만으로 8/21 06:05Z 즉시 복구**(Android 200 재개, 신규 android 등록 5건/첫 15분). 기존 설치본은 다음 실행에 자동 복구.
- **1.5.4 보강(feature/v1.5.4-appcheck-resilience, dev 머지)**: App Check 토큰 선취득+실패 사유 Crashlytics 기록, 등록 정책(24h/변경 시만)+WorkManager 재시도, BoM 33.16.0. 1044 tests GREEN.
- **⚠️ 교훈**: "사이드로드 403 = 사이드로드 탓" 오진. **App Check 는 Play 설치본으로 실측**(서버 로그 okhttp 200 / Firestore android createdAt). iOS 팀 전달은 @docs/handoff/ios-appcheck-401-handoff-2026-08-21.md(SendMessage unreachable).
- **✅ v1.6.0(vc26) production 업로드(8/21 15:35, 69번)** — App Check 보강 + BoM 33.16.0 + **강제 업데이트 patch 단위 비교**(TDD 17/17). 1.5.4 가 아닌 1.6.0 인 이유: 배포된 1.5.x 는 major.minor 만 비교라 RC `1.6` 으로만 강제 가능. production=[26] → 공개 리스팅 1.6.0 전파 07:58Z → **RC Android force `1.6` / minimum `1.6.0` 게시 완료(1.0.x~1.5.x 전원 강제, E2E: v1.5.3 에뮬에서 PlayCore requestUpdateInfo 발동 확인)**. 다음 배포 vc27. macOS 401 도 iOS 세션이 DeviceCheck provider 콘솔 등록으로 복구(06:57Z 200) → 3플랫폼 App Check 정상.

## 최신 상태 (2026-08-19, v1.5.3 배포 + 전수 검증)

- **✅ v1.5.3(vc25) production 업로드 — Play 검토 중**(관리형 게시 OFF → 승인 즉시 자동 게시). 내용 = 배너 콜드스타트 fix(66) + 알림 보관 표시/영속 분리(65) + 어댑터 진단(62). 게이트 전부 통과(1019/0, SHA-1 일치, 심볼 0). release 머지 + v1.5.3 태그 + 전체 push 완료.
- **✅ 전수 검증 GREEN(67번)**: API health 9종 / 푸시 임계치 **이상(KOSPI 51)+이하(Crypto 46) 실수신** + 알림 내역 기록 / 결제 Play 시트 실진입(사이드로드 거부는 환경 제약) / 앱오픈 정책 실기기(콜드 제외·복귀 노출).
- **⚠️ 핵심 함정**: 사이드로드 release=App Check 403(E2E는 에뮬 debug), S22=AdMob 테스트 기기(테스트 광고만), 콘솔 URL 기본 계정 수시 변경(u/1 또는 ?authuser=), 다음 배포 vc26부터.
- **다음**: 게시 확인 → 실결제 완주(사용자) → 배포 후 감시(#96·배너 match rate·프리미엄 non-fatal). 사용자 결정 2건(알림내역 전용 유닛·IAP 표시명).

## 최신 상태 (2026-08-18 저녁, 프리미엄 parity 4종 — iOS v1.9.4 이식 완료, dev 머지)

- **✅ 프리미엄 parity 4종 dev 머지 완료(beebbe8, push 미실행)**: ①프리미엄 게이트(광고제거 구매=프리미엄, 새 SKU 없음, `isPremium=isAdFree`) ②점수별 과거 수익률 슬라이더(차트 탭, 정확 버킷·보간 금지·표본수·저표본 배지·프리미엄 잠금) ③알림 내역(홈 🔔, 무료 30일/프리미엄 무제한, JSONL 서버비 0) ④DEBUG 결제 테스트 토글(release 심볼 0). i18n 55키×45 locale. 상세: @memory/bugs-fixed.md 59번.
- **검증 완료**: 유닛 1014/0 + 계측 QA 3시나리오 GREEN(헤드리스 에뮬 API 36) + release 빌드/심볼 0. **계측 QA 3함정**(Espresso 3.7.0 필수/에뮬 스토리지/M3 Slider SetProgress)은 59번 참조.
- **실결함 fix**: 알림내역 markSeen 클럭 스큐 무한루프(FCM sentTime 미래 → 영원히 unread) — `lastMarkedNewest` 가드.
- **에뮬 정리**: Medium_Phone_API_36.1 의 shadewalk/bamfiresurvive debug 앱 제거(스토리지 확보, 각 레포에서 재설치 가능).
- **✅ v1.5.2 production 배포(8/18 18:09, vc24)**: fastlane production 성공, production=[24], 관리형 게시 OFF(빠른 검사→자동 검토→승인 즉시 게시). vc23 은 fastlane internal 이 draft 로만 올려 소모(60번 교훈: internal lane 에 release_status 없음). release Timber→Crashlytics 트리 추가(프리미엄 경로 실패 Firebase 추적). **다음**: 게시 확인 → API 36 경고 카드 소멸 확인, RC `app_open_ads_enabled` 게시 판단, Crashlytics #96 MotionEvent·배너 match rate·프리미엄 non-fatal 감시. release 머지+v1.5.2 태그+push 완료.
- **남은 사용자 결정**: ① 알림 내역 전용 AdMob 배너 유닛(현재 홈 유닛 fallback) ② 스토어 IAP 표시명 갱신 여부.

## 최신 상태 (2026-08-18, Play 계정 이전 후속 — 배포 파이프라인 막힘 + API 36 경고 원인 + Next-Gen 보류)

- **🚨 Play Console 개발자 계정이 바뀜**: 8/4~8/6 앱 3개(FearIndex·딸깍·그늘길)가 조직 계정 "Myeongjin Lee"(5351376807423705889) → **개인 계정 "이명진"(5573450681823453997)** 으로 이전 완료. 로그인은 `mjplist@gmail.com`(또는 dlaudwls1203) → 계정 선택 "이명진". FearIndex app ID `4973920645070208584`.
- **✅ fastlane 복구(11:01)**: SA `fastlane-deploy@fear-index-a4f4b.iam.gserviceaccount.com`이 계정 이전으로 빠져 있어 403이었음 → 사용자가 이명진 계정에 SA 초대 → 트랙 조회 정상. 상세 53번.
- **✅ API 36 경고 원인 제거(11:03)**: 원인 = 내부 테스트 + 비공개 알파 트랙의 vc3(1.0.2). fastlane `track:production track_promote_to:internal|alpha version_code:22`로 vc22 승격 → 세 트랙 모두 [22]. 알파는 "검토 중"(심사 후 vc3 대체). 경고 카드는 스캐너 갱신 후 자동 소멸 예상. 상세 54번.
- **결제 프로필 문제(계정 삭제 예고 9/16, "판매자 결제 수단 인증 불가")**: **사용자가 직접 진행 중 — 관여하지 말 것.**
- **GMA Next-Gen SDK**: 조사 완료, **v1.6.0으로 보류**(하드 데드라인 없음, 1.3.x 크래시 보고, Kotlin ≥2.2 선행). 체크리스트 55번.
- **dev 머지 완료(로컬, push 미실행)**: origin/dev(1.5.1) ← Pangle 어댑터. GMA 23.6.0→**24.8.0**(23.x deprecated), Unity 4.13.1.0 + Pangle 7.8.0.8.0 공존. 791 테스트 GREEN + release AAB 빌드 OK. 버전은 아직 vc22/1.5.1(bump 안 함).
- **✅ push 완료**(dev = origin/dev, 78759074).
- **AdMob 실사(56번)**: 공포지수 Android 준비됨·게재 사용중, 단위 6개(배너5+전면1, ID 코드와 일치), 미디에이션 입찰 소스 = AdMob+**Pangle**+**Unity**(활성, 매핑 6/6), 폭포식 0, **AppLovin 미연결**(코드에도 없음). **앱오프닝 단위 없음**(코드만 있음). Pangle 매핑은 이미 완료 → v1.5.2(Pangle 어댑터 포함) 배포 시 실가동.
- **✅ Crashlytics 미해결 5건 정리(8/18)**: Glance 트램폴린·Billing Proxy 크래시 = **동일 봇 세션 액티비티 fuzzing**(exported=false, OnePlus8Pro) → 코드 무관, 종료. ensureConnected ANR → **startConnection IO 이동 fix**(dev). ART ANR/WebView 종료. 실사용자 비치명 "상품 정보 미로드" 2명 관찰. 상세 58번.
- **✅ GMA Next-Gen SDK 1.3.1 마이그레이션 완료(8/18, dev 로컬)**: 레거시 play-services-ads 제거, Kotlin 2.2.21+KSP2+Hilt 2.58, Pangle 8.2.0.4.0/Unity 4.19.0.1/UMP 4.0.0, 광고 4파일 재작성(콜백 메인 디스패치 + AdSdkState 초기화 게이트 + BannerAdSlot dispose 가드). 803 테스트 GREEN + release AAB + 에뮬 배너/인터스티셜/앱오픈 전 경로 실검증. **배포 후 Crashlytics(#96 MotionEvent 크래시)·배너 match rate 감시 필수.** 상세 57번.
- **✅ 앱오프닝 단위 발급(8/18)**: AdMob `AppOpen` = `ca-app-pub-5283496525222246/6583206280`. app/presentation release `ADMOB_APP_OPEN` 반영(feature/v1.5.2-app-open-unit → dev --no-ff). **실가동 조건**: (a) 이 커밋 포함 빌드 배포(v1.5.2) + (b) Firebase RC `app_open_ads_enabled=true`(+session_cap/cooldown/min_background 키, bugs-fixed 41번) 게시 — 둘 다 아직. 미디에이션 그룹(앱오프닝)은 미생성 — AdMob 네트워크만으로 우선 가동, 원하면 Pangle/Unity 앱오픈 매핑 추가.
- **사용자 결정(8/18)**: v1.5.2(Pangle 포함) 배포는 **대기**. AppLovin은 사용자가 직접 검토.
- **다음**: (1) 알파 심사 통과·대시보드 API 36 카드 소멸 확인 (2) 사용자 지시 시 v1.5.2(vc23) 배포 + RC 앱오픈 키 게시 (3) 실기기 Billing 8 결제 재검증(미완).
## 최신 상태 (2026-08-04, 앱 이전 진행 중 — 보고서 백업 완료 + 15% 수수료 미등록 발견)

- **앱 이전 대기 중** (Play Console 설정→앱 이전, 2026-08-04 요청·수락 완료): 기존 계정 **Myeongjin Lee `5351376807423705889`** (구글 로그인 dlaudwls1203@gmail.com) → 새 계정 **이명진 `5573450681823453997`** (구글 로그인 **mjplist** 계정). 이전 앱 3개 = FearIndex + 그늘길 + 딸깍 (전부). 이전 이유 = 결제 프로필 문제(지급 보류/신원 확인). Google 안내: 취소 없으면 영업일 2일 후 진행, 처리 영업일 2일.
- **✅ 이전 전 보고서 백업 완료** → `~/Desktop/FearIndex-Play보고서-이전전백업-20260804/`. GCS 버킷 `gs://pubsite_prod_5351376807423705889/` 전체 미러(gsutil, dlaudwls1203 gcloud 인증): stats(installs 35/crashes 16/ratings 21/store_performance 20) + reviews 3 + sales 2(202607·202608) + play_balance_krw. **실주문 GPA.3334-7862-8616-69040(HUF 1,999) salesreport_202607.csv에 포함 확인.** 딸깍 앱 신규 주문도 발견(8/3, 코인550 ₩1,100). ⚠️ **7월 수익(earnings) 보고서는 아직 미생성** — 재무 데이터는 기존 결제 프로필에 남으므로 이전 후에도 기존 계정에서 다운로드 가능(생성되면 받을 것).
- **✅ 15% 서비스 수수료 프로그램 등록 완료** (2026-08-04, 새 계정 u/2에서 진행): 절차 = ① 새 계정에서 계정 그룹(이명진)에 기존 계정(Myeongjin Lee `5351376807423705889`, "법인 소유") 추가 요청 → ② 기존 계정(u/1)이 자기 단독 그룹('Myeongjin Lee' 그룹) **삭제** 후 요청 수락(기존 그룹의 기본 계정이면 수락 불가라 삭제가 선행 필수) → ③ 새 계정에서 "검토 및 등록" → 약관(연 첫 $1M 15%, 초과분 30%, 그룹 합산) 수락. 결과: **그룹(이명진+Myeongjin Lee) 전체에 15% 적용, 등록일부터** (소급 없음 — 7월 HUF 주문은 30% 시절). 새 계정 알림에 "본인 인증이 완료되었습니다" = 새 계정 결제 프로필은 지급 보류 없음.
- **기존 계정 잔여 이슈**: 지급 보류(신원 확인 미완, ₩6,211 잔액) + 싱가포르 세금 정보 경고 — 기존 결제 프로필에 남는 문제이므로 잔액 수령하려면 신원 확인은 여전히 필요(사용자 직접).

## 최신 상태 (2026-07-31 새벽, v1.5.1 vc22 — Billing 8 마이그레이션 + 배포)

- **v1.5.1(vc22) production 업로드** — 1.5.0(vc21)이 심사 중인 상태에서 **대체 업로드**. 내용 = 1.5.0 전부(코스피 신호 분해 + targetSdk 36) + **Play Billing 7.1.1→8.3.0**(정책 기한 8/31, 미준수 시 업데이트 거부). changelog 22 45 locale. 관리형 게시 OFF → 승인 즉시 자동 게시.
- **⚠️ Billing 9.x 는 Kotlin 2.3 메타데이터 요구로 불가** (현재 2.1.0). 8.3.0 채택. 다음에 9.x 가려면 Kotlin/Compose/KSP 동반 업그레이드 필요. 상세: @memory/bugs-fixed.md 50번.
- **API/푸시 전수 실측 정상** (11종 200, 푸시 경로 정상, targetSdk 36 영향 없음). BTC 공매도 복구됨. 상세: 52번. **오늘 시장 계획: @docs/checkpoints/MARKET-READINESS-20260731.md**
- **✅ v1.5.0(vc21) 게시 완료** (2026-07-31, 공개 스토어 리스팅 1.5.0 확인). **v1.5.1(vc22)은 빠른 검사 → 검토 중** (관리형 게시 OFF라 승인 시 자동 게시).
- **✅ 태그/브랜치 정리 완료**: 그동안 누락됐던 **v1.4.2**(vc20, 7/18 게시)와 **v1.5.0**(vc21, 7/31 게시)을 release 브랜치에 순차 --no-ff 머지 후 태깅. dev/release/태그 전부 origin push 완료. **v1.5.1 태그는 게시 확인 후** (게시 감시 중).
- **💰 실결제 확인 (주문 관리)**: `remove_ads_lifetime` **실주문 1건 존재** — 주문 ID `GPA.3334-7862-8616-69040`, 2026-07-23 07:15 UTC, **HUF 1,999**(헝가리 사용자), 상태 **처리됨**. 즉 IAP 결제 경로는 **Billing 7.1.1 기준으로 실증됨**. ⚠️ 단 v1.5.1의 Billing 8.3.0은 offerToken 필수로 플로우가 바뀌었으므로 **게시 후 실기기 재검증 필수**(설정→Premium 가격 표시 + 구매 시트 진입).
- **✅ v1.5.1(vc22) 게시 완료** (2026-07-31, 공개 스토어 1.5.1 확인 + 번들 탐색기 `22.aab(1.5.1)` **활성**·**타겟 SDK 36**). **release 머지 + v1.5.1 태그 + push 완료.** 태그 상태: v1.4.1 → v1.4.2 → v1.5.0 → v1.5.1 (누락분 전부 소급 정리).
- **⚠️ 정책센터 경고 2건(API 36 / Billing 8.0.0+)은 게시 직후에도 아직 표시됨 = 갱신 지연**. 근거: Google 자체 번들 탐색기가 활성 번들(vc22)의 타겟 SDK를 36으로 인식, 우리 merged manifest도 `billingclient.version=8.3.0`. targetSdk 36은 1.5.0으로 몇 시간 전 이미 게시됐는데도 경고가 남아 있음 → 스캐너 주기 문제. 하루 이내 자동 해제 예상, 다음 세션에서 재확인만.
- **다음 세션**: (1) 정책 경고 2건 자동 해제 확인, (2) **실기기 결제 재검증**(Billing 8 첫 배포 — 설정→Premium ₩7,500 표시 + 구매 시트 진입), (3) 잔여 정리(`KospiFearIndexApi.history` boolean 지뢰, Yahoo spark 死코드, 미사용 BOOT_COMPLETED 권한).
- **⚠️ Play Console 브라우저 주의**: Chrome 창에 따라 기본 로그인 계정이 다름. `veoaudwls@gmail.com` 이 뜨면 **약관 동의 클릭 금지**, URL의 `/u/0` → `/u/1` 로 바꿔 `dlaudwls1203@gmail.com` 계정으로 접근할 것.
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

## 지난 릴리즈 이력 (상세는 @memory/bugs-fixed.md)

- **v1.4.0**(vc18, 미배포) — iOS v1.8.8 parity 5건 + 광고 개선 3건. IAP는 43·44번 사유로 제거됐다가 v1.4.1에서 재도입.
- **v1.4.1**(vc19) 2026-07-18 게시 — 온보딩 코치마크 투어 8단계 + Glance 위젯 4종 + 광고 제거 IAP 재도입 + 알림 허브/상세 UX. Korean law 게이트 해제(43번 종결), 관리형 게시 OFF 전환.
- **v1.4.2**(vc20) 2026-07-18 게시 — IAP 플로우 경화 2건(AlreadyOwned 플래그 누수, 조회/로드 timeout).

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
