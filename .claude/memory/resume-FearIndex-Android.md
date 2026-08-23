# Resume — FearIndex-Android

## Date / Branch
2026-08-23 / `dev` (ab1e31a = origin/dev, 전 브랜치·태그 원격 동기화 완료, worktree 없음)

## 🚨 재개 시 첫 행동 (어느 맥이든)
1. `git fetch origin && gh api repos/thingineeer/FearIndex-Android/commits/7ca2711227bdbdb049261c2108553d024ad58228 --jq .sha` — **1.6.0 빌드 커밋 `7ca2711` 이 GitHub 에 있는지**. 없으면 1.6.0 빌드한 맥에서 `git push origin HEAD` 먼저(사용자 작업, 2026-08-23 안내 완료).
2. 있으면: `7ca2711` 에 **v1.6.0 태그** + release 머지 → dev(인터스티셜 fix + 10분 + v1.6.1 bump + changelog 27)를 그 위에 머지(충돌 해결) → `bash ~/thingineeer-env/android/fearindex/install.sh`(이 맥 `~/.gradle/gradle.properties` 부재) → `./gradlew test` → `bundle exec fastlane production`(관리형 게시 OFF, 승인 즉시 게시, **사용자 A 선택으로 게시 승인됨**) → Play 트랙 API 로 vc27 completed 확인 → v1.6.1 태그 + release 머지 → deployment.md 출시 이력.
3. 게시 후 실기기: 10분 백그라운드 → 코스피 재진입 → 전면광고 노출 1회 확인.

## ⚡ 한 줄 상태
**Play production 은 v1.6.0(vc26, 8/21 게시 100%)인데 이 레포엔 v1.5.3(vc25)까지만 있음 — 1.6.0 소스 push 누락(다른 맥).** 광고 "안 뜬다" 제보는 GA 실측으로 재현 안 됨(배너 실패 481→80 급감, 사용자당 노출 증가). 4계정 8월 요금 ≈₩8.4k 정상. 상세: @.claude/memory/bugs-fixed.md 68번.

## 2026-08-23 세션 요약
- [x] 광고 전수 점검: 코드(1.5.3)·RC(v49: force 1.6/min 1.6.0 = 1.6.0 게시와 정합)·GA·Crashlytics(FATAL 0)·서버(ERROR 1건 일시)·Play 트랙(API 직접) — 결론 정상
- [x] 4개 결제 계정 8월 MTD: 푸시알림 ₩2,009 / 비트코인매매 ₩4,360(7월 ₩43k ddalggak 제거됨) / Firebase ₩2,005(Vertex AI 신규) / 세번째 ₩0
- [x] 배너 잠재 결함 2건 발견(320dp 하한 → 폭 352dp 미만 기기 배너 0 / AndroidView factory 1회 → 폭 변경 시 빈 배너) — 미수정, 1.6.1 후보
- [x] AdMob 콘솔 실측 — 7일 수입 US$5.74(+80%)/노출 +40%/일치율 84.7% 전 지표 상승, 앱오픈 첫 가동. 정책 제한은 구버전 잔존만(68번)
- [x] MEMORY.md stash 충돌 마커 정리(08-04 앱 이전 기록 보존)
- [x] **인터스티셜 세션 리셋·재로드 수정(A안, TDD)** — dev ba21b53. 유닛 1,037/0 + 서명 release APK OK
- [x] 세션 리셋 **30분 → 10분**(사용자 결정, iOS 30분과 divergence) — RED 5 → GREEN 19/19, dev 8442f5b
- [x] **v1.6.1(vc27) bump + changelog 27 45 locale**(릴리즈노트에 `v1.6.1:` 명시) — dev ab1e31a. **미게시** — 1.6.0 소스 합친 뒤 게시
- [x] 스테이징 정리: `docs/play-reports-backup-20260804/`(앱 이전 전 Play 보고서 유일본, 레포 PUBLIC 이라 커밋 불가) → `thingineeer-env/android/fearindex/play-reports-backup-20260804/` 이관 + DEPLOY-KEYS 색인 + push
- [x] dev/main/release/태그 전부 원격 동기화

## Completed (이번 세션, 2026-08-18~19)
- [x] **v1.5.2(vc24) production 배포·게시** — 프리미엄 parity 4종(점수 탐색기·알림 내역·프리미엄 게이트·DEBUG 토글) + GMA Next-Gen + Pangle. 상세: @.claude/memory/bugs-fixed.md 59~60번
- [x] **v1.5.3(vc25) production 업로드** — 배너 콜드스타트 fix(66번) + 알림 보관 표시/영속 분리(65번) + 어댑터 진단 로그(62번). production=[25] 트랙 API 확인, 콘솔 "검토 중" 실측. 게이트: 1019 tests/0, SHA-1 `CE:08:B4` 일치, DEBUG 심볼 0, 45 locale 대칭
- [x] **푸시 임계치 E2E 실수신 검증** — 에뮬 debug + App Check 토큰(`Claude push E2E emulator 2026-08-19`) 등록. "이상"=KOSPI 51 selling opportunity / "이하"=Crypto 46 buying opportunity 실수신 + 알림 내역 화면 기록 확인. 상세: @.claude/memory/bugs-fixed.md 67번
- [x] **API health 전수 GREEN** — KOSPI v2/short·CNN·Alternative.me·Yahoo·CoinGecko·환율·Naver (67번에 정확한 URL/헤더)
- [x] **결제 검증** — S22 release에서 ₩7,500 조회→Play 결제 시트 실진입→사이드로드 거부→앱 실패 처리 정상. TDD(IAP 유닛+계측 QA 3종) green
- [x] **AdMob 테스트 기기 등록(S22)** + 앱오픈 광고 정책 실기기 검증(콜드 제외/복귀 노출). RC `app_open_*` 4키 게시 완료(61번)
- [x] Unity 대시보드/AdMob 콘솔 실측 — Pangle 수익 출처=딸깍, iOS Unity 매핑 정상, Android Game ID `800107232`(63번)

