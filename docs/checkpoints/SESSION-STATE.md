# Session State — FearIndex-Android

## Date
2026-05-09

## Version (출시 임박)
- 다음 출시: `versionCode 4` / `versionName 1.0.3`
- AAB 재빌드 완료 (활성 keystore SHA1=`CE:08:B4:...` 서명) → Play Console 업로드 가능 상태
- Play Console 출시 노트 45 locale 임시저장 완료

## Branch / Worktrees

| Worktree | 경로 | 브랜치 |
|---|---|---|
| 본진 (dev) | `~/Desktop/FearIndex-Android` | `dev` |
| docs/memory | `~/Desktop/FearIndex-Android-docs-memory` | `feature/v1.0.3-docs-memory` |

이전 promo / v103 worktree 는 dev 머지 완료 상태. 추가 작업 시 새 worktree 필요.

## ✅ Completed (이번 세션 / 2026-05-09)

### Keystore 두 키 혼동 사고 → 해결
- 새 머신에 옛 keystore (SHA1=`81:AD:...`, alias=fearindex) 만 있어 v1.0.3 AAB 업로드 거부
- 진짜 활성 키 발견: `~/thingineeer-env/android/fearindex/` (SHA1=`CE:08:B4:...`, alias=upload)
- 옛 secrets 백업: `~/fearindex-secrets.bak.20260509_163426/`
- `bash ~/thingineeer-env/android/fearindex/install.sh` → keystore + gradle.properties install
- google-services.json 누락 발견 → 백업에서 복원
- AAB 재빌드 + `keytool` 로 SHA1 = `CE:08:B4:...` 검증 완료

### Disk Space 16GB 확보
- 작업 시작 시점 1.9GB 만 남아 에뮬레이터 부팅 실패 위기
- 정리 대상: Chrome cache (`~/Library/Caches/Google/Chrome`), Homebrew, pip, pnpm
- 결과: 16GB 확보, 데이터 손실 없음

### 메모리/문서 큐레이션 (이 작업)
- `.claude/memory/bugs-fixed.md` 17번 항목 추가 (keystore 사고 상세 기록)
- `.claude/memory/deployment.md` SHA1/SHA256 활성 키로 갱신 + 키 이력 표 추가
- `.claude/memory/secrets-env.md` 활성 SHA1 갱신 + 머신 간 sync 절차 추가
- `.claude/rules/secrets.md` SSOT (thingineeer-env) 명시 + 옛 AirDrop 절차 deprecated
- `CLAUDE.md` Release Signing 섹션 갱신
- `~/thingineeer-env/projects/fearindex-android/.env` KEYSTORE_SHA1 정정 + V101 보존
- `~/thingineeer-env/android/fearindex/install.sh` google-services.json 처리 추가

### Play Console
- 출시 노트 45 locale 임시저장 완료 (이전 세션 ko_KR + 이번 세션에서 다른 에이전트가 글자수 초과 fix 진행 중)

## ⏳ Remaining — v1.0.3 출시 작업

### 1. AAB 업로드 (사용자 직접)
- Play Console → Closed Testing Alpha → 새 버전 만들기
- 이미 빌드된 AAB 드래그&드롭: `app/build/outputs/bundle/release/app-release.aab`
- 출시 노트 검토 → 미리보기 → 출시

### 2. 14일 카운트다운
- 12명 테스터 opt-in 유지
- Production 신청 자격 획득

### 3. dev → release 머지 + 태그 (Production 통과 시)
```bash
git checkout release
git merge --no-ff dev
git tag v1.0.3
git push origin release v1.0.3
```

## Key Files (집에서 이어가기 위해 먼저 읽을 것)

| 파일 | 역할 |
|---|---|
| `CLAUDE.md` | 프로젝트 절대 규칙 (Release Signing 섹션 thingineeer-env SSOT 명시) |
| `.claude/memory/MEMORY.md` | 메모리 인덱스 |
| `.claude/memory/bugs-fixed.md` | 버그 이력 (17번까지, 2026-05-09 keystore 사고 포함) |
| `.claude/memory/deployment.md` | AAB / Play Console 절차 + 활성 키 SHA1/SHA256 |
| `.claude/memory/secrets-env.md` | .env 키 목록 + 머신 간 sync 절차 |
| `.claude/rules/secrets.md` | SSOT (thingineeer-env) 규약 |
| `~/thingineeer-env/android/fearindex/README.md` | 활성 keystore SHA1/SHA256 원본 출처 |
| `app/build.gradle.kts` | versionCode 4, versionName 1.0.3 |

## Notes

### 새 머신 셋업 (절대 규칙)
```bash
# 1. clone (이미 했으면 skip)
gh repo clone thingineeer/FearIndex-Android ~/Desktop/FearIndex-Android
cd ~/Desktop/FearIndex-Android

# 2. SSOT (thingineeer-env) clone + install (옛 AirDrop 절차 deprecated)
gh auth login
gh repo clone thingineeer/thingineeer-env ~/thingineeer-env
bash ~/thingineeer-env/android/fearindex/install.sh

# 3. 검증
keytool -list -v -keystore ~/fearindex-secrets/fearindex-release.keystore -alias upload \
  | grep "SHA1:"
# → CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16
```

### 알려진 이슈
- iOS-Android Similar Events 카드 점수 불일치 (게이지 vs 마지막 historical) — 의도된 동작, v1.0.4 백로그
- Play Console 메타 글자수 초과 (title 5, short 16, full 9 locale) — 다른 에이전트가 fix 진행 중. 수동 업로드 영향 없음.

### Chrome MCP / fastlane supply
- Chrome MCP 로 Play Console 이미지 업로드 불가 (macOS 네이티브 다이얼로그)
- fastlane supply 자동화 위해서는 service account JSON 발급 필요 (`~/fearindex-secrets/play-store-service-account.json`)
