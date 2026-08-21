# iOS 메인 세션 전달용 — App Check 401 (2026-08-21, Android 세션 작성)

> SendMessage 로 "fearindex 메인 세션"에 전달 시도했으나 unreachable → 파일로 남김. iOS 세션 resume 시 읽어 주세요.

## 1) 서버 App Check 관련 발견 (Android 쪽 원인은 Android가 콘솔에서 해결 완료 — 서버 코드 변경 불필요)
- Android 프로덕션은 **6/22(App Check hard mode 실배포, iOS repo `403a40b3a` 6/10 커밋) 이후 `enforceAppCheck` Callable(registerFCMToken/getStuckCount/getSimilarEvents/submitStuckStatus/updateNotificationSettings)이 ~100% 401**이었음 — 30일 로그 매일 ~1,000건, Firestore `users` android 신규 등록 6/22 이후 0건.
- 원인 = Firebase Android 앱 지문이 v1.0.0 폐기 키 1개뿐(Play 앱 서명 키 미등록) + Play Console Play Integrity API Cloud 프로젝트 미연결 → 8/21 06:05Z 콘솔 수정으로 복구(Android 200 재개, 신규 android 등록 시작). 상세: `FearIndex-Android/.claude/memory/bugs-fixed.md` 68번.
- **부탁**: 서버 App Check 모드(soft→hard)/enforce 변경 시 **App Attest(iOS)뿐 아니라 Play Integrity(Android) verified 메트릭도 함께 확인**해 주세요. 6/10 커밋 메모는 App Attest만 확인했고, Android는 soft mode 가 가려주다 hard mode 에서 전부 떨어졌습니다.
  - 검증 쿼리(Android = okhttp UA):
    ```bash
    gcloud logging read 'resource.type="cloud_run_revision" logName="projects/fear-index-a4f4b/logs/run.googleapis.com%2Frequests" httpRequest.userAgent:"okhttp" resource.labels.service_name="registerfcmtoken"' \
      --project=fear-index-a4f4b --freshness=24h --limit=5000 --format="value(httpRequest.status)" | sort | uniq -c
    ```

## 2) iOS/macOS 쪽 소수 401 (iOS 영역, 확인 요청)
- 최근 24h registerFCMToken 401 중 iOS UA: `th1ngjin.FearIndex-iOS/1.8.0 MacOSX/27.0.0`·`MacOSX/15.7.x`·`MacOSX/26.5.x` 등 **macOS 1.8.0 약 15건**, iPhone 1.9.3/1.9.4 약 6건(iPhone17_1, iPhone14_2, iPhone14_5, iPhone12_1, iPad15_5).
- macOS 1.8.0 이 대부분 → macOS App Check provider 점검 필요해 보임. iPhone 소수는 App Attest 실패(기기 미지원/재시도) 범주인지 확인 부탁. 규모 작아 긴급 아님.
