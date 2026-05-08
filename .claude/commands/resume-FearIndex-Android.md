# Resume — FearIndex-Android

## 1. Sync with remote
Run `git fetch origin`. If the local `dev` is behind, run `git pull --ff-only origin dev`.
If diverged, warn the user — do NOT force pull.

## 2. Project rules
@CLAUDE.md

## 3. Work state
@docs/checkpoints/SESSION-STATE.md

## 4. Git status
Run `git status`, `git branch -vv`, `git worktree list`. Show recent commits using the larger of:
- All commits from today: `git log --oneline --since="midnight"`
- Last 10 commits: `git log --oneline -10`

## 5. Key files
Read all files listed in the "Key Files" section of `docs/checkpoints/SESSION-STATE.md`.

## 6. Build environment
Verify `./gradlew tasks --quiet | head` runs (Gradle wrapper alive). Don't full build unless asked.

## 7. Briefing
Print:

---
**Project**: FearIndex-Android (v1.0.3 출시 준비)
**Branch**: dev
**Worktrees**: 본진(dev) / -promo(feature/v1.0.3-promo-graphics) / -v103(feature/v1.0.3-ads-locale-ui)
**Done**: 45 locale × 5 합성 promo 이미지 정착, promo 자동 촬영 스크립트, debug-only push receiver, 광고 차단 모드
**Current**: v1.0.3 출시 준비 (versionCode 3 → 4 bump 대기)
**Next**: AAB 빌드 → Play Console Closed Testing 업로드 → 출시 노트 44 locale 추가 작성
---

Ask: "Ready to continue?"
