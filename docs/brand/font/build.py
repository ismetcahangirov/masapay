import json
import math
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path as FsPath

import pathops
from fontTools.feaLib.builder import addOpenTypeFeaturesFromString
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.t2CharStringPen import T2CharStringPen
from fontTools.pens.transformPen import TransformPen
from fontTools.svgLib.path import parse_path
from fontTools.ttLib import TTFont


FAMILY = "Masapay Sans"
STYLES = {400: "Regular", 500: "Medium", 600: "SemiBold", 700: "Bold"}
ASCENT, DESCENT = 960, -240

KERN = """
@ROUND_LC = [a c d e g o q s schwa odieresis ccedilla scedilla gbreve];
@SMALL_LC = [m n p r u v w x y z udieresis dotlessi];
@DOTS = [period comma];
@DIAG_UC = [V W Y];
feature kern {
  pos T @ROUND_LC -70;
  pos T @SMALL_LC -55;
  pos @DIAG_UC @ROUND_LC -45;
  pos @DIAG_UC @SMALL_LC -30;
  pos A [T V W Y] -50;
  pos [T V W Y] A -50;
  pos L [T V W Y] -60;
  pos F [a e o schwa] -30;
  pos [F P] A -40;
  pos [T V W Y F P] @DOTS -70;
  pos [r v w y] @DOTS -45;
  pos [v w y] @ROUND_LC -12;
  pos @ROUND_LC [v w y] -12;
} kern;
"""


def circle_to(pen, cx, cy, rx, ry):
    k = 4 / 3 * math.tan(math.pi / 8)
    pen.moveTo((cx + rx, cy))
    for a in range(4):
        t0, t1 = a * math.pi / 2, (a + 1) * math.pi / 2
        p0 = (cx + rx * math.cos(t0), cy + ry * math.sin(t0))
        p3 = (cx + rx * math.cos(t1), cy + ry * math.sin(t1))
        pen.curveTo(
            (p0[0] - k * rx * math.sin(t0), p0[1] + k * ry * math.cos(t0)),
            (p3[0] + k * rx * math.sin(t1), p3[1] - k * ry * math.cos(t1)),
            p3,
        )
    pen.closePath()


def strip(tag):
    return tag.split("}")[-1]


def shapes(el):
    for node in el.iter():
        tag = strip(node.tag)
        if tag in ("path", "circle", "ellipse", "polygon", "rect"):
            yield tag, node


def draw_shape(tag, node, pen):
    if tag == "path":
        parse_path(node.get("d"), pen)
    elif tag in ("circle", "ellipse"):
        cx, cy = float(node.get("cx", 0)), float(node.get("cy", 0))
        rx = float(node.get("r") or node.get("rx"))
        ry = float(node.get("r") or node.get("ry"))
        circle_to(pen, cx, cy, rx, ry)
    elif tag == "polygon":
        nums = [float(n) for n in re.split(r"[\s,]+", node.get("points").strip())]
        pts = list(zip(nums[::2], nums[1::2]))
        pen.moveTo(pts[0])
        for p in pts[1:]:
            pen.lineTo(p)
        pen.closePath()
    elif tag == "rect":
        x, y = float(node.get("x", 0)), float(node.get("y", 0))
        w, h = float(node.get("width")), float(node.get("height"))
        pen.moveTo((x, y))
        pen.lineTo((x + w, y))
        pen.lineTo((x + w, y + h))
        pen.lineTo((x, y + h))
        pen.closePath()


def view_width(svg_file):
    root = ET.parse(svg_file).getroot()
    return float(root.get("viewBox").split()[2])


