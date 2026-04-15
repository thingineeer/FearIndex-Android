# Session State — FearIndex-Android

## Date
2026-04-15

## Version
- `versionCode`: 2
- `versionName`: 1.0.1
- AAB 산출물: `~/Downloads/FearIndex-v1.0.1.aab` (9.7MB)

## Branch
`dev` (origin/dev 기준 9 commits ahead, push 대기)

## Active Worktrees

| 경로 | 브랜치 | 용도 |
|---|---|---|
| `/Users/imyeongjin/Desktop/side/FearIndex-Android` | `dev` | 본진 |
| `/Users/imyeongjin/Desktop/side/FearIndex-Android-share-and-gauge` | `feature/v1.0.0-share-and-gauge` | 레거시 (v1.0.0 share 정리 보류) |

## Completed

### v1.0.0 베이스라인 (이전 세션)
- [x] 5개 모듈 Clean Architecture + Kotlin 2.x + Compose + Hilt + Retrofit + Firebase + AdMob
- [x] Package 통일 `th1ngjin.fearindex` (+`.debug`)
- [x] 45개 locale strings.xml + rating 5단계 + share 다국어
- [x] Firebase 공유 프로젝트 `fear-index-a4f4b` (iOS와 동일)
- [x] Analytics iOS 1:1 이벤트 + platform UserProperty
- [x] Functions 공유 (`submitStuckStatus`, `getStuckCount`, asia-northeast3)
- [x] AdMob 실제 App ID + HomeBanner
- [x] Release keystore + AAB 빌드 + SHA fingerprint
- [x] Play Console 신규 앱 생성 + 내부 테스트 트랙 1.0.0 업로드
- [x] 문서/메모리 체계 + PreToolUse hook (구 package 차단)
- [x] Git `dev` 브랜치 + worktree 패턴 확립

### v1.0.1 — iOS parity 풀 포팅 (이번 세션 집중)
- [x] **W1 홈 화면 레이아웃** iOS 순서: Gauge → Comparison → AdBanner → InsightTeaser → StuckCounter
- [x] **W2 차트 인사이트 피드** — 차트 탭 하단에 InsightFeedView 전체 표시
- [x] **W3 투표 탭 정리** — VoteCard 제거, StuckCounter만 유지 (iOS v1.7.7 대칭)
- [x] **W4 설정 AdBanner** — 설정 화면 하단에 배너
- [x] **W5 폰트 iOS 정밀 대칭** — largeTitle 34sp / title 28sp / headline 17sp / subheadline 15sp / caption 12sp
- [x] **W7 게이지 크기 최적화** — 300dp Box, 텍스트 offset 230dp (needle 겹침 해결)
- [x] **인사이트 6종 전체 구현** (buySignal/historicalReturn/returnChart/drawdownTolerance/nudge/fearVelocity)
- [x] **DefaultReturnData 1:1 포팅** (iOS DefaultReturnData.swift) — anchor 6개 → 101점 선형 보간
- [x] **ReturnDataInterpolator + FearVelocityCalculator** 포팅
- [x] **InsightDetailSheet** — 6종 타입별 Content + HistoricalContextCard + DrawdownContextCard + InflectionBanner
- [x] **S&P 500 / Bitcoin 기준 명시** — 모든 인사이트 카드 및 DetailSheet 섹션 제목에 종목 기준 표시
- [x] **Firebase 하드닝** (AppCheck, Crashlytics, RemoteConfig)
- [x] **MarketIndexTicker 실제 데이터** (Yahoo Finance v8 Spark API)
- [x] **암호화폐 차트 5Y 기간 추가**
- [x] **Unit 테스트 21개 클래스 / 123 test cases** 모두 green

