# Session State — FearIndex-Android

## Date
2026-07-31 (새벽 세션, 2026-07-30 밤부터 연속)

## Branch
`dev` (origin/dev 동기화 완료). 피처 브랜치 전부 머지 후 삭제 — 남은 브랜치는 `dev` / `main` / `release`뿐.

## ⚡ 한 줄 요약

**v1.5.0(vc21) → v1.5.1(vc22) 연속 게시 완료.** 코스피 신호 분해 UI 신규 + targetSdk 36 + Play Billing 8.3.0 마이그레이션까지 끝냈고, 밀려 있던 태그(v1.4.2/v1.5.0/v1.5.1)와 release 머지도 전부 정리해 push했다.

---

## Completed (이번 세션)

- [x] **코스피 신호 분해 카드 + 산출 방식 시트** (iOS parity) — 홈 KOSPI 탭에 신호별 점수/프로그레스바/클러스터·가중치 + USD/KRW 환율 행 + 결측 신호 표시. ⓘ → "코스피 산출 방식" 시트 7섹션(산출 방식/데이터 품질/현재 계산 정보/환율/신호 분해/클러스터 점수/결측 처리). strings 46키 × 45 locale(iOS xcstrings 추출), `KospiSignalText` TDD 4케이스. **데이터 계층은 원래 완비돼 있었고 UI만 없던 상태**였음. 상세: @.claude/memory/bugs-fixed.md 47번
- [x] **targetSdk 35 → 36** (Play 정책 2026-08-31 기한). compileSdk는 원래 36. 매니페스트에 차단 요소 없음(엣지투엣지/백/방향 고정 전무).
- [x] **Unity Ads SDK 본체 의존성 추가** — 어댑터만 있고 SDK가 없어 release R8 빌드가 `Missing class`로 깨져 있었음(7/26 커밋 이후 release 빌드를 한 번도 안 돌린 탓). 48번
- [x] **v1.5.0(vc21) 업로드 → 게시 완료** (2026-07-31). 공개 스토어 1.5.0 확인.
- [x] **Play Billing 7.1.1 → 8.3.0 마이그레이션** (정책 8/31 기한, 미준수 시 앱 업데이트 거부). 9.x는 Kotlin 2.3 메타데이터 요구로 불가. breaking change 2건 대응 + `IapOfferSelection` TDD 5케이스. 50번
- [x] **v1.5.1(vc22) 업로드 → 게시 완료** (2026-07-31). 스토어 1.5.1 + 번들 탐색기 `22.aab` 활성/타겟SDK 36 확인.
- [x] **태그·release 정리** — 밀려 있던 v1.4.2, v1.5.0, v1.5.1을 순차 `--no-ff` 머지 후 태깅. dev/release/태그 전부 origin push.
- [x] **API/푸시 전수 실측 검증** — 외부 API 11종 전부 200 + 스키마 일치, 푸시 경로 정상, targetSdk 36 영향 없음. 52번
- [x] **결제 실증** — 주문 관리에서 실주문 1건 확인(`remove_ads_lifetime`, GPA.3334-7862-8616-69040, 2026-07-23, HUF 1,999, 처리됨). 수익은 광고가 아니라 IAP에서 나온 것.
- [x] **시장 준비 계획 문서** — @docs/checkpoints/MARKET-READINESS-20260731.md

## In Progress

없음. 이번 세션 작업은 전부 닫혔고 working tree clean.

## Remaining (다음 세션 우선순위)

1. **정책 경고 2건 자동 해제 확인** — API 36 / Billing 8.0.0+ 경고가 게시 직후에도 정책센터에 표시됨. **갱신 지연으로 판단**(근거: Google 자체 번들 탐색기가 활성 번들 vc22의 타겟 SDK를 36으로 인식, 우리 매니페스트도 `billingclient.version=8.3.0`). 하루 이내 자동 해제 예상 → 재확인만.
2. **실기기 결제 재검증 (필수)** — Billing 8 첫 배포. 폰에서 1.5.1 업데이트 후 설정 → Premium에서 **₩7,500 표시 + 구매 시트 진입**까지 확인. 에뮬레이터는 Play 서명 앱이 아니라 원천적으로 검증 불가.
3. **`KospiFearIndexApi.history` boolean 지뢰** — 파라미터가 개수처럼 보이나 서버는 boolean 취급. `history=365` 같은 숫자를 넣으면 **KOSPI 차트/RSI가 에러 없이 빈 화면**이 됨. Boolean 타입으로 교체 권장.
4. **死코드 정리** — Yahoo spark API 3종(`MarketIndexApi`/`DataSource`/`RepositoryImpl` + DI 3곳, 실제 429 반환 중이나 참조 0건), 미사용 `RECEIVE_BOOT_COMPLETED` 권한.
5. (사용자 직접) **결제 프로필 지급 보류 해제** — Google payments 신원 확인 미완료. 신분증 제출은 본인만 가능. 배포·결제 동작과는 무관.

## Key Files

