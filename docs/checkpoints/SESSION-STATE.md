# Session State — FearIndex-Android

> 최신 진입점: @../../.claude/memory/resume-FearIndex-Android.md (이 파일과 동기, resume 스킬용)

## Date
2026-08-23 (광고 미노출 제보 분석 + 요금 점검 세션)

## Branch
`dev`(ea09ed7) = origin/dev. worktree 없음. **⚠️ Play production 은 v1.6.0(vc26)인데 이 레포엔 없음 — 다른 맥에서 push 필요.**

## ⚡ 한 줄 요약

**광고 "안 뜬다"는 GA/Crashlytics/서버 실측으로 재현 안 됨(배너 실패 481→80 급감).** 진짜 문제는 v1.6.0 소스 부재. 4계정 요금 8월 ≈₩8.4k 정상. 배너 잠재 결함 2건(320dp 하한, AndroidView stale) 발견. 상세 @../../.claude/memory/bugs-fixed.md 68번.

## 이전 세션 요약 (2026-08-19)

v1.5.3(vc25) production 업로드 → 게시 완료(이후 1.6.0 으로 대체됨). API health·푸시 임계치·결제 시트·광고 전수 검증 GREEN.

## Completed (이번 세션)

- [x] v1.5.2(vc24) → v1.5.3(vc25) 연속 배포. vc23은 fastlane internal draft로 소모(60번 함정)
- [x] 홈 배너 콜드스타트 미노출 규명·수정 — Next-Gen 동일 AdView 재-loadAd 무산(CANCELLED+NO_FILL 쌍) → 재시도마다 새 AdView + ON_RESUME 복귀 재시도 + `FearIndexAds` 진단 로그(66번)
- [x] 알림 내역 prune 표시/영속 분리 — 프리미엄 구매 즉시 복원 보장 + 시계 스큐 손실 해소, 회귀 7건(65번)
- [x] 푸시 임계치 E2E — KOSPI 이상/Crypto 이하 실수신 + 내역 기록 + 서버 즉시체크 16초(67번)
- [x] 결제 — Play 결제 시트 실진입까지 검증(사이드로드 거부는 환경 제약), TDD green(67번)
- [x] AdMob 테스트 기기(S22) 등록 + 앱오픈 정책 실기기 검증 + RC app_open 4키 게시(61·66번)
- [x] Unity/AdMob/Firebase 콘솔 실측 — Pangle 출처=딸깍, iOS Unity 매핑 정상(61·63번)

## Next (다음 세션) — 상세는 resume 파일

0. **v1.6.0(vc26) 소스 push(다른 맥)** → release 머지·태그·deployment.md 행 추가
0-1. AdMob 콘솔 실측(확장 같은 계정 + apps.admob.com 권한) / 배너 결함 2건 수정(68번)
1. ~~1.5.3 게시 확인~~ 완료(1.6.0 으로 대체)
2. 게시 후 Play 설치본 실결제 완주(사용자)
3. 배포 후 감시: #96 MotionEvent·프리미엄 non-fatal·배너 match rate
4. 사용자 결정 2건(알림내역 전용 유닛·IAP 표시명) + 1.5.4 후보(@docs/checkpoints/RELEASE-1.5.3-CHECKLIST.md A~H)

## 참조

- @../../.claude/memory/bugs-fixed.md 59~67번 — 세션 상세
- @../../.claude/memory/deployment.md — 출시 이력·상수
- @RELEASE-1.5.3-CHECKLIST.md — 잔여 이슈·게이트
- @../QA-PREMIUM.md — 결제 QA 시나리오·주의