### 이번 세션 신규 작업
- [x] **46점 중립 구간 인사이트** (`feature/v1.0.1-neutral-insights` → merged)
  - `buildHistoricalReturn`을 iOS parity로 **항상 생성** (점수 조건 제거)
  - nullable 제거 (`MarketInsight?` → `MarketInsight`), matched 비어도 카드 렌더
  - summary에 현재 점수 포함: "S&P 500 기준 — 현재 46점과 비슷했던 과거 3개 시점"
  - 홈 티저 `InsightTeaserCard` 점수 조건(`≤25 || ≥75`) 제거 → 전 구간 노출
  - `buildBuySignal`(≤25/≥75), `buildReturnChart`(≤30/≥70)는 iOS parity 유지
- [x] **성능 개선** (`feature/v1.0.1-performance` → merged)
  - `InsightViewModel.observeHome` — `InsightInputSnapshot + distinctUntilChanged` 적용
    → 티커 3초 갱신/로딩 플래그 변경 시 재계산 스킵 (이전: 매 HomeUiState 변경마다 101점 보간 + matchingEvents)
  - `HomeScreen.formatPrice` — `DecimalFormat` lazy val (이전: 3초마다 새 인스턴스)
  - `HomeScreen.timestampFormatter` — `DateTimeFormatter` lazy val
  - `FearGaugeView.arcSegments` — 파일 레벨 private val (이전: Canvas draw 블록 안 매 frame data class + listOf 재생성)

## In Progress
없음 — 이번 세션 목표 완료.

## Remaining

### v1.0.1 체크 (테스터 피드백 대기)
- [ ] Play Console 테스터 초대 진행 (Closed Testing 12명 + 14일 opt-in 정책)
- [ ] v1.0.1 AAB `~/Downloads/FearIndex-v1.0.1.aab` → Play Console 업로드 (사용자 수동)
- [ ] origin/dev push 결정 (현재 9 commits ahead)

### v1.0.2+ 후보
- [ ] SkeletonView 재검토 (현재 FearIndexSkeletonView 사용 중 확인 필요)
- [ ] 시장/암호화폐 알림 설정 분리 (통합 1개 → 2개)
- [ ] iOS hosting URL (`https://fear-index-a4f4b.web.app/...`) 공유 링크 반영
- [ ] InsightGenerator 단위 테스트 (46점/0점/100점 경계 검증)
- [ ] iOS v1.7.8 "현재 X점과 비슷했던 과거 시점" 문구 완전 동기화 확인
- [ ] 차트 Analytics 이벤트 세부화 (드래그/툴팁 상호작용)

### 사용자 결정 대기
- "투표" 탭 이름 → "심리" 변경 여부
- AdMob 테스트 디바이스 등록
- 기존 오타 앱 `com.thingineeer.fearindex` (e3) Play Console 삭제 결정
- App Check debug token 등록 여부

## Key Files

### 프로젝트 루트
- `CLAUDE.md` — 프로젝트 규칙 (Git Workflow, Package, Firebase, AdMob)
- `.claude/memory/MEMORY.md` — 인덱스 + 상수표
- `.claude/memory/bugs-fixed.md` — 버그 이력 (9개+)
- `.claude/memory/ios-parity.md` — iOS 대칭성 체크리스트
- `.claude/memory/deployment.md` — Keystore/AAB/Play Console 절차
- `.claude/memory/firebase-setup.md` — Functions/Firestore/App Check
- `.claude/rules/git-workflow.md` — worktree/`--no-ff` merge 규칙
- `.claude/rules/package-convention.md` — `th1ngjin.fearindex` 엄수

### 핵심 코드 (iOS parity 영향 큰 순서)
- `core/src/main/java/th1ngjin/fearindex/core/util/InsightGenerator.kt` — 인사이트 6종 생성 엔진 (iOS FearIndexInteractor.buildMarketInsights 포팅)
- `core/src/main/java/th1ngjin/fearindex/core/util/ReturnDataInterpolator.kt` — 선형 보간 (iOS 1:1)
- `domain/src/main/kotlin/th1ngjin/fearindex/domain/defaults/DefaultReturnData.kt` — Market/Crypto anchor 6개 + 101점 보간 + 이벤트 12개 (iOS SSOT 포팅)
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/insight/InsightViewModel.kt` — HomeViewModel 관찰 + distinctUntilChanged
- `presentation/src/main/java/th1ngjin/fearindex/presentation/component/InsightDetailSheet.kt` — 6종 타입별 BottomSheet (872줄)
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/home/HomeScreen.kt` — 홈 레이아웃 + TickerView + formatters
- `presentation/src/main/java/th1ngjin/fearindex/presentation/component/FearGaugeView.kt` — 270° gauge + 파일 레벨 arcSegments

