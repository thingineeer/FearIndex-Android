# Session State — FearIndex-Android

## Date
2026-06-16

## Branch
`feature/v1.2.0-banner-clip-fix`

## ⚡ 최신 (2026-06-16): v1.2.0 production 배포 — 검토 전송 완료 (심사 대기)

**Android `1.2.0` / `versionCode 16` 을 production 100% rollout 으로 Play Console 검토 전송 완료.**

핵심 한 줄: fastlane 으로 올렸지만 **관리형 게시(Managed Publishing)가 ON** 이라, ①검토 전송(이번엔 Chrome MCP로 클릭 완료) + ②심사 통과 후 **"게시" 버튼 수동 클릭**이 별도로 필요하다. 강제 업데이트 RC(`force_update_minimum_version` Android=1.1→1.2)는 **1.2.0 전파 확인 후**에 올린다 (지금 올리면 받을 버전이 없어 강제창에 막힘).

---

## Completed (이번 배포 세션)

- [x] **강제 업데이트 v1.2.0 시나리오 TDD 검증** — `UpdateCheckerTest` 에 force=1.2 케이스 3개 추가 (1.0.x/1.1.x 강제, 1.2.0 제외). `compareMajorMinor([1,2,0],[1,2])=0` 확인. 커밋 `d57c1f3`.
- [x] **changelog 45 locale `16.txt` 생성** — 한국어="전체적인 성능 및 개선을 하였습니다." + 44 locale 동일 의미 번역. 500자 이하 검증. 커밋 `e1d4ab1`.
- [x] **스크린샷** — 어제(6/16 14:16) 광고 없이 촬영된 기존 5장(home/chart/vote/notification_settings/notification) × 45 locale 그대로 사용 (사용자 선택). 이미 git 커밋됨. 시장 상세는 미포함.
- [x] **테스트 + AAB** — `./gradlew test` **585 통과**(failures=0). `bundleRelease` 서명 SHA-1 `CE:08:B4:8A:...`(UPLOAD.RSA, 활성 업로드키) 일치 검증.
- [x] **fastlane production** — HTTP 200 성공. AAB+메타+changelog 16+스크린샷(phone/7inch/10inch) 업로드, 100% rollout.
- [x] **검토 전송** — Chrome MCP로 Play Console "게시 개요" → "검토를 위해 변경사항 136개 전송" 클릭 → 확인 다이얼로그 승인 → "검토 중인 변경사항" 전환 확인. 변경 항목 = 프로덕션 1.2.0(전체 출시 시작) + 스토어 등록정보 45 locale × 3 스크린샷 크기.
- [x] **메모리 갱신** — bugs-fixed 31번, deployment 출시이력 v1.2.0, MEMORY.md 최신상태. 커밋 `c5fa5a5`.

## In Progress
- 없음. v1.2.0 은 Google **심사 대기** 상태로 넘어갔고, 이 세션의 능동 작업은 종료됨.

## Remaining (다음 세션 최우선 — 순서 중요)
1. **Play Console 1.2.0 심사 통과 확인** ("검토 중인 변경사항" → 승인).
2. **관리형 게시이므로 "게시" 버튼 수동 클릭** → 실제 production 출시 (심사 통과해도 자동 게시 안 됨).
3. **Play Store 전파 확인** 후 → **Firebase Console Remote Config `force_update_minimum_version` [Android app users] = `1.2`** 설정 → 1.0.x/1.1.x 강제 업데이트 발동. (전파 전 상향 금지)
4. (선택) `feature/v1.2.0-banner-clip-fix` 브랜치를 dev/release 머지 + 태그 — **사용자 명시 요청 시에만**.

## Key Files
- @.claude/memory/MEMORY.md — 프로젝트 메모리 인덱스 + 최신 상태(v1.2.0 배포). 세션 시작 시 필독.
- @.claude/memory/bugs-fixed.md — 24~31번이 이번 v1.2.0 작업/배포 이력 (31번 = 관리형 게시 발견).
- @.claude/memory/deployment.md — 출시 이력 테이블 v1.2.0 추가, keystore/AAB/RC 절차.
- @core/src/main/java/th1ngjin/fearindex/core/update/UpdateChecker.kt — 강제 업데이트 판정 로직 (major.minor 비교).
- @core/src/test/java/th1ngjin/fearindex/core/update/UpdateCheckerTest.kt — force=1.2 시나리오 테스트 3개 추가됨.
- @core/src/main/java/th1ngjin/fearindex/core/remoteconfig/RemoteConfigManager.kt — RC 키(`force_update_minimum_version`, `minimum_app_version`) default fail-open.
- @fastlane/Fastfile — `production` lane (bundle+upload, 100% rollout). 관리형 게시라 검토전송/게시는 Console 수동.
- @app/build.gradle.kts — versionCode 16 / versionName 1.2.0 (line 24-25).

## 대화 요약

