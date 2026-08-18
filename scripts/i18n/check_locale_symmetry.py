#!/usr/bin/env python3
"""45 locale 대칭 검사: values/strings.xml 의 모든 키(translatable="false" 제외)가 모든 values-*/strings.xml 에 있어야 한다.
사용: python3 scripts/i18n/check_locale_symmetry.py [--res presentation/src/main/res] [--prefix score_explorer notification_history ...]
"""
import argparse, glob, os, re, sys
ap = argparse.ArgumentParser()
ap.add_argument("--res", default="presentation/src/main/res")
ap.add_argument("--prefix", nargs="*", default=None)
a = ap.parse_args()
base = open(os.path.join(a.res, "values/strings.xml"), encoding="utf-8").read()
keys = [k for k, tail in re.findall(r'<string name="([^"]+)"([^>]*)>', base) if 'translatable="false"' not in tail]
if a.prefix:
    keys = [k for k in keys if any(k.startswith(p) for p in a.prefix)]
dirs = sorted(d for d in glob.glob(os.path.join(a.res, "values-*")) if os.path.exists(os.path.join(d, "strings.xml")))
missing = {}
for d in dirs:
    have = set(re.findall(r'<string name="([^"]+)"', open(os.path.join(d, "strings.xml"), encoding="utf-8").read()))
    lost = [k for k in keys if k not in have]
    if lost:
        missing[os.path.basename(d)] = lost
print(f"locales={len(dirs)+1} keys checked={len(keys)}")
if missing:
    for d, lost in missing.items():
        print(f"  {d}: missing {len(lost)} → {lost[:5]}")
    sys.exit(1)
print("symmetry ok")
