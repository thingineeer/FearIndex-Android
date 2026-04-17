# Session State — FearIndex-Android

## Date
2026-04-17 (16:40 KST)

## Version
- `versionCode`: 2
- `versionName`: 1.0.1
- AAB 산출물: `app/build/outputs/bundle/release/app-release.aab` (10.1MB) — 2026-04-17 빌드 (keystore `~/fearindex-secrets/fearindex-release.keystore`, alias `upload`)

## Branch
`dev` — origin/dev보다 **16 커밋 앞선 상태** (아직 push 안 함)

## Active Worktrees
- 본진 `/Users/imyeongjin/Desktop/side/FearIndex-Android [dev]`
- `FearIndex-Android-share-and-gauge [feature/v1.0.0-share-and-gauge]` — **미머지, 미검토**

## Completed (2026-04-17 세션)

### 1. 이전 세션 잔재 정리
- `FearIndex-Android-i18n` worktree의 미커밋 변경 (splash/icons/StuckDetailSheet/fastlane 등)을 6개 논리 커밋으로 쪼개고 `feature/v1.0.0-i18n-insights` → `dev` 머지 후 worktree 제거

### 2. S&P 하드코딩 + Settings 메뉴 i18n 버그 수정 (`feature/v1.0.1-similar-events-asset-label`)
- `insight_similar_events_one_year_return` 키: "1년 후 S&P %1$s" 형태 → iOS 패턴 "1년 후 %1$s" (자산명 placeholder)
- `SimilarEventsCard` 에 `indexType` 파라미터 추가, CRYPTO면 "BTC", 아니면 "S&P" 런타임 주입
- 설정 메뉴 키 (`settings_menu_notification/rate/share/about/privacy` + `settings_about_version`) — ko 외 **43 locale 전부 누락**이었던 것을 iOS Localizable.strings 에서 일괄 동기화

### 3. Supply fastlane 규격 폴더명 통일 (`feature/v1.0.1-screenshots-and-i18n`)
- `values-XX` (Android resource 규격) ≠ Supply locale 규격 (`ko_KR`, `en_US`, `pt_BR`, `zh_CN`)
- `en-US` → `en_US`, `pt-rBR` → `pt_BR`, `zh-rCN` → `zh_CN`, `iw` → `iw_IL`, `nb` → `no_NO`, `in` → `id` 등 rename
- 중복 `ko/` + `ko-KR/` → `ko_KR/` 로 통합

### 4. Android 스크린샷 자동 순회 (45 locale × 4장 = 180장)
- `scripts/screenshots/capture-all-locales.sh` — adb `cmd locale set-app-locales` + `input tap` 시퀀스
- ANR dialog 방지: `settings put global hide_error_dialogs 1` 설정
- 각 locale: 홈 / 차트 / 투표 / 알림설정 — 1080×2400 PNG
- 저장: `fastlane/metadata/android/<supply_locale>/images/phoneScreenshots/{1_home,2_chart,3_vote,4_notification_settings}.png`
- Fastfile `screenshots` lane 추가

### 5. `comparison_*` + `comparison_card_title` 키 번역 (44 locale)
- `comparison_card_title`, `comparison_previous_close`, `_1w_ago`, `_1m_ago`, `_1y_ago` 키 — 이전엔 ko만 있고 43 locale 누락이던 것을 locale별 적절한 번역으로 추가

### 6. Play Store 텍스트 메타 45 locale (`feature/v1.0.1-store-listing-metadata`)
- 4개 병렬 에이전트로 작성 (Agent 1~4)
- title.txt (≤30자) / short_description.txt (≤80자) / full_description.txt (≤4000자)
- iOS `fastlane/metadata/<iOS_locale>/{name,subtitle,description}.txt` 참고 (33 locale) + 영어 기반 번역 (11 locale)
- Apple Watch / macOS / iPadOS 섹션 제거, "12개 언어" → "45개 언어"
- 중복 nl/ → nl_NL/ 통합 (placeholder 제거)
- 45 × 3 = 135 파일, 규격 전부 준수

### 7. 출시 노트 45 locale (`feature/v1.0.1-release-notes`)
- `fastlane/metadata/android/<locale>/changelogs/2.txt` (versionCode 2)
- 각 locale별 간결한 v1.0.1 release note (500자 제한 내)

