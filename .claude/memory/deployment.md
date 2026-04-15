---
name: Deployment
description: 릴리즈 빌드, 서명, Play Console 업로드까지의 절차와 상수.
type: reference
---

# Deployment

## 빠른 상수

| 항목 | 값 |
|---|---|
| Package (prod) | `th1ngjin.fearindex` |
| Package (debug) | `th1ngjin.fearindex.debug` |
| versionCode | `app/build.gradle.kts:20` 참고 (매 제출마다 증가) |
| versionName | `app/build.gradle.kts:21` |
| Keystore 파일 | `~/fearindex-release.keystore` (PKCS12) |
| Keystore alias | `fearindex` |
| Keystore 비밀번호 | `~/.gradle/gradle.properties`의 `FEARINDEX_STORE_PASSWORD` 키 사용 — 코드/문서에 직접 기록 금지 |
| SHA-1 | `A1:54:8A:92:C3:AF:A5:0E:BD:31:F6:6B:47:1B:9E:BB:51:5D:23:51` |
| SHA-256 | `AD:48:68:DA:81:3C:9D:39:65:D0:C8:F9:59:62:61:6F:0A:6D:3A:BF:4E:21:DA:12:C0:DF:D8:2C:11:6A:14:0D` |
| AdMob App ID | `ca-app-pub-5283496525222246~1308884877` |
| AdMob HomeBanner | `ca-app-pub-5283496525222246/3189551565` |

## AAB 빌드 (Release)

```bash
cd /Users/imyeongjin/Desktop/side/FearIndex-Android
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

- `~/.gradle/gradle.properties`에 `FEARINDEX_STORE_FILE` / `FEARINDEX_STORE_PASSWORD` / `FEARINDEX_KEY_ALIAS` / `FEARINDEX_KEY_PASSWORD` 가 설정되어 있어야 자동 서명.
- CI 환경에서는 같은 이름의 환경변수로 대체 가능 (`app/build.gradle.kts`의 `signingConfigs` 로직).

## Play Console 업로드 (수동)

절차는 @../../docs/GOOGLE-PLAY-INTERNAL-TEST.md 참조.

요약:
1. https://play.google.com/console → FearIndex 앱 선택
2. 테스트 및 출시 → **내부 테스트** 트랙 → "새 버전 만들기"
3. AAB 드래그&드롭 업로드
4. 출시명 + 출시 노트 입력
5. "검토 시작" → "내부 테스트로 출시"

## 자동 업로드 (fastlane)

`fastlane/` 구조 존재. `supply` 기반.

```bash
cd /Users/imyeongjin/Desktop/side/FearIndex-Android
bundle exec fastlane internal
```

선행 조건: Play Console에서 **service account JSON 발급** → `fastlane/Appfile`의 `json_key_file` 경로 설정.

## 프로모션 / Staged Rollout

- **Alpha → Beta → Production**: Play Console에서 "프로모션" 버튼.
- **Staged rollout**: Production 배포 시 비율 선택 (예: 10% → 50% → 100%).

## dSYM 해당사항 없음

- **iOS 대응**: Android는 ProGuard mapping 파일 필요. `app/build.gradle.kts`에서 `isMinifyEnabled = true` 일 때 자동 생성.
- **Crashlytics 심볼 업로드**: `google-services` 플러그인이 자동으로 처리 (`uploadCrashlyticsMappingFileRelease` 태스크).

## 관련 자동화

- `scripts/screenshots/capture-all-locales.sh` — 45개 locale 스크린샷 자동 촬영
- `scripts/e2e/run-all.sh` — E2E 테스트 스위트 실행

## 관련 문서

- @../../docs/GOOGLE-PLAY-INTERNAL-TEST.md — Play Console 수동 배포 8단계
- @firebase-setup.md — Firebase Functions 배포
- @../rules/package-convention.md — package name 엄수
