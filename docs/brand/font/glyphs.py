import math

UPM = 1000
X = 520
CAP = 700
ASC = 740
DSC = -200
OV = 10

WEIGHTS = {400: 80, 500: 96, 600: 112, 700: 128}
SIDE = {"S": 62, "O": 40, "D": 14, "N": 30, "J": -40, "F": -25}


class Path:
    def __init__(self):
        self.cmds = []

    def _cur(self):
        return self.cmds[-1][-1] if self.cmds else None

    def M(self, x, y):
        self.cmds.append(("M", (x, y)))
        return self

    def L(self, x, y):
        self.cmds.append(("L", (x, y)))
        return self

    def H(self, x):
        return self.L(x, self._cur()[1])

    def V(self, y):
        return self.L(self._cur()[0], y)

    def arc(self, cx, cy, rx, a0, a1, ry=None):
        ry = rx if ry is None else ry
        start = (cx + rx * math.cos(math.radians(a0)), cy + ry * math.sin(math.radians(a0)))
        cur = self._cur()
        if cur is None:
            self.M(*start)
        elif math.dist(cur, start) > 0.01:
            self.L(*start)
        n = max(1, math.ceil(abs(a1 - a0) / 90))
        step = (a1 - a0) / n
        for i in range(n):
            t0 = math.radians(a0 + step * i)
            t1 = math.radians(a0 + step * (i + 1))
            k = 4 / 3 * math.tan((t1 - t0) / 4)
            p0 = (cx + rx * math.cos(t0), cy + ry * math.sin(t0))
            p3 = (cx + rx * math.cos(t1), cy + ry * math.sin(t1))
            p1 = (p0[0] - k * rx * math.sin(t0), p0[1] + k * ry * math.cos(t0))
            p2 = (p3[0] + k * rx * math.sin(t1), p3[1] - k * ry * math.cos(t1))
            self.cmds.append(("C", p1, p2, p3))
        return self

    def end(self):
        return self._cur()

    def map(self, fn):
        out = Path()
        out.cmds = [(c[0], *[fn(p) for p in c[1:]]) for c in self.cmds]
        return out

    def samples(self):
        pts, cur = [], None
        for c in self.cmds:
            if c[0] in "ML":
                cur = c[1]
                pts.append(cur)
            else:
                p0, (p1, p2, p3) = cur, c[1:]
                for i in range(1, 17):
                    t = i / 16
                    u = 1 - t
                    pts.append((
                        u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0],
                        u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1],
                    ))
                cur = p3
        return pts

    def svg(self, fx):
        parts = []
        for c in self.cmds:
            pts = " ".join(f"{fx(p)[0]:.2f},{fx(p)[1]:.2f}" for p in c[1:])
            parts.append(f"{c[0]}{pts}")
        return " ".join(parts)


def line(*pts):
    p = Path().M(*pts[0])
    for q in pts[1:]:
        p.L(*q)
    return p


def circ(cx, cy, r, ry=None):
    return Path().arc(cx, cy, r, 90, 450, ry)


def rot180(paths, dots, cx, cy):
    f = lambda p: (2 * cx - p[0], 2 * cy - p[1])
    return [p.map(f) for p in paths], [(2 * cx - x, 2 * cy - y, r) for x, y, r in dots]


def on_line(a, b, y):
    t = (y - a[1]) / (b[1] - a[1])
    return (a[0] + t * (b[0] - a[0]), y)


