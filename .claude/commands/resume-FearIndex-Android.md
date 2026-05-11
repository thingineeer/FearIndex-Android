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

Also show tags: `git tag -l "v*"` (v1.0.0 부터 정식 태깅 시작).

## 5. Key files
Read all files listed in the "Key Files" section of `docs/checkpoints/SESSION-STATE.md`.

## 6. Build environment
Verify `./gradlew tasks --quiet | head` runs (Gradle wrapper alive). Don't full build unless asked.

## 7. Briefing
Print:

---
**Project**: FearIndex-Android (v1.0.0 출시 완료 — 이제 개선 단계)
**Branch**: dev (clean)
**Tag**: v1.0.0 (release 브랜치 = `acf9876`)
**Worktrees**: 본진만 (피처 브랜치 모두 머지/정리됨)
**Done**: v1.0.0 Production 출시 통과, ko-KR 앱 이름 "공포지수" 단축 + iOS description 동기화, 빌드 산출물/일회성 docs 정리
**Current**: v1.0.1+ 개선 백로그 대기
**Next**: v1.0.1 시작 시 `git checkout -b feature/v1.0.1` → worktree 분기
---

Pending (사용자 작업): Play Console에서 Celestial Oracle/FLIPOP/옛 공포지수 3개 영구 삭제 (transaction ID 입력 필요).

Ask: "Ready to continue? v1.0.1에서 작업할 항목은 뭐예요?"
