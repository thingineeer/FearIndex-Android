# Resume — FearIndex-Android

## Date / Branch
2026-08-25 / `dev` (= origin/dev, 8/24 두 맥 통합 + 8/25 '줍줍' ASO 머지까지 push 완료)

## ⚡ 한 줄 상태
**Play production = v1.6.0(vc26). v1.6.1(vc27: 인터스티셜 10분 리셋 + changelog 27 + ko '줍줍' 설명문) 준비 완료 — 다음: v1.6.1 게시.** 마케팅 급증 대비 점검 GREEN(FCM 등록 99.4%, 신규 123명 임계값 전부 포함), 서버 레이트리밋 60→300/min 전후 대조 종결(71번).

## 🚨 재개 시 첫 행동 (어느 맥이든)
1. `git fetch origin && git pull` — dev 최신화(8/24 머지 커밋 포함 확인).
2. **v1.6.1(vc27) 게시** — @docs/checkpoints/HANDOFF-v1.6.1.md 절차: (그 맥 최초면 `bash ~/thingineeer-env/android/fearindex/install.sh`) → `./gradlew test` → `bundle exec fastlane production`(관리형 게시 OFF, **사용자 게시 승인됨**) → 트랙 `[27]` 확인 → v1.6.1 태그 + release 머지 + deployment.md 행. versionCode 는 이미 27/1.6.1(머지에서 채택, changelog 27 존재).
3. 게시 후 실기기: 10분 백그라운드 → 코스피 재진입 → 전면광고 노출 1회 확인.

## 2026-08-27 세션 (이 맥, 상세 72번)
- [x] 탐욕(≥70) 인앱 카피 과열 프레임 통일 — GreedFrame(TDD) + 3곳 문구 분기 + 3키×45 locale, dev 머지·push. 푸시 문구는 서버(메인 세션) 담당. **v1.6.1 미게시 상태 유지 → 이 변경도 v1.6.1 에 포함됨**

## 2026-08-25 세션 (이 맥, 상세 71번)
- [x] 마케팅 급증 대비 점검 — FCM 등록 200=656/401=4, 신규 android 123건(1.6.0=117) **임계값 123/123 포함**(즉시체크 게이트 OK), 스토어 production=[26]. 메인 세션 회신 완료
- [x] 서버 공유 레이트리밋 60→300/min(메인 세션 TDD 배포) **전후 대조 종결** — 공유 IP 리밋발 429 소멸·5xx 0·401 불변. 잔여 429 = getSimilarEvents per-device 리밋(설계 동작). ⚠️ 교훈: 공유 모듈 상수 변경은 **재배포한 함수만 반영** — 1차 배포 누락 3함수를 대조로 발견해 재배포 유도
- [x] ASO: ko_KR 설명문 '줍줍' 3회 자연 삽입(dev 3bbd530b push) — v1.6.1 fastlane 업로드 시 스토어 반영

## 2026-08-24 통합 머지 (이 맥)
- [x] 분기 원인: 이 맥의 8/21 작업(1.6.0 소스·App Check 보강·patch 단위 강제 업데이트, 16커밋)이 미push 상태에서 다른 맥이 origin/dev 에 8/22~23 작업(26커밋)을 push → merge --no-ff 로 통합(충돌 3: build.gradle.kts→1.6.1/vc27 채택, MEMORY, resume).
- [x] **bugs-fixed 번호 재정리**: 광고 미노출 세션 항목 **68→70**(중복 해소; 그 세션 커밋 메시지엔 68로 남음). **68=App Check 전수 실패, 69=1.6.0 배포+강제 업데이트, 70=광고 제보+소스 부재**.
- [x] `release` 에 dev(1.6.0) 머지 + **v1.6.0 태그**(b62ccd4, `7ca2711` 내용) → dev/release/태그 push.

## 2026-08-23 세션 요약 (다른 맥, 상세 70번)
- [x] 광고 "안 뜬다" 제보 — GA/Crashlytics/서버 실측 재현 불가(배너 실패 481→80 급감, AdMob 7일 전 지표 상승, 앱오픈 첫 가동)
- [x] 제보 원인 확정 = 인터스티셜 세션 미리셋(iOS v1.9.2 미포팅) → **A안 TDD 수정 + 10분 단축(사용자 결정) dev 머지**
- [x] v1.6.1(vc27) bump + changelog 27 45 locale 준비(미게시), 4계정 8월 요금 ≈₩8.4k 정상, Play 보고서 백업 thingineeer-env 이관
- [ ] 배너 잠재 결함 2건(320dp 하한 → 352dp 미만 기기 배너 0 / AndroidView factory stale) — 미수정, 1.6.1+ 후보

