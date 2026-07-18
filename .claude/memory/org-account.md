# 조직 계정 전환 & DUNS

개인 개발자 계정을 **조직(법인) 계정**으로 전환하기 위한 정보와 절차 메모.
FearIndex는 iOS/macOS/Android 단일 프로덕트이므로 조직명/식별자는 플랫폼 간 일관성 유지 대상.

## DUNS 발급 정보 (2026-06-26 발급 통보)

| 항목 | 값 |
|---|---|
| **D-U-N-S Number** | `696610806` |
| **Legal Business Name** | `ImaJine` (Apple 등록 시 대소문자 그대로) |
| 발급기관 | D&B (Dun & Bradstreet) |
| Case # | `34617394` |
| 발급 통보일 | 2026-06-26 |
| **Apple 등록 가능 시점** | 발급 후 24-48시간 후 (≈ 2026-06-27 ~ 2026-06-28 이후) |
| 1차 용도(안내문 기준) | **Apple** Developer Program 조직 계정 등록 |

> D&B 안내문 원문: *"To register with Apple, please enter your D-U-N-S Number and Business Name exactly as we have provided above. Your newly-created DUNS number will be eligible for Apple Registration after 24-48 hours."*
> Apple 등록 문제 시 연락처: https://developer.apple.com/contact/phone/

### ⚠️ DUNS 조회 실패 (2026-06-26, Google Play 조직 전환 중) — 24-48h 후 재시도 대기

- Play Console 개인→조직 전환 → 결제 프로필 연결 → **"조직의 D-U-N-S 번호 입력"** 화면에서 `696610806` 입력 시 **"D-U-N-S 번호를 조회할 수 없습니다. 다시 시도해 보세요."** 에러.
- 화면 경고: **"올바른 DUNS를 입력할 수 있는 시도 횟수가 제한되어 있으니 정확하게 입력하세요"** → **무작정 재시도 금지**(계정 잠김 위험). 다이얼로그는 "취소"로 안전하게 닫음(시도 횟수 미소모).
- **가장 유력한 원인 = 전파 지연**: DUNS가 오늘(2026-06-26) 막 발급되어 Google이 아직 D&B에서 696610806을 조회 못 함. Apple뿐 아니라 Google 조회도 24-48h 전파 지연 탑.
- **조치 (사용자 결정)**: **24-48h 기다렸다 재시도** (≈ 2026-06-27~28 이후). 그동안 "딸깍" 개발 진행. 재시도해도 실패하면 ① D&B에 Case #34617394로 활성화 상태+사업자명/주소 확인 ② Google Play KYD 문의 `https://support.google.com/googleplay/android-developer/contact/kyd`.
- **재시도 시 주의**: 결제 프로필의 조직명/주소가 D&B 등록 정보(ImaJine)와 **정확히 일치**해야 함. DUNS는 9자리 숫자만(`696610806`).

## ✅ 핵심 사실 (2026-06-26 Google 공식 문서로 확정): 조직 계정은 14일 테스트 "면제"

사용자 동기 = **"신규 앱(딸깍 등)을 Closed Testing 14일 없이 빨리 production 내보내려고 조직계정으로 가면 빨라지나?"** → **YES. 사용자 서치가 맞았음.**

확정된 사실:

1. **14일 × 12명 Closed Testing 요건은 *개인(personal)* 계정 전용.** Google 공식 문서 제목 자체가 **"App testing requirements for new *personal* developer accounts"** (support.google.com/.../answer/14151465), 적용 대상 = **"personal accounts created after November 13, 2023"**. → **조직(organization) 계정은 이 요건의 적용 대상이 아님 = 면제(exempt).** 검증된 조직 계정은 14일 테스트 없이 바로 production 가능.
2. **단, 조직 계정 생성/검증에는 D-U-N-S 필수.** (개인 계정과 달리 조직은 DUNS 필요.) → **이미 696610806/ImaJine 확보 완료** = 조직 계정 생성 조건 충족.
3. **딸깍(대기자 多) 빠른 출시 = 조직계정 전환이 정확한 해법.** 개인 계정으로 새 앱 내면 14일 테스트 강제, 조직 계정이면 면제.
4. FearIndex(개인 계정·170명·production)는 **이미 14일 통과·운영 중**이라 후속 업데이트는 영향 없음. 조직 전환의 실익은 *신규 앱*(딸깍 등)에 집중됨.

> ⚠️ deep-research(workflow `wfj00k3sz`)는 이 "조직 면제"를 rate limit으로 못 잡고 "면제 근거 없음"으로 결론냈으나, 후속 WebSearch+공식문서 WebFetch로 **조직 면제가 사실임을 확정**. 교훈: deep-research가 rate-limited면 결론을 단정하지 말고 핵심 클레임만 직접 재확인.

