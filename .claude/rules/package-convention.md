---
name: Package Convention Rule
description: Android package name 엄수 규칙. 오타 절대 금지.
type: rule
---

# Package Convention (절대 규칙)

## 결정된 Package Name

- **Production**: `th1ngjin.fearindex`
- **Debug**: `th1ngjin.fearindex.debug`

## 금지어

다음 표기는 **절대 사용 금지**:

- ❌ `com.thingineer.fearindex` (e 2개) — 과거 오타, Firebase/Play Console에서 삭제 완료
- ❌ `com.thingineeer.fearindex` (e 3개) — 이전 Play Console 오타 앱, 삭제 대기 중
- ❌ `th1ngjin.FearIndex-Android` — iOS처럼 대문자 섞는 건 Android 관례 위반
- ❌ `com.th1ngjin.fearindex` — `com.` 접두사 불필요 (iOS `th1ngjin.FearIndex-iOS`와 비대칭)

## 이유

사용자의 iOS/macOS 앱 식별자는 `th1ngjin.FearIndex-iOS`, `th1ngjin.FearIndex-macOS`. **대시보드/Analytics/Crashlytics에서 같이 비교되므로** Android도 `th1ngjin` 접두사 공유.

Android package는 소문자 관습(`java.lang.Package` 네이밍 룰과 일치)이라 `fearindex`는 모두 소문자.

## 영향 범위

Package name 변경 시 건드려야 하는 곳:

1. **5개 모듈 `build.gradle.kts`의 `namespace`** (app/core/data/presentation 4개 Android 모듈)
2. **`app/build.gradle.kts`의 `applicationId` + `applicationIdSuffix`**
3. **모든 `*.kt` 파일의 `package ...` 선언**
4. **모든 `*.kt` 파일의 `import ...` 선언**
5. **디렉토리 구조** `src/main/java/th1ngjin/fearindex/...`
6. **`app/src/main/res/xml/gma_ad_services_config.xml`** — AdMob 네트워크 구성
7. **Firebase 등록 package** + `app/google-services.json` 재다운로드
8. **Play Console** 신규 앱 (한 번 만들면 package name 변경 불가)
9. **AdMob** 앱 연결 (package name 교체)
10. **`settings.gradle.kts`** — rootProject.name 은 무관 (프로젝트 디렉토리 이름)

## 재발 방지 — Pre-edit Hook (선택)

`.claude/settings.local.json`의 `PreToolUse`에 등록 가능:

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "jq -r '.params.file_path // \"\"' | grep -qE 'com/thingineer|com/thingineeer' && echo '❌ 오류: 구 package 경로입니다. th1ngjin/fearindex/ 사용' >&2 && exit 2 || exit 0"
          }
        ]
      }
    ]
  }
}
```

## 검증 명령

```bash
# 구 package 참조가 소스 코드에 남아있는지 확인 (결과는 비어있어야 함)
cd FearIndex-Android   # 레포 루트
grep -rE "com\.thingineer\.fearindex|com\.thingineeer\.fearindex" \
  --include="*.kt" --include="*.kts" --include="*.xml" . \
  | grep -v "/build/"
```

## 관련 문서

- @../memory/bugs-fixed.md 4번 — 오타 발견 및 재정비 이력
- @../memory/ios-parity.md — iOS/macOS와 대칭성 이유
- @../memory/firebase-setup.md — Firebase 등록 방법