def load_outlines(svg_file, metrics, scale):
    root = ET.parse(svg_file).getroot()
    by_id = {}
    for el in root.iter():
        gid = el.get("id") or ""
        m = re.fullmatch(r"g([0-9A-F]{4})(?:_\d+_)?", gid)
        if m:
            by_id.setdefault(int(m.group(1), 16), []).append(el)
    out = {}
    for name, meta in metrics.items():
        path = pathops.Path()
        tp = TransformPen(path.getPen(), (scale, 0, 0, -scale, meta["lsb"] - meta["x0"], meta["yb"]))
        for el in by_id.get(meta["cp"], []):
            for tag, node in shapes(el):
                draw_shape(tag, node, tp)
        if meta["cp"] != 0x20 and not list(path.segments):
            raise SystemExit(f"missing outline for {name}")
        out[name] = pathops.simplify(path, fix_winding=True, clockwise=False)
    return out


def notdef(pen):
    pen.moveTo((50, 0))
    pen.lineTo((450, 0))
    pen.lineTo((450, 700))
    pen.lineTo((50, 700))
    pen.closePath()
    pen.moveTo((110, 60))
    pen.lineTo((110, 640))
    pen.lineTo((390, 640))
    pen.lineTo((390, 60))
    pen.closePath()


def build(weight, svg_file, skeleton_file, metrics_file, out_dir):
    metrics = json.loads(FsPath(metrics_file).read_text(encoding="utf-8"))
    scale = view_width(skeleton_file) / view_width(svg_file)
    outlines = load_outlines(svg_file, metrics, scale)
    order = [".notdef"] + list(metrics)
    cmap = {m["cp"]: n for n, m in metrics.items()}
    charstrings, hmtx = {}, {}
    pen = T2CharStringPen(500, None)
    notdef(pen)
    charstrings[".notdef"] = pen.getCharString()
    hmtx[".notdef"] = (500, 50)
    for name in metrics:
        adv = metrics[name]["adv"]
        p = outlines[name]
        pen = T2CharStringPen(adv, None)
        p.draw(pen)
        charstrings[name] = pen.getCharString()
        xmin = math.floor(p.bounds[0]) if list(p.segments) else 0
        hmtx[name] = (adv, xmin)
    style = STYLES[weight]
    ps = f"MasapaySans-{style}"
    fb = FontBuilder(1000, isTTF=False)
    fb.setupGlyphOrder(order)
    fb.setupCharacterMap(cmap)
    fb.setupCFF(ps, {"FullName": f"{FAMILY} {style}", "Weight": style}, charstrings, {})
    fb.setupHorizontalMetrics(hmtx)
    fb.setupHorizontalHeader(ascent=ASCENT, descent=DESCENT)
    fb.setupNameTable({
        "familyName": FAMILY,
        "styleName": style,
        "uniqueFontIdentifier": f"{ps};1.000",
        "fullName": f"{FAMILY} {style}",
        "psName": ps,
        "version": "Version 1.000",
        "copyright": "Copyright 2026 masapay",
    })
    fb.setupOS2(
        version=4,
        usWeightClass=weight,
        sTypoAscender=ASCENT,
        sTypoDescender=DESCENT,
        sTypoLineGap=0,
        usWinAscent=ASCENT + 20,
        usWinDescent=-DESCENT + 20,
        sxHeight=520,
        sCapHeight=700,
        fsSelection=0x80 | (0x20 if weight >= 700 else 0x40),
        achVendID="MSPY",
    )
    fb.setupPost()
    fb.font["head"].macStyle = 1 if weight >= 700 else 0
    addOpenTypeFeaturesFromString(fb.font, KERN)
    out = FsPath(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    otf = out / f"masapay-sans-{weight}.otf"
    fb.font.save(otf)
    f = TTFont(otf)
    f.flavor = "woff2"
    f.save(out / f"masapay-sans-{weight}.woff2")
    return otf


if __name__ == "__main__":
    src, out = sys.argv[1], sys.argv[2]
    weights = [int(w) for w in sys.argv[3:]] or list(STYLES)
    for w in weights:
        print(build(w, f"{src}/outlined-{w}.svg", f"{src}/skeleton-{w}.svg", f"{src}/metrics-{w}.json", out))