> 미해결 디테일(다음 확인): ① 기존 개인 계정을 조직으로 *전환*하는 경로가 있는지, 아니면 *신규 조직 계정 생성*만 되는지(신규 생성이면 별도 $25 + 검증). ② 신규 조직 계정도 "조직이라서 처음부터 면제"가 맞는지(개인 계정의 14일은 생성시점 기준이므로, 조직은 유형 기준으로 면제 — 공식문서상 personal 한정이라 조직은 무관).

## 플랫폼별 조직 전환 정리

| 플랫폼 | DUNS 필요? | 14일 테스트와 관계 | 이 repo 작업 가능? |
|---|---|---|---|
| **Apple Developer** | ✅ 필수 (696610806/ImaJine) | 무관 (App Store 심사 1-3일) | ❌ iOS 영역, 읽기 전용 (가이드/문서만) |
| **Google Play** | ✅ **조직 계정 생성/전환에 필수** | **조직 계정 = 14일 면제** (개인 전용 규칙) | ✅ Android 배포 계정 |

## Google Play 개인→조직 전환 절차 (2026-06-26 콘솔 실측)

**기존 개인 계정을 새로 만들 필요 없이 직접 전환 가능** (앱·평점 유지). 경로:

1. Play Console → **개발자 계정**(좌측 메뉴) → **내 정보** 섹션 → 우측 `→`(편집) → **"계정 유형 변경"** 파란 링크.
   - 직접 URL: `/console/u/0/developers/5351376807423705889/account/developer-details?tab=aboutYou`
2. **"계정 유형을 변경하는 데 필요한 사항"** 다이얼로그 — 필요 3가지: ① 조직 D-U-N-S ② 개발자 프로필 전화·이메일 ③ Google 연락처 전화·이메일. → **다음**.
3. **"먼저 결제 프로필을 연결하여 조직 세부정보를 제공하고 조직을 인증하세요"** 단계.
   - ⚠️ **DUNS는 이 다이얼로그에 직접 입력 안 함**. **Google 결제 프로필(Payments profile)**에 조직 등록 → Play Console이 조직명·주소·DUNS를 끌어옴.
   - "결제 프로필 만들기 또는 선택" → Google Payments 폼에서: 계정유형=**비즈니스/조직**, 조직 이름=**`ImaJine`**, 주소, **D-U-N-S=`696610806`** 입력 → 저장.
   - **이 단계(결제 프로필 생성·조직 정보 입력)는 계정/결제 설정 생성이라 Claude가 직접 못 함 → 사용자 직접 입력.**
4. 결제 프로필 연결 후 Google이 조직 인증(공식 비즈니스 문서 ↔ DUNS 정보 대조). 인증 완료 시 계정 유형 = 조직.

**복사용 값**: 조직명 `ImaJine` · DUNS `696610806` · (Case #34617394).

## 다음 액션 (우선순위)

1. **(사용자 직접) Google 결제 프로필에 조직(ImaJine/DUNS 696610806) 등록** → Play Console 개인→조직 전환 완료. 화면은 "결제 프로필 연결" 단계까지 열어둠.
2. 전환·조직 인증 완료 후 → **"딸깍"(ddalggak) 등 신규 앱을 14일 테스트 없이 바로 production** 출시 가능.
3. **(Apple, 24-48h 후) DUNS 696610806 / ImaJine 으로 Apple Developer 조직 등록** — iOS 영역. 이 Android 세션에서는 가이드만.
4. 조직명 `ImaJine` 확정 시 → Play Console 개발자명/Apple 팀명/스토어 리스팅 일관성 점검 (iOS parity).
5. **(백그라운드) 0명 앱 3개 영구 삭제** — Celestial Oracle(`4975814764787797048`)/공포지수 구오타(`4973406323299470181`)/FLIPOP(`4974542515963092356`). 전부 초안·미심사·0명이라 삭제 가능. 단 좌측 메뉴에 "앱 삭제" 항목 안 보임 → 삭제 진입 경로 추가 확인 필요. FearIndex(`4973920645070208584`, 170명)는 보존.

## 관련 문서
- @../../CLAUDE.md — Git Workflow(Closed Testing 14일), 작업 범위(iOS 읽기 전용)
- @deployment.md — Play Console 업로드 + 출시 이력(v1.0.0~v1.2.0 production 게시 = 14일 통과 가능성)
- @secrets-env.md — 콘솔 계정(`PLAY_CONSOLE_ACCOUNT`=dlaudwls1203@gmail.com)
- @../../docs/GOOGLE-PLAY-INTERNAL-TEST.md — Play Console 트랙 절차
