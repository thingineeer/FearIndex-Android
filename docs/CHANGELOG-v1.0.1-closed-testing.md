# v1.0.1 Closed Testing 제출 전 변경사항

**기준**: Internal Testing 업로드 v1(versionCode=1, versionName=1.0.0, 2026-04-15 빌드)
**대상**: Closed Testing 업로드 v2(versionCode=2, versionName=1.0.1, 2026-04-20 빌드)
**작성일**: 2026-04-20 (KST)

내부 테스트로 본인이 사용해보며 발견한 QA 이슈 10건을 전부 해결한 버전입니다. 12명 테스터에게 배포하기 전 UX/안정성 개선 완료.

---

## 🐛 버그 수정 (10건)

### QA#1 — 알림 설정 토글 잘림
- **증상**: 알림 설정 화면의 "푸시알림" 우측 Switch 토글이 화면 끝에 잘려 보였다는 제보
- **원인**: 구버전(v1.0.0) ListItem 레이아웃 이슈
- **해결**: 현재 코드는 이미 Material 3 ListItem + Switch 표준 패턴 사용 중. 재빌드로 자동 해소
- **영향 파일**: `NotificationSettingsScreen.kt`

### QA#2 — 설정 탭 버튼 무반응 (공유 / 평가 / 개인정보)
- **증상**: 설정 메뉴 3개 버튼 눌러도 아무 일도 일어나지 않음
- **원인**:
  - `shareApp` Intent에 `FLAG_ACTIVITY_NEW_TASK` 누락 → Application context로 실행 시 crash 가능성
  - 테스터가 "눌렸다"는 시각 피드백 없음
- **해결**:
  - `Intent.createChooser` 에 `addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)` 추가
  - `SettingsItem` clickable에 `HapticFeedbackType.LongPress` 햅틱 추가
  - 개인정보처리방침 / 평가하기는 이미 Navigation/Play Store intent 정상 연결
- **영향 파일**: `SettingsScreen.kt`

### QA#3 — 투표 탭 "물렸어요/안물렸어요" 집계 미반영
- **증상**: 토글 눌러도 5초간 반응 없고 카운트 미변화
- **원인 1**: `StuckStatusDebouncer` 디바운스 시간 **5초 → 1.5초** 단축
- **원인 2**: 진입 즉시 stuckCounter 초기 fetch가 없어 `StuckCounterResult.EMPTY` 만 보였음 → `loadInitialStuckResults()` 메서드 추가
- **원인 3**: 화면에 "총 N명 참여" 카운트 미표시 → `StuckCounterCard` 에 `totalResponded` 파라미터 + 45개 locale 번역 키 `stuck_total_responded` 추가
- **영향 파일**:
  - `data/service/StuckStatusDebouncer.kt` (1.5초)
  - `presentation/feature/vote/VoteViewModel.kt` (초기 fetch)
  - `presentation/component/StuckCounterCard.kt` (카운트 표시)
  - `presentation/res/values*/strings.xml` (45 locale 번역)

### QA#4 — 차트 햅틱 이벤트
- **상태**: 현재 코드에 이미 `HapticFeedbackType.TextHandleMove` 구현됨 (iOS `TextHandleMove` 대칭)
- **조치**: 재빌드로 체감 가능

### QA#5 — 암호화폐 차트 5Y 확장
- **상태**: 현재 코드에 `CryptoChartPeriod` 3M/6M/1Y/2Y/3Y/5Y 이미 존재 (iOS `ChartPeriod` 와 동일)
- **조치**: 재빌드로 체감 가능

### QA#6 — 차트 인사이트 카드 포팅
- **상태**: 현재 코드에 `InsightFeedView` + `InsightTeaserCard` + `InsightDetailSheet` 이미 포팅됨
- **조치**: 재빌드로 체감 가능

