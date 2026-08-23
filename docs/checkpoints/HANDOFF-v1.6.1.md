# HANDOFF — v1.6.1 게시 (다른 맥 세션용 goal)

작성 2026-08-23. 이 맥(cgmsw) 작업은 전부 `origin/dev`(50fb3c4)에 있다. 아래를 **1.6.0을 빌드했던 맥**에서 실행한다.

## 배경 (한 줄)
Play production 1.6.0(vc26)은 커밋 `7ca2711227bdbdb049261c2108553d024ad58228`로 빌드됐는데 이 커밋이 GitHub에 없다(그 맥 로컬에만 있음). `origin/dev`에는 1.5.3 위에 인터스티셜 수정 + v1.6.1(vc27) bump가 올라가 있다. 둘을 합쳐 1.6.1을 게시한다.

## 완료 조건 (전부 충족해야 끝)
1. `7ca2711`이 `origin/dev`(또는 그 브랜치)에 push됨
2. `v1.6.0` 태그가 `7ca2711`에 찍혀 push됨, `release`에 머지됨
3. `dev` = `7ca2711` 위에 origin/dev 변경 전부 머지(충돌 해결), `versionName 1.6.1 / versionCode 27`
4. `./gradlew test` 실패 0 (StuckStatusDebouncerImplTest 1회 flaky면 단독 재실행으로 판정)
5. AAB SHA-1 = `CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16`
6. `bundle exec fastlane production` 성공 → Play 트랙 API로 production = vc27 `completed` 확인
7. `v1.6.1` 태그 + `release` 머지 + dev/release/태그 push
8. `.claude/memory/deployment.md` 출시 이력에 v1.6.1 행, `bugs-fixed.md` 68번에 게시 기록

## 절차
```bash
cd <FearIndex-Android>
git status                                  # 미커밋 변경 있으면 먼저 커밋 (1.6.0 잔여분)
git log --oneline -5 | grep 7ca2711         # 있어야 함. 없으면 중단하고 보고
git fetch origin
git push origin HEAD:refs/heads/feature/v1.6.0-source   # 1.6.0 소스 보존
git tag -a v1.6.0 7ca2711 -m "v1.6.0 (vc26) — 2026-08-21 production 게시" && git push origin v1.6.0

git checkout dev && git pull --ff-only origin dev       # 50fb3c4
git merge --no-ff feature/v1.6.0-source -m "merge: feature/v1.6.0-source — 1.6.0 배포 소스 합류"
# 충돌 예상 지점: app/build.gradle.kts(versionCode/Name → 27/1.6.1 유지), changelog(26.txt는 1.6.0 것 그대로 두고 27.txt 유지),
# FearIndexApp.kt(양쪽 변경 모두 유지 — 1.6.0 의 AppCheckTokenProbe 호출 + dev 의 handleInterstitialForegroundEntry/recordBackgroundEntry)
grep -n "versionCode\|versionName =" app/build.gradle.kts   # 27 / 1.6.1
./gradlew test
bash ~/thingineeer-env/android/fearindex/install.sh        # 서명 설정(이미 있으면 no-op)
./gradlew clean :app:bundleRelease
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab | grep SHA1   # CE:08:B4…
bundle exec fastlane production            # 관리형 게시 OFF → 승인 즉시 게시. 사용자 승인 완료(2026-08-23 "A")
```
트랙 확인: `~/fearindex-secrets/play-store-service-account.json`로 androidpublisher v3 `edits.insert` → `tracks.get production` (bugs-fixed 68번 ④ 절차) 또는 `bundle exec fastlane run google_play_track_version_codes track:production` → `[27]`.

```bash
git checkout release && git merge --no-ff dev -m "merge: v1.6.1 production 게시 반영 (versionCode 27)"
git tag -a v1.6.1 -m "v1.6.1 (vc27) — 인터스티셜 세션 리셋(10분)·재로드" && git push origin dev release v1.6.1
git branch -d feature/v1.6.0-source && git push origin --delete feature/v1.6.0-source
```

## 이번 변경 요약 (origin/dev, 코드 읽지 않아도 되게)
- `InterstitialForegroundGate`(신규): 백그라운드 ≥10분이면 새 세션
- `InterstitialAdPolicy.recordBackgroundEntry/handleForegroundEntry`: 세션 리셋(cap·쿨다운·KOSPI 진입 1회 플래그)
- `InterstitialAdCoordinator`: 광고 미준비 시 재로드, 복귀 preload, cap 도달 시 preload 생략, `InterstitialAdSessionState.lifecycleCoordinator`
- `FearIndexApp`: ProcessLifecycle onStop/onStart 훅
- changelog 27 × 45 locale(`v1.6.1:` 명시), versionCode 27
- 근거·검증: @.claude/memory/bugs-fixed.md 68번, @.claude/memory/resume-FearIndex-Android.md

## 게시 후
- 실기기: 앱 → 백그라운드 10분+ → 복귀 → 홈에서 코스피 탭 → 5초 후 전면광고 노출 1회 확인
- Crashlytics 1.6.1 FATAL 0 / `AppCheckTokenProbe` non-fatal 추이 / GA `인터스티셜광고노출` 증가 확인
- 메모리·SESSION-STATE 갱신 후 push
