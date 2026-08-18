#!/usr/bin/env python3
"""
iOS `Localizable.xcstrings` 의 특정 키들을 Android 45 locale `values-*/strings.xml` 로 이식한다.

- 번역값은 데이터로 그대로 이식 (iOS 가 SSOT). 플레이스홀더 `%@`/`%d`/`%lld` → `%1$s`/`%1$d` (등장 순서대로 번호).
- Android 이스케이프: `'` → `\'`, `&` → `&amp;`, `<`/`>` 엔티티, 줄바꿈 → `\n`.
- 이미 존재하는 키는 건너뛴다(덮어쓰지 않음). 누락 locale 이 있으면 실패(exit 1).

사용:
  python3 scripts/i18n/import_xcstrings_keys.py --xcstrings <path> --map <keymap.json> [--res presentation/src/main/res] [--check]

keymap.json: {"ios.key": "android_key", ...}
"""
import argparse, json, os, re, sys

# iOS locale → Android values-* suffix
LOCALE_MAP = {
    "en": "", "id": "in", "he": "iw", "pt-BR": "pt-rBR", "pt-PT": "pt-rPT",
    "zh-Hans": "zh-rCN", "zh-Hant": "zh-rTW",
}

def android_dir(res, ios_locale):
    suffix = LOCALE_MAP.get(ios_locale, ios_locale)
    return os.path.join(res, "values" if suffix == "" else f"values-{suffix}")

def convert_placeholders(value):
    # %@ / %d / %lld / %ld / %.1f 등 → 순서 번호 부여
    idx = [0]
    def repl(m):
        idx[0] += 1
        spec = m.group(1)
        if spec == "@":
            return f"%{idx[0]}$s"
        if spec in ("d", "lld", "ld", "i", "u"):
            return f"%{idx[0]}$d"
        if spec.endswith("f"):
            return f"%{idx[0]}$" + spec
        return f"%{idx[0]}$s"
    # 이미 %1$s 형태면 건드리지 않음
    if re.search(r"%\d+\$", value):
        return value
    return re.sub(r"%(@|lld|ld|d|i|u|\.\d+f|f)", repl, value)

def android_escape(value):
    v = value.replace("\\", "\\\\")
    v = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    v = v.replace("'", "\\'").replace('"', '\\"')
    v = v.replace("\n", "\\n")
    # 리터럴 % (플레이스홀더 아님) → %%
    v = re.sub(r"%(?!\d+\$)", "%%", v)
    return v

def existing_keys(path):
    if not os.path.exists(path):
        return set()
    return set(re.findall(r'<string name="([^"]+)"', open(path, encoding="utf-8").read()))

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--xcstrings", required=True)
    ap.add_argument("--map", required=True)
    ap.add_argument("--res", default="presentation/src/main/res")
    ap.add_argument("--check", action="store_true", help="쓰지 않고 누락만 보고")
    a = ap.parse_args()
    strings = json.load(open(a.xcstrings, encoding="utf-8"))["strings"]
    keymap = json.load(open(a.map, encoding="utf-8"))
    locales = sorted({lc for k in keymap for lc in strings.get(k, {}).get("localizations", {})})
    if len(locales) != 45:
        print(f"[warn] iOS locales for these keys = {len(locales)} (expected 45)")
    problems = []
    written = 0
    for lc in locales:
        d = android_dir(a.res, lc)
        path = os.path.join(d, "strings.xml")
        if not os.path.exists(path):
            problems.append(f"missing Android dir for {lc}: {d}")
            continue
        have = existing_keys(path)
        lines = []
        for ios_key, akey in keymap.items():
            unit = strings.get(ios_key, {}).get("localizations", {}).get(lc, {}).get("stringUnit")
            if not unit:
                problems.append(f"{ios_key} missing locale {lc}")
                continue
            if akey in have:
                continue
            val = android_escape(convert_placeholders(unit["value"]))
            lines.append(f'    <string name="{akey}">{val}</string>')
        if not lines or a.check:
            continue
        src = open(path, encoding="utf-8").read()
        block = "\n    <!-- v1.9.4 premium parity (iOS xcstrings 이식) -->\n" + "\n".join(lines) + "\n"
        src = src.replace("</resources>", block + "</resources>", 1)
        open(path, "w", encoding="utf-8").write(src)
        written += len(lines)
    if problems:
        print("\n".join(problems)); sys.exit(1)
    print(f"ok: wrote {written} strings across {len(locales)} locales" if not a.check else "check ok")

if __name__ == "__main__":
    main()
