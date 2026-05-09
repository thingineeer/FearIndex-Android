# 태블릿 스크린샷 자동화 (7"/10")

`fastlane/metadata/android/<locale>/images/sevenInchScreenshots/` 와 `tenInchScreenshots/` 슬롯 자동 캡처.

## 1. 사전 조건

### 1-A. 태블릿 AVD 생성 (Android Studio)

**Android Studio → Device Manager → Create Device**

#### 7인치 태블릿
- Hardware: **Nexus 7** 또는 **Nexus 7 (2012)**
- Resolution: 1024×600 또는 800×1280
- Density: 213 dpi (tvdpi)
- API: 34 (현 targetSdk)
- Device name 권장: `Pixel_Tablet_7inch_API_34`

#### 10인치 태블릿
- Hardware: **Pixel Tablet** 또는 **Nexus 9** / **Nexus 10**
- Resolution: 1600×2560 또는 2560×1600
- Density: 320 dpi (xhdpi)
- API: 34
- Device name 권장: `Pixel_Tablet_10inch_API_34`

### 1-B. 디스크 공간 확보

태블릿 AVD 1개 = ~4GB. 두 개 동시 부팅 어려우면 한 번에 하나씩.

```bash
df -h /
# 최소 10GB free 권장
```

## 2. 사용법

### 2-A. 7" 태블릿

```bash
# AVD 부팅
$HOME/Library/Android/sdk/emulator/emulator -avd Pixel_Tablet_7inch_API_34 -no-snapshot-load &

# 부팅 대기
$HOME/Library/Android/sdk/platform-tools/adb wait-for-device
until [ "$($HOME/Library/Android/sdk/platform-tools/adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 3; done

# 앱 install
./gradlew :app:installRelease   # 또는 :app:installDebug

# 스크린샷 캡처 (45 locale × 5 = 225장)
bash scripts/screenshots/capture-tablet-all-locales.sh seven
```

### 2-B. 10" 태블릿

```bash
# 7" AVD 종료 후 10" 부팅
adb emu kill
$HOME/Library/Android/sdk/emulator/emulator -avd Pixel_Tablet_10inch_API_34 -no-snapshot-load &
# ... 부팅 대기 + install ...
bash scripts/screenshots/capture-tablet-all-locales.sh ten
```

## 3. 산출물 검증

```bash
# 7" 태블릿 5장 × 45 locale = 225장
find fastlane/metadata/android -path "*/sevenInchScreenshots/*.png" | wc -l

# 10" 태블릿 동일
find fastlane/metadata/android -path "*/tenInchScreenshots/*.png" | wc -l

# locale 별 샘플 확인
ls fastlane/metadata/android/ko_KR/images/sevenInchScreenshots/
ls fastlane/metadata/android/en_US/images/tenInchScreenshots/
```

## 4. Play Console 업로드

### 4-A. fastlane supply 자동 (권장)

```bash
# 24h 후 service account 권한 전파 끝나면
bash scripts/deploy/check-fastlane-ready.sh   # ✓ 통과 확인
bash scripts/deploy/upload-metadata-all-locales.sh   # 45 locale 메타+휴대전화+태블릿 일괄 sync
```

`Fastfile` 의 `upload_metadata` lane 이 `skip_upload_screenshots: false` 라 모든 size 자동 업로드.

### 4-B. Manual (각 locale 별)

Play Console → Main store listing → locale 선택 → 7인치/10인치 태블릿 스크린샷 영역 → "애셋 추가" → 5장 업로드 → 저장.

45 locale × 2 size = 90회 반복 — **fastlane supply 자동화 권장**.

## 5. 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| ANR 다이얼로그 누적 | 연속 locale 전환 부하 | 스크립트가 `hide_error_dialogs 1` 자동 set |
| 캡처 실패 (PNG 0byte) | adb pull 권한 또는 sdcard 부족 | `adb shell df /sdcard` 확인, 재실행 |
| locale 전환 안됨 | API 33 미만 | API 34+ AVD 사용 |
| 화면 일부만 캡처 | 화면 회전 (가로/세로) 다름 | `adb shell settings put system user_rotation 0` 세로 고정 |
| 알림 설정 화면 못 찾음 | Settings 메뉴 좌표 다름 | 스크립트의 `T4X` (설정 탭 좌표) 와 알림 메뉴 y 비율 조정 |

## 6. 관련 문서

- @../fastlane/SUPPLY-SETUP.md — fastlane 자동화
- @./V103-MANUAL-RELEASE.md — manual 출시 절차
- @../scripts/screenshots/capture-all-locales-v103.sh — 휴대전화 (참조)
- Play Console 태블릿 정책: https://support.google.com/googleplay/android-developer/answer/9866151
