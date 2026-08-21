# Resume — FearIndex-Android

## Date / Branch
2026-08-21 / `dev` (feature/v1.5.4-appcheck-resilience --no-ff 머지, push 미실행)

## ⚡ 한 줄 상태
**v1.5.3(vc25) 게시 완료 + Android App Check 전수 실패(68번)를 콘솔 수정으로 8/21 06:05Z 복구.** 1.5.4 클라이언트 보강 코드는 dev 에 머지됨(미배포). 다음 배포 vc26.

## Completed (이번 세션, 2026-08-21)
- [x] **v1.5.3 게시 모니터링** — Play "Google Play에 제공됨"(8/19 16:58), 설치 403, Crashlytics crash-free 100%/ANR 0/미해결 크래시 0, 정책 위반 해소(API 36 카드 소멸), AdMob 신규 이슈 0·공포지수 Android 7일 노출 +52%/eCPM +34%. 상세: @.claude/memory/deployment.md v1.5.3 행
- [x] **🚨 App Check 401 근본 원인 규명** — Android 보호 Callable 401 ~1,000/일 vs 200 ≈ 0(30일), Firestore android 신규 등록 6/22 이후 0. 원인 = Firebase 지문이 v1.0.0 폐기 키(`AD:48…`)뿐 + Play Integrity API Cloud 프로젝트 미연결. 상세: @.claude/memory/bugs-fixed.md 68번
- [x] **콘솔 복구(A)** — Firebase Management API 로 Play 앱 서명 키 `EF:5D:B8:C8…` + 업로드 키 `91:47…` SHA-256 등록 / Play Console Play Integrity API ↔ fear-index(8243517543) 연결 / App Check `requireLicensed=false`. 06:05:44Z 부터 Android 200 재개, 신규 android 등록 5건(15분). @.claude/memory/secrets-env.md 지문 표 갱신
- [x] **클라이언트 보강(B)** — `AppCheckTokenProbe`+`AppCheckFailureClassifier`(core, TDD), `FcmRegistrationPolicy`(domain, TDD), Repository 게이트+스냅샷, `FcmRegistrationWorker` 재시도, BoM 33.16.0. 1044 tests/0 fail + release AAB 빌드 확인
- [x] 메모리/문서 갱신 (68번, deployment v1.5.3 행, secrets-env, MEMORY, 이 파일)

## 미해결 / 다음 할 일 (우선순위순)
1. **복구 추세 검증(C 계속)** — 하루 뒤 재측정: `gcloud logging read 'resource.type="cloud_run_revision" logName="projects/fear-index-a4f4b/logs/run.googleapis.com%2Frequests" httpRequest.userAgent:"okhttp" resource.labels.service_name="registerfcmtoken"' --project=fear-index-a4f4b --account=dlaudwls1203@gmail.com --freshness=24h --limit=5000 --format="value(httpRequest.status)" | sort | uniq -c` → 200 ≫ 401 이어야 함. Firestore `users` android createdAt 오늘 이후 증가, Crashlytics `Unauthenticated` 비치명 감소 확인
2. **1.5.4 배포 판단** — dev 의 App Check 보강 + BoM 33.16.0 을 vc26 으로. 배포 전 **Play 내부 테스트 트랙 설치본으로 App Check 실측**(사이드로드로 검증 금지, 68번 교훈). Fastfile internal lane 에 `release_status: "completed"` 추가(60번) 같이 처리
3. **iOS 팀 전달** — 서버 App Check soft→hard 전환 시 Android verified 메트릭도 확인할 것 + macOS 1.8.0 registerFCMToken 401 소수 존재
4. 실결제 완주(사용자, Play 설치본 1.5.3) / 사용자 결정 2건(알림 내역 전용 AdMob 유닛, IAP 표시명) / 1.5.4 잔여 후보(@docs/checkpoints/RELEASE-1.5.3-CHECKLIST.md B·C·D·H)
5. (선택) Firebase 지문에서 폐기 키 `AD:48…` 제거, App Check 토큰 TTL/기기 무결성 수준 재검토

## 주의 (다음 세션이 밟을 함정)
- **App Check 검증은 Play 설치본으로만** — 사이드로드 release 403 은 정상(Play Integrity 는 Play 배포본 전용). 서버 연동 E2E 는 에뮬 debug + 콘솔 debug token(67번)
- **Firebase 지문 확인 API 는 `x-goog-user-project: fear-index-a4f4b` 헤더 필수**(없으면 SERVICE_DISABLED 403 오진)
- Play Console 은 `u/1`(dlaudwls1203) — `u/0`/다른 계정 약관·signup 화면 뜨면 진행 금지. Chrome MCP 끊기면 Aside 는 Google 로그아웃 상태(사용자 로그인 필요)
- gcloud 기본 계정은 회사(isenssw2023) — FearIndex 는 항상 `--account=dlaudwls1203@gmail.com`
- worktree 새로 만들면 `app/google-services.json` 이 없다 — `~/fearindex-secrets/` 에도 없으므로 본 레포 `app/google-services.json` 을 복사(심볼릭 링크 대상 부재)
- S22 는 AdMob 테스트 기기 / 다음 배포 vc26 / 알림 내역 저장소 정렬 비보장(65번)

## 참조
- @.claude/memory/bugs-fixed.md — 68번(이번 세션 핵심), 59~67번(직전 배포 세션)
- @.claude/memory/deployment.md — 출시 이력 v1.5.3 행, 상수
- @.claude/memory/secrets-env.md — 키 지문 표(Play 앱 서명 키 추가) + Firebase 등록 지문 확인 명령
- @docs/checkpoints/RELEASE-1.5.3-CHECKLIST.md — 1.5.4 잔여 이슈 표
- @.claude/memory/ios-parity.md — App Check 플랫폼 차이(App Attest vs Play Integrity)
