# Session State — FearIndex-Android

## Date
2026-04-17

## Version
- `versionCode`: 2
- `versionName`: 1.0.1
- AAB 산출물: `~/Downloads/FearIndex-v1.0.1.aab` (9.9MB) — 2026-04-16 빌드 (새 keystore, 광고 비활성화, 45 locale i18n)

## Branch
`dev` (origin/dev와 동기화됨)

## Active Worktrees
본진 `/Users/imyeongjin/Desktop/FearIndex-Android [dev]` 1개만.

## Completed (이번 세션 2026-04-16~17)

### 1. 이 노트북 개발환경 전체 셋업
- Android Studio + SDK brew 설치
- Pixel_3a_API_34 AVD 에뮬레이터 구성
- `local.properties` 생성 (SDK 경로)
- Firebase CLI로 `google-services.json` 재생성 → 심볼릭 링크 연결

### 2. `~/fearindex-secrets/` 표준 시크릿 폴더 규약
- 폴더 생성 + `README.md` + `install.sh` (idempotent)
- 새 릴리즈 keystore 생성 (`fearindex-release.keystore`, PKCS12, alias `fearindex`)
- `gradle.properties` (FEARINDEX_STORE_* 비밀번호)
- `google-services.json` (Firebase CLI 다운로드)
- `.claude/rules/secrets.md` 절대 규칙
- `.githooks/pre-commit` 시크릿 커밋 차단 훅
- `.gitignore` 보강
- `CLAUDE.md` / `MEMORY.md` 업데이트

### 3. v1.0.1 광고 비활성화
- `AdBanner.kt` early-return (다음 버전에서 한 줄 제거로 복원)

### 4. 완전한 로컬라이제이션 (하드코딩 제거 + 45 locale 163-key parity)
- 전체 Compose UI 하드코딩 문자열 32개 → `stringResource` 추출 (8개 파일)
- ~90개 신규 키 × 45 locale = 4,050개 번역 엔트리 생성
- 함수 시그니처 개선: `nudgeMainMessage`/`nudgeTips`/`momentumLabel`/`trendLabel` → `@StringRes Int` 반환
- Analytics 이벤트 파라미터 14개 `private const val` 분리
- 5개 병렬 로컬라이제이션 전문가: CJK+SA / 서유럽 / 슬라브 / 북유럽+발트 / MENA+SEA
- xmllint + placeholder 검증 통과
- values-tr `FOMO'dan` apostrophe escape 수정 (AAPT2 컴파일 에러)

### 5. UI 리디자인
- SimilarEventsCard: Material 3 Card (`surfaceContainerHighest`), 좌우 패딩 20→10dp, 내부 `surfaceContainerLowest`
- NotificationSettings: `ListItem` 기반 토글, `Slider steps=0` (점선 노이즈 제거), `surfaceContainer` 카드, InfoFooter 인라인

### 6. FCM 알림 end-to-end 검증
- 에뮬레이터에서 FCM 토큰 발급 → 서버 등록 성공
- FCM HTTP v1 API 테스트 푸시 발송 → 알림 트레이 수신 확인 (`importance=4 HIGH`)
- App Check debug token: `f7fe2893-4eec-4b98-8d7e-301cfa31651d`

### 7. 릴리즈 AAB 빌드 성공
- 새 keystore (`~/fearindex-secrets/fearindex-release.keystore`) 로 서명
- SHA-1: `81:AD:9D:5D:9A:E1:50:EB:F1:AE:9D:AF:86:CB:03:3D:67:6B:2A:75`
- SHA-256: `15:8F:BB:0F:B6:BD:B4:30:09:D1:B5:83:A7:42:93:A4:C6:9E:6B:25:1C:34:44:07:84:D2:41:54:05:58:88:CD`
- PEM 인증서: `~/fearindex-secrets/upload_cert.pem`

## In Progress
없음

## Remaining

### Play Console AAB 업로드 (사용자 수동)
- [ ] Play Console 내부 테스트 → "내부 테스트 버전 만들기" → AAB 드래그앤드롭 (`~/Downloads/FearIndex-v1.0.1.aab`)
- [ ] **업로드 키 리셋 필요**: 새 keystore SHA가 기존 v1.0.0과 다름 → 앱 무결성 → 업로드 키 재설정 → `~/fearindex-secrets/upload_cert.pem` 업로드
- [ ] 출시명: `2 (1.0.1)`, 출시 노트 입력, 검토 시작
- [ ] 테스터 그룹 "내부테스터" 이메일 목록 갱신 + 초대 링크 공유
- [ ] Closed Testing 12명 + 14일(3주) opt-in → Production 신청

### Firebase Console
- [ ] App Check debug token `f7fe2893-4eec-4b98-8d7e-301cfa31651d` 등록
- [ ] 새 keystore SHA-1 `81:AD:...` Firebase Console에 등록

### v1.0.2+ 후보
- [ ] SkeletonView 재검토
- [ ] 시장/암호화폐 알림 설정 분리 (통합 1개 → 2개)
- [ ] iOS hosting URL 공유 링크 반영
- [ ] InsightGenerator 단위 테스트 (경계값)
- [ ] 차트 Analytics 이벤트 세부화