### 8. Fastfile lane 추가
- `upload_screenshots` — 스크린샷만 Play Console 업로드
- `upload_metadata` — 스크린샷 + 메타 + changelog 업로드 (AAB 제외)

### 9. Android FCM 푸시 테스트
- FCM 토큰 캡처 (debug 빌드에 임시 로깅 후 revert)
- Firebase Admin SDK 로 4건 발송 (시장/암호화폐 × lower/upper), ja 로케일 번역 재발송 검증
- 에뮬레이터 알림 수신 확인 (`fear_index_alerts` 채널, importance=4)

### 10. Chrome MCP로 Play Console 진입 시도
- `dlaudwls1203@gmail.com` 계정 로그인 + 내부 테스트 트랙 진입 성공
- AAB 업로드 시도 → **업로드 키 재설정 대기** 에러 (`2026-04-19 04:33 UTC` 이후 유효)

## In Progress
없음 — 대기 중 (업로드 키 재설정 승인 대기)

## Remaining

### Play Console 업로드 키 재설정 대기
- [ ] **2026-04-19 04:33 UTC (한국 4/19 13:33)** 이후 AAB 재업로드 시도
- [ ] 업로드 파일: `app/build/outputs/bundle/release/app-release.aab` (재빌드 불필요, 이미 올바른 keystore로 서명됨)

### Play Console 후속 작업 (AAB 업로드 성공 후)
- [ ] 출시명 `2 (1.0.1)` 입력
- [ ] 출시 노트 — changelogs/2.txt 45 locale 사용
- [ ] 스토어 등록정보 — 45 locale 스크린샷 + title/short/full 업로드
- [ ] 필수 선언: 콘텐츠 등급 설문, 데이터 보안, 앱 액세스, 광고, 타겟층, 개인정보처리방침 URL
- [ ] 테스터 이메일 목록 "Internal Testers" 생성 + 등록
- [ ] "내부 테스트로 출시" → 테스터 초대 링크 공유

### Fastlane 자동화 (선택)
- [ ] Play Console → 설정 → API 액세스 → 서비스 계정 JSON 발급
- [ ] `~/fearindex-secrets/play-store-service-account.json` 저장
- [ ] `bundle exec fastlane upload_metadata` → 메타 + 스크린샷 + changelog 자동 업로드

### Firebase Console
- [ ] App Check debug token 등록 (이전 세션의 `f7fe2893-...` 또는 최신)
- [ ] 새 keystore SHA-1 등록 (기등록 상태 확인 필요)

### Git
- [ ] `git push origin dev` (dev가 origin/dev보다 16 커밋 앞선 상태)

### 별도 worktree 정리
- [ ] `FearIndex-Android-share-and-gauge [feature/v1.0.0-share-and-gauge]` 상태 점검 후 머지 or 삭제

## Key Files

### 이번 세션 핵심
- `fastlane/metadata/android/<locale>/title.txt|short_description.txt|full_description.txt` — 45 × 3 = 135 파일
- `fastlane/metadata/android/<locale>/changelogs/2.txt` — 45 locale 출시 노트
- `fastlane/metadata/android/<locale>/images/phoneScreenshots/{1_home,2_chart,3_vote,4_notification_settings}.png` — 45 × 4 = 180장
- `scripts/screenshots/capture-all-locales.sh` — 45 locale adb 순회 촬영
- `fastlane/Fastfile` — screenshots / upload_screenshots / upload_metadata / internal / promote_to_closed / promote_to_production lane
- `presentation/src/main/java/th1ngjin/fearindex/presentation/component/SimilarEventsCard.kt` — indexType 파라미터, asset label 분기
- `presentation/src/main/java/th1ngjin/fearindex/presentation/feature/home/HomeScreen.kt` — SimilarEventsCard 호출부
- `presentation/src/main/res/values-*/strings.xml` — comparison_*, settings_menu_*, insight_similar_events_one_year_return 동기화

### AAB 산출물
- `app/build/outputs/bundle/release/app-release.aab` (10.1MB)

## Notes

