# Fastlane Supply 셋업 가이드 (Play Console 자동화)

`fastlane supply` 가 Google Play Console 의 메타데이터 / 스크린샷 / icon / featureGraphic / changelog / AAB 모두 자동 업로드.

## 0. 사전 조건

- macOS + Ruby 3.x + bundler 2.x
- `~/fearindex-secrets/` 디렉토리 존재 (keystore 폴더와 동일 위치에 service account JSON 보관)
- Play Console 개발자 계정 (`dlaudwls1203@gmail.com`)

## 1. fastlane 설치 (둘 중 하나)

### 1-A. System fastlane (Homebrew, 권장 — 즉시 사용)

```bash
brew install fastlane     # 이미 설치되어 있으면 skip
fastlane --version        # 2.232.0+ 확인
```

이 머신은 이미 `/opt/homebrew/Cellar/fastlane/2.232.0` 에 설치되어 있어 그대로 사용 가능. 본 가이드의 모든 명령은 `bundle exec` 없이 `fastlane <lane>` 형태.

### 1-B. Bundler 기반 (CI/CD 또는 Ruby 3.x 환경)

```bash
# Ruby 3.x 필요 (system Ruby 2.6 으로는 안됨 — rbenv 또는 asdf 사용)
rbenv install 3.2.0
rbenv local 3.2.0

cd /Users/imyeongjin/Desktop/APP/FearIndex-Android
bundle install
```

`Gemfile.lock` 이 자동 생성되어 fastlane 버전이 머신 간 고정됨. 명령은 `bundle exec fastlane <lane>` 형태.

## 2. Service Account 발급 (한 번)

### 2.1 Google Cloud Console

1. https://console.cloud.google.com/iam-admin/serviceaccounts?project=fear-index-a4f4b
2. **서비스 계정 만들기** 클릭
3. 입력:
   - 이름: `fastlane-supply`
   - 설명: `Fastlane Play Console upload automation`
4. **만들고 계속하기** → 권한 부여 단계는 **건너뛰기** (Play Console 에서 별도 부여)
5. **완료**
6. 만들어진 계정 행 → **작업** 메뉴 → **키 관리** → **키 추가** → **새 키 만들기** → JSON
7. 다운로드된 JSON 파일을 보관:

```bash
mv ~/Downloads/fear-index-*-*.json ~/fearindex-secrets/play-store-service-account.json
chmod 600 ~/fearindex-secrets/play-store-service-account.json
```

### 2.2 Play Console 권한 부여

1. https://play.google.com/console
2. 왼쪽 하단 **사용자 및 권한**
3. **새 사용자 초대**
4. 이메일: 위 service account 의 이메일 (`fastlane-supply@fear-index-a4f4b.iam.gserviceaccount.com`)
5. 앱 권한 부여:
   - **공포지수 (`th1ngjin.fearindex`)** 만 선택
   - 권한:
     - 출시 만들기, 출시판, 기기 카탈로그 등록정보
     - 스토어 등록정보, 맞춤설정, 실험 관리
     - 가격 및 배포 관리
6. **사용자 초대**
7. 자동 승인됨 (service account 라 이메일 응답 불필요).

### 2.3 검증

```bash
cd /Users/imyeongjin/Desktop/APP/FearIndex-Android
bundle exec fastlane run validate_play_store_json_key json_key:~/fearindex-secrets/play-store-service-account.json
```

성공 시 `Successfully established connection to Google Play Store.` 출력.

## 3. 첫 동기화 — Play Console → 로컬 (`supply init`)

이미 Play Console 에 등록된 모든 자산 (icon, featureGraphic, 메타, 스크린샷, changelog) 을 `fastlane/metadata/android/` 하위에 다운로드.

```bash
cd /Users/imyeongjin/Desktop/APP/FearIndex-Android
bundle exec fastlane supply init --json_key ~/fearindex-secrets/play-store-service-account.json --package_name th1ngjin.fearindex
```

> 스크린샷과 동영상 URL 은 API 로 다운로드 불가 (Google 정책). 텍스트 메타 + icon + featureGraphic + changelogs 만 받음.

다운로드 후 `git status` 로 변경사항 확인. 우리가 수동으로 작성한 changelog 와 충돌하면 우리 버전 우선.

## 4. 실 업로드 — 로컬 → Play Console

