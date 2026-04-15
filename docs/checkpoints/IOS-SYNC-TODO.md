# iOS Sync TODO — v1.0.1 Android 작업에서 iOS가 반영해야 할 항목

이 문서는 Android v1.0.1 작업 중 발견/결정된 iOS 쪽 동반 수정 항목입니다.
iOS 다음 릴리즈(v1.7.8 또는 v1.7.9)에서 순서대로 반영하세요.

---

## 1. Firebase Analytics UserProperty 명시 설정 (중요, 블로커)

**상황**: iOS/Android가 같은 Firebase 프로젝트(`fear-index-a4f4b`)를 공유. 플랫폼 분리를 위한
UserProperty를 심지 않으면 Firebase Console 대시보드/Funnel/Audience에서 iOS와 Android가
섞여서 집계된다.

**Android 반영**: `FearIndexApp.onCreate()`에서
`AnalyticsManager.setStandardUserProperties(appVersion, buildType, language)` 호출 →
`platform="android"`, `app_version`, `build_type`, `language` 설정.

**iOS 해야 할 일**:
- `LocalPackages/Core/Sources/Core/Analytics/AnalyticsManager.swift`에
  `setStandardUserProperties(appVersion:buildType:language:)` 함수 추가.
- 설정 키:
  - `platform = "ios"`
  - `app_version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString")`
  - `build_type = DEBUG ? "debug" : "release"`
  - `language = Locale.current.identifier`
- `FearIndexApp.swift` 또는 `AppDelegate.application(_:didFinishLaunchingWithOptions:)`에서
  `FirebaseApp.configure()` 직후 호출.

**검증**: Firebase Console → Analytics → User properties 탭에서 `platform` 필터로 iOS/Android 분리 확인.

---

## 2. Firebase Crashlytics custom key `platform`

**Android 반영**: `CrashReporter.setStandardKeys()`로 동일 4개 key 설정.

**iOS 해야 할 일**:
- Crashlytics에 iOS 앱 연결 + `Crashlytics.crashlytics().setCustomValue("ios", forKey: "platform")` 설정.
- 현재 iOS는 Crashlytics 의존성이 없는 것으로 확인됨 → 추가 필요.

---

## 3. Firebase Remote Config 키 합의

**Android**가 정의한 키:
- `ads_enabled` (Boolean, default=true)
- `vote_enabled` (Boolean, default=true)

**iOS 해야 할 일**:
- iOS `RemoteConfigManager` 클래스에 **동일한 키**를 사용하는지 확인/추가.
- Firebase Console → Remote Config에서 두 키를 등록하고, 플랫폼 분기가 필요하면
  UserProperty `platform` 기반 조건부 값 설정.

---

## 4. 공유 URL 스킴 (v1.0.1 작업 중)

**Android**가 생성할 URL 포맷: `https://fear-index-a4f4b.web.app/?score={n}&type={market|crypto}&rating={rating_key}`

**iOS 해야 할 일**:
- `ShareSheetView.swift`에서 `shareURL` 파라미터에 **동일한 URL 포맷** 생성 로직 반영.
- Firebase Hosting의 OG 태그가 `score`/`type`/`rating` query param으로 동적 렌더링되는지 확인.

---

## 5. Vote Firestore 구조 합의 (v1.0.1 Android에서 신규 추가 예정)

Android가 Buy/Hold/Sell 투표를 구현하면서 Firestore 구조를 정의합니다. iOS는 이미 구현되어
있는 것으로 추정되나, **구조를 비교해 불일치 시 iOS 기준으로 Android 맞춤** (iOS가 원본 원칙).

예상 경로:
- `votes/{weekId}/choices/{deviceId}` — {choice, timestamp}
- `votes/{weekId}/aggregate` — {buyCount, holdCount, sellCount, totalCount}

**iOS 해야 할 일**: 현재 iOS Firestore 경로와 필드명을 확인 후 Android 팀에 공유 →
Android가 동일하게 구현.

---

## 검증 명령

```bash
# Firebase Console에서 iOS/Android 이벤트 비교
# Analytics → DebugView (실시간) 또는 Events (1일 지연)에서
# 같은 이벤트 이름(앱시작, 탭선택 등)이 양 플랫폼에서 나와야 함

# Remote Config
# Firebase Console → Remote Config → 키 리스트 비교
```

---

## 관련 문서

- Android: `.claude/memory/ios-parity.md`
- Android: `.claude/memory/firebase-setup.md`
- iOS: (있다면) iOS parity 문서
