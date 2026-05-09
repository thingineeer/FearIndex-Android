# Secrets (절대 규칙)

## SSOT (Single Source of Truth)

**`~/thingineeer-env/android/fearindex/`** (GitHub private repo `thingineeer/thingineeer-env` 에서 clone) — 모든 빌드/배포용 비밀 파일의 원본.

`~/fearindex-secrets/` 는 install.sh 가 복사한 **결과물 폴더** (== 원본 동일).
**옛 AirDrop 절차는 deprecated** — 2026-05-09 사고 이후 단일화 (`@../memory/bugs-fixed.md` 17번 참조).

## 포함 파일 (SSOT 위치)

| 파일 (SSOT) | install.sh 처리 결과 | 용도 |
|---|---|---|
| `~/thingineeer-env/android/fearindex/fearindex-release.keystore` | `~/fearindex-secrets/fearindex-release.keystore` 복사 | 릴리즈 서명 키 (alias=upload) |
| `~/thingineeer-env/android/fearindex/gradle.properties` | `~/.gradle/gradle.properties`로 append | keystore 비밀번호 (FEARINDEX_*) |
| `~/thingineeer-env/android/fearindex/upload_certificate.pem` | `~/fearindex-secrets/upload_certificate.pem` 복사 | Play Console 업로드 키 재설정용 PEM |
| `~/thingineeer-env/android/fearindex/google-services.json` | `~/fearindex-secrets/google-services.json` 복사 → `app/google-services.json` 심볼릭 링크 | Firebase 설정 |

## install.sh

새 맥 / 새 세션 시:

```bash
# 1. env repo clone (한 번만)
gh auth login
gh repo clone thingineeer/thingineeer-env ~/thingineeer-env

# 2. install (idempotent, 여러 번 실행 안전)
bash ~/thingineeer-env/android/fearindex/install.sh
```

`install.sh` 가 처리하는 항목:
- `~/fearindex-secrets/` 디렉토리 생성 (chmod 700) + keystore/cert/gradle.properties 복사 (chmod 600)
- `~/.gradle/gradle.properties` 에 `FEARINDEX_*` append (기존 값 제거 후)
- `~/fearindex-secrets/google-services.json` 복사 + `app/google-services.json` 심볼릭 링크
- `local.properties` 없으면 SDK 경로 자동 생성

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
