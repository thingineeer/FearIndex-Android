#!/usr/bin/env python3
"""
gen-default-return-data.py

firebase-functions/src/data-aggregation/output/{market,crypto}.json (iOS 레포) →
domain/src/main/kotlin/th1ngjin/fearindex/domain/defaults/DefaultReturnData.kt

다시 쓰는 범위 (그 외는 그대로 보존):
  1) KDoc 헤더의 "데이터 생성일 / Market / Crypto" 3줄
  2) `marketDataPoints()` 본문 101행
  3) `cryptoDataPoints()` 본문 101행
  4) `marketSourceRange()` / `cryptoSourceRange()` (fngRange from/to)
KOSPI dataPoints, historicalEvents, dp()/r()/c() 헬퍼는 건드리지 않는다.

iOS 대응 스크립트: scripts/gen-default-return-data.js (FearIndex-iOS). 출력 포맷만 Kotlin.

실행:
  python3 scripts/gen-default-return-data.py                      # iOS worktree 기본 경로
  python3 scripts/gen-default-return-data.py market.json crypto.json
  python3 scripts/gen-default-return-data.py --check              # 갱신 필요 여부만 (exit 1 = stale)

의존성 없음 (Python 3.8+).
"""

import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
KOTLIN_PATH = os.path.join(
    ROOT, "domain/src/main/kotlin/th1ngjin/fearindex/domain/defaults/DefaultReturnData.kt"
)
DEFAULT_OUTPUT_DIR = (
    "/Users/imyeongjin/Desktop/APP/worktrees/FearIndex-iOS/fi-v194-design/firebase-functions/src/data-aggregation/output"
)
HORIZON_KEYS = ("oneMonth", "threeMonth", "sixMonth", "oneYear")
DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


class GenError(Exception):
    pass


# ---------------------------------------------------------------- load / validate

def load_output(name, path):
    if not os.path.exists(path):
        raise GenError(f"집계 결과 없음: {path}")
    with open(path, encoding="utf-8") as fh:
        data = json.load(fh)
    validate_output(name, data)
    return data


def validate_output(name, data):
    if data.get("dataset") != name:
        raise GenError(f"{name}.json dataset={data.get('dataset')}")
    points = data.get("dataPoints")
    if not isinstance(points, list) or len(points) != 101:
        raise GenError(f"{name}.json dataPoints 가 101개가 아님: {points and len(points)}")
    for i, p in enumerate(points):
        validate_point(name, p, i)
    rng = data.get("fngRange") or {}
    if not (DATE_RE.match(str(rng.get("from", ""))) and DATE_RE.match(str(rng.get("to", "")))):
        raise GenError(f"{name}.json fngRange 형식 오류: {rng}")
    if rng["from"] > rng["to"]:
        raise GenError(f"{name}.json fngRange from > to: {rng}")


def validate_point(name, p, i):
    if p.get("score") != i:
        raise GenError(f"{name}.json score 순서 오류 index={i} score={p.get('score')}")
    hc = p.get("horizonCounts")
    if not hc:
        raise GenError(f"{name}.json score={i} horizonCounts 누락")
    if hc.get("oneMonth") != p.get("sampleCount"):
        raise GenError(f"{name}.json score={i} sampleCount≠horizonCounts.oneMonth")
    for key in HORIZON_KEYS:
        for block in ("returns", "worstCase", "bestCase"):
            v = p[block][key]
            if isinstance(v, bool) or not isinstance(v, (int, float)) or v != v:
                raise GenError(f"{name}.json score={i} {block}.{key} 숫자 아님")
        if not isinstance(hc[key], int):
            raise GenError(f"{name}.json score={i} horizonCounts.{key} 정수 아님")


# ---------------------------------------------------------------- render

def dbl(v):
    """JSON 숫자 → Kotlin Double 리터럴 (`8` → `8.0`, `-0` → `0.0`)."""
    if v == 0:
        return "0.0"
    if float(v).is_integer():
        return f"{int(v)}.0"
    return repr(float(v))


def ret(block):
    return "r(" + ", ".join(dbl(block[k]) for k in HORIZON_KEYS) + ")"


def counts(hc):
    return "c(" + ", ".join(str(int(hc[k])) for k in HORIZON_KEYS) + ")"


def row(p):
    return (
        f"        dp({p['score']}, {ret(p['returns'])}, {ret(p['worstCase'])}, "
        f"{ret(p['bestCase'])}, {int(p['sampleCount'])}, {counts(p['horizonCounts'])}),"
    )


