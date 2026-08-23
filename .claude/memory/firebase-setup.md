---
name: Firebase Setup
description: Firebase 프로젝트 구조, google-services.json, Functions, Firestore, App Check.
type: reference
---

# Firebase Setup

## 프로젝트

- **Project ID**: `fear-index-a4f4b`
- **Project Number**: `8243517543`
- **Blaze 요금제**: 사용 중 (Functions 배포용)
- **Region**: `asia-northeast3` (서울)

## 등록된 앱 (2026-04-15 기준)

| 플랫폼 | 패키지/Bundle ID | 앱 닉네임 |
|---|---|---|
| Android | `th1ngjin.fearindex` | 공포지수 Android |
| Android | `th1ngjin.fearindex.debug` | 공포지수 Android Debug |
| iOS | `th1ngjin.FearIndex-iOS` | 공포지수앱 |
| iOS (macOS target) | `th1ngjin.FearIndex-macOS` | 공포지수 macOS |

**삭제 대기 중 (30일 후 영구 삭제)**:
- `th1ngjin.FearIndexSkip` (무관 프로젝트)
- `com.thingineer.fearindex` (e2 오타)
- `com.thingineer.fearindex.debug` (e2 오타)

## `google-services.json`

- **위치**: `app/google-services.json`
- **gitignore**: 커밋 금지 (`/.gitignore`에 이미 등록)
- **재다운로드**: Firebase Console → 프로젝트 설정 → Android 앱 중 아무거나 선택 → "google-services.json 다운로드" 클릭. 프로젝트의 **모든 Android 앱 정보가 한 파일에 담김**.
- **검증**:
  ```bash
  grep package_name app/google-services.json
  # th1ngjin.fearindex
  # th1ngjin.fearindex.debug
  ```

## Firebase Functions

iOS/Android 공유 백엔드.

- **Source**: `FearIndex-iOS/firebase-functions/` (iOS 프로젝트 내 관리)
- **배포 명령**:
  ```bash
  cd FearIndex-iOS/firebase-functions
  firebase deploy --only functions
  ```

### Stuck Counter 엔드포인트

| Function | 타입 | 용도 |
|---|---|---|
| `submitStuckStatus` | Callable (HTTPS) | 사용자 물림 상태 제출 → Firestore 카운터 +/- |
| `getStuckCount` | Callable | 현재 글로벌 카운트 조회 |

- **Request**: `{ "data": { "deviceId": "<UUID>", "indexType": "market"|"crypto", "status": "stuck"|"safe"|"none" } }`
- **Response**: `{ "result": { "stuckPercentage": N, "safePercentage": N, "totalResponded": N } }`
- **리전**: `asia-northeast3`
- **Rate limit**: deviceId당 분당 15회 (App Check + Functions 내부 로직)

기타 Functions: `checkFearIndexAndNotify` (15분 주기 푸시), `registerFCMToken`, `updateNotificationSettings`, `unregisterDevice`.

## Firestore

- **`stuckStatus/global/{indexType}`**: 글로벌 카운터 (`market` / `crypto`)
  - `stuckCount`, `safeCount`, `totalResponded`, `lastUpdated`
- **`stuckStatus/users/{deviceId}`**: 사용자 상태 (Admin SDK 전용 쓰기)
- **`users/{deviceId}`**: FCM 토큰, 알림 설정

### Security Rules

클라이언트 직접 쓰기 금지. 모든 mutation은 Callable Function을 경유.

## App Check

- **Android debug**: Debug Provider (런타임에 token 출력되며, Firebase Console에 수동 등록)
- **Android release**: Play Integrity
- **iOS**: App Attest

설정: Firebase Console → App Check → 앱별로 Provider 선택.

`app/build.gradle.kts`에 이미 의존성 포함:
- `firebase-appcheck-playintegrity` (release)
- `firebase-appcheck-debug` (debugImplementation)

## Analytics

- **이벤트 naming**: iOS와 완전 일치해야 함. 스네이크 케이스 (`home_tab_opened`, `stuck_status_toggled` 등).
- **User property**: 최소화 권장.

상세는 iOS의 `AnalyticsManager.swift`를 참고해서 Android의 분석 이벤트 이름 정하기.

## Crashlytics

- **자동 업로드**: `google-services` 플러그인 + `firebase-crashlytics` 플러그인이 release 빌드 시 mapping 파일 자동 업로드.
- **버그 확인**: Firebase Console → Crashlytics (Android/iOS 별도 대시보드).

## 관련 문서

- @deployment.md — AAB 빌드 + Play Console 업로드
- @ios-parity.md — iOS와 Firebase 공유 구조
- @../rules/package-convention.md — package name 이력 (왜 `th1ngjin.fearindex`인가)
