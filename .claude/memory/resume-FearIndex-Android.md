# Resume — FearIndex-Android

## Date / Branch
2026-08-28 / `dev` (= origin/dev, v1.6.1 production 업로드 완료·release 머지·v1.6.1 태그)

## ⚡ 한 줄 상태
**v1.6.1(vc27) 게시·전파 완료(12:49 KST) + RC 상향 완료: `force_update_minimum_version`[Android] `1.6`→`1.6.1`, `minimum_app_version`[Android] `1.6.0`→`1.6.1`(diff 2줄만, iOS default 1.9.0/1.9.2 불변, 14 params, 라이브 재조회 확인) → 1.6.0(vc26) 전원 강제 업데이트 발동(patch 비교, 69번 로직). 다음: Crashlytics 1.6.1 감시 + Play 권장 조치 5개는 전부 라이브러리 내부/기준선 항목으로 판정(우리 코드 무관, 43번 참고 항목과 동일 — manifest 방향/리사이즈 제한 0, R8 minify+shrink ON, >200KB 비트맵 0, deprecated display API 0). 1.6.2 후보: Google 메모리 최적화 정책(2027-02, R8 커버리지 25%+·백그라운드 비트맵 해제) + Restore Credentials API(2027-04, 로그인 없는 앱이라 해당 없음 추정).** 마케팅 급증 대비 점검 GREEN(FCM 등록 99.4%, 신규 123명 임계값 전부 포함), 서버 레이트리밋 60→300/min 전후 대조 종결(71번).

## 🚨 재개 시 첫 행동 (어느 맥이든)
1. `git fetch origin && git pull` — dev 최신화.
2. **v1.6.1 게시·전파 확인**: `bundle exec fastlane run google_play_track_version_codes track:production` → [27] + 공개 리스팅(`play.google.com/store/apps/details?id=th1ngjin.fearindex&hl=en&gl=US`)에 "1.6.1" 등장 확인. 전파 전엔 RC 상향 금지(23·31번 원칙).
3. 전파 후: RC `force_update_minimum_version`[Android] `1.6`→`1.6.1` 상향 판단(get→한 줄 수정→deploy, 32번 절차). 위젯 리뷰 답글은 사용자 직접. Crashlytics 1.6.1 FATAL 0·위젯 ANR·`AppCheckTokenProbe` 추이 감시. 다음 배포는 vc28.

## 2026-08-28 세션 (S22+에뮬, 위젯 개선 대공사 — push 완료, 이어서 검증 필요)
- [x] 앱 이름 45 locale 현지화(ko=공포지수, iOS CFBundleDisplayName 이식) / 온보딩: 카드 하이라이트 밀착(TDD)+투표 단계 컷아웃 터치 통과+4단계 20dp 상향 — 에뮬 전 단계 검증
- [x] 위젯: 게이지 끝 캡(o) 제거, 1×1 정사각 카드, 통합 위젯 ROW/LIST 비율 배치(TDD)+어느 리사이즈에도 안 잘림(S22 실기기 루프 검증), 피커 설명 "S&P 500 추종" 축약, 새로고침 스피너(구글식)
- [x] 차트 위젯: 기간 3M/6M/1Y/3Y/5Y 탭 전환(TDD, 인스턴스별 저장)+peak 보존 다운샘플+렌더 우선 구조(provideContent 내 LaunchedEffect — 탭 즉시 반응) / **지수 스왑 폐기 → Global/KOSPI/Crypto 차트 3종 분리**(사용자 결정, 라벨·설명·프리뷰 ×45)
- [x] ↻ 새로고침 브로드캐스트 ANR → WidgetRefreshWorker(expedited) 이관
- [x] **ANR 최종 판정 = 통과(2026-08-28, release 1.6.1 S22 실기기, dev 51082e0f)** — 3시나리오 전부 ANR 0: ① 콜드 실행(MainActivity 포커스 정상) ② 콜드 ↻(통합·차트 각각 force-stop 후 탭 → WidgetRefreshWorker 1.1s SUCCESS) ③ 콜드 기간 연타 15회(재렌더 정상, `/data/anr` 최신 파일 6/22 그대로). 잔여 ANR 은 debug DEX 검증 원인으로 확정 — release 무관.
- [x] KOSPI/Crypto 차트 위젯: Crypto 차트 release 실배치 육안 OK(73·3M·y 77/41/5·x 5.31→8.28·기준시각), 통합(2×2 리스트)·Global 차트도 release 배치 OK. ⚠️ S22 의 debug 앱은 검증 위해 **제거함**(사용자 승인) — S22 에는 release(vc27 사이드로드)만 존재, 위젯 3개(통합·Global 차트·Crypto 차트) 배치 상태
- [x] 머지 완료 feature 브랜치 5개 로컬+origin 삭제(사용자 지시) → 브랜치 dev/main/release 만 남음. dev 51082e0f 전체 테스트 1,132/0
- [x] **KOSPI 진입 인터스티셜 5초 지연 제거 → 즉시 노출(dev 80962a97 push, 사용자 결정, iOS 동일)** — TDD(`DEFAULT_KOSPI_ENTRY_DELAY_MILLIS` 0), S22 debug E2E: 코스피 탭 → 300ms 내 AdActivity + GA 인터스티셜광고노출. release 는 Timber 가 logcat 에 안 남아 로드 결과를 못 봄(60번) — 광고 E2E 는 debug(테스트 유닛)로. S22 는 검증 후 debug 재제거, release(vc27 사이드로드)만 유지
- [x] **v1.6.1(vc27) production 업로드(2026-08-28 12:08 KST)** — API 9종 200, 1,132 tests/0, changelog 27 위젯 문구로 갱신(45 locale), SHA-1 일치, fastlane 성공, 트랙 [27]. release 머지 + v1.6.1 태그. 상세 deployment.md 행 + bugs-fixed 73번

