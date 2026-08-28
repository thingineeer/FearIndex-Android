# Session State — FearIndex-Android

> 최신 진입점: @../../.claude/memory/resume-FearIndex-Android.md (이 파일과 동기, resume 스킬용)

## Date
2026-08-28 (v1.6.1 배포 세션)

## Branch
`dev` = origin/dev (clean). 브랜치는 `dev`/`main`/`release` 3개뿐(머지 완료 feature 브랜치 local·remote 전부 삭제). worktree 없음.

## 배포 상태
- **Play production = v1.6.1 / versionCode 27** — 2026-08-28 12:08 KST `fastlane production` 업로드 → 관리형 게시 OFF, 자동 검토 통과 → **12:49 KST 공개 리스팅 1.6.1 전파 확인**(릴리즈노트 "Redesigned home screen widgets…"). 트랙 production=[27], internal=[23,22].
- `release` = dev 머지(b654a7dc), 태그 **v1.6.1** push 완료. 다음 배포는 vc28.
- 서명: 업로드 키 SHA-1 `CE:08:B4…`(SSOT `~/thingineeer-env/android/fearindex/`, 로컬 `~/fearindex-secrets/` 동일 확인).
- RC `force_update_minimum_version`[Android] = `1.6` 유지 — **1.6.1 상향 여부 사용자 결정 대기**.

## ⚡ 한 줄 요약
v1.6.1 = 위젯 전면 리디자인(1×1 게이지 3종·통합·차트 Global/KOSPI/Crypto·↻ WorkManager·피커 previewImage/label) + 위젯 가이드 Android 재작성 + 앱명 45 locale + 온보딩 코치마크 개선 + 등급 원점수 기준·경계 윗 밴드(55.0=탐욕) 3플랫폼 통일 + 탐욕 카피 통일 + 인터스티셜 10분 세션 리셋·KOSPI 진입 즉시 노출 + ko '줍줍' ASO. 1,132 tests/0, S22 release ANR 3시나리오 통과.

## Completed (이번 세션, 2026-08-27~28)
- [x] 위젯 리디자인·피커 미리보기·가이드 재작성(73번) / 등급 원점수·경계 통일(73번 후속 2·3) / release S22 ANR 판정 통과(후속 4)
- [x] KOSPI 인터스티셜 5초 지연 제거 → 즉시 노출(TDD + S22 E2E 300ms)
- [x] changelog 27 위젯 문구로 45 locale 갱신 → v1.6.1 업로드·전파·release 머지·태그
- [x] AdMob 정책센터 실측: 제한 1%(965요청) = 1.0.1 구버전 잔여, 조치 불필요
- [x] 3세션(메인/토스/안드로이드) save-session + push 완료

## Next
1. RC force `1.6`→`1.6.1` 상향 여부(사용자) → 상향 시 32번 절차(get→한 줄→deploy→라이브 재조회)
2. Crashlytics 1.6.1 FATAL 0·위젯 ANR·`AppCheckTokenProbe` 추이 감시(배포 +1일)
3. Play 리뷰 2건 답글(사용자 직접), 푸시 트레이 표시 최종 1회 확인(메인 세션 요청)
4. 백로그: 배너 320dp 하한·AndroidView stale(70번), Fastfile internal `release_status`, Firebase 폐기 지문 AD:48 제거

## 참조
- @../../.claude/memory/bugs-fixed.md 72~73번 — 세션 상세
- @../../.claude/memory/deployment.md — 출시 이력(v1.6.1 행)·상수
- @HANDOFF-v1.6.1.md — (완료) 게시 절차
