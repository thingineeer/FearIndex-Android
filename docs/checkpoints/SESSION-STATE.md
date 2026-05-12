# Session State — FearIndex-Android

## Date
2026-05-12

## 🚀 Release Status

**v1.0.1 — 정식 게시 완료 (LIVE on Google Play, 2026-05-12)**

- 트랙: **Production** (100% rollout)
- versionCode: **9** / versionName: **1.0.1**
- 게시 방식: `fastlane production` 한 줄 자동화 (Google 검토 통과 후 즉시 자동 게시)
- 출시 노트: **"안정성을 개선하였습니다."** (45 locale 동일 메시지)
- v1.0.0 사용자 전원에게 점진 자동 업데이트 진행 중

## Branch
`dev` (clean) — `release` 브랜치 + `v1.0.1` annotated tag 머지/푸시 완료, **production 100% rollout 게시 완료** ✅

## Version
- **v1.0.1 production 정식 게시 완료** ✅ (versionCode 9) — 2026-05-12 Google 검토 통과 → 100% rollout 라이브
- `fastlane production` 한 줄로 AAB + 45 locale 메타 + 스크린샷 + changelog 일괄 푸시 검증됨
- 다음: v1.0.2+ 부터는 `dev`에서 `feature/v1.0.2-<기능>` 분기로 시작

## 📊 출시 이력

| 버전 | versionCode | 게시일 | 상태 | 주요 변경 |
|---|---|---|---|---|
| v1.0.0 | 7 | 2026-05-09 | 정식 게시 ✅ | 최초 출시 |
| **v1.0.1** | **9** | **2026-05-12** | **정식 게시 ✅ (LIVE)** | **Android 15 edge-to-edge 호환성 + 안정성 개선, `fastlane production` 자동화 첫 사용** |

## ✅ Completed (이번 세션)

### v1.0.1 production 출시 자동화 — fastlane 한 줄
- `fastlane production` lane 신규 추가 (`Fastfile`): AAB 빌드 + 45 locale 메타 + 스크린샷 + changelog → production 트랙 100% rollout completed
- Service Account JSON 발급/검증/secrets 보관 자동화
- ko/en/43 locale 메타 + 5장 phone + 4장 7"/10" 태블릿 스크린샷 + changelog 일괄 푸시 (총 ~600개 자산, 220초)
- Google Play Console 페이지에서 "변경된 항목: 프로덕션 1.0.1 전체 출시 시작" 확인됨

### Android 15 (targetSdk 35) edge-to-edge 호환성
- Play Console 경고 2건 해결: "더 넓은 화면 표시 안 됨" + "지원 중단된 API 사용"
- `themes.xml` deprecated `android:statusBarColor` + `android:navigationBarColor` 제거
- MainActivity 의 `enableEdgeToEdge()` + Material3 Scaffold 자동 inset 처리 검증 완료
- 에뮬레이터 (Medium_Phone_API_36.1) 실측: status/navigation bar 영역 정상 회피

### Play Console Service Account 발급
- `fastlane-deploy@fear-index-a4f4b.iam.gserviceaccount.com` SA 의 옛 키 2개 (2026-02-19, 2026-05-09) 삭제 + 신규 키 발급 (private_key_id `7c24583643f1...`)
- `~/fearindex-secrets/play-store-service-account.json` (chmod 600) + `~/thingineeer-env/android/fearindex/play-store-service-account.json` (private repo SSOT) 양쪽 보관
- `install.sh` 에 SA JSON 자동 처리 추가 → 새 머신 셋업 시 한 줄로 복원 가능
- Play Console 에서 SA 에 "관리자(모든 권한)" 부여 (사용자 수동)

### Analytics 영문화 시도 → 잘못된 진단으로 revert
- "Firebase Analytics 가 한글 이벤트 이름을 drop 한다" 단언했으나 사용자가 Console 스크린샷 (iOS 한글 이벤트 632K건 정상 수집) 으로 반박 → 영문화 머지 커밋 `6d2d126` revert
- 메모리 `bugs-fixed.md` 18번에 잘못된 진단 + 교훈 기록 (실측 우선 원칙)