### 4.1 메타 + 사진 + changelog 만 (AAB 제외)

```bash
bundle exec fastlane upload_metadata
```

`Fastfile` 의 `upload_metadata` lane: `skip_upload_apk: true, skip_upload_aab: true, skip_upload_metadata: false, skip_upload_changelogs: false, skip_upload_images: false, skip_upload_screenshots: false`.

### 4.2 AAB 까지 한 방 (Internal Testing 트랙)

```bash
bundle exec fastlane internal
```

> 단 현재 `internal` lane 은 `skip_upload_images: true, skip_upload_screenshots: true` 라 사진은 업로드 안 함. 사진까지 자동화하려면 lane 정의에서 false 로 변경하거나 `upload_metadata` 를 별도로 실행.

### 4.3 검증만 (실제 업로드 안 함)

```bash
bundle exec fastlane run upload_to_play_store \
  json_key:~/fearindex-secrets/play-store-service-account.json \
  package_name:th1ngjin.fearindex \
  validate_only:true \
  skip_upload_apk:true \
  skip_upload_aab:true
```

## 5. 트랙 승격

```bash
# Internal → Closed (Alpha)
bundle exec fastlane promote_to_closed

# Closed → Production (10% staged rollout)
bundle exec fastlane promote_to_production
```

## 6. 자산 디렉토리 구조 (참고)

```
fastlane/metadata/android/
├── ko_KR/                          ← fastlane supply 표준은 underscore (en_US, ko_KR ...)
│   ├── title.txt                   30자
│   ├── short_description.txt       80자
│   ├── full_description.txt        4000자
│   ├── video.txt                   YouTube URL (옵션)
│   ├── changelogs/
│   │   ├── default.txt             fallback
│   │   └── 4.txt                   versionCode 별
│   └── images/
│       ├── icon.png                512×512
│       ├── featureGraphic.png      1024×500
│       ├── promoGraphic.png        180×120 (옵션)
│       ├── tvBanner.png            1280×720 (TV 전용, 옵션)
│       ├── phoneScreenshots/       1.png ... 8.png  (320~3840px)
│       ├── sevenInchScreenshots/   7" 태블릿
│       ├── tenInchScreenshots/     10" 태블릿
│       ├── tvScreenshots/          (옵션)
│       └── wearScreenshots/        (옵션)
├── en_US/
└── ... (45 locale)
```

## 7. 트러블슈팅

| 증상 | 해결 |
|---|---|
| `Forbidden — The current user has insufficient permissions` | Play Console 에서 service account 권한 재확인. 앱별 권한 부여 누락. |
| `Package not found` | `--package_name th1ngjin.fearindex` 명시 또는 `Appfile` 의 package_name 확인. |
| `Localized text too long` | title 30자 / short 80자 / full 4000자 (character count). Play Console 한도 |
| `Image dimension invalid` | icon 512×512 정확히, featureGraphic 1024×500 정확히, 24bit PNG (alpha 없음) |
| `validate_play_store_json_key` 실패 | service account 이메일이 Play Console 에서 승인됐는지 (1~2분 지연 가능) |

## 8. CI/CD (옵션)

GitHub Actions 또는 Bitrise 등에서 자동화 시 service account JSON 을 base64 인코딩해서 secret 으로 주입:

```bash
base64 -i ~/fearindex-secrets/play-store-service-account.json | pbcopy
# → GitHub Actions secrets.PLAY_SA_JSON_BASE64 에 붙여넣기
```

CI 단계:

```yaml
- name: Decode service account
  run: |
    echo "${{ secrets.PLAY_SA_JSON_BASE64 }}" | base64 -d > /tmp/play-sa.json
- name: Upload to Play Console
  env:
    FEARINDEX_PLAY_SA_JSON: /tmp/play-sa.json
  run: bundle exec fastlane internal
```

## 9. 관련 문서

- [공식: fastlane supply](https://docs.fastlane.tools/actions/supply/)
- [공식: upload_to_play_store action](https://docs.fastlane.tools/actions/upload_to_play_store/)
- [Google Play Console: Service accounts](https://developers.google.com/android-publisher/getting_started)
- @../.claude/memory/deployment.md — Keystore + AAB 배포 절차
- @./Fastfile — lane 정의
- @./Appfile — package_name + json_key_file 경로
