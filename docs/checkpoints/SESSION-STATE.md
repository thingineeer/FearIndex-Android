# Session State — FearIndex-Android

## Date
2026-07-03

## Branch
`dev` (v1.3.0 작업 브랜치는 머지 후 삭제 — 브랜치 정리 완료)

## ⚡ 최신 (2026-07-03): v1.3.0 게시 완료 ✅ + FCM 정책 개편 검증 완료 ✅

**Android `1.3.0` / `versionCode 17` 이 Play Store production에 게시 완료** (당일 심사 통과, 관리형 게시 "게시"는 사용자 수동 클릭). `release` 머지 + `v1.3.0` 태그 완료. 강제 업데이트 RC 게이트는 `1.2` 유지 — 1.3.0 통과라 조치 불필요.

핵심 한 줄: iOS SSOT(RSICalculator/ShortPressureCalculator)를 TDD로 1:1 포팅해 3자산 RSI(14)/공매도 동향 카드를 홈에 추가하고, AAB+changelog(45 locale "투자 지표를 추가하였습니다.")만 업로드 → 빠른 검사 통과 시 자동 검토 전송(이번엔 수동 "검토 전송" 불필요) → 당일 게시.

---

## Completed (이번 세션)

- [x] **FCM 알림 정책 개편 검증** — ① payload 그대로 표시(가공/파싱 0) ② 클릭 문구 파싱 없음 ③ registerFCMToken 매 실행 호출. admin SDK 실수신 테스트(새 포맷, 백/포그라운드/탭) 통과. App Check debug token 등록(MacBook emulator 2026-07-03). 상세: @../../.claude/memory/bugs-fixed.md 33번.
- [x] **v1.3.0 RSI/공매도 지표** — Domain(계산기 2종+UseCase 2종)/Data(API 5원천+TTL캐시+8s timeout)/Presentation(카드 2종+설명 시트+45 locale 35키). 신규 테스트 45개, 전체 GREEN. 실기기 3탭 육안 검증. 상세: 34번.
- [x] **배포** — versionCode 17, SHA-1 `CE:08:B4:...` 일치, fastlane run upload_to_play_store(AAB+changelog만, 스크린샷/메타 skip), 자동 검토 전송 → 당일 게시.
- [x] **Git** — dev→feature/v1.3.0→worktree(5커밋+세션저장) --no-ff 머지, release 머지+`v1.3.0` 태그, 머지 완료 브랜치 삭제.

## 미해결 / 다음 세션

1. **서버 팀 전달 2건 (iOS repo 관할)**:
   - `updateNotificationSettings`에 즉시체크(dispatchInstantCheck) 훅 없음 → Android 신규 유저 즉시 알림 못 받음 (`device-callables.ts:245`는 registerFCMToken 신규 유저 분기뿐).
   - `/api/kospi/short` 미집계 당일 값 `0` → "0.0% 숏커버링" 오신호 (iOS 동일 영향).
2. **KOSPI SimilarEvents raw key 노출** — `insight.kospi.event.tradeWar2018` 그대로 표시 (기존 버그, 번역 키 누락).
3. **push 미수행** — dev/release/태그 전부 로컬. 명시 요청 시 push.
4. AdMob 정책센터 상태 재확인 (구버전 트래픽 수렴 여부).
5. 에뮬레이터 `/data` 6GB 중 400MB 남음 — 정리 또는 AVD 확장 필요 (이번 세션 설치 실패 원인).

## 참조

- @../../.claude/memory/MEMORY.md — 메모리 인덱스 (최신 상태)
- @../../.claude/memory/bugs-fixed.md 33·34번 — 이번 세션 상세
- @../../.claude/memory/deployment.md — v1.3.0 출시 이력