## 2026-08-21 세션 요약 (이 맥, 상세 68·69번)
- [x] **App Check 전수 실패 복구(68)** — Firebase 지문(Play 앱 서명 키 `EF:5D…`+업로드 키) 등록 + Play Integrity API↔fear-index 연결 + requireLicensed=false → 06:05Z부터 Android 200 재개, 신규 android 등록 재개
- [x] **v1.6.0(vc26) 게시(69)** — App Check 보강(토큰 선취득·실패 분류 Crashlytics·FcmRegistrationPolicy·재시도 워커) + BoM 33.16.0 + patch 단위 강제 로직(TDD 17/17). 전파(07:58Z) 후 **RC Android force `1.6`/minimum `1.6.0` 게시** → 1.0.x~1.5.x 전원 강제(E2E: v1.5.3 에뮬 ForceUpdateView+PlayCore requestUpdateInfo 확인)
- [x] iOS 세션 협업 — macOS 401 원인(DeviceCheck vs 콘솔 App Attest 불일치) 전달 → iOS 가 콘솔 등록으로 복구(06:57Z 200), 3플랫폼 App Check 정상

## 미해결 / 다음 할 일 (우선순위순)
1. **v1.6.1(vc27) 게시** — 위 '재개 시 첫 행동' 2번.
2. ~~App Check 복구 추세 재측정~~ **8/25 완료(200=656/401=4, 71번)** — 이후는 관찰만: Crashlytics `AppCheckTokenProbe` kind 분포(-9 THROTTLED 추이) + 강제 업데이트로 1.5.x 버전 분포 소멸. 마케팅 피크 중 등록/429 재측정 쿼리는 71번 참조.
3. 배너 잠재 결함 2건(70번 ①②) 수정 검토(1.6.1 이후), Fastfile internal `release_status: "completed"`(60번).
4. 실결제 완주(사용자, Play 설치본) / 사용자 결정 2건(알림 내역 전용 AdMob 유닛, IAP 표시명) / RELEASE-1.5.3-CHECKLIST 잔여(B·C·D·H) / (선택) Firebase 지문 폐기 키 `AD:48…` 제거.

## 주의 (다음 세션이 밟을 함정)
- **App Check 검증은 Play 설치본으로만** — 사이드로드 release 403 정상. 서버 E2E 는 에뮬 debug + 콘솔 debug token(67번)
- **Firebase REST 는 `x-goog-user-project: fear-index-a4f4b` 헤더 필수** / gcloud 는 `--account=dlaudwls1203@gmail.com`(기본이 회사 계정)
- 콘솔 URL 기본 계정 수시 변경 — signup/약관/MFA 화면 뜨면 진행 금지, `u/1` 또는 `?authuser=dlaudwls1203@gmail.com`. Chrome MCP AdMob 은 `admob.google.com` 권한 + 리다이렉트 직후 첫 액션 실패 재시도(70번 도구 함정)
- 새 worktree 엔 `app/google-services.json` 없음 — 본 레포에서 복사(`~/fearindex-secrets/` 에도 없음)
- 커밋 메시지의 "68번"(8/22~23 다른 맥 세션분)은 **70번**을 가리킴
- S22 는 AdMob 테스트 기기 / 알림 내역 저장소 정렬 비보장(65번) / 다음 배포 vc27(=1.6.1)

## 참조
- @.claude/memory/bugs-fixed.md — 68(App Check)·69(1.6.0 배포)·70(광고 제보·소스 부재) + 59~67
- @docs/checkpoints/HANDOFF-v1.6.1.md — v1.6.1 게시 절차(다음 작업)
- @.claude/memory/deployment.md — 출시 이력(v1.6.0 행), 상수
- @.claude/memory/secrets-env.md — 키 지문 표(Play 앱 서명 키) + Firebase 등록 지문 확인 명령
- @docs/handoff/ios-appcheck-401-handoff-2026-08-21.md — iOS 전달분(전달 완료)
- @docs/checkpoints/SESSION-STATE.md — 8/23 세션 상태(70번 기준)
