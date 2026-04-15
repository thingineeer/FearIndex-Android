---
name: Git Workflow Rule
description: 브랜치 전략, Worktree 단위 작업, 머지 방식, 금지 사항.
type: rule
---

# Git Workflow (절대 규칙)

## Author & Co-Author

- **Author**: `thingineeer <dlaudwls1203@gmail.com>` — 예외 없음
- **Co-Authored-By 금지**: AI (Claude, Copilot 등) 문구 절대 포함 금지

## 브랜치 구조 (FearIndex 표준 — iOS와 동일)

```
release       ◄── (Play Store 배포 완료 후 dev 머지 + 태그)
  ↑
dev           ◄── (모든 개발의 기준선, 테스터 빌드 모음)
  ↑
feature/vX.Y.Z (버전 브랜치, dev에서 분기)
  ↑
feature/vX.Y.Z-기능A  (worktree 1, n개 커밋)  ──git merge──┐
feature/vX.Y.Z-기능B  (worktree 2, n개 커밋)  ──git merge──┼─→ feature/vX.Y.Z
feature/vX.Y.Z-기능C  (worktree 3, n개 커밋)  ──git merge──┘            │
                                                                       │
                                              모든 worktree 완료 시:    │
                                                       dev ←─git merge─┘
                                                       │
                                          Play Store 배포 통과 후:
                                              release ←─git merge─┘ + tag vX.Y.Z
```

## 핵심 규칙

1. **`dev`가 개발 기준선**. `main`이 아니라 `dev`. (iOS 프로젝트는 `main`이 그 역할이지만 Android는 `dev`로 통일)
2. 새 버전 시작 시 **`dev`에서 `feature/vX.Y.Z` 버전 브랜치** 생성.
3. **모든 작업은 반드시 worktree 단위 피처 브랜치**에서 수행: `feature/vX.Y.Z-기능명`.
4. 한 worktree(피처 브랜치)에 **n개 커밋** 만들어도 OK. 단위는 "이 작업이 끝나면 머지할 만한 의미 단위" 기준.
5. 작업 완료 → 버전 브랜치(`feature/vX.Y.Z`)에 **`git merge` (squash 절대 금지)**.
6. 모든 피처 머지 완료 → `dev`에 `git merge`.
7. Play Store 배포 통과 → `release`에 `git merge` + 태그 `vX.Y.Z`.
8. 그래프는 **분기/합류 형태**여야 함 (일자 히스토리 금지).

## Worktree 사용 패턴

병렬/독립 작업은 `git worktree`로 분리해 충돌 최소화:

```bash
# 버전 브랜치에서 worktree 생성
git worktree add ../FearIndex-Android-share feature/v1.0.1-share-feature
git worktree add ../FearIndex-Android-charts feature/v1.0.1-crypto-5y
git worktree add ../FearIndex-Android-skeleton feature/v1.0.1-skeleton-loading

# 각 worktree에서 독립 작업, 여러 커밋
cd ../FearIndex-Android-share
# ... commit, commit, commit
git push -u origin feature/v1.0.1-share-feature

# 작업 완료 → 버전 브랜치로 돌아와서 머지
cd ../FearIndex-Android  # 본진
git checkout feature/v1.0.1
git merge feature/v1.0.1-share-feature   # squash 금지 (그냥 merge)
git merge feature/v1.0.1-crypto-5y
git merge feature/v1.0.1-skeleton-loading

# worktree 정리
git worktree remove ../FearIndex-Android-share
git worktree remove ../FearIndex-Android-charts
git worktree remove ../FearIndex-Android-skeleton

# 모든 피처 합류 후 dev로 머지
git checkout dev
git merge feature/v1.0.1
```

## 절대 금지

| 금지 | 이유 |
|---|---|
| `git cherry-pick` | 일자 히스토리 → 그래프 깨짐 |
| `git merge --squash` | n개 커밋 의미 손실, 부분 revert 불가 |
| `gh pr merge` (squash 옵션) | 위와 동일 |
| 버전 브랜치/`dev`/`release`에 직접 커밋 | 반드시 worktree 피처 브랜치 거쳐야 함 |
| `git push --force` | 명시적 요청 없이 절대 금지 |
| `--no-verify` | hook 건너뛰기 금지 |
| 다른 브랜치 임의 삭제 | 명시적 요청 없이 금지 |

## 커밋 메시지 형식

```
feat: 신규 기능
fix: 버그 수정
chore: 유지보수
refactor: 리팩터링
docs: 문서
test: 테스트
```

HEREDOC으로 작성:

```bash
git commit -m "$(cat <<'EOF'
feat: 차트 드래그 상호작용 추가

- 터치 시 가장 가까운 데이터 포인트에 스냅
- 햅틱 피드백 (TextHandleMove)
- 툴팁 (점수 + 등급 + 날짜)
EOF
)"
```

## Push 규칙

- **Push는 명시적 요청 시에만** 수행
- **커밋 전 현재 브랜치 확인** 필수 — 다른 브랜치에 커밋 금지
- worktree마다 별도 push: `git push -u origin feature/vX.Y.Z-기능명`

## 작업 단위 분할 원칙 (worktree 분리 기준)

다음 단위로 worktree/브랜치를 쪼갤 것:

- **기능별** (예: `share-feature`, `crypto-5y`, `skeleton-loading`)
- **레이어별** (Domain/Data/Presentation 변경이 큰 경우)
- **iOS 포팅 단위별** (한 iOS 컴포넌트 → 한 worktree)
- **버그 fix 별** (관련 없는 fix는 절대 한 브랜치에 묶지 말 것)

같은 worktree 안 커밋들은 **같은 머지 단위**여야 함. 예:
- ✅ `feature/v1.0.1-share` 안: "feat: share intent 추가" + "feat: 45 locale 추가" + "fix: apostrophe escape" + "test: share E2E"
- ❌ `feature/v1.0.1-share` 안: "feat: share intent" + "fix: chart period bug" (관련 없음 → 별도 브랜치)

## 관련 문서

- 사용자 글로벌 규칙 (`~/.claude/CLAUDE.md`) — 절대 규칙의 근거
- @../memory/deployment.md — 태그/릴리즈 절차
- @../../CLAUDE.md — 프로젝트 루트 지침