- @core/src/main/java/th1ngjin/fearindex/core/purchases/PurchaseManager.kt — Billing 8.3.0 마이그레이션 핵심. `queryProductDetailsAsync` 콜백이 `QueryProductDetailsResult`로 바뀐 부분, `RemoveAdsOffering`(상품+오퍼토큰 묶음), `launchFlow`의 `setOfferToken`
- @core/src/main/java/th1ngjin/fearindex/core/purchases/IapOfferSelection.kt — 일회성 상품 오퍼 선택 순수 로직 (신규, TDD)
- @presentation/src/main/java/th1ngjin/fearindex/presentation/component/KospiSignalBreakdownCard.kt — 코스피 신호 분해 카드
- @presentation/src/main/java/th1ngjin/fearindex/presentation/component/KospiMethodInfoSheet.kt — 코스피 산출 방식 시트 7섹션
- @presentation/src/main/java/th1ngjin/fearindex/presentation/common/KospiSignalText.kt — 서버 신호명/클러스터/신뢰도 → 리소스 매핑
- @presentation/src/main/java/th1ngjin/fearindex/presentation/feature/home/HomeScreen.kt — 카드 배치 위치(배너 바로 아래, iOS와 의도적 divergence)
- @gradle/libs.versions.toml — billing 8.3.0 / targetSdk 36 / unityAds 4.13.1
- @docs/checkpoints/MARKET-READINESS-20260731.md — 시장 준비 계획(타임라인 + 점검 명령)
- @.claude/memory/bugs-fixed.md — 47~52번이 이번 세션 기록

## 대화 요약

### 이번 세션에서 결정한 것

- **코스피 신호 분해 카드를 홈 상단(배너 바로 아래)에 배치** — 이유: iOS는 인사이트 티저 아래지만, 사용자가 "산출 근거를 상단에 노출해 지수 신뢰도를 먼저 보여달라"고 지시. **의도적 iOS divergence**이므로 parity 점검 시 인지 필요.
- **Billing은 9.1.0이 아니라 8.3.0 채택** — 이유: 9.1.0이 Kotlin 메타데이터 2.3.0을 요구해 현재 Kotlin 2.1.0에서 컴파일 실패. 9.x로 가려면 Kotlin/Compose 컴파일러/KSP 동반 업그레이드 필요. 정책 요건(8.0.0+)은 8.3.0으로 충족.
- **v1.5.0 심사 중에 v1.5.1을 덮어쓰기 업로드** — 이유: Billing 8/31 기한이 하드 데드라인이고, 1.5.1이 1.5.0 내용을 전부 포함하므로 합치는 게 이득. 결과적으로 둘 다 당일 게시됨.
- **가격 표시와 결제에 같은 오퍼를 쓰도록 `RemoveAdsOffering` 한 객체로 묶음** — 이유: v8에서 오퍼가 리스트로 오는데 가격은 A 오퍼, 결제는 B 오퍼로 어긋나면 사용자가 다른 금액을 결제하게 됨.
- **밀려 있던 태그를 소급 정리** — v1.4.1 이후 게시된 v1.4.2/v1.5.0이 태그도 release 머지도 안 된 상태였음. 순차 `--no-ff` 머지로 그래프 유지하며 채움.

### 시도했다 접은 것

- **Billing 9.1.0** — Kotlin 메타데이터 비호환으로 컴파일 실패. 8.3.0으로 회귀.
- **에뮬레이터 ANR을 코드 문제로 추적** — ANR Reason이 `Input dispatching timed out`이고 **load average 47→95**, 같은 시각 `com.google.android.gms.persistent`가 ANR로 강제 종료됨. `-wipe-data` 콜드부팅 직후 폭주로 확정. 부하 진정 후 재실행 시 정상. 51번
- **`/data/anr/` 트레이스 직접 읽기** — Play 이미지는 root 불가. `logcat -b events|system`이 유일한 근거.

### 명시된 사용자 선호

- 릴리즈노트는 "알아서 깔끔하게" — 가운뎃점·굵은글씨·괄호 부연 없이 짧게.
- 배포는 승인 대기 없이 끝까지 완주("배포해", "지금 바로 시작해", "게시까지 깔끔하게").
- 결제 프로필 신원 확인은 본인이 직접 처리하니 신경 쓰지 말 것.
- 브라우저 작업은 Chrome MCP 사용 OK.

### 다음 세션이 알아야 할 맥락

- **정책 경고 2건이 아직 떠 있어도 놀라지 말 것** — 갱신 지연. 번들 탐색기가 이미 타겟SDK 36을 인식하고 있고, targetSdk 36은 1.5.0으로 몇 시간 전 게시됐는데도 경고가 남아 있었음.
- **Play Console 브라우저 주의** — Chrome 창에 따라 기본 계정이 다름. `veoaudwls@gmail.com`이 뜨면 **약관 동의 클릭 금지**, URL `/u/0` → `/u/1`로 바꿔 `dlaudwls1203@gmail.com`으로 접근.
- **BTC 공매도 카드 복구됨** — `cryptoOfficialIndicatorsV1`의 short이 `available:true`로 돌아옴. KOSPI 공매도는 여전히 `available:false`(설계대로 카드 숨김).
- **에뮬 스모크는 load average를 먼저 볼 것** — 부하 40+ 상태의 결과는 신뢰하지 말 것.
- 오늘 코스피 17(극단적 공포). 신호 분해: 모멘텀 0 / 52주 강도 0 / 변동성 57 / 신용 1 / 안전자산 2 / 외국인 13 / 거래과열 12.

## Notes

- 빌드/서명: `~/.gradle/gradle.properties`의 `FEARINDEX_*` 4개 필요. 없으면 `bash ~/thingineeer-env/android/fearindex/install.sh`.
- 배포: `bundle exec fastlane production` (AAB 빌드 + 메타 + changelog + 스크린샷 업로드). **관리형 게시 OFF** — 승인 즉시 자동 게시, 수동 클릭 불필요.
- 릴리즈 서명 SHA-1 `CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16`.
- 테스트 791개 GREEN 기준. `./gradlew test`.
- 의존성 변경 후에는 **반드시 release 빌드까지** 돌릴 것 — debug는 minify가 없어 R8 실패를 못 잡는다(48번 교훈).