class G:
    def __init__(self, sw):
        self.sw = sw
        self.h = sw / 2
        self.dot = self.h * 1.15
        self.ro = (X + 2 * OV) / 2 - self.h
        self.rO = (CAP + 2 * OV) / 2 - self.h
        self.R = self.ro * 0.86

    # lowercase -----------------------------------------------------------
    def o(self):
        h, ro = self.h, self.ro
        return [circ(h + ro, X / 2, ro)], [], "OO"

    def c(self):
        h, ro = self.h, self.ro
        return [Path().arc(h + ro, X / 2, ro, 48, 312)], [], "OD"

    def e(self):
        h, ro = self.h, self.ro
        cx = h + ro
        return [Path().arc(cx, X / 2, ro, 0, 322), line((cx - ro, X / 2), (cx + ro, X / 2))], [], "OD"

    def schwa(self):
        p, d, _ = self.e()
        p, d = rot180(p, d, self.h + self.ro, X / 2)
        return p, d, "DO"

    def a(self):
        h, ro = self.h, self.ro
        cx = h + ro
        return [circ(cx, X / 2, ro), line((cx + ro, X - h), (cx + ro, h))], [], "OS"

    def b(self):
        h, ro = self.h, self.ro
        return [line((h, ASC - h), (h, h)), circ(h + ro, X / 2, ro)], [], "SO"

    def d(self):
        h, ro = self.h, self.ro
        return [circ(h + ro, X / 2, ro), line((h + 2 * ro, ASC - h), (h + 2 * ro, h))], [], "OS"

    def p(self):
        h, ro = self.h, self.ro
        return [line((h, X - h), (h, DSC + h)), circ(h + ro, X / 2, ro)], [], "SO"

    def q(self):
        h, ro = self.h, self.ro
        return [circ(h + ro, X / 2, ro), line((h + 2 * ro, X - h), (h + 2 * ro, DSC + h))], [], "OS"

    def g(self):
        h, ro = self.h, self.ro
        cx = h + ro
        xs = cx + ro
        rg = ro * 0.82
        hook = Path().M(xs, X - h).L(xs, DSC + h + rg).arc(xs - rg, DSC + h + rg, rg, 0, -158)
        return [circ(cx, X / 2, ro), hook], [], "OS"

    def dotless_i(self):
        h = self.h
        return [line((h, X - h), (h, h))], [], "SS"

    def i(self):
        p, _, s = self.dotless_i()
        return p, [(self.h, ASC - self.dot, self.dot)], s

    def dotless_j(self):
        h = self.h
        rj = 120
        x = h + rj * 1.6
        return [Path().M(x, X - h).L(x, DSC + h + rj).arc(x - rj, DSC + h + rj, rj, 0, -150)], [], "JS"

    def j(self):
        p, _, s = self.dotless_j()
        x = self.h + 120 * 1.6
        return p, [(x, ASC - self.dot, self.dot)], s

    def l(self):
        h = self.h
        return [line((h, ASC - h), (h, h))], [], "SS"

    def f(self):
        h = self.h
        rf = 135
        x = h + 95
        hook = Path().M(x, h).L(x, ASC - h - rf).arc(x + rf, ASC - h - rf, rf, 180, 40)
        bar = line((x - 95, X - h), (x + 170, X - h))
        return [hook, bar], [], "DF"

    def t(self):
        h = self.h
        rt = 110
        x = h + 90
        stem = Path().M(x, X + 150 - h).L(x, h + rt).arc(x + rt, h + rt, rt, 180, 300)
        bar = line((x - 90, X - h), (x + 175, X - h))
        return [stem, bar], [], "DD"

    def n(self, top=X):
        h, R = self.h, self.R
        arch = Path().M(h, X - h - R).arc(h + R, X - h - R, R, 180, 0).L(h + 2 * R, h)
        return [line((h, top - h), (h, h)), arch], [], "SS"

    def h_(self):
        return self.n(ASC)

    def m(self):
        h = self.h
        Rm = self.R * 0.72
        a1 = Path().M(h, X - h - Rm).arc(h + Rm, X - h - Rm, Rm, 180, 0).L(h + 2 * Rm, h)
        a2 = Path().M(h + 2 * Rm, X - h - Rm).arc(h + 3 * Rm, X - h - Rm, Rm, 180, 0).L(h + 4 * Rm, h)
        return [line((h, X - h), (h, h)), a1, a2], [], "SS"

    def u(self):
        p, d, _ = self.n()
        p, d = rot180(p, d, self.h + self.R, X / 2)
        return p, d, "SS"

    def r(self):
        h = self.h
        Rr = self.R * 0.82
        arch = Path().M(h, X - h - Rr).arc(h + Rr, X - h - Rr, Rr, 180, 62)
        return [line((h, X - h), (h, h)), arch], [], "SD"

    def k(self):
        h = self.h
        x2 = h + 300
        yj = X * 0.34
        arm = line((x2, X - h), (h, yj))
        j = (h + (x2 - h) * 0.42, yj + (X - h - yj) * 0.42)
        leg = line(j, (x2 + 15, h))
        return [line((h, ASC - h), (h, h)), arm, leg], [], "SD"

    def v(self):
        h = self.h
        w = 430
        return [line((h, X - h), (h + w / 2, h), (h + w, X - h))], [], "DD"

    def w(self):
        h = self.h
        a = 165
        return [line((h, X - h), (h + a, h), (h + 2 * a, X - h), (h + 3 * a, h), (h + 4 * a, X - h))], [], "DD"

    def x(self):
        h = self.h
        w = 400
        return [line((h, X - h), (h + w, h)), line((h, h), (h + w, X - h))], [], "DD"

    def y(self):
        h = self.h
        w = 430
        top_r, mid = (h + w, X - h), (h + w / 2, h)
        tail = on_line(top_r, mid, DSC + h)
        return [line((h, X - h), mid), line(top_r, tail)], [], "DD"

    def z(self):
        h = self.h
        w = 370
        return [line((h, X - h), (h + w, X - h), (h, h), (h + w, h))], [], "DD"

    def s(self, top=X, k=1.12):
        h = self.h
        rs = (top - 2 * h) / 4
        cx = h + rs * k
        p = Path().arc(cx, top - h - rs, rs * k, 28, 270, rs).arc(cx, h + rs, rs * k, 90, -152, rs)
        return [p], [], "OO"

    # uppercase -----------------------------------------------------------
    def O(self):
        h, r = self.h, self.rO
        return [circ(h + r, CAP / 2, r)], [], "OO"

    def C(self):
        h, r = self.h, self.rO
        return [Path().arc(h + r, CAP / 2, r, 45, 315)], [], "OD"

    def G_(self):
        h, r = self.h, self.rO
        cx = h + r
        p = Path().arc(cx, CAP / 2, r, 42, 360).L(cx + r * 0.25, CAP / 2)
        return [p], [], "OO"

    def Q(self):
        h, r = self.h, self.rO
        cx = h + r
        tail = line((cx + r * 0.38, CAP / 2 - r * 0.38), (cx + r * 1.02, CAP / 2 - r * 1.05))
        return [circ(cx, CAP / 2, r), tail], [], "OO"

    def schwa_cap(self):
        h, r = self.h, self.rO
        cx = h + r
        p = [Path().arc(cx, CAP / 2, r, 180, 180 + 322), line((cx - r, CAP / 2), (cx + r, CAP / 2))]
        return p, [], "DO"

    def D(self):
        h = self.h
        rd = CAP / 2 - h
        L = 110
        p = Path().M(h, CAP - h).L(h + L, CAP - h).arc(h + L, CAP / 2, rd, 90, -90).L(h, h).L(h, CAP - h)
        return [p], [], "SO"

    def B(self):
        h = self.h
        H = CAP - 2 * h
        r1, r2 = H * 0.235, H * 0.265
        x1, x2 = h + 135, h + 150
        up = Path().M(h, h).L(h, CAP - h).L(x1, CAP - h).arc(x1, CAP - h - r1, r1, 90, -90).L(h, CAP - h - 2 * r1)
        low = Path().M(h, h + 2 * r2).L(x2, h + 2 * r2).arc(x2, h + r2, r2, 90, -90).L(h, h)
        return [up, low], [], "SO"

    def P(self):
        h = self.h
        H = CAP - 2 * h
        rp = H * 0.275
        xb = h + 150
        p = Path().M(h, h).L(h, CAP - h).L(xb, CAP - h).arc(xb, CAP - h - rp, rp, 90, -90).L(h, CAP - h - 2 * rp)
        return [p], [], "SO"

    def R_(self):
        p, _, _ = self.P()
        h = self.h
        H = CAP - 2 * h
        rp = H * 0.275
        xb = h + 150
        leg = line((xb + 10, CAP - h - 2 * rp), (xb + rp + 20, h))
        return p + [leg], [], "SD"

    def E(self):
        h = self.h
        w = 330
        return [line((h + w, CAP - h), (h, CAP - h), (h, h), (h + w, h)), line((h, CAP / 2), (h + w * 0.88, CAP / 2))], [], "SD"

    def F(self):
        h = self.h
        w = 320
        return [line((h + w, CAP - h), (h, CAP - h), (h, h)), line((h, CAP / 2 - 10), (h + w * 0.88, CAP / 2 - 10))], [], "SD"

    def H_(self):
        h = self.h
        w = 450
        return [line((h, CAP - h), (h, h)), line((h + w, CAP - h), (h + w, h)), line((h, CAP / 2), (h + w, CAP / 2))], [], "SS"

    def I(self):
        h = self.h
        return [line((h, CAP - h), (h, h))], [], "SS"

    def J(self):
        h = self.h
        rj = 145
        x = h + 2 * rj
        return [Path().M(x, CAP - h).L(x, h + rj).arc(x - rj, h + rj, rj, 0, -180)], [], "DS"

    def K(self):
        h = self.h
        w = 420
        yj = CAP * 0.36
        arm = line((h + w, CAP - h), (h, yj))
        j = (h + w * 0.42, yj + (CAP - h - yj) * 0.42)
        return [line((h, CAP - h), (h, h)), arm, line(j, (h + w + 15, h))], [], "SD"

    def L_(self):
        h = self.h
        return [line((h, CAP - h), (h, h), (h + 300, h))], [], "SD"

    def M_(self):
        h = self.h
        w = 600
        return [line((h, h), (h, CAP - h), (h + w / 2, h), (h + w, CAP - h), (h + w, h))], [], "SS"

    def N(self):
        h = self.h
        w = 470
        return [line((h, h), (h, CAP - h), (h + w, h), (h + w, CAP - h))], [], "SS"

    def S(self):
        return self.s(CAP + OV, 1.1)

    def T(self):
        h = self.h
        w = 460
        return [line((h, CAP - h), (h + w, CAP - h)), line((h + w / 2, CAP - h), (h + w / 2, h))], [], "DD"

    def U(self):
        h = self.h
        R = 235
        p = Path().M(h, CAP - h).L(h, h + R).arc(h + R, h + R, R, 180, 360).L(h + 2 * R, CAP - h)
        return [p], [], "SS"

    def V(self):
        h = self.h
        w = 560
        return [line((h, CAP - h), (h + w / 2, h), (h + w, CAP - h))], [], "DD"

    def W(self):
        h = self.h
        a = 195
        return [line((h, CAP - h), (h + a, h), (h + 2 * a, CAP - h), (h + 3 * a, h), (h + 4 * a, CAP - h))], [], "DD"

    def X_(self):
        h = self.h
        w = 520
        return [line((h, CAP - h), (h + w, h)), line((h, h), (h + w, CAP - h))], [], "DD"

    def Y(self):
        h = self.h
        w = 540
        m = (h + w / 2, CAP * 0.42)
        return [line((h, CAP - h), m, (h + w, CAP - h)), line(m, (m[0], h))], [], "DD"

    def Z(self):
        h = self.h
        w = 470
        return [line((h, CAP - h), (h + w, CAP - h), (h, h), (h + w, h))], [], "DD"

    def A(self):
        h = self.h
        w = 600
        l, t, r = (h, h), (h + w / 2, CAP - h), (h + w, h)
        yb = CAP * 0.3
        return [line(l, t, r), line(on_line(l, t, yb), on_line(r, t, yb))], [], "DD"

    # digits --------------------------------------------------------------
    DW = 300

    def zero(self):
        h = self.h
        rx = 215 - h
        return [circ(h + rx, CAP / 2, rx, CAP / 2 + OV - h)], [], "NN"

    def one(self):
        h = self.h
        x = h + 150
        return [line((h, CAP - h - 130), (x, CAP - h), (x, h))], [], "NN"

    def two(self):
        h = self.h
        r = 165
        cx = h + r
        p = Path().arc(cx, CAP - h - r, r, 158, -32)
        p.L(h, h).L(h + 2 * r + 15, h)
        return [p], [], "NN"

    def three(self):
        h = self.h
        H = CAP - 2 * h
        ra, rb = H * 0.225, H * 0.275
        cx = h + rb * 1.05
        p = Path().arc(cx, CAP - h - ra, ra * 1.05, 150, -90, ra).arc(cx, h + rb, rb * 1.05, 90, -150, rb)
        return [p], [], "NN"

    def four(self):
        h = self.h
        x = h + 330
        return [line((x, h), (x, CAP - h), (h, 230), (h + 430, 230))], [], "NN"

    def five(self):
        h = self.h
        r = 205
        cx = h + r * 1.02
        a = 135
        sx = cx + r * math.cos(math.radians(a))
        sy = h + r + r * math.sin(math.radians(a))
        p = Path().M(cx + r * 0.85, CAP - h).L(sx, CAP - h).L(sx, sy).arc(cx, h + r, r, a, -150)
        return [p], [], "NN"

    def six(self):
        h = self.h
        r = 205
        cx = h + r
        cy = h + r
        R6 = 430
        e = 180 - math.degrees(math.asin((CAP - h - cy) / R6))
        tail = Path().arc(cx - r + R6, cy, R6, 180, e)
        return [circ(cx, cy, r), tail], [], "NN"

    def seven(self):
        h = self.h
        return [line((h, CAP - h), (h + 400, CAP - h), (h + 120, h))], [], "NN"

    def eight(self):
        h = self.h
        H = CAP - 2 * h
        ra, rb = H * 0.225, H * 0.275
        cx = h + rb * 1.02
        return [circ(cx, CAP - h - ra, ra * 0.98, ra), circ(cx, h + rb, rb * 1.02, rb)], [], "NN"

    def nine(self):
        p, d, _ = self.six()
        h = self.h
        r = 205
        p, d = rot180(p, d, h + r, CAP / 2)
        return p, d, "NN"

    # punctuation ---------------------------------------------------------
    def space(self):
        return [], [], None

    def period(self):
        return [], [(self.dot, self.dot, self.dot)], "NN"

    def comma(self):
        h = self.h
        return [line((h + 40, h + 30), (h, -110))], [], "NN"

    def colon(self):
        d = self.dot
        return [], [(d, d, d), (d, X - d, d)], "NN"

    def semicolon(self):
        p, _, s = self.comma()
        return p, [(self.h + 40, X - self.dot, self.dot)], s

    def exclam(self):
        h, d = self.h, self.dot
        return [line((d, CAP - h), (d, 230))], [(d, d, d)], "NN"

    def question(self):
        h, d = self.h, self.dot
        r = 150
        cx = h + r
        p = Path().arc(cx, CAP - h - r, r, 155, -60)
        p.L(cx, 280).L(cx, 230)
        return [p], [(cx, d, d)], "NN"

    def hyphen(self):
        h = self.h
        return [line((h, 290), (h + 170, 290))], [], "NN"

    def endash(self):
        h = self.h
        return [line((h, 290), (h + 420, 290))], [], "NN"

    def emdash(self):
        h = self.h
        return [line((h, 290), (h + 860, 290))], [], "NN"

    def quotesingle(self):
        h = self.h
        return [line((h, CAP - h), (h, CAP - 180))], [], "NN"

    def quotedbl(self):
        h = self.h
        return [line((h, CAP - h), (h, CAP - 180)), line((h + 150, CAP - h), (h + 150, CAP - 180))], [], "NN"

    def quoteright(self):
        h = self.h
        return [line((h + 40, CAP - h), (h, CAP - 190))], [], "NN"

    def quoteleft(self):
        h = self.h
        return [line((h, CAP - 190 + 2 * h), (h + 40, CAP - h + 20))], [], "NN"

    def quotedblright(self):
        h = self.h
        return [line((h + 40, CAP - h), (h, CAP - 190)), line((h + 190, CAP - h), (h + 150, CAP - 190))], [], "NN"

    def quotedblleft(self):
        h = self.h
        return [line((h, CAP - 190 + 2 * h), (h + 40, CAP - h + 20)), line((h + 150, CAP - 190 + 2 * h), (h + 190, CAP - h + 20))], [], "NN"

    def parenleft(self):
        h = self.h
        R = 560
        cy = CAP / 2 - 40
        return [Path().arc(h + R, cy, R, 180 - 42, 180 + 42)], [], "ND"

    def parenright(self):
        p, _, _ = self.parenleft()
        xs = [q[0] for q in p[0].samples()]
        m = min(xs) + max(xs)
        return [p[0].map(lambda q: (m - q[0], q[1]))], [], "DN"

    def slash(self):
        h = self.h
        return [line((h, -110), (h + 300, CAP + 30))], [], "DD"

    def periodcentered(self):
        return [], [(self.dot, 300, self.dot)], "NN"

    def ellipsis(self):
        d = self.dot
        return [], [(d, d, d), (d + 230, d, d), (d + 460, d, d)], "NN"

    def copyright(self):
        h = self.h
        R = CAP / 2 - h
        cx = h + R
        r = R * 0.48
        return [circ(cx, CAP / 2, R), Path().arc(cx, CAP / 2, r, 50, 310)], [], "OO"

    def bullet(self):
        return [], [(90, 330, 90)], "NN"

    def multiply(self):
        h = self.h
        s = 250
        return [line((h, 210), (h + s, 210 + s)), line((h, 210 + s), (h + s, 210))], [], "NN"

    def plus(self):
        h = self.h
        s = 360
        return [line((h, 330), (h + s, 330)), line((h + s / 2, 330 - s / 2), (h + s / 2, 330 + s / 2))], [], "NN"

    def plusminus(self):
        h = self.h
        s = 360
        return [line((h, 380), (h + s, 380)), line((h + s / 2, 230), (h + s / 2, 530)), line((h, 110), (h + s, 110))], [], "NN"

    def equal(self):
        h = self.h
        s = 360
        return [line((h, 410), (h + s, 410)), line((h, 250), (h + s, 250))], [], "NN"

    def arrowright(self):
        h = self.h
        s = 560
        return [line((h, 330), (h + s, 330)), line((h + s - 190, 330 + 190), (h + s, 330), (h + s - 190, 330 - 190))], [], "NN"

    def manat(self):
        h = self.h
        R = 220
        cx = h + R
        legs = 300
        p = Path().M(h, h).L(h, legs).arc(cx, legs, R, 180, 0).L(h + 2 * R, h)
        return [p, line((cx, h), (cx, CAP - h))], [], "SS"

    def percent(self):
        h = self.h
        r = 95
        return [circ(h + r, CAP - h - r, r), circ(h + 330 + r, h + r, r), line((h + 30, h), (h + 2 * r + 300, CAP - h))], [], "NN"

    def asterisk(self):
        h = self.h
        cx, cy, r = h + 150, CAP - 160, 150
        ps = []
        for a in (90, 30, 150):
            dx, dy = r * math.cos(math.radians(a)), r * math.sin(math.radians(a))
            ps.append(line((cx - dx, cy - dy), (cx + dx, cy + dy)))
        return ps, [], "NN"

    def underscore(self):
        h = self.h
        return [line((h, -120), (h + 480, -120))], [], "NN"

    def bar(self):
        h = self.h
        return [line((h, CAP + 60), (h, -160))], [], "SS"

    def bracketleft(self):
        h = self.h
        return [line((h + 150, CAP + 40), (h, CAP + 40), (h, -140), (h + 150, -140))], [], "SD"

    def bracketright(self):
        h = self.h
        return [line((h, CAP + 40), (h + 150, CAP + 40), (h + 150, -140), (h, -140))], [], "DS"

    def less(self):
        h = self.h
        return [line((h + 330, 520), (h, 330), (h + 330, 140))], [], "NN"

    def greater(self):
        h = self.h
        return [line((h, 520), (h + 330, 330), (h, 140))], [], "NN"

    def numbersign(self):
        h = self.h
        return [line((h + 120, h + 20), (h + 180, CAP - h - 20)), line((h + 320, h + 20), (h + 380, CAP - h - 20)),
                line((h, 470), (h + 480, 470)), line((h, 230), (h + 480, 230))], [], "NN"

    def dollar(self):
        p, d, _ = self.S()
        h = self.h
        rs = (CAP + OV - 2 * h) / 4
        cx = h + rs * 1.1
        return p + [line((cx, CAP + 60), (cx, -80))], d, "NN"


