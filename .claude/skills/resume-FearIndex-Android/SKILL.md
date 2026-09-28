---
name: resume-FearIndex-Android
description: "[v2.4.0] Restore the FearIndex-Android session — attaches the saved state and referenced files, re-checks reality, then continues. Run git pull first."
disable-model-invocation: true
---
# Resume — FearIndex-Android

## 1. 저장된 상태 (호출 즉시 자동 첨부)
@.claude/memory/resume-FearIndex-Android.md
@.claude/memory/MEMORY.md
@CLAUDE.md
@docs/checkpoints/HANDOFF-v1.6.1.md
@.claude/memory/bugs-fixed.md
@.claude/memory/deployment.md
@.claude/memory/secrets-env.md
@docs/handoff/ios-appcheck-401-handoff-2026-08-21.md
@docs/checkpoints/SESSION-STATE.md

- 첨부되지 않은 파일(없거나 새로 생김)이 resume 파일 안에 `@` 로 적혀 있으면 Read 로 직접 연다.

## 2. 실측 재검증 (git pull 은 사용자가 호출 전에 직접 한다 — 스킬은 pull 하지 않는다)
`git branch --show-current`, `git log --oneline -10`, `git status`. 기록된 배포/외부 상태는 가능한 명령·API·콘솔로 다시 확인. 기록과 현실이 다르면 **차이를 먼저 보고**하고 resume 파일을 현행화한다.

## 3. 브리핑 후 바로 시작
브랜치 / 한 줄 상태 / 완료 / 다음 할 일 3~5줄 브리핑 → "다음 세션 첫 명령"부터 이어서 작업. 보고는 한국어.
