# Google Play Console 내부 테스트 배포 체크리스트

FearIndex Android 앱을 처음으로 Google Play Console 내부 테스트 트랙에 올리기 위한 단계별 체크리스트입니다.
순서대로 따라가면 됩니다. 보안상 민감한 작업(Google 계정 로그인, 비밀번호 입력, 키스토어 생성)은 모두 사용자가 직접 수행합니다.

- 대상 패키지: `com.thingineer.fearindex` (release), `com.thingineer.fearindex.debug` (debug)
- 목표 트랙: **Internal testing (내부 테스트)**

---

## 1. 선행 조건 체크

작업을 시작하기 전에 아래 항목이 모두 준비되어 있는지 확인합니다.

- [ ] **Google Play Console 개발자 계정 등록 완료**
  - 신규 등록 비용: **25 USD (1회성)**
  - 등록 페이지: https://play.google.com/console/signup
  - 개인 계정의 경우 신원 확인 절차에 1~3일 소요될 수 있음
- [ ] **macOS에 Java(JDK) 설치 — `keytool` 명령 사용 가능 여부 확인**
  ```bash
  keytool -help | head -n 1
  # → "키 및 인증서 관리 도구" 또는 "Key and Certificate Management Tool" 출력되면 OK
  ```
  설치되어 있지 않다면 Android Studio가 번들로 가지고 있는 JDK를 사용하거나 `brew install --cask temurin` 으로 설치합니다.
- [ ] **Android Studio 최신 안정 버전 설치**
  - 다운로드: https://developer.android.com/studio
- [ ] **앱이 release 빌드로 정상 동작하는지 디바이스에서 확인**
  - 최소 1회 release variant 실행해 크래시/네트워크 이상 없는지 점검

---

## 2. Release Signing Key 생성

Google Play Console에 업로드할 AAB는 반드시 release 키스토어로 서명되어야 합니다.

### 2.1 키스토어 생성 명령

원하는 디렉토리(예: `~/keystores/`)에서 아래 명령을 실행합니다.

