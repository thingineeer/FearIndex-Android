# Session State — FearIndex-Android

> 최신 진입점: @../../.claude/memory/resume-FearIndex-Android.md (이 파일과 동기, resume 스킬용)

## Date
2026-08-19 (v1.5.3 배포 + 전수 검증 세션)

## Branch
`dev`(85b683e) = origin/dev. release 브랜치에 v1.5.2·v1.5.3 머지 + 태그 push 완료. 트리 clean, worktree 없음, 로컬 feature 브랜치 정리 완료(dev/main/release만 남음).

## ⚡ 한 줄 요약

**v1.5.3(vc25) production 업로드 완료 — Play 검토 중(관리형 게시 OFF → 자동 게시).** 내용 = 배너 콜드스타트 fix + 알림 보관 표시/영속 분리 + 어댑터 진단. API health·푸시 임계치(이상/이하 실수신)·결제 시트·광고(테스트 기기) 전수 검증 GREEN.

## Completed (이번 세션)

- [x] v1.5.2(vc24) → v1.5.3(vc25) 연속 배포. vc23은 fastlane internal draft로 소모(60번 함정)
- [x] 홈 배너 콜드스타트 미노출 규명·수정 — Next-Gen 동일 AdView 재-loadAd 무산(CANCELLED+NO_FILL 쌍) → 재시도마다 새 AdView + ON_RESUME 복귀 재시도 + `FearIndexAds` 진단 로그(66번)
- [x] 알림 내역 prune 표시/영속 분리 — 프리미엄 구매 즉시 복원 보장 + 시계 스큐 손실 해소, 회귀 7건(65번)
- [x] 푸시 임계치 E2E — KOSPI 이상/Crypto 이하 실수신 + 내역 기록 + 서버 즉시체크 16초(67번)
- [x] 결제 — Play 결제 시트 실진입까지 검증(사이드로드 거부는 환경 제약), TDD green(67번)
- [x] AdMob 테스트 기기(S22) 등록 + 앱오픈 정책 실기기 검증 + RC app_open 4키 게시(61·66번)
- [x] Unity/AdMob/Firebase 콘솔 실측 — Pangle 출처=딸깍, iOS Unity 매핑 정상(61·63번)

## Next (다음 세션) — 상세는 resume 파일

1. 1.5.3 게시 확인(콘솔 u/1 주의) → deployment.md 출시 이력 행 갱신
2. 게시 후 Play 설치본 실결제 완주(사용자)
3. 배포 후 감시: #96 MotionEvent·프리미엄 non-fatal·배너 match rate
4. 사용자 결정 2건(알림내역 전용 유닛·IAP 표시명) + 1.5.4 후보(@docs/checkpoints/RELEASE-1.5.3-CHECKLIST.md A~H)

## 참조

- @../../.claude/memory/bugs-fixed.md 59~67번 — 세션 상세
- @../../.claude/memory/deployment.md — 출시 이력·상수
- @RELEASE-1.5.3-CHECKLIST.md — 잔여 이슈·게이트
- @../QA-PREMIUM.md — 결제 QA 시나리오·주의
