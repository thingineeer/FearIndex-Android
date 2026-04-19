# Session State — FearIndex-Android

## Date
2026-04-19 (12:40 KST 진행 중)

## Version
- `versionCode`: 2
- `versionName`: 1.0.1
- AAB 산출물: `app/build/outputs/bundle/release/app-release.aab` (10.3MB, 2026-04-16 빌드)
- AAB 서명 SHA-1: `81:AD:9D:5D:9A:E1:50:EB:F1:AE:9D:AF:86:CB:03:3D:67:6B:2A:75` — **upload_cert.pem 과 완전 매칭 검증됨**

## Branch
`dev` — origin/dev 동기화, working tree clean

## Active Worktrees
본진 `/Users/imyeongjin/Desktop/FearIndex-Android [dev]` 1개만

## Completed (이번 세션 2026-04-19)

### 1. 빌드 검증
- AAB 파일 존재 확인 (`app/build/outputs/bundle/release/app-release.aab`, 10.3MB)
- `jarsigner -verify` → 서명자 `CN=LeeMyeongJin, OU=Mobile, O=th1ngjin, L=Seoul, ST=Seoul, C=KR`
- AAB 서명 SHA-1/256 와 `~/fearindex-secrets/upload_cert.pem` SHA 완전 일치 확인 ✅
- 13:33 KST (업로드 키 유효 시각) 이후 재빌드 없이 그대로 업로드 가능
- `./gradlew :app:assembleDebug` BUILD SUCCESSFUL

### 2. Play Console Closed Testing (비공개 테스트 - Alpha) 세팅
Chrome MCP로 `dlaudwls1203@gmail.com` Play Console 접근 (u/1).
앱 ID `4973920645070208584` (공포지수, `th1ngjin.fearindex`).

트랙 진행 상황 **2/4 완료**:
- ✅ **국가 선택**: 대한민국만 (추후 Production 승급 시 전세계 확장 예정)
- ✅ **테스터 선택**: 기존 "내부테스터" 이메일 목록 이 트랙에 연결
- 🟡 **버전 생성 및 출시**: 대기 (AAB 업로드 키 유효시각 13:33 KST 이후)
- 🔒 **버전 미리보기 및 확인** → **검토를 위해 Google에 버전 전송**: 위 단계 완료 후

### 3. 14일 × 12명 정책 정리 (Google 신규 개인 개발자 요건)
- Internal Testing은 14일 카운트 **안 쌓임** (100명 제한만)
- **Closed Testing에서 12명 전원이 동시에 14일 연속 opt-in** 유지해야 Production 자격
- 12명이 모인 가장 늦은 시점 이후부터 14일 타이머
- 중도 이탈 시 타이머 흔들림 → 실제론 14~16명 여유있게 권장

## In Progress
- **이메일 목록 "내부테스터" 편집** (Chrome MCP로 모달 열린 상태로 세션 종료되었을 수 있음)
- 현재 등록: `dlaudwls1203@gmail.com` 본인 1명만
- 사용자가 8명 이메일 모은 상태, 본인이 직접 전달 대기
- 목표: 12명+ (넉넉히 14~16명)

## Remaining

### 1. 이메일 목록 채우기 (사용자 액션 필요)
- [ ] Closed Testing "내부테스터" 목록에 **최소 11명 Google 계정 이메일** 추가 (본인 제외)
- 쉼표 구분으로 일괄 입력 가능 (모달의 "이메일 주소 추가" 필드)
- 또는 CSV 파일 업로드 옵션 존재
- 현재 보유: 8명 / 필요: 11명+ (본인 제외)

### 2. AAB 업로드 (13:33 KST 이후)
- [ ] Chrome Play Console → 비공개 테스트 - Alpha → "새 버전 만들기"
- [ ] 업로드 파일: `/Users/imyeongjin/Desktop/FearIndex-Android/app/build/outputs/bundle/release/app-release.aab`
- [ ] 출시명 `2 (1.0.1)` / 출시 노트 (45 locale — `fastlane/metadata/android/<locale>/changelogs/2.txt` 자동)
- **Chrome MCP 제약**: `<input type="file">` macOS 네이티브 다이얼로그 제어 불가 → **사용자 수동 드래그앤드롭**

### 3. Google 심사 전송
- [ ] "검토를 위해 Google에 버전 전송" 버튼 클릭
- [ ] 심사 대기 (신규 앱 첫 심사 보통 수 시간~며칠)
- [ ] 심사 통과 → `(unreviewed)` 임시 이름 정식 이름으로 전환

### 4. 테스터 opt-in → 14일 타이머 시작
- [ ] 심사 통과 후 테스터 초대 링크 공유 (Web + Android)
- [ ] 12명 전원 "테스터 되기" 클릭 + Play Store에서 앱 설치 확인
- [ ] 마지막 opt-in 시점부터 14일 카운트다운 시작
- [ ] 14일 후 Production 트랙 신청 자격 획득

### 5. Play Console 필수 선언 (심사 전 완료 필수)
- [ ] 콘텐츠 등급 설문
- [ ] 데이터 보안 양식
- [ ] 앱 액세스 / 광고 / 타겟층 선언
- [ ] 개인정보처리방침 URL

