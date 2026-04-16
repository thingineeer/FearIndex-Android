# Secrets (절대 규칙)

## 보관 위치 — 이 규약만 사용

**로컬 시크릿 폴더: `~/fearindex-secrets/`** (home 디렉토리, 프로젝트 외부)

모든 빌드/배포 관련 비밀 파일은 **반드시 이 폴더 안에 저장**. 다른 경로 사용 금지.

## 포함 파일

| 파일 | 심볼릭/복사 대상 | 용도 |
|---|---|---|
| `~/fearindex-secrets/fearindex-release.keystore` | (그대로 사용) | 릴리즈 서명 키 |
| `~/fearindex-secrets/gradle.properties` | `~/.gradle/gradle.properties`로 복사/append (install.sh가 처리) | keystore 비밀번호 (FEARINDEX_*) |
| `~/fearindex-secrets/google-services.json` | `app/google-services.json`로 심볼릭 링크 | Firebase 설정 |

## install.sh

다른 맥으로 이동/새 셋업 시:

```bash
# 1. 다른 맥에서 ~/fearindex-secrets/ 통째로 AirDrop/iCloud으로 복사
# 2. 이 맥에서:
bash ~/fearindex-secrets/install.sh
```

- idempotent — 여러 번 실행해도 안전
- google-services.json을 `app/` 에 심볼릭 링크
- `~/.gradle/gradle.properties`에 FEARINDEX_* 없으면 append
- `local.properties` 없으면 SDK 경로 생성

## 절대 금지

- **Git 커밋 금지** — keystore/google-services.json/gradle.properties 어떤 경우에도 커밋 금지
  - `.gitignore`에 이미 등록 (@../../.gitignore 참조)
  - 만약 실수로 stage되면 `git rm --cached` 로 제거 후 커밋
- **Slack/이메일/클라우드 공유 링크 금지** — AirDrop 또는 USB만
- **public/private repo 구분 없이 모든 원격 저장소 금지**
- **`~/fearindex-secrets/`를 git 서브모듈로 만들지 말 것**

## 권한

```
~/fearindex-secrets/                  drwx------  (0700, 본인만)
~/fearindex-secrets/*.keystore        -rw-------  (0600)
~/.gradle/gradle.properties           -rw-------  (0600)
~/fearindex-secrets/google-services.json  -rw-r--r--  (공개 가능 — Firebase public config)
```

## 분실 시

| 파일 | 복구 |
|---|---|
| `fearindex-release.keystore` | 다른 백업 매체(다른 맥/iCloud/외장SSD). 전부 분실 시 Play Console → 앱 서명 → **업로드 키 재설정** 신청 |
| `google-services.json` | Firebase Console 또는 `firebase apps:sdkconfig ANDROID <app-id> --out ~/fearindex-secrets/google-services.json` 으로 재다운로드 |
| `gradle.properties` | 템플릿은 `~/fearindex-secrets/README.md` 참조. 비밀번호는 1Password/개인 비밀번호 매니저에서 복구 |

## 백업 원칙

- **주 작업본**: 사용 중인 맥 `~/fearindex-secrets/`
- **보조 백업**: 다른 맥 `~/fearindex-secrets/` (AirDrop 주기 동기화)
- **장기 백업**: iCloud Drive 개인 폴더 또는 외장 SSD (연 1회 이상)

절대 하나의 매체에만 두지 말 것. 분실 시 복구 비용이 큼.

## Firebase CLI로 google-services.json 재생성

```bash
firebase login:list  # 로그인 확인 (dlaudwls1203@gmail.com)
firebase --project fear-index-a4f4b apps:list android  # App ID 확인
firebase --project fear-index-a4f4b apps:sdkconfig ANDROID <app-id> --out ~/fearindex-secrets/google-services.json
```

- Production app ID: `1:8243517543:android:4a16add6d8688aea131cc2`
- Debug app ID: `1:8243517543:android:14bef5a3884ce4e6131cc2`
- **Production 쪽 json이 양쪽 package_name을 모두 포함**하므로 production 하나만 다운받으면 충분.

## 검증

```bash
# 시크릿 폴더 상태 확인
ls -la ~/fearindex-secrets/
# 심볼릭 링크 검증
ls -la app/google-services.json | head -1
# gradle.properties에 FEARINDEX_* 존재 확인
grep FEARINDEX_ ~/.gradle/gradle.properties
```

## 관련 문서

- `~/fearindex-secrets/README.md` — 파일 포맷 상세
- `~/fearindex-secrets/install.sh` — 자동 셋업 스크립트
- @../memory/deployment.md — 릴리즈 빌드 절차 (FEARINDEX_* 참조)
- @../memory/firebase-setup.md — Firebase 앱 ID / CLI 사용법
