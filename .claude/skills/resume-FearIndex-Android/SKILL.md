---
name: resume-FearIndex-Android
description: Resume FearIndex-Android session — sync with remote, read save point, and brief current state.
disable-model-invocation: true
---

# Resume — FearIndex-Android

## 1. Sync with remote
Run git fetch. If the local branch is behind, run git pull.
If diverged, warn the user — do NOT force pull.

## 2. Project rules
@CLAUDE.md

## 3. Work state
@docs/checkpoints/SESSION-STATE.md

## 4. Memory
@.claude/memory/MEMORY.md

## 5. Git status
Run git status and git branch, then show recent commits using the larger of:
- All commits from today: `git log --oneline --since="midnight"`
- Last 10 commits: `git log --oneline -10`
Use whichever returns more results.

## 6. Key files
Read all files listed in the "Key Files" section of SESSION-STATE.md.

## 7. Worktrees
Run `git worktree list` to show active worktrees.
Each worktree has its own branch — tell the user which is currently checked out where.

## 8. Build environment
Verify build commands work:
```bash
./gradlew :app:assembleDebug 2>&1 | grep "BUILD"
```

## 9. Briefing
Print:

---
**Project**: FearIndex-Android (공포지수 Android)
**Package**: `th1ngjin.fearindex` (+ `.debug`)
**Branch**: {branch}
**Version**: {SESSION-STATE.md에서 최신 버전}
**Active worktrees**: {git worktree list 요약}
**Done**: {SESSION-STATE.md Completed 요약}
**Current**: {In Progress 또는 "없음"}
**Next**: {Remaining 첫 항목}
---

Ask: "Ready to continue?"