```bash
mkdir -p ~/keystores
cd ~/keystores

keytool -genkey -v \
  -keystore fearindex-release.keystore \
  -alias fearindex \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

명령 실행 시 아래 정보를 입력하라는 프롬프트가 뜹니다.

| 항목 | 권장 입력 |
|------|-----------|
| 키스토어 비밀번호 (store password) | **강력한 비밀번호** (최소 16자, 1Password 생성 권장) |
| 키 비밀번호 (key password) | 키스토어 비밀번호와 **다르게** 설정 권장 |
| 이름 (CN) | 본인 이름 또는 회사명 |
| 조직 단위 (OU) | `Mobile` 등 |
| 조직명 (O) | 본인 또는 회사명 |
| 도시 (L) / 시도 (ST) / 국가 (C) | 예: `Seoul` / `Seoul` / `KR` |

### 2.2 비밀번호 및 키스토어 보관 — 매우 중요

- [ ] 키스토어 **비밀번호 2개**(store, key)와 **alias 이름**을 1Password 등 비밀번호 매니저에 즉시 저장
- [ ] 키스토어 파일(`fearindex-release.keystore`)을 **2곳 이상**에 백업
  - 권장: iCloud Drive 개인 폴더 + 외장 SSD/USB
  - **절대 git 저장소에 커밋 금지**
- [ ] 프로젝트 루트 `.gitignore`에 다음 항목이 포함되어 있는지 확인

```gitignore
# Signing
*.keystore
*.jks
keystore.properties
```

> **경고**: 키스토어를 분실하거나 비밀번호를 잊으면 **해당 앱을 영원히 업데이트할 수 없습니다.** 새 패키지명으로 처음부터 다시 게시해야 합니다. Play App Signing(아래 5번 단계)을 활성화하면 일부 복구 경로가 열리지만, **업로드 키 자체는 사용자가 직접 보관해야 합니다.**

---

## 3. build.gradle.kts 설정

### 3.1 비밀번호를 코드 외부로 분리

키스토어 비밀번호를 `build.gradle.kts`에 직접 적지 말고, **글로벌 Gradle properties** 파일에 보관합니다.

`~/.gradle/gradle.properties` (없으면 생성)에 아래 추가:

```properties
FEARINDEX_RELEASE_STORE_FILE=/Users/<your-username>/keystores/fearindex-release.keystore
FEARINDEX_RELEASE_STORE_PASSWORD=<store-password>
FEARINDEX_RELEASE_KEY_ALIAS=fearindex
FEARINDEX_RELEASE_KEY_PASSWORD=<key-password>
```

> 이 파일은 사용자 홈 디렉토리에 있으므로 git에 들어가지 않습니다. 다만 다른 머신에서 작업할 때마다 직접 다시 작성해야 합니다.

### 3.2 `app/build.gradle.kts` 수정

`android { ... }` 블록 안에 `signingConfigs`와 `buildTypes.release`를 다음과 같이 구성합니다.

```kotlin
android {
    // ... namespace, compileSdk 등 기존 설정 유지

    signingConfigs {
        create("release") {
            val storeFilePath = (project.findProperty("FEARINDEX_RELEASE_STORE_FILE") as String?) ?: ""
            if (storeFilePath.isNotEmpty()) {
                storeFile = file(storeFilePath)
                storePassword = project.findProperty("FEARINDEX_RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("FEARINDEX_RELEASE_KEY_ALIAS") as String?
                keyPassword = project.findProperty("FEARINDEX_RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3.3 설정 검증

```bash
./gradlew :app:signingReport
```

- `Variant: release`의 `Store: ...keystore` 경로와 `Alias: fearindex`가 정상 출력되는지 확인
- `Variant: debug`는 기존 `~/.android/debug.keystore`를 사용해야 정상

---

## 4. AAB(Android App Bundle) 빌드

### 4.1 빌드 실행

```bash
cd /Users/imyeongjin/Desktop/side/FearIndex-Android
./gradlew clean :app:bundleRelease
```

### 4.2 출력 확인

빌드 성공 시 산출물 위치:

```
app/build/outputs/bundle/release/app-release.aab
```

파일 크기와 서명 검증:

```bash
ls -lh app/build/outputs/bundle/release/app-release.aab

# bundletool로 서명 검증 (선택, 미설치 시 건너뜀)
# brew install bundletool
bundletool dump manifest --bundle=app/build/outputs/bundle/release/app-release.aab | head -n 20
```

### 4.3 사전 점검 체크리스트

- [ ] `applicationId`가 `com.thingineer.fearindex` 인지 (`app/build.gradle.kts`)
- [ ] `versionCode`가 정수, `versionName`이 SemVer 형태인지
- [ ] minSdk / targetSdk가 Play 정책에 부합 (2026년 기준 targetSdk 34 이상 필수)
- [ ] R8/ProGuard 규칙으로 인해 release에서 크래시가 나지 않는지 — 디바이스 설치 테스트 권장
  ```bash
  ./gradlew :app:installRelease
  ```

---

## 5. Google Play Console 내부 테스트 트랙 설정 (수동)

이 섹션은 **사용자가 브라우저에서 직접 수행**합니다.

### 5.1 새 앱 만들기

1. https://play.google.com/console 접속 후 로그인
2. 좌측 상단 **모든 앱(All apps) → 앱 만들기(Create app)** 클릭
3. 입력 항목:
   - **앱 이름(App name)**: `Fear & Greed Index`
   - **기본 언어(Default language)**: `한국어 - ko-KR`
   - **앱 또는 게임**: `앱(App)`
   - **무료 또는 유료**: `무료(Free)`
4. 정책 동의 체크박스 모두 확인 후 **앱 만들기**

### 5.2 필수 선언 사항 작성

대시보드의 "앱 설정(App setup)" 섹션에서 다음을 모두 완료해야 트랙 출시가 가능합니다.

- [ ] 앱 액세스(App access) — 로그인 필요 여부
- [ ] 광고(Ads) — AdMob 사용 여부
- [ ] 콘텐츠 등급(Content rating) — 설문 진행
- [ ] 타겟층 및 콘텐츠(Target audience) — 13+ 등 선택
- [ ] 뉴스 앱 여부
- [ ] COVID-19 추적 앱 여부
- [ ] 데이터 보안(Data safety) — 수집 데이터 항목 선언
- [ ] 정부 앱 여부
- [ ] 금융 상품 및 서비스 여부 — **공포지수는 정보성이지만 금융 카테고리에 속하므로 정확히 선언**
- [ ] 개인정보처리방침 URL 등록 (FearIndex 웹사이트의 privacy 페이지)

### 5.3 내부 테스트 트랙 생성 및 AAB 업로드

1. 좌측 메뉴: **테스트(Testing) → 내부 테스트(Internal testing)**
2. **새 버전 만들기(Create new release)** 클릭
3. **Play 앱 서명(Play App Signing)**: **사용 설정(권장)** — Google이 앱 서명 키를 안전하게 관리, 사용자는 업로드 키만 보관
4. **App bundles** 영역에 `app-release.aab` 드래그앤드롭 업로드
5. 업로드 완료 후 자동 분석 결과 확인 (경고가 있으면 모두 검토)
6. **출시명(Release name)**: 자동 채워지는 `1 (1.0.0)` 그대로 두거나 수정
7. **출시 노트(Release notes)** — 언어별 입력:
   ```xml
   <ko-KR>
   내부 테스트 첫 버전입니다.
   - 공포지수 표시 기능
   </ko-KR>
   <en-US>
   First internal test build.
   - Fear &amp; Greed Index display
   </en-US>
   ```
8. **다음(Next) → 저장(Save) → 검토(Review release)**
9. **내부 테스트로 출시 시작(Start rollout to Internal testing)** 클릭

### 5.4 테스터 그룹 설정

1. **내부 테스트 → 테스터(Testers) 탭**
2. **이메일 목록 만들기(Create email list)**
3. 입력:
   - 목록 이름: `Internal Testers`
   - 이메일 주소: 테스터들의 **Google 계정 이메일** 한 줄에 하나씩 (예: `tester1@gmail.com`)
4. **저장(Save changes)**
5. 만든 목록을 체크하여 트랙에 연결

---

## 6. 테스터 초대 링크 공유

1. **내부 테스트 → 테스터** 탭 하단의 **참여 URL 복사(Copy link)**
2. 테스터에게 다음 안내 메시지와 함께 전달:

```
[FearIndex Android 내부 테스트 안내]

1. 아래 링크를 Android 기기의 Chrome으로 엽니다.
   <참여 URL>
2. "테스터 되기(Become a tester)" 버튼을 누릅니다.
3. 같은 페이지의 "Google Play에서 다운로드" 링크를 누르면 Play Store 앱이 열립니다.
4. 설치 후 피드백을 카카오톡으로 전달해 주세요.

주의:
- Play Console에 등록된 Google 계정으로 Play Store에 로그인되어 있어야 합니다.
- 트랙에 추가된 직후에는 Play Store 반영까지 최대 수 시간이 걸릴 수 있습니다.
```

---

## 7. 주의사항 & 자주 겪는 실수

### 7.1 첫 출시 시 자주 발생하는 이슈

- **Play Console 신규 앱 검토 지연**: 첫 내부 테스트 트랙 출시도 **2~3시간**에서 길게는 1~2일 걸릴 수 있음. 즉시 다운로드되지 않아도 정상.
- **테스터 페이지가 "앱을 사용할 수 없습니다"로 표시**: 트랙이 아직 처리 중이거나, 테스터 계정이 트랙에 연결되지 않은 경우. 이메일 목록 저장 후 재확인.
- **`applicationId`(패키지명)는 1회 확정 후 변경 불가**: 잘못 등록 시 새 앱을 처음부터 다시 만들어야 함. 업로드 전 반드시 재확인.
- **업로드 키 vs Play App Signing 키 혼동**:
  - **업로드 키(Upload Key)**: 사용자가 만든 `fearindex-release.keystore`. AAB 서명용.
  - **앱 서명 키(App Signing Key)**: Google이 보관. 최종 사용자 기기에 설치되는 APK에 적용됨.
  - 두 키는 다름. SHA-1을 외부 서비스(Firebase, OAuth)에 등록할 때 어떤 SHA를 요구하는지 정확히 구분.

### 7.2 보안 관련

- 키스토어 파일과 비밀번호가 담긴 `~/.gradle/gradle.properties` 모두 git에 절대 들어가지 않도록 확인
- 팀이 늘어나면 키스토어를 1Password Vault나 GCP Secret Manager 등으로 공유 (이메일/슬랙 첨부 금지)
- CI 환경에서는 base64 인코딩된 키스토어를 환경 변수로 주입하는 방식 권장

### 7.3 정책 관련

- targetSdk 34 미만이면 **신규 앱 게시 불가** (2026년 기준)
- 데이터 보안 양식의 선언과 실제 앱 동작이 다르면 정책 위반 → 추후 프로덕션 출시 시 거부될 수 있음
- 64비트 지원 필수 — `arm64-v8a` ABI가 AAB에 포함되어 있는지 확인

---

## 8. 후속 배포 워크플로우

내부 테스트 트랙이 정상 동작하면, 이후 배포는 Fastlane으로 자동화합니다.

### 8.1 버전 올리기

`app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2          // 빌드마다 +1
    versionName = "1.0.1"    // SemVer
}
```

### 8.2 Fastlane으로 자동 업로드 (사전 세팅 필요)

```bash
cd /Users/imyeongjin/Desktop/side/FearIndex-Android
bundle exec fastlane android internal
```

> Fastlane lane이 아직 없다면 별도 작업으로 `fastlane/Fastfile`에 `internal` lane을 추가해야 합니다. Service Account JSON 키 발급 방법은 https://docs.fastlane.tools/actions/upload_to_play_store/ 참조.

### 8.3 트랙 승격(Promote)

내부 테스트에서 검증이 끝나면 Play Console에서 동일 빌드를 **비공개(Closed) → 공개(Open) → 프로덕션(Production)** 트랙으로 승격할 수 있습니다.

```
Internal → Closed (Alpha) → Open (Beta) → Production
```

각 단계에서 새 AAB를 다시 빌드할 필요 없이 동일 버전을 승격하면 됩니다.

---

## 참고 자료

- Google Play Console 공식 문서: https://support.google.com/googleplay/android-developer
- Android 앱 서명 가이드: https://developer.android.com/studio/publish/app-signing
- Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756
- Fastlane Supply (Google Play 자동 업로드): https://docs.fastlane.tools/actions/upload_to_play_store/