### 6. Firebase Console
- [ ] App Check debug token 등록
- [ ] 새 keystore SHA-1 `81:AD:9D:5D:9A:E1:50:EB:F1:AE:9D:AF:86:CB:03:3D:67:6B:2A:75` 등록 확인

### 7. 추후 (v1.0.2+)
- [ ] SkeletonView 검토
- [ ] 시장/암호화폐 알림 설정 분리
- [ ] iOS hosting URL 공유 링크 반영
- [ ] InsightGenerator 단위 테스트

## Key Files

### 현재 관여 파일 (세션 재개 시 먼저 읽을 곳)
- `docs/checkpoints/SESSION-STATE.md` — 이 파일 (save point)
- `app/build/outputs/bundle/release/app-release.aab` — Play Console 업로드 대상 (10.3MB, 서명 검증 완료)
- `~/fearindex-secrets/upload_cert.pem` — 업로드 인증서 (Play Console 키 재설정 매칭 대상)
- `fastlane/metadata/android/*/changelogs/2.txt` — v1.0.1 출시 노트 45 locale
- `fastlane/metadata/android/*/{title,short_description,full_description}.txt` — Store listing 45 locale
- `fastlane/metadata/android/*/images/phoneScreenshots/{1_home,2_chart,3_vote,4_notification_settings}.png` — 45 × 4 = 180장

### Play Console 상수
- 개발자 계정 ID: `5351376807423705889`
- 앱 ID: `4973920645070208584` (공포지수, `th1ngjin.fearindex`)
- Closed Testing 트랙 ID: `4699045907541260404`
- Chrome URL: `https://play.google.com/console/u/1/developers/5351376807423705889/app/4973920645070208584/closed-testing`

## Notes

### Chrome MCP 계정 주의
- 이 세션 시작 시 `/swap-server`로 Chrome Profile 18 (cgmsw 회사AI계정) 열림 → Play Console 접근 불가 (개인 Gmail 없음)
- **다음 세션**: 개인 Gmail `dlaudwls1203@gmail.com` 로그인된 Chrome Profile 사용 필수
- Chrome MCP `u/0`, `u/1`, `u/2` 인덱스 주의 — 계정별 다른 결과

### 업로드 키 재설정 상태
- Google 재설정 승인 완료 (이전 세션에 요청)
- 유효 시작 시각: `2026-04-19 04:33:09 UTC` = `2026-04-19 (일) 13:33 KST`
- 현재 AAB는 새 keystore (`~/fearindex-secrets/fearindex-release.keystore`, alias `upload`) 서명됨
- **검증 완료**: AAB SHA-1 = PEM SHA-1 완전 일치 → 13:33 이후 바로 업로드 통과 예정

### Google 14일 × 12명 정책 (중요)
- Internal Testing ≠ Closed Testing
- Closed Testing에서만 14일 타이머 유효
- 12명이 한꺼번에 opt-in하는 게 가장 빠름 — 늦게 들어온 사람이 있으면 그 시점부터 타이머 재시작
- 본인 포함 현재 계획: 9명 (사용자 8명 + 본인) → 최소 3명 더 필요
- 안전 권장 14~16명

### Chrome MCP 파일 업로드 제약
- `<input type="file">` macOS 네이티브 다이얼로그 제어 불가
- AAB 업로드는 사용자 수동 드래그앤드롭 필수
- 또는 Fastlane + Play Store service account JSON 발급하여 자동화 가능

### Keystore 정보 (변경 없음)
- 위치: `~/fearindex-secrets/fearindex-release.keystore` (PKCS12)
- Alias: `upload`
- 비밀번호: `~/.gradle/gradle.properties` 의 `FEARINDEX_STORE_PASSWORD`
- PEM: `~/fearindex-secrets/upload_cert.pem`

## 다음 세션 시작 시
1. `/resume-FearIndex-Android` 실행
2. 현재 시각이 13:33 KST 지났는지 확인
3. 사용자에게 12명 이메일 모집 진행률 확인
4. 준비되면 Chrome에서 개인 Gmail 프로필로 Play Console 재진입
5. 우선순위:
   1. 이메일 목록에 11명+ 추가 (쉼표 구분 일괄 입력)
   2. AAB 업로드 (사용자 수동 드래그앤드롭)
   3. 출시 노트 + 검토 전송
   4. 심사 통과 대기 → 테스터 초대 링크 공유 → 14일 타이머 시작

## 이번 세션 주요 교훈
- Closed Testing과 Internal Testing을 처음엔 혼동하기 쉬움 — 14일 정책은 **Closed Testing** 에서만 쌓임
- Chrome Profile 계정 index(`u/0`, `u/1`...) 매번 확인 필수. `/swap-server` 후에는 Play Console 계정이 다를 수 있음
- AAB 서명 검증은 `jarsigner -verify -verbose -certs <aab>` + PEM과 SHA-1 직접 비교로 정확 확인 가능
- 업로드 키 재설정은 Google 승인 후 즉시 적용되지 않고 지정된 유효 시각 이후 활성
