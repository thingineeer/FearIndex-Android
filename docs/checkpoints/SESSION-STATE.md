# Session State — FearIndex-Android

## Date
2026-08-18 (저녁 세션 — 프리미엄 parity ultracode)

## Branch
`dev`(beebbe8) — `feature/v1.5.2-premium-parity` --no-ff 머지 완료. 트리 clean, worktree 없음. **push 미실행.**

## ⚡ 한 줄 요약

**iOS v1.9.4 프리미엄 parity 4종을 Android 로 이식 완료 + 전 검증 GREEN + dev 머지.** ①프리미엄 게이트(광고제거 구매=프리미엄) ②점수별 과거 수익률 슬라이더 ③알림 내역(홈 🔔) ④DEBUG 결제 토글. 유닛 1014/0 + 헤드리스 에뮬(API 36) 결제 QA 3/3 + release 심볼 0.

---

## Completed (이번 세션)

- [x] **4 sub-worktree 병렬 구현** — returndata(도메인/기본 데이터 재생성)/history(도메인+data JSONL)/explorer-ui(카드/VM)/history-ui(화면/🔔/기록 3경로). 전부 `--no-ff` 통합 → dev.
- [x] **i18n 55키 × 45 locale** — `scripts/i18n/import_xcstrings_keys.py`(iOS xcstrings 이식) + `check_locale_symmetry.py`(495키 대칭 통과).
- [x] **결제 QA 3시나리오 계측 GREEN** — `PremiumQaTest`(잠금/해제+슬라이더 목표값·리셋/전환 즉시 해제·재잠금). 함정 3개 해결: Espresso 3.6.1→**3.7.0**(API 36 InputManager 제거), 에뮬 스토리지 확보(shadewalk·bamfiresurvive debug 앱 제거), M3 Slider 는 swipe 대신 **SetProgress semantics action**.
- [x] **실결함 fix** — 알림내역 markSeen 클럭 스큐 무한루프(`lastMarkedNewest` 가드). 이 루프가 테스트 워커 GC livelock hang 의 원인이기도 했음(jstack 진단).
- [x] **release 검증** — assembleRelease OK + dex/mapping 에 DEBUG 결제 심볼 0.
- [x] 메모리 기록 — bugs-fixed **59번**, MEMORY.md 최신 상태.

## Next (다음 세션)

1. **push** (사용자 지시 시) — dev + feature 브랜치들.
2. **사용자 결정 2건**: ① 알림 내역 전용 AdMob 배너 유닛 발급(현재 홈 유닛 fallback, `NotificationHistoryScreen` TODO) ② 스토어 IAP 표시명("광고 제거" → 프리미엄 혜택 3종 반영 여부).
3. **v1.5.2 배포 대기 중** — 누적: GMA Next-Gen 1.3.1 + Pangle + AppOpen 유닛 + ensureConnected ANR fix + 프리미엄 parity 4종. 배포 시 vc23 bump + RC 앱오픈 키 게시 + 배포 후 Crashlytics(#96 MotionEvent)·배너 match rate 감시.
4. 실기기 Billing 8 구매 시트 진입 재검증(미완).

## 참조

- @../../.claude/memory/bugs-fixed.md 59번 — 이번 세션 상세
- @../../.claude/memory/MEMORY.md — 최신 상태
