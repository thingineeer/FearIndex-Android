# v1.4.1 — 온보딩 코치마크 투어 + 위젯 (iOS v1.9.3 parity)

브랜치: `feature/v1.4.1-onboarding-tour` (dev 기준). 배포 금지 — 검증까지만.
SSOT: `@/Users/imyeongjin/Desktop/FearIndex-iOS/docs/handoff/onboarding-parity-android.md`

## 유저 승인 결정 (확정)
- 8단계 투어 전부 유지, iOS 라벨 100% 동일.
- 7단계(위젯 사용법) 대상 확보를 위해 **위젯 기능 신규 구현**.
- 위젯: **개별 2×2 3종(글로벌/코스피/암호화폐) + 통합 4×2 1종** = Glance 4 provider.
- Glance 1.1.1 + glance-material3, WorkManager 갱신, 탭→앱 딥링크.

## 8단계 매핑 (확정)
| # | 대상 | destination | anchor |
|---|---|---|---|
| 1 | 게이지(market) | 홈+세그먼트 MARKET, 최상단 | FearGaugeView |
| 2 | 게이지(KOSPI) | 세그먼트 KOSPI 자동전환(인터스티셜 억제) | FearGaugeView |
| 3 | 게이지(CRYPTO) | 세그먼트 CRYPTO | FearGaugeView |
| 4 | 인사이트 카드 | 홈 market + 인사이트로 스크롤 | SimilarEvents/InsightTeaser |
| 5 | 투표(물렸어요) | vote 탭 | StuckCounterCard |
| 6 | 알림 설정 행 | settings 탭 | 알림 설정 SettingsItem |
| 7 | 위젯 사용법 행 | settings 탭 + 스크롤 | 위젯 사용법 SettingsItem(신규) |
| 8 | 없음(중앙 카드) | 홈 market 유지 | — |

## 핵심 로직 (iOS mirror)
- **게이팅**: `stuck_counter_prefs/deviceId` raw getString 을 FCM(FearIndexApp registerFCMToken, onCreate) **이전에** 판별 → 새 `onboarding_prefs`에 `onboardingTourEligibleV1` 영구 저장. 신규설치=키없음=자격.
- 투어 뜬 순간 `hasSeenOnboardingTourV1=true` (재노출 차단).
- 종료 시 홈 market 최상단 복귀.
- 투어 중: 인터스티셜(KOSPI 진입)·배너·**앱오픈** 광고 억제, KOSPI/crypto 사전로드(스켈레톤 방지), 세그먼트 애니메이션 최소화.
- 설정 "앱 사용법" 행 → 1단계 재생(신규/기존 공통).
- GA: onboarding_done/onboarding_skip, param step(1-based). AnalyticsEvent.kt 2개 추가.

## 시각 (iOS 동일, 브랜드색 = colorScheme.primary 0xFF007AFF)
- 딤 45% + 대상 라운드 컷아웃(코너 14dp, inset -6)
- 마칭앤츠 링: dash[7,6], phase -13 선형 0.6s 무한, reduce-motion off
- 카드: N/M 브랜드 캡슐 뱃지 + [건너뛰기] + (8단계 심볼) + 제목 + 본문 + [다음]/[시작하기] 48dp 캡슐
- 대상 위→카드 아래(top72), 대상 아래→카드 위(bottom108), 없음→중앙
- 뒤 탭 흡수

## 구현 단계
1. [x] Discovery(§1) + 매핑 승인
2. [ ] Analytics 이벤트 2개 (AnalyticsEvent.kt)
3. [ ] 20 온보딩 라벨 + settings.appUsageGuide 45 locale (iOS xcstrings 그대로)
4. [ ] Eligibility 저장/캡처 (TDD) + FearIndexApp 배선
5. [ ] OnboardingTour 오버레이 Compose (dim/cutout/ring/card/anchor, 8-step config, TDD 로직)
6. [ ] Tour controller + 배선(nav hoist, segment, scroll, ad suppress, preload)
7. [ ] Widget: Glance 4 provider(2×2×3 + 4×2) + EntryPoint fetch + WorkManager + manifest
8. [ ] Widget guide screen(HorizontalPager 5p) + settings 위젯사용법/앱사용법 행
9. [ ] Widget/guide 문자열 45 locale + 감수 패널
10. [ ] 검증: build, test, 에뮬 스크린샷 3+단계(2단계 KOSPI 자동), 3시나리오, 재생, GA, 광고 미노출

## 완료조건 (handoff §7)
빌드 에러0 / 스크린샷 3+단계+랜딩 / 3시나리오(신규 노출·기존 미노출·강제종료후 미노출) / 재생 / GA / 20키 45locale iOS동일·감수0 / 광고·스켈레톤 미노출 / 배포는 승인 게이트.