## 2026-08-27 세션 (이 맥, 상세 72·73번)
- [x] 탐욕(≥70) 인앱 카피 과열 프레임 통일 — GreedFrame(TDD) + 3곳 문구 분기 + 3키×45 locale, dev 머지·push. 푸시 문구는 서버(메인 세션) 담당. **v1.6.1 미게시 상태 유지 → 이 변경도 v1.6.1 에 포함됨**
- [x] **위젯 전면 리디자인(73번, dev 28b0320e push)** — Play 리뷰 대응: 1×1 게이지 3종(targetCell 1×1 재등록) + 통합 2×2(게이지 3개) + **차트 4×2 신규**(30일 라인·y 3눈금·x 날짜·"HH:mm 기준") + 새로고침 버튼 + 로드 실패 10분 재시도. 에뮬 실배치 육안 검증(게이지 55/49/71 채움 정확성 포함), 1,096 tests/0, locale 대칭 500키. ⚠️ Glance 함정(Row weight/SweepGradient 캡/SessionWorker 45초)·30일 잘라내기는 73번 참조
- [x] **위젯 피커 미리보기·이름·설명 정비(dev 68d0c48e push)** — previewImage 5장(PIL 실물 1:1) + receiver label 5종 + 설명 문장형 ×45 locale. 에뮬 피커 검증 완료
- [x] **위젯 피커 지적(사용자) → previewImage 5장 + receiver label + 설명 문장형(dev 68d0c48e push)** — 에뮬 피커 검증 완료(미리보기/이름/설명 정상)
- [x] **등급/색 원점수 기준 통일(dev 4d04e642 push, 사용자 결정)** — 상세 73번 후속 2
- [x] **경계값 윗 밴드 통일(dev 43ee54d7 push)** — iOS·서버 확인 후 Android `<=55/<=75` → `<55/<75` (55.0=탐욕, 75.0=극탐, 크립토 정수 점수 실발생 케이스). 상세 73번 후속 3
- [ ] **S22 최종 확인(사용자가 직접)** — 마지막 설치본은 피커 미리보기 이전 빌드. S22 재연결 시 dev(4d04e642) `:app:assembleDebug` 재설치 필요(재연결 Monitor 대기 중이었음 — 새 세션이면 직접 설치). 항목: 피커 미리보기/이름/설명, 1×1 3종, 통합 2×2, 차트 4×2, ↻, 게이지 등급(원점수)
- [x] **위젯 사용법 가이드 Android 전용 재작성(dev f95643e8 push)** — iOS식 페이저 → 세로 스크롤 3섹션(위젯 종류 배지 카드·번호 단계·팁 5개), 신규 8키×45 locale(대칭 508키), 에뮬 ko/en 육안 확인, 전체 테스트 GREEN
- [ ] 토스 세션발 온보딩 코치마크 개선 이식 — ①카드=하이라이트 하단 5dp 밀착+게이지 단계 상단 정렬 ②투표 단계 interactive(컷아웃 터치 통과) (③알림 축소는 Android 행 단위 하이라이트라 해당 없음 판단)
- [ ] Play 리뷰 답글: 행복회로(reviewId 0c0b628e-e186-45f1-a2b0-c221dc3f39a5) 1.6.1 배포 문구로 갱신 필요 — **API 게시는 권한 차단으로 사용자 직접**(문구는 세션에서 전달됨). jjj(3044efb3…)도 1×1 배포 안내 갱신 후보
- [ ] **v1.6.1(vc27) 게시** — 위젯/등급 변경 전부 dev 에 포함됨. @docs/checkpoints/HANDOFF-v1.6.1.md 절차(테스트 → fastlane production → 태그/release 머지)

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
