# v1.5.3 릴리스 사전 점검 목록

기준: **v1.5.2(vc24) production 배포 완료**(2026-08-18). 이 문서는 다음 릴리스에 들어갈 변경과 검증 항목.

## 1. 이미 dev 에 머지된 변경 (배포 대기)

| 항목 | 커밋 | 위험도 | 비고 |
|---|---|---|---|
| 미디에이션 어댑터 런타임 진단 로그 | `da46b56` | 낮음 | 로깅만 추가, 동작 변경 0. release 에서 `FearIndexAdapters` 태그 + Crashlytics 양쪽 기록 |

## 2. 배포 후 확인해야 할 것 (vc24 관찰)

- [x] **실기기(S22) vc24 업데이트 경로 검증 완료** — 크래시/ANR 0, 알림내역·점수탐색기·실광고 정상(64번)
- [ ] **Crashlytics #96 MotionEvent** (GMA Next-Gen 1.3.x 알려진 이슈) — 실사용자 발생 여부
- [ ] **프리미엄 경로 non-fatal** — CrashlyticsTree 가 올리는 `W/` 로그(수익률 Firestore fallback, 알림내역 read/rewrite 실패)
- [ ] **배너 match rate / eCPM** — Next-Gen 마이그레이션 전 기준선(AdMob Network eCPM $4.09, 일치율 86.66%) 대비 회귀 여부
- [ ] **Pangle 실가동** — vc24 가 Pangle 어댑터 최초 포함본. 데이터 쌓이면 fill 기여도 확인
- [ ] **앱오픈 광고 실노출** — RC `app_open_ads_enabled`(Android=true) 게시 완료, vc24 유저부터 가동
- [ ] **API 36 정책 경고 카드 소멸** — 프로덕션 게시 후 스캐너 갱신 대기

## 3. 알려진 미해결 이슈 (1.5.3 후보)

| # | 항목 | 위치 | 심각도 | 상태 |
|---|---|---|---|---|
| A | **알림 트레이 직행분 누락** | `NotificationHistoryRecorder.kt:78-90` | 중 | 서버가 `notification`+`data` 동시 발송 → 백그라운드에서 onMessageReceived 미호출. 트레이 알림 extras 에 data 가 없어 kind=OTHER 로 적재되고, **탭 없이 스와이프 삭제 시 영구 누락**. ⚠️ **FCM 은 최상위 `notification` 을 플랫폼별로 제거할 수 없다**(공식 문서: "All app instances, regardless of platform, can interpret … message.notification"; AndroidConfig 에 억제 필드 없음) → 유일한 해법은 **플랫폼별 이중 발송**(iOS=notification+data / Android=data-only). 비용 대비 효과 불명 → **1.5.3 보류 권장, 실사용 데이터 확인 후 판단**(63번) |
| B | `KospiFearIndexApi.history` boolean 지뢰 | `KospiFearIndexDataSource.kt:34` | 낮음 | 서버가 boolean 취급인데 파라미터가 개수처럼 보임. 누가 `history=365` 넣으면 KOSPI 차트/RSI 가 조용히 빈 화면. Boolean 타입 교체 권장 |
| C | Yahoo spark 死코드 | `YahooSparkDTO.kt` / `MarketIndexApi.kt` / `MarketIndexDataSource.kt` + DI 3곳 | 낮음 | 참조 0건, 서버는 429 반환 중. 제거 |
| D | 미사용 `RECEIVE_BOOT_COMPLETED` 권한 | `AndroidManifest.xml:8` | 낮음 | 수신자 0건. 제거하면 권한 노출 축소 |
| E | Fastfile `internal` lane 에 `release_status` 없음 | `fastlane/Fastfile:40-52` | 중 | draft(임시)로만 올라가 테스터에게 안 감. vc23 이 이 함정으로 소모됨(bugs-fixed 60번). `release_status: "completed"` 추가 |
| F | 알림 내역 전용 AdMob 배너 유닛 미발급 | `NotificationHistoryScreen.kt:232` | 낮음 | 현재 홈 유닛 fallback. 리포트에서 홈과 합산돼 구분 불가. **사용자 결정 대기** |
| G | 스토어 IAP 표시명 | Play Console | 낮음 | "광고 제거" → 프리미엄 혜택 3종 반영 여부. **사용자 결정 대기** |
| H | **KOSPI 번들 fallback 데이터 빈약** | `DefaultReturnData.kt:166-178` | 중 | 검증 버킷 11개뿐 → Firestore 실패 시 KOSPI 탐색기 범위의 **80.7%가 빈 화면**. 실서버는 건전(빈칸 1%)이라 평시엔 미발현하나, 프리미엄 결제 후 빈 화면은 CS 리스크. market/crypto 처럼 스크립트 생성으로 보강 권장(64번) |
| I | 알림내역 prune 시계 의존 | `NotificationHistoryPolicy.kt:27-28` | 중 | 기기 시계가 미래로 튀면 30일 초과 판정 → **정상 레코드 물리 삭제**(무료 사용자, 복구 불가). 조회 시점 필터링으로 변경 검토(64번) |

## 4. 미검증 (실기기 필요)

- [ ] **Billing 8 구매 시트 진입** — 1.5.1 에서 Billing 8.3.0 마이그레이션했으나 실결제 경로는 미검증. 라이선스 테스터 + 실기기 필요
- [ ] 프리미엄 잠금/해제 실동작 (계측 QA 는 GREEN, 실결제 연동은 별개)

## 5. 릴리스 게이트 (배포 직전 필수)

```bash
./gradlew test                      # 전 모듈 GREEN (현재 1014)
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest  # 결제 QA 3/3
./gradlew :app:bundleRelease        # AAB + 서명 SHA-1 CE:08:B4 확인
python3 scripts/i18n/check_locale_symmetry.py   # 45 locale 누락 0
# release AAB dex/mapping 에 DEBUG 결제 심볼 0 확인
# 신규 문자열 있으면 changelog <vc>.txt 45 locale 작성
```

⚠️ **버전 bump 시 주의**: vc23 은 draft 로 소모됨. 다음은 **vc25** 부터.

## 참조

- @../../.claude/memory/bugs-fixed.md 59~62번
- @../../.claude/memory/deployment.md — 출시 이력·게이트
- @../../docs/QA-PREMIUM.md — 프리미엄 QA 시나리오
