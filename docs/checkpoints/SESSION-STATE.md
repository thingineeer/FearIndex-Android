# Session State — FearIndex-Android

## Date
2026-08-18 (오전 세션)

## Branch
`dev` — 로컬에서 origin/dev(v1.5.1 배포분) ← Pangle 어댑터 머지 완료(--no-ff, 충돌 2파일 해소). **push 미실행.** worktree 없음.

## ⚡ 한 줄 요약

**AdMob 메일 2건 판단 완료 + 배포 파이프라인 막힌 원인 규명.** API 36은 코드상 이미 해결(1.5.1)이고 남은 경고는 옛 테스트 트랙 번들 탓, Next-Gen SDK는 v1.6.0으로 보류. 진짜 문제는 **앱이 다른 Play 개발자 계정으로 이전되면서 fastlane SA 권한이 사라져 배포가 403**이라는 것.

---

## Completed (이번 세션)

- [x] **origin/dev 머지** — 로컬 Pangle 커밋(2) vs 원격 20커밋 diverge → `git merge --no-ff origin/dev`. 충돌: `app/build.gradle.kts`(어댑터 의존성 줄) + `libs.versions.toml`(GMA 버전/어댑터 항목) → 양쪽 유지, GMA **24.8.0**(23.6.0은 2026-02-17 deprecated). runtime classpath에서 Unity 어댑터의 23.6.0이 24.8.0으로 승격 확인.
- [x] **791 테스트 GREEN + `:app:bundleRelease`(R8) 성공** — 머지 후 검증(48번 교훈대로 release까지).
- [x] **Play Console 실측(Chrome MCP)** — 계정 이전 사실, SA 부재, 트랙별 활성 번들, 계정 삭제 예고 배너 확인. 상세 @.claude/memory/bugs-fixed.md 53~55번.
- [x] **GMA Next-Gen SDK 조사** — 리서치 에이전트 + 공식 문서 검증. 결론 "보류". 체크리스트 55번.
- [x] 메모리/세션 기록 갱신.

- [x] **fastlane SA 복구** — 사용자가 이명진 계정에 SA 초대(Claude 폼 입력은 분류기 차단) → `google_play_track_version_codes` 정상(11:01).
- [x] **vc22 → internal/alpha 승격** — `upload_to_play_store track:production track_promote_to:{internal,alpha} version_code:22 track_promote_release_status:completed skip_upload_*:true` → 세 트랙 모두 [22]. 내부 "제공됨", 알파 "검토 중". API 36 경고의 원인(vc3) 제거 완료.

## In Progress

없음. (알파 심사 대기는 Google 측.)

## Remaining (다음 세션 우선순위)

1. **push** (dev, 로컬 ahead 5) — 사용자 승인 대기.
2. 알파 1.5.1 심사 통과 + 대시보드 "8월 31일까지 조치" 카드 소멸 확인(스캐너 갱신 지연 가능).
3. Pangle 어댑터 포함 v1.5.2(vc23) 배포 여부 — 사용자 결정. (fastlane production 정상 동작 확인됨.)
4. 실기기 Billing 8 결제 재검증(1.5.1, 미완), `KospiFearIndexApi.history` boolean 지뢰, Yahoo spark 死코드, BOOT_COMPLETED 권한 정리(이월).
5. **결제 프로필/계정 삭제 예고(9/16)** — 사용자가 직접 진행 중. 관여 금지.
6. (v1.6.0) GMA Next-Gen 이전 — Kotlin ≥2.2 bump 선행. 55번 체크리스트.

## Key Files

