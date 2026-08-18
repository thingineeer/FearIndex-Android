# 프리미엄 QA 가이드 (v1.5.2+)

계측 자동화: `app/src/androidTest/java/th1ngjin/fearindex/qa/PremiumQaTest.kt`
실행: 헤드리스 에뮬레이터 부팅 후
```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest
```

## 시나리오 3종 (iOS PremiumQAUITests 대칭)

| # | 상태 | 기대 동작 |
|---|---|---|
| T1 | 결제 안 함 | 점수 탐색기 잠금(티저 슬라이더+잠금 CTA, 리셋 미노출) · 설정 "광고 제거됨" 미표시 · 알림 내역 빈 상태/잠금 row |
| T2 | 결제함 | 잠금 CTA 없음 · 슬라이더 조작 시 점수 반영 + 리셋→현재 점수 복귀 · "광고 제거됨" 표시 · 내역 잠금 row 없음 |
| T3 | 전환 | DEBUG 토글 구매 안 함→잠금 → 구매함→**즉시** 해제 → 구매 안 함→재잠금 |

수동 확인 시 결제 상태 강제는 **설정 하단 "DEBUG: 결제 테스트" 카드**(debug 빌드 전용, 실제/구매함/구매 안 함).

## ⚠️ 주의 (데이터 실삭제)

**DEBUG 토글을 '구매 안 함' 상태로 두고 알림 내역에 진입하면 30일 초과 기록이 실제로 삭제된다** (무료 보관 정책 prune 이 저장소에 그대로 적용됨 — 시뮬레이션이 아님). 장기 보관 기록이 있는 기기/에뮬레이터에서 QA 할 때는:
1. 먼저 `adb shell run-as th1ngjin.fearindex.debug cat files/notification_history/notification-history.jsonl > backup.jsonl` 로 백업하거나,
2. 30일 초과 기록이 없는 깨끗한 설치본에서 진행할 것.
QA 종료 시 토글을 반드시 **'실제'** 로 복귀 (PremiumQaTest 는 tearDown 에서 자동 복귀).

## 참고

- 계측 함정: Espresso ≥3.7.0 필요(API 36), M3 Slider 는 swipe 대신 SetProgress semantics action 사용. 상세: `.claude/memory/bugs-fixed.md` 59번.
- 실결제 검증(라이선스 테스터)은 내부 테스트 트랙 게시 후 실기기에서 별도 진행.