### 사용자 결정 대기
- "투표" 탭 이름 → "심리" 변경 여부
- 기존 오타 앱 `com.thingineeer.fearindex` Play Console 삭제 결정
- `com.thingineer.fearindex` ("FearIndex") 삭제 결정

## Key Files

### 이번 세션 수정 (핵심)
- `presentation/.../component/AdBanner.kt` — v1.0.1 광고 비활성화 (early-return)
- `presentation/.../component/SimilarEventsCard.kt` — Material 3 Card + 패딩 축소
- `presentation/.../feature/notification/NotificationSettingsScreen.kt` — Material 3 리디자인
- `presentation/.../component/InsightDetailSheet.kt` — 66키 i18n + @StringRes 함수 변환
- `presentation/.../component/StuckCounterCard.kt` — i18n 6키
- `presentation/.../component/VoteCardView.kt` — i18n 8키
- `presentation/.../component/VoteCountdownView.kt` — i18n 1키
- `presentation/.../feature/chart/ChartScreen.kt` — i18n 5키 + analytics 상수 분리
- `presentation/.../feature/vote/VoteScreen.kt` — i18n 4키 + analytics 상수 분리
- `presentation/.../feature/home/HomeScreen.kt` — i18n 4키 + analytics 상수 분리
- `presentation/src/main/res/values/strings.xml` — English 163키 (source of truth)
- `presentation/src/main/res/values-ko/strings.xml` — Korean 163키
- `presentation/src/main/res/values-*/strings.xml` — 43 locale 각 163키

### 인프라
- `.claude/rules/secrets.md` — `~/fearindex-secrets/` 규약
- `.githooks/pre-commit` — 시크릿 커밋 차단
- `~/fearindex-secrets/` — keystore + gradle.properties + google-services.json + install.sh + upload_cert.pem

### 빌드/설정
- `app/build.gradle.kts` — versionCode=2 / versionName=1.0.1
- `app/google-services.json` → `~/fearindex-secrets/google-services.json` 심볼릭 링크
- `~/fearindex-secrets/fearindex-release.keystore` — 새 릴리즈 키
- `~/.gradle/gradle.properties` — FEARINDEX_* secrets (install.sh가 복사)
- `local.properties` — SDK 경로 (gitignored)

## Notes

### Git 상태
- 브랜치 `dev`: origin/dev와 동기화 (push 완료)
- 최근 merge 그래프:
  ```
  *   50313b6 merge: v1.0.1 i18n + secrets + UX 리디자인 → dev
  |\
  | * 6938455 i18n: 43 locale 163-key parity
  | * 412431e refactor(i18n): 하드코딩 32개 → stringResource
  | * 3783150 feat(ui): NotificationSettings Material 3 리디자인
  | * cc1db4f feat(ui): SimilarEventsCard 라이트 카드 + 패딩 10dp
  | * 8d2233b fix(ads): v1.0.1 광고 비활성화
  | * 7507bca chore(secrets): ~/fearindex-secrets/ 규약
  |/
  * 07c6f9d (이전 세션 head)
  ```

### 새 Keystore 정보
- 위치: `~/fearindex-secrets/fearindex-release.keystore` (PKCS12)
- Alias: `fearindex`
- SHA-1: `81:AD:9D:5D:9A:E1:50:EB:F1:AE:9D:AF:86:CB:03:3D:67:6B:2A:75`
- SHA-256: `15:8F:BB:0F:B6:BD:B4:30:09:D1:B5:83:A7:42:93:A4:C6:9E:6B:25:1C:34:44:07:84:D2:41:54:05:58:88:CD`
- PEM: `~/fearindex-secrets/upload_cert.pem`
- **기존 v1.0.0과 다른 keystore** → Play Console 업로드 키 리셋 필요

### Play Console 업로드 키 리셋 절차
1. Play Console → 앱 무결성 → 업로드 키 탭
2. "업로드 키 재설정 요청"
3. `~/fearindex-secrets/upload_cert.pem` 업로드
4. Google 승인 (즉시~수 시간)
5. 승인 후 `~/Downloads/FearIndex-v1.0.1.aab` 업로드 가능

### 에뮬레이터
- AVD: `Pixel_3a_API_34_extension_level_7_arm64-v8a`
- adb: `~/Library/Android/sdk/platform-tools/adb`
- 한국어 locale 설정: `adb shell cmd locale set-app-locales th1ngjin.fearindex.debug --user 0 --locales "ko-KR"`

### Chrome MCP 파일 업로드 한계
- Play Console file input (`<input type="file">`)은 Chrome extension으로 프로그래밍 업로드 불가 — macOS 네이티브 파일 다이얼로그 제어 불가
- Google Play Developer API는 `androidpublisher` OAuth 스코프 필요 — gcloud 기본 스코프에 미포함
- AAB 업로드는 사용자 수동 드래그앤드롭 또는 Fastlane/Service Account 설정 필요

## 다음 세션 시작 시
1. `/resume-FearIndex-Android` 실행
2. 브리핑 확인 후 "Ready to continue?" 응답
3. 우선순위:
   1. Play Console AAB 업로드 (사용자 수동 드래그앤드롭 또는 Fastlane 세팅)
   2. 업로드 키 리셋 (새 keystore SHA)
   3. Firebase Console 새 SHA-1 등록 + App Check debug token 등록
   4. 테스터 12명 이메일 초대 + 3주 opt-in 시작