### 이번 세션에서 결정한 것
- **v1.2.0 production 배포** — 사용자 `/goal`: "1.2.0 배포, 새 스크린샷 함께, mcp로도 배포, 릴리즈 노트='전체적인 성능 및 개선을 하였습니다.', 1.2.0 강제업데이트 가능하게". → 6단계(강제업데이트 코드검증 → changelog 45locale → 스크린샷 → 테스트+AAB → Play업로드 → RC타이밍)로 분해 실행.
- **changelog 한국어 = "전체적인 성능 및 개선을 하였습니다."** (사용자 지정 문구 그대로), 나머지 44 locale 동일 의미 번역.
- **스크린샷은 기존 5장 그대로** — 사용자가 AskUserQuestion에서 선택. 어제 광고 없이 촬영·검증된 것이라 재촬영 불필요. 시장 상세는 이번 스크린샷에 미포함.
- **검토 전송까지 진행** — 사용자가 "전송해서 심사 시작" 선택. Chrome MCP로 클릭.
- **강제 업데이트 RC는 지금 1.1 유지, 전파 후 1.2** — 사용자가 "지금은 1.1 유지, 전파 후 1.2 (권장)" 선택. 이유: 1.2.0이 Play 전파 전에 RC를 1.2로 올리면 1.0~1.1 유저가 받을 버전이 스토어에 없어 In-App Update 폴백/강제창 막힘 (bugs-fixed 23번 참고 원칙).

### 시도했다 접은 것
- **AAB 서명 검증에서 keytool 한글 locale 버그** — `keytool -printcert` 가 한국어 환경에서 `IllegalFormatConversionException` 발생. → `LANG=en_US.UTF-8 keytool -J-Duser.language=en` 로 우회해 SHA-1 `CE:08:B4:...` 확인.
- **gradle.properties 에서 store password grep 추출** — 빈 값 나옴(형식 차이). → AAB 의 UPLOAD.RSA 인증서를 직접 추출해 서명 검증하는 방식으로 전환.

### 명시된 사용자 선호
- **관리형 게시/배포 확정 동작 전 확인받기** — outward-facing 단계(fastlane 실행, 검토 전송)마다 AskUserQuestion 으로 승인 후 진행했고, 사용자가 모두 "권장" 선택.
- **강제 업데이트 타이밍 안전 우선** — fail-open 유지, 전파 전 RC 상향 금지.
- (이전 세션 누적) TDD 위주, 1px도 어긋나면 안 됨, 실기기 release 빌드 필수, iOS 로직 그대로만, 푸시는 절대 전역 발송 금지(단일 토큰만).

### 다음 세션이 알아야 할 맥락
- **이 앱은 관리형 게시(Managed Publishing) ON**. fastlane production 은 "업로드+검토전송 대기"까지만 자동. 매 배포 시 Play Console "게시 개요"에서 대기 변경사항 확인 + 검토 전송 + (심사 후)게시 버튼을 수동으로 처리해야 함.
- **현재 RC 값**: `force_update_minimum_version` default=1.6.0(iOS), [Android]=`1.1`. `minimum_app_version` default=1.8.2(iOS), [Android]=`1.1.3`.
- Play Console app ID `4973920645070208584`. Firebase project `fear-index-a4f4b`. RC 조회: `firebase remoteconfig:get --project fear-index-a4f4b -o rc.json`.
- 모든 v1.2.0 코드/배포 변경은 `feature/v1.2.0-banner-clip-fix` 에 커밋. push/dev머지 미실행(명시 요청 대기).

### 이 프로젝트 세션 이력 (이 기기)
- 14:52~15:20 — 차트 peak 고점/저점 마커(TDD, iOS parity), 홈 공유→Play 스토어 링크, 현재지수 info 버튼+KOSPI 장상태/업데이트시각 구현. 릴리즈 빌드 반복.
- 16:01~16:03 — 암호화폐 비교 수치 날짜기반 앵커 수정(20/10/31/61), 코스피 36 vs 37 검증(코드 정상), 시장 상세 화면(3탭) Material 구현.
- 17:08~17:28 — 푸시 알림 단일 토큰 테스트(에뮬레이터 수신 확인, 전역 발송 안 함). Chrome MCP 사용.
- 17:30~18:00 — `/goal` v1.2.0 배포. 강제업데이트 검증 → changelog → AAB → fastlane production → 검토 전송 → RC 타이밍 결정 → 세션 저장.

## Notes
- 빌드: `./gradlew test` (585 통과), `./gradlew clean :app:bundleRelease` (서명 자동, `~/.gradle/gradle.properties` 의 FEARINDEX_* 필요).
- 배포: `bundle exec fastlane production` → HTTP 200 후 **반드시 Play Console 게시 개요에서 검토 전송 + 게시 수동 처리** (관리형 게시).
- Secrets: keystore `~/fearindex-secrets/fearindex-release.keystore` (alias=upload, SHA-1 `CE:08:B4:...`). SSOT는 `~/thingineeer-env/android/fearindex/`.
- 알려진 잔존 이슈: AdMob 배너 "적용 불가"(1.0.1 정책 위반) — 1.0.x/1.1.x 트래픽이 강제 업데이트로 0 수렴해야 자연 해소. v1.2.0 강제 기준 상향이 이를 가속.
