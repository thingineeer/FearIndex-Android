#!/usr/bin/env python3
"""위젯 피커 previewImage PNG 생성 — 런타임 렌더(WidgetGaugeRenderer/WidgetContent) 1:1.

디자인 규칙 (2026-08-27):
- 게이지: 270° 아크(135° 시작), 빨강→주황→노랑→초록, **BUTT 캡(끝 동그라미 없음)**
- 다크 카드 #1C1C1E, 흰 점수, 풀네임(Global/KOSPI/Crypto)
- 대시보드는 2×1(넓고 낮은 카드), 차트는 4×2

사용: python3 scripts/widgets/gen_widget_previews.py
출력: app/src/main/res/drawable-nodpi/widget_preview_*.png
"""
import math, os
from PIL import Image, ImageDraw, ImageFont

SS = 4  # supersample
CARD = (0x1C, 0x1C, 0x1E, 255)
TRACK = (0x3A, 0x3A, 0x3C, 255)
GRAD = [(0xE5, 0x39, 0x35), (0xF5, 0x7C, 0x00), (0xFD, 0xD8, 0x35), (0x43, 0xA0, 0x47)]
TEXT = (255, 255, 255, 255)
DIM = (0xF2, 0xF2, 0xF2, 255)
OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'app/src/main/res/drawable-nodpi')

def font(size, bold=True):
    for p in ('/System/Library/Fonts/Helvetica.ttc', '/Library/Fonts/Arial Bold.ttf'):
        try:
            return ImageFont.truetype(p, size, index=1 if bold and p.endswith('.ttc') else 0)
        except Exception:
            continue
    return ImageFont.load_default()

def grad_color(t):
    stops = [0.0, 0.33, 0.66, 1.0]
    for i in range(len(stops) - 1):
        if t <= stops[i + 1]:
            f = (t - stops[i]) / (stops[i + 1] - stops[i])
            a, b = GRAD[i], GRAD[i + 1]
            return tuple(int(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)
    return GRAD[-1] + (255,)

def draw_gauge(draw, cx, cy, r, stroke, score):
    """BUTT 캡 270° 아크 — 1° 단위 세그먼트로 그라데이션."""
    bbox = [cx - r, cy - r, cx + r, cy + r]
    draw.arc(bbox, 135, 405, fill=TRACK, width=stroke)
    sweep = 270 * score / 100
    step = 1
    a = 0
    while a < sweep:
        seg = min(step, sweep - a)
        draw.arc(bbox, 135 + a, 135 + a + seg + 0.6, fill=grad_color(a / 270), width=stroke)
        a += seg

def text_center(draw, cx, cy, s, f, fill):
    l, t, r, b = draw.textbbox((0, 0), s, font=f)
    draw.text((cx - (r - l) / 2 - l, cy - (b - t) / 2 - t), s, font=f, fill=fill)

def rounded_card(size, radius):
    img = Image.new('RGBA', (size[0] * SS, size[1] * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, size[0] * SS - 1, size[1] * SS - 1], radius * SS, fill=CARD)
    return img, d

def gauge_with_score(d, cx, cy, r, stroke, score, score_f):
    draw_gauge(d, cx, cy, r, stroke, score)
    text_center(d, cx, cy, str(score), score_f, TEXT)

def save(img, name):
    img = img.resize((img.width // SS, img.height // SS), Image.LANCZOS)
    path = os.path.join(OUT, name)
    img.save(path)
    print(path, img.size)

def single(name, score, label):
    W = H = 300
    img, d = rounded_card((W, H), 48)
    cx, cy = W * SS // 2, int(H * SS * 0.46)
    gauge_with_score(d, cx, cy, int(W * SS * 0.30), int(W * SS * 0.075), score, font(64 * SS))
    text_center(d, cx, H * SS * 0.86, label, font(26 * SS, bold=False), DIM)
    save(img, name)

def dashboard():
    W, H = 640, 300  # 2×1 비율
    img, d = rounded_card((W, H), 44)
    entries = [(55, 'Global'), (49, 'KOSPI'), (71, 'Crypto')]
    for i, (score, label) in enumerate(entries):
        cx = int(W * SS * (i + 0.5) / 3)
        cy = int(H * SS * 0.44)
        gauge_with_score(d, cx, cy, int(H * SS * 0.27), int(H * SS * 0.065), score, font(48 * SS))
        text_center(d, cx, H * SS * 0.84, label, font(22 * SS, bold=False), DIM)
    # ↻ 는 폰트 tofu 위험이라 프리뷰에선 생략 (기존 결정 유지)
    save(img, 'widget_preview_dashboard.png')

def chart(name='Global', score='55', line=(0xFD, 0xD8, 0x35, 255), fname='widget_preview_chart.png'):
    W, H = 640, 320
    img, d = rounded_card((W, H), 40)
    f_name = font(22 * SS, bold=False); f_score = font(34 * SS); f_axis = font(15 * SS, bold=False)
    d.text((28 * SS, 22 * SS), name, font=f_name, fill=DIM)
    d.text(((30 + 13 * len(name)) * SS, 12 * SS), score, font=f_score, fill=TEXT)
    pts = [32, 35, 41, 47, 52, 58, 57, 61, 64, 62, 66, 70, 67, 63, 60, 57, 55, 52, 54, 51, 49, 52, 55, 53, 55]
    left, right, top, bottom = 64 * SS, (W - 24) * SS, 80 * SS, (H - 48) * SS
    lo, hi = 30, 72
    axis = (0x8A, 0x8F, 0x98, 255); grid = (255, 255, 255, 40)
    for v in (70, 51, 32):
        y = top + (bottom - top) * (1 - (v - lo) / (hi - lo))
        d.line([left, y, right, y], fill=grid, width=SS)
        d.text((22 * SS, y - 8 * SS), str(v), font=f_axis, fill=axis)
    xy = []
    for i, v in enumerate(pts):
        x = left + (right - left) * i / (len(pts) - 1)
        y = top + (bottom - top) * (1 - (v - lo) / (hi - lo))
        xy.append((x, y))
    d.line(xy, fill=line, width=int(4.5 * SS), joint='curve')
    d.ellipse([xy[-1][0] - 7 * SS, xy[-1][1] - 7 * SS, xy[-1][0] + 7 * SS, xy[-1][1] + 7 * SS], fill=line)
    for i, s in ((0, '7.29'), (len(pts) // 2, '8.13'), (len(pts) - 1, '8.27')):
        d.text((xy[i][0] - 14 * SS, (H - 34) * SS), s, font=f_axis, fill=axis)
    save(img, fname)

single('widget_preview_market.png', 55, 'Global')
single('widget_preview_kospi.png', 49, 'KOSPI')
single('widget_preview_crypto.png', 71, 'Crypto')
dashboard()
chart()
chart('KOSPI', '49', (0xB5, 0x90, 0x00, 255), 'widget_preview_chart_kospi.png')
chart('Crypto', '71', (0x4C, 0xAF, 0x50, 255), 'widget_preview_chart_crypto.png')