### QA#7 — 홈 "암호화폐 1년전 --"
- **증상**: 홈 비교 카드에서 암호화폐 탭의 "1년 전" 값이 항상 "--"로만 표시
- **원인**: `CryptoFearIndexRepositoryImpl.fetchCurrent` 가 31일치만 fetch하여 365일 전 score 계산 불가 + `HomeScreen` 에서 `if (indexType == CRYPTO) null` 하드코딩
- **해결**:
  - iOS `CryptoFearIndexRepository.fetchPrevious1Year()` 패턴 포팅 — 31일치 + 365일치 **병렬 async fetch** (`coroutineScope { ... }`)
  - 365일 응답 마지막 원소를 `previous1Year` 로 사용 (Alternative.me API 검증됨)
  - 실패 시 null 반환 (전체 흐름 안전)
  - `HomeScreen.kt` 의 `null` 하드코딩 제거
- **영향 파일**:
  - `data/repository/CryptoFearIndexRepositoryImpl.kt`
  - `presentation/feature/home/HomeScreen.kt`

### QA#8 — 물림 카운터 footer UX 개선
- **증상**: "지금 몇 명이 물려있는지 실시간으로 볼 수 있어요 >" 영역 클릭해도 반응 없음
- **해결**: `FooterGuide` 에 `onClick = onInfoClick` 콜백 추가 → `StuckDetailSheet` 바텀시트 오픈. `semantics { role = Button }` 접근성 태그 추가
- **영향 파일**: `presentation/component/StuckCounterCard.kt`

### QA#9 — 알림 설정 슬라이더 Material 3 검증
- **상태**: `androidx.compose.material3.Slider` 표준 API 준수 확인 완료
- **추가 개선**: `modifier.semantics { contentDescription = "$label ${value.toInt()}" }` 추가 → TalkBack 스크린리더가 현재 값 정확히 읽음
- **영향 파일**: `presentation/feature/notification/NotificationSettingsScreen.kt`

### QA#10 — Splash 중복 노출 수정
- **증상**: OS SplashScreen(아이콘만) + Compose SplashView(아이콘+텍스트+disclaimer) 2번 연속 노출
- **원인**: Android 12+ 강제 OS splash와 커스텀 Compose splash 중복 표시
- **해결**:
  - `Theme.FearIndex.Splash` 의 `windowSplashScreenAnimatedIcon` 을 **투명 1x1 vector** (`splash_icon_invisible.xml`) 로 교체 → OS splash 깜빡만 뜨고 아이콘 안 보임
  - `Compose SplashView` 를 **Google Play Store 런처 아이콘(`R.mipmap.ic_launcher`) + 타이틀 + 서브타이틀 + info + disclaimer** 레이아웃으로 iOS 대칭 복원 (1.5초 노출)
  - `MainActivity` 에 `installSplashScreen()` 유지하되 fade 전환으로 seamless 연결
  - `FearIndexApp` 의 `MobileAds.initialize()` 를 `appScope.launch` 로 비동기화하여 cold start 단축
- **영향 파일**:
  - `app/AndroidManifest.xml`
  - `app/res/values/themes.xml`
  - `app/res/drawable/splash_icon_invisible.xml` (신규)
  - `app/MainActivity.kt`
  - `app/FearIndexApp.kt`
  - `presentation/feature/splash/SplashView.kt`
  - `presentation/res/mipmap-*/ic_launcher.png` (app → presentation 복사)

---

## 🧪 무결성 테스트 추가

내부 테스트에서 발견된 회귀 방지를 위해 핵심 QA 수정 영역에 unit 테스트 작성.

### data 모듈
- `CryptoFearIndexRepositoryImplTest` (+3 케이스)
  - previous1Year 365일 fetch 성공
  - 365일 fetch 실패 시 null + 앱 전체 흐름 유지
  - 기존 테스트에 `days=365` mock 보강