def sum_samples(points):
    return sum(int(p["sampleCount"]) for p in points)


# ---------------------------------------------------------------- replace

def replace_header(src, market, crypto):
    def line(label, data, source):
        rng = data["fngRange"]
        return f" * {label}: {source} ({rng['from']} ~ {rng['to']}, Σn(1M)={sum_samples(data['dataPoints'])})"

    pattern = re.compile(
        r"^ \* 데이터 생성일: .*\n \* Market: .*\n \* Crypto: .*$", re.MULTILINE
    )
    if not pattern.search(src):
        raise GenError("헤더 마커(데이터 생성일/Market/Crypto 3줄) 를 찾지 못함")
    block = "\n".join(
        [
            f" * 데이터 생성일: {market['generatedAt']}",
            line("Market", market, "CNN F&G × S&P 500"),
            line("Crypto", crypto, "alternative.me × BTC"),
        ]
    )
    return pattern.sub(lambda _m: block, src, count=1)


def replace_function_body(src, fn_name, points):
    pattern = re.compile(
        r"(    private fun " + fn_name + r"\(\): List<ReturnDataPoint> = listOf\(\n)"
        r"[\s\S]*?"
        r"(\n    \)\n)"
    )
    if not pattern.search(src):
        raise GenError(f"{fn_name}() 본문을 찾지 못함")
    body = "\n".join(row(p) for p in points)
    return pattern.sub(lambda m: m.group(1) + body + m.group(2), src, count=1)


def replace_source_range(src, fn_name, rng):
    pattern = re.compile(
        r'(    private fun ' + fn_name + r'\(\): DateRange = range\()"[^"]*", "[^"]*"(\))'
    )
    if not pattern.search(src):
        raise GenError(f"{fn_name}() 를 찾지 못함")
    return pattern.sub(lambda m: f'{m.group(1)}"{rng["from"]}", "{rng["to"]}"{m.group(2)}', src, count=1)


def assert_helpers(src):
    if not re.search(r"counts: HistoricalSampleCounts\? = null", src):
        raise GenError("dp(..., counts: HistoricalSampleCounts? = null) 헬퍼가 없음")
    if not re.search(r"private fun c\(m1: Int, m3: Int, m6: Int, y1: Int\)", src):
        raise GenError("c(m1, m3, m6, y1) 헬퍼가 없음")


def generate(market_path, crypto_path):
    market = load_output("market", market_path)
    crypto = load_output("crypto", crypto_path)
    with open(KOTLIN_PATH, encoding="utf-8") as fh:
        before = fh.read()
    assert_helpers(before)
    after = replace_header(before, market, crypto)
    after = replace_function_body(after, "marketDataPoints", market["dataPoints"])
    after = replace_function_body(after, "cryptoDataPoints", crypto["dataPoints"])
    after = replace_source_range(after, "marketSourceRange", market["fngRange"])
    after = replace_source_range(after, "cryptoSourceRange", crypto["fngRange"])
    return before, after, market, crypto


# ---------------------------------------------------------------- main

def parse_args(argv):
    check = "--check" in argv
    paths = [a for a in argv if not a.startswith("--")]
    if len(paths) == 0:
        market = os.path.join(DEFAULT_OUTPUT_DIR, "market.json")
        crypto = os.path.join(DEFAULT_OUTPUT_DIR, "crypto.json")
    elif len(paths) == 2:
        market, crypto = paths
    else:
        raise GenError("사용법: gen-default-return-data.py [market.json crypto.json] [--check]")
    return check, market, crypto


def main(argv):
    check, market_path, crypto_path = parse_args(argv)
    before, after, market, crypto = generate(market_path, crypto_path)
    changed = before != after
    if check:
        print("[gen] STALE — 재생성 필요" if changed else "[gen] up-to-date")
        return 1 if changed else 0
    with open(KOTLIN_PATH, "w", encoding="utf-8") as fh:
        fh.write(after)
    rel = os.path.relpath(KOTLIN_PATH, ROOT)
    print(f"[gen] {rel} {'갱신' if changed else '변경 없음'}")
    print(f"[gen] generatedAt={market['generatedAt']}")
    m, c = market["fngRange"], crypto["fngRange"]
    print(
        f"[gen] market {m['from']}~{m['to']} Σn={sum_samples(market['dataPoints'])}"
        f" | crypto {c['from']}~{c['to']} Σn={sum_samples(crypto['dataPoints'])}"
    )
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except GenError as err:
        print(f"[gen] 실패: {err}", file=sys.stderr)
        sys.exit(1)
