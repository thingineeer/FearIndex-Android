---
name: iOS Parity Rule
description: 모든 변경은 iOS/macOS 프로젝트와 일관성 유지. 대시보드 공유의 최소 원칙.
type: rule
---

# iOS Parity (강제 규칙)

## 원칙

FearIndex는 **iOS/macOS/Android 3플랫폼 단일 프로덕트**. Firebase 프로젝트/Analytics/Crashlytics/Firestore/Functions 모두 공유.

따라서 **어느 플랫폼에서든 변경 시 다른 플랫폼도 일관성 유지**가 의무.

## 필수 동기화 항목

### 데이터 계약
- **Firestore 컬렉션/필드 이름**: 한쪽이 바뀌면 다른 쪽도 동일하게.
- **Firebase Functions 시그니처** (`submitStuckStatus` 등): request/response 형식 변경 시 양쪽 클라이언트 모두 수정.
- **Firestore Security Rules**: iOS 팀이 수정해도 Android 영향. 수정 시 양쪽 팀 알림.

### 사용자 문자열
- **Rating 5단계** (`rating.extremeFear` 등 45개 locale): iOS `Localizable.strings` ↔ Android `strings.xml` **완전 동일**.
- 번역이 바뀌면 iOS → Android 순서로 반영 (iOS가 원본).

### Analytics 이벤트
- **이벤트 이름**: 완전 동일 (예: `home_tab_opened`). 이름 다르면 Firebase Analytics에서 플랫폼 비교 불가.
- **이벤트 파라미터 키**: 동일.

### 기능 대칭
- iOS에 A 피처가 있으면 Android에도 동등 피처 구현 (또는 명시적으로 "Android 제외" 결정 문서화).
- 버전 번호 정책 통일: iOS `v1.7.8`이면 Android도 `v1.7.8` 시리즈.

## 동기화 불필요 항목

### 플랫폼별 구현 디테일
- iOS: SwiftUI NavigationStack, Actor
- Android: Compose Navigation, Hilt, Flow
- 같은 화면이어도 코드는 다름. **UX/결과물만 동일**하면 OK.

### 패키징 / 배포
- iOS: App Store Connect, TestFlight, dSYM
- Android: Play Console, 내부 테스트, ProGuard mapping
- 배포 주기도 심사 지연으로 어긋나는 건 허용.

### 플랫폼 특화 API
- Push: iOS APNs vs Android FCM (Firebase가 추상화)
- App Check: iOS App Attest vs Android Play Integrity
- 광고: AdMob 공유하지만 단위 ID는 플랫폼별 별도 발급

## 변경 시 체크리스트

iOS에서 X 변경 → Android 개발자에게 알림 + 아래 체크:

- [ ] Firestore 스키마 변경? → @../memory/firebase-setup.md 업데이트 + Android DTO 수정
- [ ] Functions 시그니처 변경? → Android 호출부 + 응답 파싱 수정
- [ ] 신규 화면? → Android Navigation + UI 구현
- [ ] 신규 Analytics 이벤트? → Android 에서도 동일 이름으로 로그
- [ ] Rating 문자열 변경? → 45개 locale 전부 동기화
- [ ] 버전 업? → Android versionName/Code 업데이트

## 검증 도구

### Rating 문자열 비교

```bash
# iOS 원본
grep -h "^\"rating\." FearIndex-iOS/FearIndex-iOS/Resources/ko.lproj/Localizable.strings

# Android 번역
grep "rating_" FearIndex-Android/presentation/src/main/res/values-ko/strings.xml
```

값이 같은지 육안 비교. 다르면 iOS 기준으로 Android 수정.

### Firestore 규칙 동기화

```bash
diff FearIndex-iOS/firebase-functions/firestore.rules \
     FearIndex-Android/firebase-functions/firestore.rules
# 아무것도 출력 안되면 동기화 OK (Android 디렉토리가 없다면 문제없음 - iOS가 원본)
```

## 관련 문서

- @../memory/ios-parity.md — 상세 매핑 테이블
- @../memory/firebase-setup.md — 공유 Firebase 구조
- @../memory/bugs-fixed.md 3번 — 다국어 불일치 해결 이력
