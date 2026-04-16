# 실시간 returnData 로딩 + 이벤트 추가 기획

## Date
2026-04-16

## 문제 정의

현재 인사이트 카드의 과거 이벤트 데이터가 **앱 내 하드코딩**이라 앱 업데이트 없이 추가/수정 불가.

- Android: `DefaultReturnData.kt` (anchor 6 + events 12개)
- iOS: `DefaultReturnData.swift` (동일)
- Firebase 프로젝트에 이미 `returnData/{market|crypto}` 스키마 존재 (iOS v1.7.9)
- iOS는 `ReturnDataRepository` + `ReturnDataSource`로 Firestore fetch 로직 **완성**
- **Android는 Firestore fetch 로직 없음** — fallback만 사용 중

### 현재 이벤트 리스트 (iOS/Android 동일)

Market (S&P 500):
- covid(2020-03-12, score=2), tariff-2025(2025-04-08, score=4), inflation-2022(score=15),
  delta-2021(score=20), trade-war-2019(score=22), svb-2023(score=23),
  reopening-2021(score=55), post-election-2024(score=68), 2021bull(score=73),
  mid-2023(score=80), pre-covid-high(score=89), dotcom(score=95)
- **최근 이벤트 누락**: 2025-2026년의 이란-이스라엘 전쟁 저점

Crypto (Bitcoin):
- covid, luna, ftx, china-ban, svb, sec, halving-2024, etf-approval,
  defi-summer, 2021-peak, trump-pump, 2017-peak

## 목표

### Phase 1 — 이벤트 추가 (iOS/Android)
- 이란 전쟁 저점 이벤트 1개 추가 (iOS가 실측 후 추가)
- Android는 iOS 커밋 후 동일 데이터 포팅

### Phase 2 — Android Firestore 동적 로딩
- `data/Repositories/ReturnDataRepository` 신규 구현 (iOS 구조와 대칭)
- Firestore `returnData/{market|crypto}` 문서 fetch
- 캐싱 + fallback → `DefaultReturnData`
- `InsightViewModel`에서 `DefaultReturnData` 직접 참조 제거, Repository 주입
- Firestore 로드 실패 시에도 앱 정상 동작 보장

### Phase 3 — UX 강화
- `InsightDetailSheet` 이벤트 카드에 "최근 추가" 뱃지 (예: 30일 이내)
- 데이터 업데이트 시각 표시 (`returnData.updatedAt`)
- 이벤트 정렬: distance 기반 유지 (iOS 대칭)

## 스코프 경계

- **이벤트 저점 감지는 자동화 불가** — 실측 데이터가 있어야 하고 "왜 떨어졌는지" 해석은 사람이 큐레이션.
- **Firebase Functions 코드 작업은 iOS 세션 담당** — iOS가 `seed-return-data.ts`에 이벤트 추가 + 배포.
- Android는 클라이언트 fetch + `DefaultReturnData` 포팅만.

## iOS 세션 작업 지시 (프롬프트)

아래 블록을 iOS 터미널의 Claude 세션(`~/Desktop/side/FearIndex-iOS`)에 붙여넣어 실행하세요.

