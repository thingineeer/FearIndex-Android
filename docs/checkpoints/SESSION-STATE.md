# Session State — FearIndex-Android

## Date
2026-05-11

## Branch
`dev` (clean) — `release` 브랜치 + `v1.0.0` annotated tag 신규 생성, 원격 push 완료

## Version
- **v1.0.0 정식 태깅 완료** (versionCode 7) — 2026-05-09 Production 출시 통과
- 다음: v1.0.1+ 부터는 개선만. `dev`에서 `feature/v1.0.1-<기능>` 분기로 시작

## ✅ Completed (이번 세션)

### Play Console — 한국어 앱 이름 단순화
- `Fear & Greed Index - VIX` (영문) / `공포지수 - Fear & Greed Index` (ko-KR) → **`공포지수`** 로 단축
- ko-KR 자세한 설명도 iOS `fastlane/metadata/ko/description.txt` 기반으로 동기화
- Play Console 검토 전송 완료 (수 시간 ~ 1~2일 내 한국 스토어 반영)
- ⚠️ 차트 기간/물림 카운터 등 Android 실제 기능 일부 description에서 빠짐 — iOS SSOT가 틀린 것으로 결론, **iOS 쪽에서 수정 예정**

### v1.0.0 정식 태깅
- `release` 브랜치 신규 생성 (CLAUDE.md 규칙 따름)
- `v1.0.0` annotated tag (author: thingineeer, HEREDOC 메시지)
- 원격 push 완료: `dev`, `release`, `v1.0.0`

### 워크트리/브랜치 정리
- `feature/v1.0.3-promo-graphics` → 미머지 settings 커밋 dev로 cherry-pick 후 삭제
- `feature/v1.0.3-ads-locale-ui` → 단순 머지만 있어서 그냥 삭제
- 워크트리 2개 (`FearIndex-Android-promo`, `FearIndex-Android-v103`) 제거
- 결과: 브랜치 3개(`dev` / `main` / `release`), 워크트리 1개(본진)만 남음

### 빌드 산출물 정리
- `app/build` 655M, `presentation/build` 51M 등 778M → 0
- 프로젝트 총 용량: 778M → **247M** (약 530M 절감)
- `.gradle/` configuration-cache 포함 모두 삭제 — 다음 빌드 시 자동 재생성

### docs 정리 (5건 삭제)
- 일회성 문서 5개 git rm + 커밋:
  - `CHANGELOG-v1.0.1-closed-testing.md`
  - `V103-MANUAL-RELEASE.md`
  - `planning/iOS-prompt-v1.7.10.md`
  - `checkpoints/IOS-SYNC-TODO.md`
  - `planning/returnData-dynamic-events.md`
- 유지 3건: `GOOGLE-PLAY-INTERNAL-TEST.md`, `checkpoints/SESSION-STATE.md`, `TABLET-SCREENSHOTS.md`

## 🛑 미완료 (사용자 직접 작업 필요)

### Play Console — Celestial Oracle / FLIPOP / 옛 공포지수 영구 삭제
- 진입 경로 확인됨: 각 앱 → **테스트 및 출시 → 고급 설정 → 앱 이용 가능 여부 → 앱 삭제**
- ⚠️ 본인 인증 다이얼로그가 **거래 ID + 패키지명** 요구 → Gmail에서 "개발자 등록 수수료" 검색해서 transaction ID 찾아야 함
- 자동화 불가 (transaction ID는 사용자만 알 수 있음)

| 앱 | 패키지명 |
|---|---|
| Celestial Oracle - Saju | `com.celestialoracle.saju_app` |
| FLIPOP | `com.thingineeer.flipop` |
| 공포지수 (옛 e3) | `com.thingineeer.fearindex` |

## Key Files

| 파일 | 역할 |
|---|---|
| `CLAUDE.md` | 프로젝트 절대 규칙 (스크린샷 모드 / 패키지 컨벤션 / Git 워크플로우) |
| `.claude/memory/MEMORY.md` | 메모리 인덱스 |
| `.claude/memory/bugs-fixed.md` | 버그 이력 (17번까지) |
| `.claude/memory/deployment.md` | AAB / Play Console / 활성 키 SHA1 |
| `.claude/rules/git-workflow.md` | 브랜치/워크트리 규칙 (release/dev/feature) |
| `presentation/.../component/AdBanner.kt` | `isScreenshotMode()` SystemProperties 체크 — 캡처 시 광고 hide |
| `scripts/screenshots/capture-tablet-all-locales.sh` | 7"/10" 태블릿 캡처 |
| `app/build.gradle.kts` | versionCode 7, versionName 1.0.0 |
| `docs/GOOGLE-PLAY-INTERNAL-TEST.md` | Play Console 수동 배포 절차 |

## Notes

### Git 상태 (push 완료)
- `dev` HEAD: `398a8cc` chore(docs): v1.0.0 후 일회성 docs 5건 정리
- `release` HEAD: `acf9876` (= `tag: v1.0.0`)
- `main`: `4372efb` (초기 scaffold 시점 유지 — 의도적)

### 새 머신 셋업 (절대 규칙)
```bash
gh repo clone thingineeer/FearIndex-Android ~/Desktop/FearIndex-Android
cd ~/Desktop/FearIndex-Android
gh repo clone thingineeer/thingineeer-env ~/thingineeer-env
bash ~/thingineeer-env/android/fearindex/install.sh
```

### v1.0.1 시작 시 워크플로우
```bash
# 1. dev에서 버전 브랜치 분기
git checkout dev && git pull
git checkout -b feature/v1.0.1

# 2. 기능별 worktree
git worktree add ../FearIndex-Android-<기능> feature/v1.0.1-<기능>

# 3. 작업 → --no-ff merge (squash 절대 금지)
git checkout feature/v1.0.1
git merge --no-ff feature/v1.0.1-<기능>

# 4. dev 머지 → Play Store 통과 시 release 머지 + tag
```
