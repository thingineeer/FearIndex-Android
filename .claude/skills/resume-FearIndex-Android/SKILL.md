---
name: resume-FearIndex-Android
description: Resume FearIndex-Android session - auto-pulls, reads save point, and prints briefing.
disable-model-invocation: true
---

# Resume - FearIndex-Android

## 1. Sync with remote - must run first
Before reading any project file, run:
- `git fetch`
- `git status -sb | head -1`
- If the current branch is behind, run `git pull --ff-only`
- If the branch is diverged, stop and warn the user. Do not force pull.

Do not use `@` file references before this sync step. They can load stale files before `git pull`.

## 2. Read refreshed files
After sync, read these files directly:
- `CLAUDE.md` - project rules
- `docs/checkpoints/SESSION-STATE.md` - current save point and conversation summary
- `.claude/memory/MEMORY.md` - memory index
- Every file listed in `SESSION-STATE.md` under "Key Files"

All paths are project-root relative. Do not use machine-specific absolute paths in saved instructions.

## 3. Git status
Run:
- `git status`
- `git branch -vv --all`
- `git worktree list`
- Show recent commits using the larger of:
  - `git log --oneline --since="midnight"`
  - `git log --oneline -10`

Tell the user which branch is checked out and whether it is synced with origin.

## 4. Build environment
Verify at least one lightweight build/test command before resuming code work:

```bash
./gradlew test
```

If the user only asks for a quick status briefing, report the previous saved test result first and ask before spending time on a fresh test run.

## 5. Briefing
Print:

---
**Project**: FearIndex-Android
**Branch**: {current branch and sync state}
**Version**: {SESSION-STATE current version}
**Done**: {SESSION-STATE Completed summary}
**Current**: {SESSION-STATE In Progress}
**Next**: {SESSION-STATE Remaining first actionable item}
**지난 대화 핵심**: {SESSION-STATE 대화 요약 decision/context 1-2 lines}
---

Ask: "Ready to continue?"
