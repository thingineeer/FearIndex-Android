# Session State — FearIndex-Android

## Date
2026-05-11

## Branch
`dev` (clean — code/docs only committed; capture PNG들은 백그라운드 진행 중이라 별도 commit 보류)

## Version
- v1.0.0 (versionCode 7) — 2026-05-09 Production 출시 통과
- 다음 출시: v1.0.1 또는 v1.1.0 (시안에 따라). 백로그.

## ✅ Completed (이번 세션 / 2026-05-10~11)

### thingineeer.github.io 개발자 사이트 리디자인
- Apple/Figma/Notion/Supabase 톤 결합 Hybrid Bold 시안 적용
- 4개 앱 카드 (공포지수 / Runnect / PumpWater / CozyDay) + 아이콘 (iTunes Search API)
- 다크 contact 섹션 + 모노 이메일 카드
- 모바일 대응 검수 (320 / 390 / 1280 viewport)
- commit + push: `https://thingineeer.github.io/`

### 통합 Privacy Policy 페이지
- `https://thingineeer.github.io/privacy/` 신규 작성
- 기리고 톤 따라 매우 간결 (한국어 + 영어 단락 + AdMob 한 줄)
- contact label: `// contact: dlaudwls1203@gmail.com`
- footer 에 GITHUB 링크 추가

### Privacy URL 교체 (4 store 중 처리 가능한 곳)
- ✅ App Store Connect — PumpWater iOS: `thingjin.notion.site/...` → `thingineeer.github.io/privacy/` (review 진행 중이라 편집 가능, 통과 시 자동 적용)
- ⏸ App Store Connect — 공포지수 iOS / CozyDay iOS: 라이브 상태라 직접 편집 불가. **다음 빌드 제출 시** 자동 적용 (Apple 의 새 정책: Privacy URL 변경은 다음 버전 출시 동반).
- ✅ Play Console — 공포지수 Android: 스토어 등록정보 연락처 → 웹사이트 = `https://thingineeer.github.io/` 등록 (즉시 게시). AdMob 인증 핵심 이슈 해결.

### AdMob 재인증 트리거
- "공포지수(Android)을(를) 확인할 수 없습니다" 메시지 — Play Console 개발자 웹사이트 미등록이 핵심 원인
- 웹사이트 등록 후 AdMob "업데이트 확인" 클릭 → 재크롤링 시작
- 인증 통과 예상: 수 분 ~ 24h

### CLAUDE.md — 스크린샷 모드 절대 규칙 추가
- AdMob 섹션 아래 "스크린샷 모드 (절대 규칙)" 항목
- 테스트/프로덕션 광고 모두 캡처 금지
- 메커니즘: `adb shell setprop debug.screenshot_mode 1` → `AdBanner.kt` 의 `isScreenshotMode()` 가 광고 hide
- `capture-*.sh` 시작 시 반드시 setprop 호출

### 7" 태블릿 캡처 스크립트 정착
- 이전 사고: 모든 locale 의 `1_notification.png` + `5_notification_settings.png` 가 Contacts 앱으로 잘못 캡처 (90장)
- 원인 1: PKG 자동 감지 (production 우선, 없으면 debug)
- 원인 2: 태블릿 taskbar (launcher dock) 가 화면 하단 ~60px 차지 → 우리 앱 nav bar 좌표는 그 위 (TABY = H*89/100)
- 원인 3: cold-start 후 splash 통과까지 약 15-25초 → sleep 25
- 원인 4: dumpsys 가 multi-window 시 launcher 를 top 으로 잘못 보고 → fg 검증 무시
- 원인 5: 광고 hide 누락 → setprop debug.screenshot_mode 1 추가
- 모든 패치 완료. 좌표 + sleep + screenshot mode 모두 정착.

### 7" 태블릿 캡처 백그라운드 실행 (이 글 시점)
- `bash scripts/screenshots/capture-tablet-all-locales.sh seven` (PID 추적 background)
- 진행률: ar (2/45) … 약 30-35분 소요 예상
- 산출물: `fastlane/metadata/android/<locale>/images/sevenInchScreenshots/{1..5}_*.png`
- ko_KR 의 옛 잘못된 파일명 (1_home / 2_chart / 3_vote / 4_notification_settings) 4장은 `git rm` 처리 완료 (새 표준 파일명으로 덮어쓰는 중)

