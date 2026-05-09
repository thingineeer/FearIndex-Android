# v1.0.3 Manual 출시 (한/영 우선)

오늘 (2026-05-09) 기준 fastlane 자동화는 service account 권한 전파 24h 대기. 그 사이 manual 업로드.

## 산출물

```
AAB:        /Users/imyeongjin/Desktop/FearIndex-Android/app/build/outputs/bundle/release/app-release.aab
versionCode: 4
versionName: 1.0.3
서명 SHA1:  CE:08:B4:8A:FA:1C:29:8B:51:22:AC:82:9F:B7:78:12:CF:DD:0F:16
크기:       10MB
```

## Step 1 — Play Console 새 출시 (Internal Testing)

1. https://play.google.com/console → 공포지수 → **테스트 → 내부 테스트**
2. **새 버전 만들기**
3. **App Bundles** 영역에 AAB 드래그&드롭 (위 경로)
4. 자동 분석 후 경고 모두 검토

## Step 2 — 출시명 + 출시 노트

### 출시명
```
1.0.3 (4)
```

### 출시 노트 — 한국어 + 영어만 (다른 43 locale 은 default fallback)

`fastlane/metadata/android/ko_KR/changelogs/4.txt` 내용 그대로 ko-KR 슬롯에:

```
공포 탐욕 지수 v1.0.3 업데이트.
- 7인치/10인치 태블릿 지원
- 43개 언어 누락 번역 추가 (개인정보처리방침·물림카운터·인사이트)
- 푸시 알림 안정성 개선 (채널 ID 일관성)
- iOS와 데이터 동기화 검증 완료 (지수·물림카운터·임계값)
```

`fastlane/metadata/android/en_US/changelogs/4.txt` 내용 그대로 en-US 슬롯에:

```
Fear & Greed Index v1.0.3 update.
- Tablet support (7"/10")
- 43 missing translations added (privacy, stuck counter, insights)
- Push notification stability improved
- iOS data sync verified
```

또는 **45 locale XML 한 번에**:
- `<en-US>...</en-US>`, `<ko-KR>...</ko-KR>` 등 형식으로 출시 노트 텍스트 영역에 붙여넣기 (Play Console 자동 분리)
- 이 텍스트는 `/tmp/release_notes_v1.0.3.xml` 에 이미 생성되어 있음. 다시 만들려면:

```bash
python3 /Users/imyeongjin/Desktop/FearIndex-Android/.claude/scripts/build_release_notes.py
```

## Step 3 — 검토 + 출시

1. **검토 시작** 클릭
2. 경고 없으면 **내부 테스트로 출시 시작**
3. 14명 테스터 14일 카운트다운 시작

## Step 4 — Production 승격 (14일 후)

```bash
# fastlane 자동화 완료된 후 (권한 전파 끝)
fastlane promote_to_closed       # internal → alpha
fastlane promote_to_production   # alpha → production (10% staged)
```

또는 manual: Play Console → 트랙 promote

## 24h 후 fastlane 자동화 검증

```bash
cd /Users/imyeongjin/Desktop/FearIndex-Android
bash scripts/deploy/check-fastlane-ready.sh
```

✓ 모든 항목 통과하면:

```bash
bash scripts/deploy/upload-metadata-all-locales.sh
```

→ 45 locale 메타 + changelog + 스크린샷 자동 sync. 다음 출시부터는 manual 작업 0.

## 관련 문서

- @../fastlane/SUPPLY-SETUP.md — service account JSON 발급 + lane 사용법
- @../.claude/memory/deployment.md — Keystore + AAB 빌드 절차
- @../.claude/memory/bugs-fixed.md 17번 — keystore 사고 이력
