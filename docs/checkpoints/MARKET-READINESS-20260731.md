# 시장 준비 계획 — 2026-07-31 (금)

작성: 2026-07-31 새벽 (KST). 근거는 같은 세션에서 실측한 API 응답과 Play Console 화면.
장 시작 09:00 KST / 정규장 마감 15:30 KST.

## 1. 현재 상태 (실측 기준)

| 항목 | 상태 | 근거 |
|---|---|---|
| 코스피 데이터 | 정상 | `/api/kospi/v2` dataDate `2026-07-30`, intScore 17(극단적 공포), `isFinal:true` `stale:false` |
| 코스피 히스토리/차트 | 정상 | history 1374건, `kospiClose` 1374/1374 존재 → 차트·RSI 계산 가능 |
| 코스피 공매도 | 미제공(설계대로) | `/api/kospi/short` `available:false` → 카드 자동 숨김 |
| BTC 지표 | 정상 | `cryptoOfficialIndicatorsV1` rsi 180건 + short 14건 모두 `available:true` |
| 글로벌 공포지수 | 정상 | CNN 37.54(fear), 응답 시각 실시간 |
| 암호화폐 공포지수 | 정상 | Alternative.me 28(Fear) |
| 시장 지수 티커 | 정상 | Yahoo chart v8 8심볼 전부 200 |
| 환율 | 정상 | USD/KRW 1444.789 (전일 1452.082) |
| FCM 푸시 | 정상 | 서버 크론 30분 주기, 등록→설정동기화 순서 정상, 즉시체크 훅 양 경로 커버 |
| 앱 배포 | 1.5.1(vc22) 업로드 예정 → 심사 | 1.5.0(vc21)은 심사 중 상태에서 대체됨 |

## 2. 오늘 타임라인

| 시각(KST) | 할 일 | 확인 방법 |
|---|---|---|
| 장 시작 전 (~08:50) | 코스피 스냅샷이 전일 종가(7/30) 확정본인지 확인 | `curl -s "https://fear-index-a4f4b.web.app/api/kospi/v2?v=20260610" \| jq '.latest.dataDate,.latest.isFinal,.latest.intScore'` |
| 09:00~09:30 | 장중 스냅샷으로 전환되는지 (`snapshotType:intraday`, dataDate가 오늘로) | 같은 명령 + 앱 KOSPI 탭 "장중 추정" 표기 |
| 09:00~15:30 | 장중 갱신 지연 감시 — 2시간 이상 정체면 앱이 stale 처리 | `latest.updatedAt` 과 현재 시각 차이 |
| 15:30~ (마감 후) | 종가 확정본 전환 (`isFinal:true`, "장마감 확정") | 앱 KOSPI 탭 상태줄 |
| 수시 | 임계값 돌파 시 푸시 (하한 25 / 상한 75 기본) | 현재 17이라 **이미 하한 아래** — 신규 알림은 쿨다운(30분/24시간) 규칙에 따름 |
| 심사 통과 시 | 1.5.1 자동 게시 확인 → release 머지 + 태그 | Play Console 게시 개요 |

## 3. 오늘 특히 볼 것

1. **코스피 17 = 극단적 공포 구간.** 장 시작 후 급반등 시 점수 변동이 커질 수 있음. 신호 분해 카드(신규)에서 어떤 신호가 움직였는지 바로 확인 가능 — 모멘텀 0 / 52주 강도 0 / 변동성 57 / 외국인 13 / 거래 과열 12 상태에서 출발.
2. **1.5.1 심사.** 통과 즉시 자동 게시(관리형 게시 꺼짐). 게시되면 targetSdk 36 경고와 Billing 8 정책 경고가 모두 자동 해제되는지 확인.
3. **결제 경로.** Billing 8.3.0으로 올린 첫 배포 — 게시 후 실기기에서 설정 → Premium → 가격 표시(₩7,500)와 구매 시트 진입까지 1회 확인 필요. 에뮬레이터는 Play Billing 제약으로 실결제 검증 불가.

## 4. 남은 리스크 / 조치 대기

| 항목 | 영향 | 조치 |
|---|---|---|
| `KospiFearIndexApi.history` 파라미터가 개수처럼 보이나 서버는 boolean 취급 | 숫자로 바꾸면 KOSPI 차트/RSI가 **에러 없이 빈 화면** | 타입을 Boolean으로 바꾸거나 경고 주석 (다음 작업) |
| Yahoo spark API 429 | 없음 — 死코드(참조 0건) | 정리 대상 |
| `RECEIVE_BOOT_COMPLETED` 권한 미사용 | 없음 | 정리 대상 |
| 결제 프로필 지급 보류 (신원 확인 미완료) | 수익 지급만 보류, 배포·결제 동작과 무관 | 사용자가 직접 처리 예정 |
| 첫 실행 시 updateSettings가 registerFCMToken보다 먼저 도달하는 경로 | 미검증 (자가 치유 추정) | 실측 필요 시 다음 세션 |

## 5. 빠른 점검 명령

```bash
# 코스피 현재값 + 신선도
curl -s "https://fear-index-a4f4b.web.app/api/kospi/v2?v=20260610" | \
  python3 -c "import json,sys; d=json.load(sys.stdin)['latest']; print(d['dataDate'], d['intScore'], d['snapshotType'], 'final=',d.get('isFinal'), 'stale=',d.get('stale'))"

# 공매도 제공 여부
curl -s "https://fear-index-a4f4b.web.app/api/kospi/short" | python3 -c "import json,sys; print(json.load(sys.stdin).get('available'))"

# 글로벌/크립토 공포지수
curl -s "https://api.alternative.me/fng/?limit=1" | python3 -c "import json,sys; d=json.load(sys.stdin)['data'][0]; print(d['value'], d['value_classification'])"
```