```
v1.7.10 기획: 이란 전쟁 이벤트 추가 + returnData 동적 시드

Android가 Firestore returnData fetch 로직을 추가할 예정. iOS 쪽에서는 아래 작업을 수행해줘.

1. 실측 데이터 조사 (실제 수치가 필요):
   - 2024-2026 기간 동안 공포지수(CNN Fear & Greed Index)가 10점대 이하로 떨어진 저점 중
     이란-이스라엘 전쟁이 배경인 날짜 선정 (예: 2024-10-03 이스라엘 레바논 침공, 
     2025-06-13 이스라엘-이란 교전, 또는 더 최근 시점)
   - 해당 날짜의:
     * 정확한 F&G 점수 (whit3rabbit CSV 또는 alternative.me API)
     * S&P 500 종가 기준 이후 1M / 3M / 6M / 1Y 수익률 (StatMuse 또는 Yahoo Finance)
   - Bitcoin 동일 기간 수익률도 같이 조사 (crypto events에도 추가)

2. seed-return-data.ts 에 이벤트 추가:
   - src/seed-return-data.ts 의 marketEvents 배열에 Iran 전쟁 이벤트 1개 추가
   - descriptionKey 는 "insight.event.iranWar" (신규)
   - cryptoEvents 에도 "crypto-iran-war" 동일 추가 (descriptionKey: "insight.crypto.event.iranWar")
   - 검증일 코멘트 갱신

3. Localization 추가 (45 locale):
   - FearIndex-iOS/Resources/ko.lproj/Localizable.strings 에:
     "insight.event.iranWar" = "이란 전쟁 저점";
     "insight.crypto.event.iranWar" = "이란 전쟁 저점";
   - en.lproj: "Iran War Low"
   - 나머지 43개 locale은 영어 fallback 또는 주요 locale만 번역 후 영어 fallback
   - 마찬가지로 Android strings.xml 에 반영하도록 나에게도 iOS 키 리스트를 알려줘
     (Android worktree에서 내가 따라 반영)

4. DefaultReturnData.swift 하드코딩도 동시 업데이트:
   - LocalPackages/Domain/Sources/Domain/Defaults/DefaultReturnData.swift
   - makeMarketEvents() 에 이란 전쟁 ev(...) 추가
   - makeCryptoEvents() 에도 추가
   - Android 쪽에 반영할 수 있도록 커밋 SHA 알려줘

5. Firebase Functions 배포:
   - cd firebase-functions
   - npx ts-node src/data-aggregation/build-return-data.ts (anchor 데이터 재집계)
   - npx ts-node src/seed-return-data.ts (Firestore returnData/market 및 returnData/crypto 시드)
   - 완료 후 Firestore Console 에서 returnData/market.historicalEvents 배열에 iran-war 포함 확인

6. 선택: returnData 문서에 updatedAt 타임스탬프 수정 시각으로 갱신 — Android/iOS 클라이언트가
   "최근 업데이트" 표시할 수 있도록.

worktree 브랜치명: feature/v1.7.10-iran-war-event (iOS CLAUDE.md의 워크플로우 따라 worktree 만들고 
의미 단위 커밋 → 본진 머지 → dev 머지).

이거 완료되면 Android 쪽에서 DefaultReturnData.kt 에 동일 이벤트 포팅 + ReturnDataRepository 
Firestore fetch 로직 추가할 예정.
```

## Android 작업 (본 세션에서 진행)

iOS 세션과 **독립적으로 진행 가능**한 부분:
1. `ReturnDataRepository` + `ReturnDataSource` 구현 (Firestore fetch + 캐싱 + fallback)
2. `InsightViewModel`에서 Repository 사용하도록 리팩터
3. iOS 커밋 후 `DefaultReturnData.kt` 이벤트 포팅 (수동)
4. strings.xml 이란 전쟁 키 추가 (iOS 번역본 따라)

Firestore에 데이터가 아직 없어도 fallback으로 정상 동작 → **순서 독립**.

## Firestore 스키마 (iOS v1.7.9 정의)

```
returnData/{indexType}
├── version: number (현재 2)
├── updatedAt: number (Unix timestamp)
├── sourceGeneratedAt: string (ISO 8601)
├── sourceFngRange: { from: string, to: string }
├── sourcePriceRange: { from: string, to: string }
├── dataPoints: [101개]
│   └── { score, returns{1M,3M,6M,1Y}, worstCase, bestCase, sampleCount }
└── historicalEvents: [N개]
    └── { id, date, score, descriptionKey, returnAfter{1M,3M,6M,1Y},
           scoreMin?, scoreMax? (deprecated, 호환용) }
```

## 테스트 시나리오

1. **Firestore 연결 성공**: 이란 전쟁 이벤트가 46점 matchingEvents 결과에 포함되는지 (distance = |46-15| = 31, 현재 55/68/23이 더 가까움 → 상위 3개에는 안 뜨지만 10점대 저점에서 조회 시 뜸)
2. **Firestore 실패 시 fallback**: 네트워크 끊김 / 문서 없음 → `DefaultReturnData.kt` 사용
3. **캐시 동작**: 같은 세션 2회 조회 시 Firestore 2번 호출 안 됨
4. **Unit test**: `ReturnDataRepository` mock DataSource로 fallback 경로 검증

## 관련 문서
- `.claude/memory/firebase-setup.md` — Firebase 설정
- `.claude/memory/ios-parity.md` — iOS 대칭성 규칙
- iOS `firebase-functions/src/seed-return-data.ts` — Firestore 시드 (SSOT)