### Git 워크플로우
- 4개 feature worktree 사용: `release-fastlane`, `analytics-en-names`(revert됨), `edge-to-edge`, `production`
- 모두 `--no-ff` 머지로 분기/합류 그래프 보존
- 9개 브랜치 + `v1.0.1` annotated tag 모두 origin push 완료

## 🛑 미완료 (사용자 직접 작업 필요)

### Play Console — Celestial Oracle / FLIPOP / 옛 공포지수(`com.thingineeer.fearindex`) 영구 삭제
- 진입 경로: 각 앱 → 테스트 및 출시 → 고급 설정 → 앱 이용 가능 여부 → 앱 삭제
- ⚠️ 본인 인증 다이얼로그가 거래 ID + 패키지명 요구 → Gmail "개발자 등록 수수료" transaction ID 필요
- 자동화 불가 (transaction ID 는 사용자만 알 수 있음)

| 앱 | 패키지명 |
|---|---|
| Celestial Oracle - Saju | `com.celestialoracle.saju_app` |
| FLIPOP | `com.thingineeer.flipop` |
| 공포지수 (옛 e3) | `com.thingineeer.fearindex` |

### iOS 동기화
- iOS 측에서도 "안정성을 개선하였습니다." changelog 로 동일 버전 출시할지 결정 필요 (별도 세션)

## Key Files

| 파일 | 역할 |
|---|---|
| `CLAUDE.md` | 프로젝트 절대 규칙 (스크린샷 모드 / 패키지 컨벤션 / Git 워크플로우) |
| `.claude/memory/MEMORY.md` | 메모리 인덱스 |
| `.claude/memory/bugs-fixed.md` | 버그 이력 (18번까지) |
| `.claude/memory/deployment.md` | AAB / Play Console / 활성 키 SHA1 |
| `.claude/rules/git-workflow.md` | 브랜치/워크트리 규칙 (release/dev/feature) |
| `fastlane/Fastfile` | `internal` / `production` / `promote_to_*` lanes — `fastlane production` 한 줄로 출시 |
| `~/fearindex-secrets/play-store-service-account.json` | fastlane supply 인증 (chmod 600) |
| `~/thingineeer-env/android/fearindex/` | secrets SSOT (private GitHub repo) |
| `app/build.gradle.kts` | versionCode 9, versionName 1.0.1 |
| `app/src/main/res/values/themes.xml` | deprecated statusBarColor/navigationBarColor 제거됨 |

## Notes

### Git 상태 (push 완료)
- `dev` HEAD: `0cd9e1f` (v1.0.1 production 배포 준비 머지)
- `release` HEAD: `e6577ea` (= `tag: v1.0.1`)
- `main`: `4372efb` (초기 scaffold 시점 유지 — 의도적)

### fastlane 워크플로우 (v1.0.2+ 기준)
```bash
# 1. 코드 작업 끝 (worktree → feature → dev 머지)
# 2. app/build.gradle.kts: versionCode 9 → 10, versionName "1.0.1" → "1.0.2"
# 3. fastlane/metadata/android/<locale>/changelogs/10.txt 작성 (45 locale)
# 4. 한 줄:
fastlane production  # AAB 빌드 + 메타 + 스크린샷 + changelog 일괄 → production 100%
# 5. Play 검토 통과 후 release 머지 + tag
```

### 새 머신 셋업 (절대 규칙)
```bash
gh repo clone thingineeer/FearIndex-Android ~/Desktop/FearIndex-Android
cd ~/Desktop/FearIndex-Android
gh repo clone thingineeer/thingineeer-env ~/thingineeer-env
bash ~/thingineeer-env/android/fearindex/install.sh
# → keystore + gradle.properties + google-services.json + play-store-service-account.json 자동 설치
```
