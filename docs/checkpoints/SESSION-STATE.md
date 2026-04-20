# Session State — FearIndex-Android

## Date
2026-04-20 (야간 진행)

## Version
- `versionCode`: 2
- `versionName`: 1.0.1
- AAB 산출물: `app/build/outputs/bundle/release/app-release.aab` (10.6MB, 2026-04-20 11:56 빌드)
- AAB 서명: jarsigner -verify OK
- AAB Play Console **Closed Testing Alpha 트랙에 업로드 완료** ✅
- **심사 전송 상태**: Google 자동 검사 중 (최대 14분) — 검사 완료 후 "검토를 위해 앱 전송" 버튼 활성화됨

## Branch
`dev` — 미커밋 변경사항 多 (10개 QA 버그 수정 + 무결성 테스트 + Splash 정비 + CLAUDE.md + CHANGELOG + fastlane metadata 업데이트)

## Active Worktrees
본진 `/Users/imyeongjin/Desktop/side/FearIndex-Android [dev]` 1개만

## ✅ Completed (이번 세션)

### 코드/QA (10개 버그 수정)
- QA#1 알림 설정 토글 잘림 (ListItem Material 3 준수)
- QA#2 설정 메뉴 공유/평가/개인정보 버튼 (FLAG_ACTIVITY_NEW_TASK + 햅틱 피드백)
- QA#3 투표 집계 실시간 (debounce 5s→1.5s + initial fetch + "N명 참여" 카운트 45 locale)
- QA#4 차트 햅틱 (이미 구현 — 재빌드로 체감)
- QA#5 암호화폐 5Y 차트 (이미 구현)
- QA#6 차트 인사이트 카드 (이미 구현)
- QA#7 홈 "암호화폐 1년전 --" (CryptoFearIndexRepository 365일 병렬 fetch)
- QA#8 StuckCounter footer 클릭 → 바텀시트 오픈
- QA#9 알림 설정 Slider semantics 접근성
- QA#10 Splash 중복 노출 수정 (OS splash만 유지, Compose SplashView 제거)

### 무결성 테스트
- `CryptoFearIndexRepositoryImplTest` +3 케이스 (previous1Year)
- `StuckStatusDebouncerImplTest` 신규 5 케이스
- `VoteViewModelTest` 신규 5 케이스
- 전체 `:data:testDebugUnitTest :presentation:testDebugUnitTest :domain:test` **BUILD SUCCESSFUL**

