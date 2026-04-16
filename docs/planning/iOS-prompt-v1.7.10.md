# iOS Claude 세션에 붙여넣을 프롬프트 (v1.7.10)

사용법: 아래 "PROMPT" 블록 전체를 복사 → iOS 프로젝트 Claude 세션 (`~/Desktop/side/FearIndex-iOS`)에 붙여넣기.

---

## PROMPT

```
v1.7.10 작업: 이란 전쟁 저점 이벤트 추가 + Firestore returnData 동적 시드.

Android 쪽에서는 이미 Firestore returnData fetch 로직을 추가하는 중이고, 너는 아래 iOS 전용 작업만 책임지면 돼. 
전체 작업은 iOS CLAUDE.md의 worktree 기반 git workflow (`--no-ff` merge, squash 금지)를 따라서 진행해.

## 1. worktree 생성
dev에서 분기:
  git worktree add ../FearIndex-iOS-iran-war feature/v1.7.10-iran-war-event

## 2. 실측 데이터 수집 (필수 — 추정치 금지)

### Market (S&P 500)
다음 중 실제 공포지수가 10점대 이하로 떨어진 이란-이스라엘 관련 저점 날짜를 확정:
- 후보 A: 2024-10-03 (이스라엘 레바논 침공)
- 후보 B: 2025-06-13 (이스라엘-이란 직접 교전 개시일)
- 후보 C: 가장 최근 10점대 저점 (2025-2026)

데이터 소스:
- CNN Fear & Greed Index 역사값: whit3rabbit F&G CSV (iOS firebase-functions에서 이미 사용 중)
- S&P 500 종가: StatMuse 또는 Yahoo Finance
- 수치:
  * 해당 날짜 F&G 점수 (정확한 정수)
  * S&P 500 기준 +1M / +3M / +6M / +1Y 수익률 (%)
  * 1Y 이 아직 경과 안 했으면 null 로 두고 3M/6M 까지만 기록

### Crypto (Bitcoin)
같은 날짜에 대해 Bitcoin F&G (alternative.me API) + BTC 종가 기준 수익률 조사.

## 3. DefaultReturnData.swift 업데이트 (SSOT)

파일: LocalPackages/Domain/Sources/Domain/Defaults/DefaultReturnData.swift

- makeMarketEvents() 의 ev(...) 배열에 Iran 이벤트 1개 추가.
  id 예: "iran-war-2025" (확정 날짜에 맞게)
  descriptionKey: "insight.event.iranWar"
- makeCryptoEvents() 의 ev(...) 배열에도 1개 추가.
  id: "crypto-iran-war-2025"
  descriptionKey: "insight.crypto.event.iranWar"
- distance 기반 매칭이므로 배열 내 위치는 score 오름차순 유지.
- 검증일 코멘트 (`// 검증일: 2026-04-16`) 로 갱신.

## 4. Localization 키 추가 (45 locale)