- @gradle/libs.versions.toml — GMA 24.8.0 / Unity 4.13.1.0 / Pangle 7.8.0.8.0 / billing 8.3.0 / targetSdk 36
- @app/build.gradle.kts — 어댑터 3줄(unity-ads, unity, pangle) 공존
- @presentation/src/main/java/th1ngjin/fearindex/presentation/component/AdBanner.kt — Next-Gen 이전 시 1순위(인라인 adaptive 높이 정책 22번 유지)
- @presentation/src/main/java/th1ngjin/fearindex/presentation/component/InterstitialAdManager.kt, AppOpenAdManager.kt, @app/src/main/java/th1ngjin/fearindex/FearIndexApp.kt — 나머지 광고 표면적
- @fastlane/Appfile — SA json 경로(`~/fearindex-secrets/play-store-service-account.json`, `fastlane-deploy@fear-index-a4f4b`)
- @.claude/memory/bugs-fixed.md — 53~55번 이번 세션

## 대화 요약

### 이번 세션에서 결정한 것

- **Next-Gen SDK는 지금 안 옮긴다** — 이유: 하드 데드라인 없음(레거시 2027-06-30 deprecated/2028-06-30 sunset), 1.3.x 크래시 보고(#96/#85)와 배너 fill 하락 보고, Kotlin 2.1.0에선 최신 어댑터(stdlib 2.3.0) 빌드 불가 가능성, Billing 8 첫 배포 검증도 미완. v1.6.0에서 Kotlin bump와 함께.
- **머지 충돌은 "둘 다 유지"** — GMA 24.8.0(Pangle 요구, 23.x deprecated), Unity 어댑터는 4.13.1.0 그대로(24.0.0 breaking change에 mediation API 없음 → 호환). 사용자 승인 후 진행.
- **결제 프로필 건은 사용자 전담** — "신경 쓰지 마, 내가 진행 중".

### 시도했다 접은 것

- **Claude가 SA 초대 폼 입력** — harness 분류기 차단(계정 권한 변경). 우회하지 않고 사용자에게 절차 전달.
- **`/u/0`,`/u/1`,`/u/2` 계정 인덱스 탐색** — `/u/1`은 무관 계정(satirepeople, 클릭 금지), `/u/0`은 dlaudwls1203, `/u/2`→`/u/6` 리다이렉트가 이명진 계정. Chrome 프로필마다 다르므로 URL 인덱스에 의존 말고 "개발자 계정 선택 → 이명진"으로.

### 명시된 사용자 선호

- Play Console은 `mjplist@gmail.com`으로 볼 것.
- "제안 실행 순서 모두 진행" — 머지/검증/콘솔 확인/기록까지 승인. push는 별도.
- 결제 프로필 문제 언급 불필요.

### 다음 세션이 알아야 할 맥락

- **AdMob 메일의 "API 36" 경고는 해결 완료** — 프로덕션 1.5.1 + 오늘 internal/alpha에도 vc22 승격. 카드가 남아 있으면 스캐너 지연/알파 심사 대기.
- **fastlane 403은 종결**(SA 초대 완료). **기존 번들 승격은 `track:<원본> track_promote_to:<대상> version_code:N`** — `track:<대상> version_code:N skip_upload_aab`는 성공 메시지만 나오고 아무 것도 안 함.
- "Myeongjin Lee" 조직 계정은 앱 0개 껍데기 — 여기서 뭔가 찾지 말 것.
- Next-Gen 이전 시 UMP 4.0.0이 transitive로 오고 App ID를 Manifest(UMP용)+InitializationConfig 두 곳에 둬야 함.

## Notes

- 빌드/서명: `~/.gradle/gradle.properties`의 `FEARINDEX_*` 4개 필요. 없으면 `bash ~/thingineeer-env/android/fearindex/install.sh`.
- 배포: `bundle exec fastlane production`. **관리형 게시 OFF** — 승인 즉시 자동 게시. (SA 복구 전엔 403.)
- 릴리즈 서명 SHA-1 `CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16`.
- 테스트 791개 GREEN 기준. `./gradlew test`.
- 의존성 변경 후에는 **반드시 release 빌드까지** 돌릴 것 — debug는 minify가 없어 R8 실패를 못 잡는다(48번 교훈).
