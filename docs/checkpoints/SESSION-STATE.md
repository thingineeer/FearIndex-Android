# Session State — FearIndex-Android

## Date
2026-08-18 (저녁 세션 — 프리미엄 parity ultracode)

## Branch
`dev` — 프리미엄 parity + release-prep(vc23) + release-observability + vc24 전부 --no-ff 머지. release 브랜치에 v1.5.2 머지 + 태그. **dev/release/태그/feature 브랜치 전부 push 완료.** 트리 clean, worktree 없음.

## ⚡ 한 줄 요약

**iOS v1.9.4 프리미엄 parity 4종 이식 + v1.5.2(vc24) production 배포 완료(관리형 게시 OFF, 빠른 검사→자동 게시).** ①프리미엄 게이트(광고제거 구매=프리미엄) ②점수별 과거 수익률 슬라이더 ③알림 내역(홈 🔔) ④DEBUG 결제 토글. 유닛 1014/0 + 헤드리스 에뮬(API 36) 결제 QA 3/3 + release 심볼 0.

---

## Completed (이번 세션)

- [x] **4 sub-worktree 병렬 구현** — returndata(도메인/기본 데이터 재생성)/history(도메인+data JSONL)/explorer-ui(카드/VM)/history-ui(화면/🔔/기록 3경로). 전부 `--no-ff` 통합 → dev.
- [x] **i18n 55키 × 45 locale** — `scripts/i18n/import_xcstrings_keys.py`(iOS xcstrings 이식) + `check_locale_symmetry.py`(495키 대칭 통과).
- [x] **결제 QA 3시나리오 계측 GREEN** — `PremiumQaTest`(잠금/해제+슬라이더 목표값·리셋/전환 즉시 해제·재잠금). 함정 3개 해결: Espresso 3.6.1→**3.7.0**(API 36 InputManager 제거), 에뮬 스토리지 확보(shadewalk·bamfiresurvive debug 앱 제거), M3 Slider 는 swipe 대신 **SetProgress semantics action**.
- [x] **실결함 fix** — 알림내역 markSeen 클럭 스큐 무한루프(`lastMarkedNewest` 가드). 이 루프가 테스트 워커 GC livelock hang 의 원인이기도 했음(jstack 진단).
- [x] **release 검증** — assembleRelease OK + dex/mapping 에 DEBUG 결제 심볼 0.
- [x] 메모리 기록 — bugs-fixed **59번**, MEMORY.md 최신 상태.

## Next (다음 세션)

0. **게시 확인**: Play Console 프로덕션 1.5.2(vc24) "게시 완료" + 공개 리스팅 1.5.2 전파 → 대시보드 API 36 경고 카드 소멸 확인. 필요 시 RC `force_update_minimum_version` 상향은 하지 않음(정책 이슈 없음).
1. **배포 후 감시**: Crashlytics #96 MotionEvent(Next-Gen 1.3.x), 프리미엄 경로 non-fatal(CrashlyticsTree W/*), AdMob 배너 match rate 기준선 비교. RC `app_open_ads_enabled` 게시 판단.
2. **사용자 결정 2건**: ① 알림 내역 전용 AdMob 배너 유닛 발급(현재 홈 유닛 fallback, `NotificationHistoryScreen` TODO) ② 스토어 IAP 표시명("광고 제거" → 프리미엄 혜택 3종 반영 여부).
3. Fastfile `internal` lane 에 `release_status: "completed"` 추가(현재 draft 로만 올라감 — 60번).
4. 실기기 Billing 8 구매 시트 진입 재검증(미완).

## 참조

- @../../.claude/memory/bugs-fixed.md 59번 — 이번 세션 상세
- @../../.claude/memory/MEMORY.md — 최신 상태