### 빌드 & 배포
- versionCode 2 / versionName 1.0.1 bump
- `./gradlew clean :app:bundleRelease` 성공 (10.6MB)
- 서명 검증 jarsigner -verify OK
- Chrome MCP로 Play Console Closed Testing 트랙에 AAB 업로드 ✅
- 출시 노트 45 locale 주입 완료 (11,138자, `<bcp-47>...</bcp-47>` 형식)
- ko_KR title / short / full_description 입력 + 임시보관함 저장
- fastlane/metadata/android/*/changelogs/2.txt 45 locale 재생성 (Closed Testing용)

### 앱 콘텐츠 선언 (10개 중 7개 자동 완료)
- ✅ 금융 기능: "앱에서 금융 기능을 제공하지 않음"
- ✅ 건강 앱: "앱에 건강 기능이 없음"
- ✅ 정부 앱: "아니요"
- ✅ 개인정보처리방침 URL: `https://thingjin.notion.site/2e85657e13c7804f810fdee2a3216c33` (iOS에서 공유)
- ✅ 광고: "예, 앱에 광고가 있습니다"
- ✅ 광고 ID: "예" + 애널리틱스 + 광고/마케팅 체크
- ✅ 앱 액세스: "액세스 제한 없이 앱의 모든 기능을 사용할 수 있음"

### 이미지 에셋 준비 (`/tmp/play-assets/`)
- `icon-512.png` (iOS AppIcon-1024 → 512×512 변환)
- `feature-graphic.png` (1024×500, 그라디언트 + 아이콘 + 텍스트)
- `screenshots/1_home.png`, `2_chart.png`, `3_vote.png`, `4_notification_settings.png`

### 서버 (iOS 팀 경계 존중)
- Firestore rules `votes/{date}/results/{indexType}` 배포 (사용자 승인하에 — iOS 로컬 파일은 iOS 팀 stash 관리 정책 존중)
- iOS v1.7.10 서버 변경 (weeklyReports index) → Android 미사용 컬렉션, 영향 없음

### 문서
- `docs/CHANGELOG-v1.0.1-closed-testing.md` 작성
- `CLAUDE.md` 갱신: iOS 파일 읽기 허용, 수정 금지 규칙 명시
- `.claude/memory/deployment.md` 광고 배포 전 체크리스트 기록

## ⏳ 2026-04-20 야간 추가 진행
- Play Console `게시 개요`(publishing) 상태 확인: "변경사항이 아직 검토를 위해 전송되지 않음" / 자동 검사 진행 중 ("최대 14분 남음")
- "검토를 위해 앱 전송" 버튼 회색 (비활성) — 검사 완료 후 자동 활성화 예정
- Internal Testing 링크 `https://play.google.com/apps/internaltest/4701735377174107144` 존재 확인 (100명 제한, 14일 카운트 **안 됨**) → **품앗이에는 사용 금지**
- 품앗이용 Closed Testing 참여 링크는 심사 통과 후 활성화됨

## 🎯 Production 액세스 신청 요건 (Play Console 표시)
- 비공개 테스트 버전 게시 ✅ (AAB 업로드 완료, 심사 대기 중)
- **12명 이상의 테스터가 비공개 테스트 참여 선택** — 현재 0명
- **12명 이상의 테스터를 대상으로 14일 이상 비공개 테스트 실행** — 카운트 시작 전
- 프로덕션 신청 시 Google 설문 답변 필요
- 품앗이 전략: 심사 통과 → Closed Testing 참여 URL → 품앗이 글 작성 → 12명 opt-in → 14일 카운트다운

## 🛑 Remaining — 사용자 수동 작업

### 1. Chrome Play Console에서 이미지 드래그 업로드 (필수, 심사 차단)
- **Finder 열림**: `/tmp/play-assets/`
- 스토어 등록정보 페이지에서 각 슬롯에 드래그:
  - "앱 아이콘" (512×512) ← `icon-512.png`
  - "그래픽 이미지" (1024×500) ← `feature-graphic.png`
  - "휴대전화 스크린샷" ← `screenshots/` 4장
- 저장 후 심사 전송 가능해짐

### 2. 앱 콘텐츠 선언 3개 직접 완료 (필수, 심사 차단)
자동화 시 답변 정확도 리스크 커서 수동 처리 권장.
- **콘텐츠 등급**: Google 외부 설문 (폭력/성적/도박 수준별 답변 10-15 질문). FearIndex는 **투자 심리 앱이라 전부 "없음" 선택** 권장.
- **타겟층 연령**: 13세 이상 권장 (금융 정보는 성인 대상)
- **데이터 보안**: 수집 데이터 선언
  - FCM 토큰 (기기 식별자, 암호화 전송, 사용자 삭제 가능)
  - Firebase Analytics (앱 사용 데이터)
  - AdMob 광고 ID

### 3. 테스터 이메일 12~16명 추가 (사용자 이미 담당)
- 현재 Alpha 트랙에 `dlaudwls1203@gmail.com` 1명만
- 12명 × 14일 opt-in 필요 (Production 자격)

### 4. 최종 심사 전송
- 위 1+2 완료 후 `/tracks/4699045907541260404/releases/1/review` 페이지에서 **"저장"** → **"검토를 위해 Google에 전송"**
- 신규 앱 첫 심사는 수 시간~며칠 소요

### 5. 심사 통과 후
- 테스터 초대 링크 공유
- 12명 전원 opt-in 확인
- 14일 카운트다운 시작 → Production 신청 자격 획득

## Key Files

### 이번 세션 변경
- `app/build.gradle.kts` (versionCode 2, versionName 1.0.1)
- `app/src/main/java/th1ngjin/fearindex/MainActivity.kt` (installSplashScreen만)
- `app/src/main/AndroidManifest.xml` (theme Theme.FearIndex.Splash 복원)
- `app/src/main/res/values/themes.xml` (windowSplashScreenAnimatedIcon → 투명 vector)
- `app/src/main/res/drawable/splash_icon_invisible.xml` (신규)
- `app/src/main/java/th1ngjin/fearindex/FearIndexApp.kt` (AdMob 비동기 init)
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/splash/SplashView.kt` (완전 제거 / OS splash만)
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/settings/SettingsScreen.kt` (FLAG_ACTIVITY_NEW_TASK + 햅틱)
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/notification/NotificationSettingsScreen.kt` (semantics)
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/vote/VoteScreen.kt` + `VoteViewModel.kt` (loadInitialStuckResults + totalResponded)
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/home/HomeScreen.kt` (previous1Year null 하드코딩 제거)
- `presentation/src/main/java/th1ngjin/fearindex/presentation/component/StuckCounterCard.kt` (totalResponded + onFooterClick)
- `presentation/src/main/res/values{-ko,-ja,...}/strings.xml` (45 locale stuck_total_responded)
- `data/src/main/java/th1ngjin/fearindex/data/service/StuckStatusDebouncer.kt` (1.5초)
- `data/src/main/java/th1ngjin/fearindex/data/repository/CryptoFearIndexRepositoryImpl.kt` (365일 병렬 fetch)
- `data/src/test/.../service/StuckStatusDebouncerImplTest.kt` (신규)
- `data/src/test/.../repository/CryptoFearIndexRepositoryImplTest.kt` (+3 케이스)
- `presentation/src/test/.../feature/vote/VoteViewModelTest.kt` (신규)
- `CLAUDE.md` (iOS 수정 금지, 읽기 허용)
- `docs/CHANGELOG-v1.0.1-closed-testing.md` (신규)
- `fastlane/metadata/android/*/changelogs/2.txt` (45 locale 재생성)
- `fastlane/metadata/android/ko_KR/full_description.txt` (위젯 제거 + 5Y 차트 반영)

### Play Console 상수
- 앱 ID: `4973920645070208584`
- Alpha 트랙 ID: `4699045907541260404`
- URL: `https://play.google.com/console/u/1/developers/5351376807423705889/app/4973920645070208584/tracks/4699045907541260404`
- 업로드된 AAB: v1.0.1 versionCode 2, 10.6MB
- 임시 앱 이름: `th1ngjin.fearindex (unreviewed)` (심사 전까지)

## Notes

### Chrome MCP 제약
- `<input type="file">` macOS 네이티브 다이얼로그 제어 불가
- 파일 드래그앤드롭 OS 레벨, 가상 파일 주입 불가
- → 이미지 업로드는 **사용자 수동** 또는 fastlane supply (service account JSON 발급 필요)

### fastlane supply 미설정
- `fastlane/Appfile` 의 `json_key_file`이 `~/fearindex-secrets/play-store-service-account.json` 기대하나 파일 없음
- 발급하면 이후 45 locale × 180 스크린샷 + 아이콘 + 그래픽 이미지 전부 자동화 가능
