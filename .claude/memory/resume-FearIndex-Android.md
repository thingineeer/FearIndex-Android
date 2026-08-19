# Resume — FearIndex-Android

## Date / Branch
2026-08-19 / `dev` (=origin/dev, 트리 clean, worktree 없음)

## ⚡ 한 줄 상태
**v1.5.3(vc25) production 업로드 완료 — Play 검토 중**(관리형 게시 OFF → 승인 즉시 자동 게시). API/푸시 임계치/결제/광고 전수 검증 GREEN.

## Completed (이번 세션, 2026-08-18~19)
- [x] **v1.5.2(vc24) production 배포·게시** — 프리미엄 parity 4종(점수 탐색기·알림 내역·프리미엄 게이트·DEBUG 토글) + GMA Next-Gen + Pangle. 상세: @.claude/memory/bugs-fixed.md 59~60번
- [x] **v1.5.3(vc25) production 업로드** — 배너 콜드스타트 fix(66번) + 알림 보관 표시/영속 분리(65번) + 어댑터 진단 로그(62번). production=[25] 트랙 API 확인, 콘솔 "검토 중" 실측. 게이트: 1019 tests/0, SHA-1 `CE:08:B4` 일치, DEBUG 심볼 0, 45 locale 대칭
- [x] **푸시 임계치 E2E 실수신 검증** — 에뮬 debug + App Check 토큰(`Claude push E2E emulator 2026-08-19`) 등록. "이상"=KOSPI 51 selling opportunity / "이하"=Crypto 46 buying opportunity 실수신 + 알림 내역 화면 기록 확인. 상세: @.claude/memory/bugs-fixed.md 67번
- [x] **API health 전수 GREEN** — KOSPI v2/short·CNN·Alternative.me·Yahoo·CoinGecko·환율·Naver (67번에 정확한 URL/헤더)
- [x] **결제 검증** — S22 release에서 ₩7,500 조회→Play 결제 시트 실진입→사이드로드 거부→앱 실패 처리 정상. TDD(IAP 유닛+계측 QA 3종) green
- [x] **AdMob 테스트 기기 등록(S22)** + 앱오픈 광고 정책 실기기 검증(콜드 제외/복귀 노출). RC `app_open_*` 4키 게시 완료(61번)
- [x] Unity 대시보드/AdMob 콘솔 실측 — Pangle 수익 출처=딸깍, iOS Unity 매핑 정상, Android Game ID `800107232`(63번)

## 미해결 / 다음 할 일 (우선순위순)
1. **1.5.3 게시 확인** — Play Console 게시 개요(`play.google.com/console/u/1/...` ⚠️ u/0은 타 계정) "검토 중"→게시 완료, 공개 리스팅 1.5.3 전파 확인. 게시되면 대시보드 API 36 경고 카드 소멸도 확인
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