### 빌드/설정
- `app/build.gradle.kts` — versionCode=2 / versionName=1.0.1 / signingConfigs / manifestPlaceholders
- `app/google-services.json` — Firebase prod+debug

## Notes

### iOS SSOT 정렬
- iOS `FearIndexInteractor.swift:725` — `historicalReturn` **항상 생성** (점수 조건 없음)
- iOS `DefaultReturnData.swift:20-52` — Market/Crypto anchor 6개(0/12/37/62/88/100 혹은 15) → `interpolate(anchors)` → 101점
- Android도 동일 anchor + 동일 선형 보간 + 동일 이벤트 12개 (market), 12개 (crypto)
- **46점도 커버됨**: matchingEvents(score=46, limit=3) → S&P 500 기준 [55 reopening-2021, 68 post-election-2024, 23 svb-2023]

### 성능 개선 효과 (예상)
- 티커 3초 fade 중 인사이트 재계산 없음 → 스크롤 끊김 감소
- 게이지 spring 애니메이션(0.6 damping) 60fps 중 Canvas 할당량 감소
- DecimalFormat/DateTimeFormatter 재생성 제거 → GC 압력 완화

### Git 상태
- 브랜치 `dev`: origin/dev 기준 9 commits ahead (push 대기)
- 최근 merge 그래프:
  - `9f664d9 merge: v1.0.1 성능 개선 → dev`
  - `8f7ac90 merge: v1.0.1 — 46점 중립 구간 인사이트 카드 → dev`
  - `5d01f60 merge: feature/v1.0.1 → dev (인사이트 라벨)`
- 모든 머지 `--no-ff` (squash 없음) — iOS와 동일 워크플로우

### Firebase 공유 프로젝트
- Project ID: `fear-index-a4f4b` (iOS/macOS/Android 공유)
- Functions 리전: `asia-northeast3`
- **iOS/macOS 앱 삭제 절대 금지**

### Release Signing
- Keystore: `~/fearindex-release.keystore`
- 비밀번호: `~/.gradle/gradle.properties`의 `FEARINDEX_STORE_PASSWORD`
- Key alias: `fearindex`
- SHA-1: `A1:54:8A:92:C3:AF:A5:0E:BD:31:F6:6B:47:1B:9E:BB:51:5D:23:51`
- SHA-256: `AD:48:68:DA:81:3C:9D:39:65:D0:C8:F9:59:62:61:6F:0A:6D:3A:BF:4E:21:DA:12:C0:DF:D8:2C:11:6A:14:0D`

### Play Console
- 개발자 계정 ID: `5351376807423705889`
- 앱 ID: `4973920645070208584`
- 내부 테스트 트랙 ID: `4701735377174107144`
- **기존 오타 앱 잔존**: `com.thingineeer.fearindex` (e3) — 사용자 삭제 결정 대기

### AdMob
- App ID: `ca-app-pub-5283496525222246~1308884877`
- HomeBanner Unit ID: `ca-app-pub-5283496525222246/3189551565`

### iOS 프로젝트 위치
- `/Users/imyeongjin/Desktop/side/FearIndex-iOS` — Bundle ID `th1ngjin.FearIndex-iOS`
- iOS/Android 같이 작업 시 `@.claude/memory/ios-parity.md` 참조

## 다음 세션 시작 시
1. `/resume-FearIndex-Android` 실행
2. 브리핑 확인 후 "Ready to continue?" 응답
3. 우선순위:
   1. `git push origin dev` 여부 결정
   2. v1.0.1 AAB Play Console 업로드 (사용자 수동)
   3. 테스터 초대 진행 (Closed Testing 정책 확인)
