# Session State — FearIndex-Android

## Date
2026-04-16

## Version
- `versionCode`: 2
- `versionName`: 1.0.1
- AAB 산출물: `~/Downloads/FearIndex-v1.0.1.aab` (9.8MB) — **이번 세션 2026-04-16 재빌드** (알림 설정 fix 포함)

## Branch
`dev` (origin/dev 기준 12 commits ahead, push 대기)

## Active Worktrees

| 경로 | 브랜치 | 상태 |
|---|---|---|
| `/Users/imyeongjin/Desktop/side/FearIndex-Android` | `dev` | 본진, clean |
| `/Users/imyeongjin/Desktop/side/FearIndex-Android-share-and-gauge` | `feature/v1.0.0-share-and-gauge` | 레거시 미완 (보류, 정리 필요하면 확인 후 제거) |

## Completed (이번 세션)

### 1. iOS v1.7.9 포팅 완료
- SimilarEvents v2 (isPinned 섹션 분리 + aggregateStats 3열 maxDrawdown/bestReturn)
- Callable Function `getSimilarEvents` fallback 패턴 (Firestore doc 미존재 대응)
- InsightDetailSheet 상단 "현재 N점에서 매수 시" 통계 카드 (3열)
- DefaultReturnData 101포인트 실측 교체
- fearVelocity weekly 체크 버그 수정
- 45개 locale strings.xml v1.7.9 키 추가 (SimilarEvents + 이벤트 26개)
- Appium E2E 검증 스크립트 (`scripts/e2e/appium_v179_check.py`)

### 2. 알림 설정 서버 동기화 버그 수정 (이번 세션 핵심)
- **증상**: 알림 설정 화면 슬라이더 드래그 시 "서버 동기화 실패: INTERNAL" Snackbar
- **원인**: `NotificationDataSource`가 flat payload 전송 — 서버는 `{ deviceId, settings: { ... } }` 중첩 기대. `settings.lowerThreshold=undefined` → INTERNAL.
- **수정**: `data/.../NotificationDataSource.kt` iOS `FCMService.swift`와 동일 payload 구조로 통일
  - `settings` 중첩 객체 + `cryptoNotificationEnabled` + ISO 639-1 `language`
  - 클라이언트 클램핑 (lower 0–50, upper 50–100)
- **검증**: 에뮬레이터에서 드래그 → "Notification settings updated" 로그 확인, Snackbar 사라짐
- **브랜치**: `feature/v1.0.1-notification-fix` → `dev`로 `--no-ff` merge 완료, worktree 제거

### 3. 전체 Callable/Firestore 서버 통신 점검
| 엔드포인트 | 상태 |
|---|---|
| `registerFCMToken` Callable | ✅ 정상 |
| `updateNotificationSettings` Callable | ✅ 수정 후 정상 |
| `submitStuckStatus` / `getStuckCount` Callable | ✅ 정상 |
| `getSimilarEvents` Callable + `insights/*` snapshot | ✅ 정상 |
| `stuckStatus/global_*` snapshot | ✅ 정상 |
| **`votes/{date}/results/{indexType}` snapshot** | ❌ **PERMISSION_DENIED** |

### 4. iOS Info.plist ↔ Android 초기화 설정 대칭성 검증
- `google-services.json` 배치 (app/) + gitignore ✅
- package_name 2개 (`th1ngjin.fearindex` / `.debug`) ✅
- `FirebaseFunctions.getInstance("asia-northeast3")` 리전 고정 ✅
- FirebaseApp 초기화 (FearIndexApp onCreate) ✅
- App Check: Debug Provider (debug) / Play Integrity (release) ✅
- Crashlytics + Analytics 플러그인 ✅
- AdMob App ID meta-data ✅
- POST_NOTIFICATIONS 권한 ✅
- FCM MessagingService + default_notification_channel_id ✅
- Firebase BoM 사용 ✅

## In Progress
없음 — 이번 세션 목표 완료.

## Remaining

### ✅ 이번 세션 해결 완료
- Vote Firestore rules 수정 + deploy 완료 (iOS `firebase-functions/firestore.rules` + `firebase deploy --only firestore:rules`)
- 에뮬레이터에서 Vote 탭 재진입 → PERMISSION_DENIED 사라짐 확인
- v1.0.1 AAB 재빌드 (9.8MB) → `~/Downloads/FearIndex-v1.0.1.aab`

### v1.0.1 업로드 (사용자 수동)
- [ ] Play Console 내부 테스트 트랙 업로드 (`~/Downloads/FearIndex-v1.0.1.aab`)
- [ ] 테스터 초대 진행 (Closed Testing 12명 + 14일 opt-in)

### v1.0.2+ 후보
- [ ] SkeletonView 재검토
- [ ] 시장/암호화폐 알림 설정 분리 (통합 1개 → 2개)
- [ ] iOS hosting URL (`https://fear-index-a4f4b.web.app/...`) 공유 링크 반영
- [ ] InsightGenerator 단위 테스트 (46점/0점/100점 경계)
- [ ] 차트 Analytics 이벤트 세부화 (드래그/툴팁)