- `StuckStatusDebouncerImplTest` (신규, 5 케이스)
  - 기본 `debounceMillis=1500L` (QA#3 응답성 기준선)
  - 1.5초 디바운스 타이밍
  - 연타 시 마지막 상태만 반영
  - MARKET/CRYPTO 독립 디바운스
  - flush 실패 시 재시도 큐 저장

### presentation 모듈
- `VoteViewModelTest` (신규, 5 케이스)
  - init 시 MARKET/CRYPTO 초기 fetchOnce 2회 호출
  - StateFlow 초기 업데이트 검증
  - fetchOnce 실패해도 ViewModel 정상 생성 (EMPTY 유지)
  - toggle 시 낙관적 업데이트 + debouncer.schedule 호출

**실행 결과**: `:data:testDebugUnitTest :presentation:testDebugUnitTest :domain:test` 전부 **BUILD SUCCESSFUL**.

---

## 🔧 서버 측 변경 (배포 완료)

### Firestore Security Rules — `votes/{date}/results/{indexType}` 읽기 허용
- **배경**: 투표 탭의 실시간 스냅샷 리스너가 `PERMISSION_DENIED` 로 실패 중이었음 (iOS도 같은 문제)
- **해결**: `match /votes/{date}/results/{indexType} { allow read: if true; allow write: if false; }` 추가 + 배포 완료 (2026-04-20 10:04 KST)
- **참고**: 로컬 `firestore.rules` 파일은 iOS 팀 stash 관리 정책에 따라 복원 상태. 서버엔 규칙 활성.

### Firestore Indexes
- iOS 팀이 배포한 `weeklyReports` `__name__` DESC 단일 필드 인덱스. Android 미사용 컬렉션이므로 영향 없음.

---

## 📦 버전 정보

| 항목 | 이전 (Internal) | 현재 (Closed Testing 제출) |
|---|---|---|
| versionCode | 1 | **2** |
| versionName | 1.0.0 | **1.0.1** |
| AAB 경로 | `app/build/outputs/bundle/release/app-release.aab` | 동일 |
| 서명 | upload keystore (`~/fearindex-secrets/fearindex-release.keystore`) | 동일 |
| Firebase 프로젝트 | `fear-index-a4f4b` | 동일 |
| Target SDK | 35 | 35 |
| Min SDK | 29 | 29 |

---

## 🚀 Closed Testing 배포 체크리스트

- [x] 버그 수정 10건 완료
- [x] Unit 테스트 작성 + 통과
- [x] versionCode bump + AAB 재빌드
- [ ] Play Console **Closed Testing** 트랙에 AAB 업로드 (Internal 아님 — 14일 타이머 이슈)
- [ ] 테스터 이메일 목록 12~16명 등록
- [ ] 심사 전송 (신규 트랙이므로 Google 심사 수 시간~며칠)
- [ ] 심사 통과 후 테스터 초대 링크 공유
- [ ] 12명 전원 opt-in 후 14일 연속 유지
- [ ] 14일 만료 후 Production 신청 버튼 활성화

---

## ⚠️ 알려진 이슈 / 다음 버전 이후 (v1.1+)

- App Check debug token 등록 필요 (Firebase Console) — debug 빌드에서 Callable Functions 실패 방지
- Production 배포 전 광고 단위 ID 재확인 필수 (`ca-app-pub-5283496525222246/3189551565`)
- Play Console 앱 콘텐츠 선언 10개 미완 (개인정보처리방침, 데이터 보안 등)
- 스토어 등록정보 45 locale fastlane supply 업로드 필요
- 차트 탭 인터스티셜 광고는 **절대 금지** (이전 bugs-fixed #1 참고)

---

## 📎 관련 문서

- `@.claude/memory/bugs-fixed.md` — 세션별 버그 이력 (QA#1~#10 추가됨)
- `@.claude/memory/deployment.md` — 배포 절차 + 광고 정책
- `@docs/GOOGLE-PLAY-INTERNAL-TEST.md` — Play Console 수동 업로드 8단계
- `@docs/checkpoints/SESSION-STATE.md` — 최신 세션 상태