FearIndex-iOS/Resources/*.lproj/Localizable.strings 45개 전부에:
"insight.event.iranWar" = "이란 전쟁 저점";
"insight.crypto.event.iranWar" = "이란 전쟁 저점";

locale별 번역:
- ko: "이란 전쟁 저점"
- en: "Iran War Low"
- ja: "イラン戦争の底値"
- zh-Hans: "伊朗战争低点"
- zh-Hant: "伊朗戰爭低點"
- de: "Iran-Krieg-Tiefpunkt"
- fr: "Creux de la guerre d'Iran"
- es: "Mínimo de la guerra de Irán"
- ar / he: RTL 고려, 적절히 번역

나머지 ~35개 locale 은 영어 fallback 허용 (기존 scheme 따라).

## 5. Firestore 시드 (firebase-functions)

cd firebase-functions

(a) anchor 집계 재실행:
  npx ts-node src/data-aggregation/build-return-data.ts
  → output/market.json, output/crypto.json 재생성

(b) seed-return-data.ts 업데이트:
  src/seed-return-data.ts 의 marketEvents / cryptoEvents 에 위 4번과 동일한 이벤트 추가.
  descriptionKey 동일.

(c) Firestore 업로드:
  npx ts-node src/seed-return-data.ts
  → returnData/market, returnData/crypto 의 historicalEvents 배열 갱신됨.

(d) Firestore Console 에서 확인:
  https://console.firebase.google.com/project/fear-index-a4f4b/firestore/databases/-default-/data/~2FreturnData
  - returnData/market.historicalEvents 에 iran-war-2025 포함
  - returnData/market.updatedAt 이 방금 시각으로 갱신
  - returnData/crypto 도 동일

## 6. 커밋 분할 (의미 단위)

worktree 안에서 다음 4-5 커밋:
- feat: 이란 전쟁 저점 이벤트 DefaultReturnData 추가
- i18n: 이란 전쟁 localization 45 locale
- feat: seed-return-data.ts 이란 전쟁 이벤트 추가
- chore: data-aggregation 재집계 결과 반영
- (선택) test: ReturnData 이란 이벤트 매칭 단위 테스트

## 7. 머지

worktree 에서 본진 으로 돌아와서:
  git checkout feature/v1.7.10  # (없으면 dev 에서 분기 후 create)
  git merge --no-ff feature/v1.7.10-iran-war-event
  git checkout dev
  git merge --no-ff feature/v1.7.10

## 8. Android 에 전달할 정보 (꼭 보고)

완료 후 나(Android 세션)에게 아래 데이터를 보고해줘:

- 확정된 이벤트 id (예: "iran-war-2025")
- 확정된 date (YYYY-MM-DD)
- 확정된 score (정수)
- Market returnAfter { oneMonth, threeMonth, sixMonth, oneYear } 수치
- Crypto 동일
- descriptionKey 문자열
- 45 locale 중 실제 번역한 locale 리스트 (나머지는 영어 fallback 처리)
- 커밋 SHA (hash 앞 7자리 정도)

그러면 Android 쪽에서:
- DefaultReturnData.kt 에 동일 이벤트 포팅
- strings.xml 에 같은 키 추가 (45 locale)
- Firestore 클라이언트 fetch 는 이미 구현되어 있으므로 자동 반영

## 금지 사항

- 추정치로 이벤트 수치 작성 금지 (실측 데이터만)
- 45 locale 기본 번역 없이 건너뛰기 금지 (최소 ko/en/ja/zh-Hans/zh-Hant/de/fr/es 8개는 실제 번역)
- Firestore updatedAt 수동 수정 금지 (seed 스크립트가 now 로 찍음)
- squash merge 금지, cherry-pick 금지

시작해.
```

---

## 전달 방법

1. iOS 터미널 (이미 Claude 세션 돌아가는 창)에 접속
2. 위 PROMPT 블록을 한 번에 붙여넣기
3. iOS Claude 가 작업 완료 후 위 "8. Android 에 전달할 정보" 보고하면
4. Android 세션(본 세션)에 그 데이터를 붙여넣으면 Android 반영 완료

## 참고: Android 가 기다리는 데이터 구조

```kotlin
// core/src/main/java/th1ngjin/fearindex/core/util/InsightGenerator.kt
// 또는 domain/src/main/kotlin/th1ngjin/fearindex/domain/defaults/DefaultReturnData.kt 에
// 아래와 같이 추가 (iOS 수치 복붙):

private fun makeMarketEvents(): List<ReturnEventEntry> = listOf(
    // ... 기존 12개
    ReturnEventEntry(
        id = "iran-war-2025",
        date = LocalDate.of(2025, 6, 13), // iOS 에서 확정
        score = 8, // iOS 에서 확정
        descriptionKey = "insight.event.iranWar",
        returnAfter = HistoricalReturns(
            oneMonth = 5.2,    // iOS 에서 확정
            threeMonth = 12.3, // iOS 에서 확정
            sixMonth = null,   // iOS 에서 확정 (아직 미경과면 null)
            oneYear = null,
        ),
    ),
)
```
