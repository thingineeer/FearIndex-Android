#!/usr/bin/env python3
"""
iOS Localizable.strings의 번역을 Android strings.xml로 일괄 inject.

문제: 43 locale의 Android strings.xml에 63개 키가 누락되어 있어 영어 fallback 노출됨.
해결: iOS ko.lproj/Localizable.strings 기준으로 매핑 가능한 키들을 파악하고,
      각 locale의 iOS strings 값을 Android 키 형식으로 변환해 inject.

iOS dot.notation -> Android snake_case 매핑 규칙:
  - `privacy.section.overview` → `privacy_section_overview`
  - `privacy.item.appUsage` → `privacy_item_app_usage`
  - `stuck.detail.myStatus.stuck` → `stuck_detail_my_status_stuck`
  - camelCase 안의 대문자는 _<lowercase>로 분해
"""
import os, re, sys, glob

ANDROID_RES = "presentation/src/main/res"
IOS_RESOURCES = "/Users/imyeongjin/Desktop/side/FearIndex-iOS/FearIndex-iOS/Resources"

# Android(snake_case) → 가능한 iOS(dotNotation) 후보 모두 생성
# 토큰을 어디까지 dot로 분리하고 어디서부터 camelCase로 합칠지 모든 조합 시도
def to_ios_key_candidates(android_key: str):
    parts = android_key.split('_')
    if len(parts) == 1:
        return [parts[0]]
    candidates = []
    # 각 부분 경계에서 dot/camelCase 결정. 2^(n-1) 조합
    n = len(parts)
    for mask in range(1 << (n - 1)):
        # mask의 i번째 비트가 1이면 parts[i]와 parts[i+1] 사이가 dot, 아니면 camelCase로 결합
        groups = [[parts[0]]]
        for i in range(1, n):
            if mask & (1 << (i - 1)):
                groups.append([parts[i]])
            else:
                groups[-1].append(parts[i])
        # 각 group을 camelCase로 합침 (첫 단어는 lowercase, 나머지는 capitalize)
        result_parts = []
        for g in groups:
            if len(g) == 1:
                result_parts.append(g[0])
            else:
                result_parts.append(g[0] + ''.join(p.capitalize() for p in g[1:]))
        candidates.append('.'.join(result_parts))
    # 가장 가능성 높은 순서로 정렬: dot가 많은(평탄한) 형식이 먼저, 같으면 길이 짧은 게 먼저
    candidates.sort(key=lambda c: (-c.count('.'), len(c)))
    return candidates

def to_ios_key(android_key: str) -> str:
    """단일 best-guess (호환용)"""
    return to_ios_key_candidates(android_key)[0]

def find_ios_key(android_key: str, ios_strings: dict) -> str | None:
    """ios_strings에 존재하는 첫 매칭 후보 반환"""
    for c in to_ios_key_candidates(android_key):
        if c in ios_strings:
            return c
    return None

# Android Locale → iOS lproj 매핑
LOCALE_MAP = {
    "af": "af", "ar": "ar", "bg": "bg", "bn": "bn", "ca": "ca",
    "cs": "cs", "da": "da", "de": "de", "el": "el", "es": "es",
    "et": "et", "fa": "fa", "fi": "fi", "fr": "fr", "hi": "hi",
    "hr": "hr", "hu": "hu", "in": "id", "it": "it", "iw": "he",
    "ja": "ja", "ko": "ko", "lt": "lt", "lv": "lv", "ms": "ms",
    "nb": "nb", "nl": "nl", "pl": "pl", "pt-rBR": "pt-BR", "pt-rPT": "pt-PT",
    "ro": "ro", "ru": "ru", "sk": "sk", "sl": "sl", "sr": "sr",
    "sv": "sv", "sw": "sw", "ta": "ta", "th": "th", "tr": "tr",
    "uk": "uk", "vi": "vi", "zh-rCN": "zh-Hans", "zh-rTW": "zh-Hant",
}

def parse_ios_strings(path: str) -> dict[str, str]:
    """iOS Localizable.strings → dict"""
    if not os.path.exists(path):
        return {}
    content = open(path, encoding='utf-8').read()
    result = {}
    # "key" = "value"; 패턴 (키나 값에 따옴표 escape 가능)
    pattern = re.compile(r'"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', re.DOTALL)
    for m in pattern.finditer(content):
        key = m.group(1).replace('\\"', '"').replace('\\n', '\n')
        val = m.group(2).replace('\\"', '"').replace('\\n', '\n')
        result[key] = val
    return result