## 미해결 / 다음 할 일 (우선순위순)
0. **🚨 v1.6.0 소스 확보** — 빌드 커밋 = **`7ca2711`**(Crashlytics buildStamp). 로컬·GitHub 모두 없음(gh API 422). 1.6.0 빌드한 맥에서 push 필요(사용자). 받으면 위 '재개 시 첫 행동' 2번
0-2. ~~인터스티셜 세션 리셋 수정~~ 완료 + 10분 + v1.6.1 bump 완료(dev ab1e31a). 배너 320dp 하한(①)은 별건
0-3. 이 맥 `~/.gradle/gradle.properties` 부재 — 배포 전 `bash ~/thingineeer-env/android/fearindex/install.sh` — `bannerAdWidthDp` 320dp 하한 완화 + `AndroidView` update/key 로 컨테이너 stale 해소. Crashlytics `AppCheckTokenProbe` PLAY_INTEGRITY_UNAVAILABLE 추이 감시
1. ~~1.5.3 게시 확인~~ → 1.5.3 은 게시됐고 이미 1.6.0 으로 대체됨 — Play Console 게시 개요(`play.google.com/console/u/1/...` ⚠️ u/0은 타 계정) "검토 중"→게시 완료, 공개 리스팅 1.5.3 전파 확인. 게시되면 대시보드 API 36 경고 카드 소멸도 확인
2. **실결제 완주** — 게시 후 Play 설치본(1.5.3)으로 광고 제거 ₩7,500 결제 1회 (사이드로드는 Play가 거부, 67번). 사용자 직접
3. **배포 후 감시** — Crashlytics #96 MotionEvent(Next-Gen), 프리미엄 non-fatal(CrashlyticsTree W/*), 배너 match rate(수정 효과), `FearIndexAds`/`FearIndexAdapters` logcat 태그로 실기기 진단 가능
4. **사용자 결정 2건** — ① 알림 내역 전용 AdMob 배너 유닛(현재 홈 유닛 fallback) ② 스토어 IAP 표시명("광고 제거"→프리미엄 3종)
5. **1.5.4 후보** — @docs/checkpoints/RELEASE-1.5.3-CHECKLIST.md 잔여 항목: A(트레이 직행 누락, 보류 권고)·B(KOSPI history boolean)·C(Yahoo spark 死코드)·D(BOOT_COMPLETED 권한)·E(**Fastfile internal lane `release_status: "completed"` 추가** — vc23 소모 원인)·H(KOSPI 번들 fallback 보강)

## 주의 (다음 세션이 밟을 함정)
- **사이드로드 release = App Check(Play Integrity) 403** → 서버 Callable 전부 거부. 서버 연동 E2E는 에뮬 debug + 콘솔 debug token으로 (67번)
- **S22(R5CT21LBABH)는 AdMob 테스트 기기** — 테스트 광고만 나옴(수익 없음). 실광고 확인하려면 콘솔에서 등록 해제. 광고 검사기=기기 흔들기
- **크론 로그 market=null은 버그 아님** — 미국 장외 ET 거래일 게이트. KOSPI도 장외(15:30 이후) skip 정상 (67번)
- **Play/Firebase/AdMob 콘솔 URL은 기본 계정이 수시로 바뀜** — signup/약관/MFA 화면 뜨면 절대 진행 말고 `u/1` 또는 `?authuser=dlaudwls1203@gmail.com`
- **같은 versionCode 재업로드 불가** — vc23은 draft로 소모됨. fastlane internal은 draft로만 올림(60번)
- **다음 배포는 vc26부터**
- 알림 내역 저장소는 이제 **정렬 비보장**(원본 삽입 순서) — 직접 읽는 코드 추가 시 정렬 가정 금지 (65번)

## 참조
- @.claude/memory/bugs-fixed.md — 59~67번: 이번 세션 전체 상세(프리미엄 parity/배포/검증/함정)
- @.claude/memory/deployment.md — 출시 이력(v1.5.2 vc24 게시·v1.5.3 vc25는 게시 확인 후 행 갱신 필요), 상수/게이트
- @docs/checkpoints/RELEASE-1.5.3-CHECKLIST.md — 잔여 이슈 A~H 표 + 릴리스 게이트 명령어
- @docs/QA-PREMIUM.md — 결제 QA 3시나리오 + '구매 안 함' 상태 30일 초과 기록 실삭제 주의
- @docs/checkpoints/SESSION-STATE.md — 세션 상태(이 파일과 동기)
- @.claude/memory/ios-parity.md — 어댑터 링크 메커니즘 표, 프리미엄 parity 대칭 항목