### Git 그래프 (2026-04-17 오늘 추가)
```
* 89c6572 merge: v1.0.1 release notes (45 locale) → dev
* 199738d feat(metadata): 45 locale changelogs/2.txt (v1.0.1 release notes)
* d664c4e merge: 45 locale Play Store 텍스트 메타 → dev
* 8bc6aa4 i18n: 45 locale Google Play 텍스트 메타 일괄 작성 (iOS 매핑 + 영어 기반 번역)
* b66161c merge: screenshots + i18n comparison + supply rename → dev
* fc364d1 chore(session): 2026-04-17 스크린샷 + i18n 세션 기록
* 802d2d2 i18n: comparison_* 키 43 locale 번역 추가
* 71fe25e fix(fastlane): Supply 공식 locale 폴더명 통일 + 45 locale 스크린샷 자동화
* bdd91ba merge: asset-label + settings menu i18n → dev
* 323dc9d fix(i18n): similar-events 자산 레이블 분기 + settings 메뉴 키 43 locale 추가
* 76186f9 merge: i18n + icons + splash + stuck-detail → dev
* 5941d2e chore: fastlane + build 설정 + misc
* af2be83 feat(presentation): 컴포넌트 연결 + i18n 키 연결
* b0592cf i18n: 44 locale strings 신규 키 일괄 반영
* 4543630 feat(ui): StuckDetailSheet + InsightText + PrivacyScreen 신규
* 3809f36 feat(splash): iOS 스타일 Compose SplashView 추가
* 7402496 feat(icons): iOS AppIcon 기반 런처/스플래시 아이콘 교체
* 376fa5f chore(session): 2026-04-17 세션 저장 (이전 세션)
```

### Play Console 업로드 키 재설정 상태
- Google에 재설정 요청 접수됨 (이전 세션)
- **유효 시작 시각**: `2026-04-19 04:33:09 UTC` = 한국 `2026-04-19 (금) 13:33`
- 현재는 업로드 시 "최근에 재설정되어 아직 유효하지 않은 업로드 인증서" 에러 발생
- 4/19 이후에는 현재 빌드된 AAB 그대로 업로드 통과 예정

### Keystore 정보 (변경 없음)
- 위치: `~/fearindex-secrets/fearindex-release.keystore` (PKCS12)
- Alias: `upload`
- 비밀번호 (store/key 동일): `~/.gradle/gradle.properties` 의 `FEARINDEX_STORE_PASSWORD`
- Upload PEM: `~/fearindex-secrets/upload_certificate.pem`

### Chrome MCP 제약
- 파일 input (`<input type="file">`) 은 macOS 네이티브 다이얼로그 통제 불가 → AAB 업로드는 사용자 수동
- Play Console 세션은 Chrome 브라우저 그대로 유지 (로그인 상태 유지)

### Supply Locale 정규화 (주의)
- Android `values-XX` (리소스) ≠ Supply `XX_YY` (Play Console 메타)
- `values-pt-rBR` (리소스) ↔ `fastlane/metadata/android/pt_BR` (메타)
- `values-zh-rCN` ↔ `zh_CN`
- `values-nb` ↔ `no_NO`
- `values-iw` ↔ `iw_IL`
- `values-in` ↔ `id`

## 다음 세션 시작 시
1. `/resume-FearIndex-Android` 실행
2. 4/19 13:33 KST 지났는지 확인
3. 지났으면: Chrome 열고 Play Console → 내부 테스트 → 버전 수정 → AAB 업로드 재시도
4. 안 지났으면: 대기하거나 service account JSON 발급으로 시간 활용

## 이번 세션 주요 교훈
- `ANR: Application Not Responding` dialog는 `hide_error_dialogs=1` 설정으로 시스템 차단 가능 (연속 locale 전환 시 필수)
- `values-XX` 와 Supply locale은 완전히 다른 체계 — 처음부터 Supply 규격 지켜야 fastlane과 호환
- iOS `Localizable.strings` 는 Android `strings.xml` 의 상위 SSOT — 키 동기화는 항상 iOS → Android 방향
- Chrome MCP로 Play Console 작업 시 `u/0`/`u/1` 등 계정 index 주의 — 본인 개인 Gmail (`dlaudwls1203`) 사용 확인 필수