def parse_android_strings(path: str) -> dict[str, str]:
    if not os.path.exists(path):
        return {}
    content = open(path, encoding='utf-8').read()
    result = {}
    pattern = re.compile(r'<string name="([^"]+)"[^>]*>(.*?)</string>', re.DOTALL)
    for m in pattern.finditer(content):
        result[m.group(1)] = m.group(2)
    return result

def android_escape(text: str) -> str:
    """Android XML attribute/text escape"""
    text = text.replace('\\', '\\\\')
    text = text.replace("'", "\\'")
    text = text.replace('"', '\\"')
    # XML special: &, <, >
    # & 먼저 (다른 entity와 충돌 방지)
    text = text.replace('&', '&amp;')
    text = text.replace('<', '&lt;')
    text = text.replace('>', '&gt;')
    return text

def main():
    # 1. base에 있고 ja에 없는 키 = 누락 키
    base = parse_android_strings(f"{ANDROID_RES}/values/strings.xml")
    ko = parse_android_strings(f"{ANDROID_RES}/values-ko/strings.xml")
    ja = parse_android_strings(f"{ANDROID_RES}/values-ja/strings.xml")
    missing_keys = sorted(set(base.keys()) - set(ja.keys()))
    print(f"[INFO] 누락 키 {len(missing_keys)}개 식별")

    # 2. iOS ko에서 매핑 시도 (모든 토큰 그룹화 조합 시도)
    ios_ko = parse_ios_strings(f"{IOS_RESOURCES}/ko.lproj/Localizable.strings")
    mappable = []
    unmappable = []
    for k in missing_keys:
        ios_key = find_ios_key(k, ios_ko)
        if ios_key:
            mappable.append((k, ios_key))
        else:
            unmappable.append(k)

    print(f"[INFO] iOS 매핑 가능: {len(mappable)} / 매핑 불가: {len(unmappable)}")
    if unmappable:
        print("[WARN] 매핑 불가 키들 (iOS에 없음 → Android base 영어 사용):")
        for k in unmappable:
            print(f"  - {k}  (추측 iOS key: {to_ios_key(k)})")

    # 3. 각 Android locale에 대해 처리
    locale_dirs = sorted(glob.glob(f"{ANDROID_RES}/values-*"))
    for d in locale_dirs:
        locale_name = os.path.basename(d).replace("values-", "")
        if locale_name == "ko":
            continue  # ko는 이미 다 있음
        ios_locale = LOCALE_MAP.get(locale_name)
        ios_strings_path = f"{IOS_RESOURCES}/{ios_locale}.lproj/Localizable.strings" if ios_locale else None
        ios_strings = parse_ios_strings(ios_strings_path) if ios_strings_path else {}

        android_path = f"{d}/strings.xml"
        existing = parse_android_strings(android_path)
        added = []
        for k, ios_key in mappable:
            if k in existing:
                continue  # 이미 있으면 skip
            # iOS 해당 locale에서 값 찾기. 없으면 ios ko, 그래도 없으면 base 영어
            val = ios_strings.get(ios_key) or ios_ko.get(ios_key) or base.get(k)
            if val is None:
                continue
            added.append((k, val))
        en_fallback_count = 0
        for k in unmappable:
            if k in existing:
                continue
            # iOS에 없으면 base(영어) 그대로 inject — fallback과 동일 효과지만 명시적으로 박아서 확인 가능
            val = base.get(k)
            if val is None:
                continue
            added.append((k, val))

        if not added:
            continue

        # XML inject — </resources> 직전에 새 string 추가
        with open(android_path, encoding='utf-8') as f:
            content = f.read()
        new_strings = "\n".join(
            f'    <string name="{k}">{android_escape(v) if "&" not in v and "<" not in v else v}</string>'
            for k, v in added
        )
        # 안전한 escape: 이미 XML escape 된 케이스(&amp; 등)는 그대로 두기 위해 위에서 분기
        # 단순화: iOS 원본은 raw text → escape 모두 적용
        new_strings = "\n".join(
            f'    <string name="{k}">{android_escape(v)}</string>'
            for k, v in added
        )
        new_content = content.replace("</resources>", new_strings + "\n</resources>")
        with open(android_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"  [{locale_name}] +{len(added)} keys ({'iOS:'+ios_locale if ios_locale else 'no iOS map'})")

    print("[DONE]")

if __name__ == "__main__":
    main()