## ⏳ Remaining

### 1. 7" 태블릿 캡처 끝까지 + 검수
- 백그라운드 약 30-35분 후 종료
- 검수: 무작위 5-10 locale 의 5장 모두 우리 앱 화면인지 + 광고 영역 비어있는지 확인
- 정상이면 fastlane metadata 에 commit (45 locale × 5 = 225장)

### 2. 10" 태블릿 캡처 (별도 AVD 필요)
- AVD 만들기: Android Studio Device Manager → Tablet (Pixel Tablet 또는 비슷, 1920x1200 이상)
- 부팅 후 debug 빌드 install
- `bash scripts/screenshots/capture-tablet-all-locales.sh ten`
- 산출물 검수 + commit

### 3. iOS Privacy URL 자연 적용 대기
- 공포지수 iOS / CozyDay iOS 의 다음 정상 업데이트 빌드 제출 시 새 URL (`https://thingineeer.github.io/privacy/`) 으로 입력
- 그때까지 라이브 production 은 옛 Notion URL 노출 (`thingjin.notion.site/...`) — Notion 페이지는 그대로 두면 됨

### 4. AdMob 인증 결과 확인
- 24h 후 https://admob.google.com/v2/apps/1308884877 페이지에서 "공포지수(Android) 확인할 수 없습니다" 메시지 사라졌는지
- 사라지지 않으면 app-ads.txt 끝에 빈 줄 추가하여 cache-bust 후 재크롤링 트리거

### 5. fastlane supply 권한 propagation
- 24h 경과 후 (대략 2026-05-12 부근): `bash scripts/deploy/check-fastlane-ready.sh`
- OK 면 `bash scripts/deploy/upload-metadata-all-locales.sh` 로 45 locale 메타데이터 + 스크린샷 일괄 업로드

## Key Files

| 파일 | 역할 |
|---|---|
| `CLAUDE.md` | 프로젝트 절대 규칙 (스크린샷 모드 규칙 추가) |
| `.claude/memory/MEMORY.md` | 메모리 인덱스 |
| `.claude/memory/bugs-fixed.md` | 버그 이력 (17번까지) |
| `.claude/memory/deployment.md` | AAB / Play Console / 활성 키 SHA1 |
| `presentation/.../component/AdBanner.kt` | `isScreenshotMode()` SystemProperties 체크 — 캡처 시 광고 hide |
| `scripts/screenshots/capture-tablet-all-locales.sh` | 7"/10" 태블릿 캡처 (좌표/sleep/screenshot mode 패치 완료) |
| `~/Desktop/thingineeer.github.io/` | 개발자 사이트 (별도 GitHub Pages repo) — Hybrid Bold 적용됨 |
| `app/build.gradle.kts` | versionCode 7, versionName 1.0.0 |

## Notes

### 백그라운드 작업 — 캡처 진행 중
- 7" 태블릿 캡처가 background 로 돌고 있음 (PID 추적 background)
- log: `/tmp/capture-seven.log`
- 캡처 중 emulator 닫지 말 것
- 끝난 후 산출물 검수: `ls fastlane/metadata/android/ko_KR/images/sevenInchScreenshots/`

### iOS Privacy URL 정정
- 사용자 질문 답: "심사 다시 안 올려도 회신만 하면 자동 적용?" → **아니**. iOS 는 다음 빌드 제출 시 새 URL 자동 사용. PumpWater 는 review 진행 중이라 이미 입력 완료.

### AdMob 인증
- 핵심 이슈: Play Console 의 "스토어 등록정보 연락처 세부정보 → 웹사이트" 가 비어있어서 AdMob 이 도메인 못 찾음
- 해결: `https://thingineeer.github.io/` 등록 (즉시 게시). app-ads.txt 자체는 정상 (`google.com, pub-5283496525222246, DIRECT, f08c47fec0942fa0`)

### 새 머신 셋업 (절대 규칙)
```bash
gh repo clone thingineeer/FearIndex-Android ~/Desktop/FearIndex-Android
cd ~/Desktop/FearIndex-Android
gh repo clone thingineeer/thingineeer-env ~/thingineeer-env
bash ~/thingineeer-env/android/fearindex/install.sh
```