### 사용자 결정 대기
- "투표" 탭 이름 → "심리" 변경 여부
- AdMob 테스트 디바이스 등록
- 기존 오타 앱 `com.thingineeer.fearindex` (e3) Play Console 삭제 결정
- App Check debug token `f2469c34-13e4-4f7b-8067-e2009ff448b5` Firebase Console 등록 여부
- `git push origin dev` (현재 12 commits ahead)
- Vote rules 수정 + iOS firestore.rules deploy 진행 여부

## Key Files

### 이번 세션 수정
- `data/src/main/java/th1ngjin/fearindex/data/datasource/NotificationDataSource.kt` — 서버 동기화 payload 중첩 구조 수정 (iOS parity)
- `.claude/memory/bugs-fixed.md` — 10, 11번 버그 기록 추가

### 프로젝트 문서
- `CLAUDE.md` — Git Workflow / Package / Firebase / AdMob 규칙
- `.claude/memory/MEMORY.md` — 인덱스 + 상수표
- `.claude/memory/bugs-fixed.md` — 버그 이력 (현재 11개)
- `.claude/memory/ios-parity.md` — iOS 대칭성 체크리스트
- `.claude/memory/firebase-setup.md` — Functions/Firestore/App Check

### 핵심 코드 (iOS parity 영향 큰 순서)
- `core/.../util/InsightGenerator.kt` — 인사이트 6종 생성 엔진
- `core/.../util/ReturnDataInterpolator.kt` — 선형 보간
- `domain/.../defaults/DefaultReturnData.kt` — 101점 anchor + 이벤트 12개
- `presentation/.../feature/insight/InsightViewModel.kt` — distinctUntilChanged 적용
- `presentation/.../component/InsightDetailSheet.kt` — 6종 BottomSheet (+ 이번 세션 3열 통계 카드)
- `presentation/.../feature/home/HomeScreen.kt` — 홈 레이아웃
- `presentation/.../feature/notification/NotificationSettingsScreen.kt` — 알림 설정 UI
- `presentation/.../feature/notification/NotificationSettingsViewModel.kt` — debounce 0.5초 + 로컬 캐시 SSOT
- `data/.../datasource/NotificationDataSource.kt` — **이번 세션 수정** (iOS parity payload)
- `data/.../datasource/VoteDataSource.kt` — **Firestore snapshot PERMISSION_DENIED 남음**

### 빌드/설정
- `app/build.gradle.kts` — versionCode=2 / versionName=1.0.1
- `app/google-services.json` — Firebase prod+debug
- `~/fearindex-release.keystore` (gitignore)
- `~/.gradle/gradle.properties` — `FEARINDEX_STORE_PASSWORD` 등 secrets

## Notes

### Git 상태
- 브랜치 `dev`: origin/dev 기준 **12 commits ahead** (push 대기)
- 최근 merge 그래프 (이번 세션):
  - `ae8e9cf docs(memory): 서버 통신 점검 기록 추가 (10, 11번 버그)`
  - `9696ea5 merge: 알림 설정 서버 동기화 fix → dev`
  - `37842c1 fix: 알림 설정 서버 동기화 payload 중첩 구조로 수정`
- 모든 머지 `--no-ff` (squash 없음)

### Firebase 공유 프로젝트
- Project ID: `fear-index-a4f4b` (iOS/macOS/Android 공유)
- Functions 리전: `asia-northeast3`
- **Firebase Rules API로 배포된 rules 직접 조회 가능**:
  ```
  https://firebaserules.googleapis.com/v1/projects/fear-index-a4f4b/releases/cloud.firestore
  ```
- **iOS/macOS 앱 삭제 절대 금지**

### 검증 절차 확립
- 에뮬레이터 ADB + `uiautomator dump` 좌표 추출 → Python 파서로 clickable 영역 추출
- `adb logcat -d --pid=$(adb shell pidof th1ngjin.fearindex.debug)` — 앱 로그만 필터
- Firebase Admin SDK + Rules API로 서버 상태 직접 검증

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

### AdMob
- App ID: `ca-app-pub-5283496525222246~1308884877`
- HomeBanner Unit ID: `ca-app-pub-5283496525222246/3189551565`

### iOS 프로젝트 위치
- `/Users/imyeongjin/Desktop/side/FearIndex-iOS` — Bundle ID `th1ngjin.FearIndex-iOS`
- Firebase Functions source: `firebase-functions/src/index.ts` (Android에는 없음, iOS가 원본)

## 다음 세션 시작 시
1. `/resume-FearIndex-Android` 실행
2. 브리핑 확인 후 "Ready to continue?" 응답
3. 우선순위:
   1. **Vote Firestore rules 수정** (iOS firestore.rules + deploy) — 사용자 승인 대기
   2. `git push origin dev` 여부 결정 (12 commits ahead)
   3. v1.0.1 AAB 재빌드 (알림 설정 fix 포함) + Play Console 업로드
   4. 테스터 초대 진행