def accented(base, kind, cap=False):
    def fn(g):
        paths, dots, sides = base(g)
        pts = [q for p in paths for q in p.samples()] + [(x, y) for x, y, _ in dots]
        cx = (min(p[0] for p in pts) + max(p[0] for p in pts)) / 2
        top = CAP if cap else X
        if kind == "dieresis":
            y = top + (130 if cap else 160) + g.dot
            dots = dots + [(cx - 105, y, g.dot), (cx + 105, y, g.dot)]
        elif kind == "dot":
            dots = dots + [(cx, top + 130 + g.dot, g.dot)]
        elif kind == "breve":
            y0 = top + (110 if cap else 140) + g.h
            paths = paths + [Path().arc(cx, y0 + 95, 95, 200, 340)]
        elif kind == "cedilla":
            hook = Path().M(cx + 10, g.h).L(cx + 10, -55).arc(cx - 40, -55, 50, 0, -150)
            paths = paths + [hook]
        return paths, dots, sides

    return fn


def glyph_table():
    g = G
    t = {}
    for ch in "abcdefgijklmnopqrstuvwxyz":
        if ch == "h":
            continue
        t[ch] = (ord(ch), getattr(g, ch))
    t["h"] = (ord("h"), g.h_)
    for ch in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
        meth = {"G": "G_", "R": "R_", "H": "H_", "L": "L_", "M": "M_", "X": "X_"}.get(ch, ch)
        t[ch] = (ord(ch), getattr(g, meth))
    t["dotlessi"] = (0x131, g.dotless_i)
    t["dotlessj"] = (None, g.dotless_j)
    t["schwa"] = (0x259, g.schwa)
    t["Schwa"] = (0x18F, g.schwa_cap)
    t["odieresis"] = (0xF6, accented(g.o, "dieresis"))
    t["udieresis"] = (0xFC, accented(g.u, "dieresis"))
    t["Odieresis"] = (0xD6, accented(g.O, "dieresis", True))
    t["Udieresis"] = (0xDC, accented(g.U, "dieresis", True))
    t["gbreve"] = (0x11F, accented(g.g, "breve"))
    t["Gbreve"] = (0x11E, accented(g.G_, "breve", True))
    t["ccedilla"] = (0xE7, accented(g.c, "cedilla"))
    t["Ccedilla"] = (0xC7, accented(g.C, "cedilla", True))
    t["scedilla"] = (0x15F, accented(g.s, "cedilla"))
    t["Scedilla"] = (0x15E, accented(g.S, "cedilla", True))
    t["Idotaccent"] = (0x130, accented(g.I, "dot", True))
    for i, nm in enumerate(["zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"]):
        t[nm] = (0x30 + i, getattr(g, nm))
    for nm, cp in [("space", 0x20), ("period", 0x2E), ("comma", 0x2C), ("colon", 0x3A), ("semicolon", 0x3B),
                   ("exclam", 0x21), ("question", 0x3F), ("hyphen", 0x2D), ("endash", 0x2013), ("emdash", 0x2014),
                   ("quotesingle", 0x27), ("quotedbl", 0x22), ("quoteright", 0x2019), ("quoteleft", 0x2018),
                   ("quotedblright", 0x201D), ("quotedblleft", 0x201C), ("parenleft", 0x28), ("parenright", 0x29),
                   ("slash", 0x2F), ("bullet", 0x2022), ("multiply", 0xD7), ("plus", 0x2B), ("plusminus", 0xB1),
                   ("equal", 0x3D), ("arrowright", 0x2192), ("manat", 0x20BC), ("percent", 0x25),
                   ("asterisk", 0x2A), ("underscore", 0x5F), ("bar", 0x7C), ("bracketleft", 0x5B),
                   ("bracketright", 0x5D), ("less", 0x3C), ("greater", 0x3E), ("numbersign", 0x23),
                   ("dollar", 0x24), ("periodcentered", 0xB7), ("copyright", 0xA9), ("ellipsis", 0x2026)]:
        t[nm] = (cp, getattr(g, nm))
    return t


TABULAR = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"}
