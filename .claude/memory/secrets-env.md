# Secrets — `~/thingineeer-env/projects/fearindex-android/.env`

GitHub private repo `thingineeer/thingineeer-env` 에 저장된 텍스트 토큰/상수 모음.
**여러 머신에서 `gh repo clone` 한 번으로 동일하게 재현** 가능 (AirDrop/이메일 불필요).

## 파일 위치

```
~/thingineeer-env/projects/fearindex-android/.env
```

## 저장된 키 목록

### Firebase
| 키 | 값 | 용도 |
|---|---|---|
| `FIREBASE_PROJECT_ID` | `fear-index-a4f4b` | Firebase 프로젝트 식별자 |
| `FIREBASE_PROJECT_NUMBER` | `8243517543` | Cloud API 프로젝트 번호 |
| `FIREBASE_REGION` | `asia-northeast3` | Functions 리전 (서울) |

### Android Package
| 키 | 값 |
|---|---|
| `ANDROID_PACKAGE_PROD` | `th1ngjin.fearindex` |
| `ANDROID_PACKAGE_DEBUG` | `th1ngjin.fearindex.debug` |

### AdMob
| 키 | 값 |
|---|---|
| `ADMOB_APP_ID` | `ca-app-pub-5283496525222246~1308884877` |
| `ADMOB_HOME_BANNER` | `ca-app-pub-5283496525222246/3189551565` |
| `ADMOB_TEST_BANNER` | `ca-app-pub-3940256099942544/9214589741` (Google 공식 테스트) |

### App Check Debug Tokens (머신별)
Firebase Console → App Check → 공포지수 Android Debug → 디버그 토큰 관리에서 발급.

| 키 | 값 | 머신 |
|---|---|---|
| `APPCHECK_DEBUG_TOKEN_MACBOOK2_EMULATOR` | `4c6f7b8f-86b9-4144-b67e-18f1ac072e2a` | MacBook-2, Medium_Phone_API_36 AVD |
| `APPCHECK_DEBUG_TOKEN_MACBOOK1_EMULATOR` | `f7fe2893-4eec-4b98-8d7e-301cfa31651d` | MacBook-1, Pixel_3a AVD |

**새 머신/에뮬레이터에서 추가 방법**:
1. 에뮬레이터 부팅 → 앱 실행
2. `adb logcat | grep "Enter this debug secret"` 로 새 토큰 확인
3. Firebase Console에 등록
4. `~/thingineeer-env/projects/fearindex-android/.env`에 `APPCHECK_DEBUG_TOKEN_<머신명>=<토큰>` 라인 추가
5. `cd ~/thingineeer-env && git add -A && git commit -m "add: <머신명> App Check debug token" && git push`

### Release Keystore
**진짜 활성 keystore 의 SSOT 는 `~/thingineeer-env/android/fearindex/fearindex-release.keystore`**.
`~/fearindex-secrets/fearindex-release.keystore` 는 install.sh 가 복사한 결과물 (== 동일 파일).
`.env` 에는 공개 가능한 지문만.

| 키 | 값 |
|---|---|
| `KEYSTORE_ALIAS` | `upload` (v1.0.3+ 활성) |
| `KEYSTORE_SHA1` | `CE:08:B4:8A:...` (v1.0.3+ 활성, Play Console 등록 키) |
| `KEYSTORE_SHA256` | `91:47:9A:4E:...` |
| `KEYSTORE_SHA1_V101` | `81:AD:9D:5D:...` (v1.0.1~v1.0.2, 폐기) |
| `KEYSTORE_SHA256_V101` | `15:8F:BB:0F:...` (v1.0.1~v1.0.2, 폐기) |
| `KEYSTORE_SHA1_V100` | `A1:54:8A:92:...` (v1.0.0, 폐기) |
| `KEYSTORE_SHA256_V100` | `AD:48:68:DA:...` (v1.0.0, 폐기) |

> 2026-05-09 사고 이력: `~/fearindex-secrets/` 옛 keystore 와 thingineeer-env 진짜 활성 키가 평행 존재해 v1.0.3 AAB 업로드 거부. 자세한 내용은 `@bugs-fixed.md` 17번.

### 콘솔 계정
| 키 | 값 |
|---|---|
| `PLAY_CONSOLE_ACCOUNT` | `dlaudwls1203@gmail.com` |
| `FIREBASE_CONSOLE_ACCOUNT` | `dlaudwls1203@gmail.com` |

### Cloud Functions (참고)
| 키 | 용도 |
|---|---|
| `FN_SUBMIT_STUCK_STATUS` | 물림 상태 제출 |
| `FN_GET_STUCK_COUNT` | 물림 카운트 조회 |
| `FN_REGISTER_FCM_TOKEN` | FCM 토큰 등록 |
| `FN_UPDATE_NOTIFICATION_SETTINGS` | 알림 설정 업데이트 |
| `FN_UNREGISTER_DEVICE` | 디바이스 해제 |
| `FN_GET_SIMILAR_EVENTS` | 유사 이벤트 조회 |

## 새 맥 셋업 (한 번만)

```bash
# 1. GitHub 로그인 (한 번만)
gh auth login

# 2. vault clone
gh repo clone thingineeer/thingineeer-env ~/thingineeer-env

# 3. keystore + gradle.properties + google-services.json 설치 (signing 자산)
bash ~/thingineeer-env/android/fearindex/install.sh

# 4. 확인
cat ~/thingineeer-env/projects/fearindex-android/.env
ls -la ~/fearindex-secrets/
keytool -list -v -keystore ~/fearindex-secrets/fearindex-release.keystore -alias upload \
  | grep "SHA1:"
# → CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16 출력되어야 함
```

## 머신 간 keystore sync (절대 규칙)

**SSOT (single source of truth)**: `~/thingineeer-env/android/fearindex/fearindex-release.keystore`
- 옛 절차 (AirDrop 으로 `~/fearindex-secrets/` 복사) 는 **deprecated** — 머신별로 옛 keystore 가 남아 사고 발생 (2026-05-09)
- 새 머신/세션 시작 시 항상 `gh repo clone thingineeer/thingineeer-env` + `bash install.sh` 부터
- keystore 변경/재설정이 발생하면:
  1. `~/thingineeer-env/android/fearindex/` 안 keystore 교체
  2. `cd ~/thingineeer-env && git add -A && git commit && git push`
  3. 다른 머신에서 `git pull && bash ~/thingineeer-env/android/fearindex/install.sh`
  4. `.env` 의 `KEYSTORE_SHA1` / `KEYSTORE_SHA256` 동시 갱신, 옛 값은 `_V<version>` suffix 로 보존

## 자주 쓰는 명령어

```bash
# 모든 값 보기
cat ~/thingineeer-env/projects/fearindex-android/.env

# 특정 키 찾기
grep APPCHECK ~/thingineeer-env/projects/fearindex-android/.env
grep KEYSTORE ~/thingineeer-env/projects/fearindex-android/.env

# 값 업데이트
vi ~/thingineeer-env/projects/fearindex-android/.env
cd ~/thingineeer-env && git add -A && git commit -m "update: <변경내용>" && git push
```

## 관련 문서

- @../../CLAUDE.md — Release Signing & Secrets 섹션
- @../rules/secrets.md — 파일 기반 비밀 (`~/fearindex-secrets/`)
- @firebase-setup.md — Firebase 상세 구조
- @deployment.md — keystore/AAB 배포 절차
