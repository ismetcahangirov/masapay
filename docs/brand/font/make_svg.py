import json
import sys
from pathlib import Path as FsPath

from glyphs import G, SIDE, TABULAR, WEIGHTS, glyph_table

OUT = FsPath(sys.argv[1] if len(sys.argv) > 1 else "out")
CELL = 1300
COLS = 12
TAB_ADV = {400: 600, 500: 610, 600: 620, 700: 630}


def build(weight):
    sw = WEIGHTS[weight]
    g = G(sw)
    table = glyph_table()
    glyphs = {}
    for name, (cp, fn) in table.items():
        if cp is None:
            continue
        paths, dots, sides = fn(g)
        if sides is None:
            glyphs[name] = dict(cp=cp, paths=[], dots=[], adv=250, lsb=0, minx=0, outer=0)
            continue
        xs = [p[0] for path in paths for p in path.samples()]
        lo = [x - g.h for x in xs] + [x - r for x, _, r in dots]
        hi = [x + g.h for x in xs] + [x + r for x, _, r in dots]
        minx, outer = min(lo), max(hi) - min(lo)
        if name in TABULAR:
            adv = TAB_ADV[weight]
            lsb = (adv - outer) / 2
        else:
            lsb = SIDE[sides[0]]
            adv = lsb + outer + SIDE[sides[1]]
        glyphs[name] = dict(cp=cp, paths=paths, dots=dots, adv=round(adv), lsb=lsb, minx=minx, outer=outer)
    return g, glyphs


def skeleton_svg(weight):
    g, glyphs = build(weight)
    rows = (len(glyphs) + COLS - 1) // COLS
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {COLS * CELL} {rows * CELL}">']
    meta = {}
    for i, (name, gl) in enumerate(glyphs.items()):
        x0 = (i % COLS) * CELL + 100
        yb = (i // COLS) * CELL + 1000
        shift = -gl["minx"]
        fx = lambda p, x0=x0, yb=yb, shift=shift: (x0 + p[0] + shift, yb - p[1])
        meta[name] = dict(cp=gl["cp"], x0=x0, yb=yb, lsb=gl["lsb"], adv=gl["adv"])
        if not gl["paths"] and not gl["dots"]:
            continue
        parts.append(f'<g id="g{gl["cp"]:04X}">')
        for p in gl["paths"]:
            parts.append(
                f'<path d="{p.svg(fx)}" fill="none" stroke="#000" stroke-width="{g.sw}" '
                f'stroke-linecap="round" stroke-linejoin="round"/>'
            )
        for x, y, r in gl["dots"]:
            cx, cy = fx((x, y))
            parts.append(f'<circle cx="{cx:.2f}" cy="{cy:.2f}" r="{r:.2f}" fill="#000"/>')
        parts.append("</g>")
    parts.append("</svg>")
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"skeleton-{weight}.svg").write_text("\n".join(parts), encoding="utf-8")
    (OUT / f"metrics-{weight}.json").write_text(json.dumps(meta), encoding="utf-8")


def specimen(lines, weights, size=0.08):
    rows = []
    y = 0
    for weight in weights:
        g, glyphs = build(weight)
        by_cp = {gl["cp"]: gl for gl in glyphs.values()}
        for text in lines:
            y += 1300
            x = 100
            for ch in text:
                gl = by_cp.get(ord(ch))
                if gl is None:
                    x += 500
                    continue
                shift = gl["lsb"] - gl["minx"]
                fx = lambda p, x=x, shift=shift, y=y: (x + p[0] + shift, y - p[1])
                for p in gl["paths"]:
                    rows.append(f'<path d="{p.svg(fx)}" fill="none" stroke="#0A0A0A" stroke-width="{g.sw}" stroke-linecap="round" stroke-linejoin="round"/>')
                for dx, dy, r in gl["dots"]:
                    cx, cy = fx((dx, dy))
                    rows.append(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r:.1f}" fill="#0A0A0A"/>')
                x += gl["adv"]
    w = 40000
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {y + 400}" width="{w * size}" height="{(y + 400) * size}">' + "".join(rows) + "</svg>"


if __name__ == "__main__":
    for w in WEIGHTS:
        skeleton_svg(w)
    sample = [
        "masapay Restoranlar üçün QR ödəniş",
        "Hesabı böl, bəxşiş əlavə et. Ödə: 24,50 ₼",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ ƏĞİÖŞÇÜ",
        "abcdefghijklmnopqrstuvwxyz əğıöşçü",
        "0123456789 (.,:;!?) — “x” ‘y’ • × ± → % * # $ [ ] < > / | _ - – + =",
    ]
    (OUT / "specimen.html").write_text(
        '<html><body style="margin:0;background:#fff">' + specimen(sample, [400, 600], 0.045) + "</body></html>",
        encoding="utf-8",
    )
    print("ok")
