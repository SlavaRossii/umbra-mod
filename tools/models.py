"""Entity models for Umbra, described as numbers.

Run from the repo root:  python3 tools/models.py
It writes BOTH
  * src/main/java/dev/umbra/client/model/GeneratedModels.java  (LayerDefinitions: cubes, pivots, rotations)
  * src/main/resources/assets/umbra/textures/entity/<model>.png  (textures painted onto each cube's UV layout)
so geometry and textures can never drift apart. Animations are hand-written in client/model/Animations.java
and refer to parts by name.

Conventions (vanilla model space): units are pixels (16 = one block), +Y is DOWN, the ground is y = 24,
the front of the creature is -Z, its right side is -X.
A cube (w, h, d) placed at texture offset (u, v) uses this UV layout:
    top (u+d, v) w x d | bottom (u+d+w, v) w x d
    right (u, v+d) d x h | front (u+d, v+d) w x h | left (u+d+w, v+d) d x h | back (u+2d+w, v+d) w x h
"""
import math
import os
import random
from PIL import Image

JAVA = "src/main/java/dev/umbra/client/model/GeneratedModels.java"
TEX = "src/main/resources/assets/umbra/textures/entity"


# ------------------------------------------------------------------ paint

class Paint:
    """Colour of a cube: a base colour with noise, plus optional per-face decorations."""

    def __init__(self, base, noise=10, faces=None, shade=True):
        self.base = base
        self.noise = noise
        self.faces = faces or {}
        self.shade = shade


def P(base, noise=10, **faces):
    return Paint(base, noise, faces)


# ------------------------------------------------------------------ model description

class Cube:
    def __init__(self, x, y, z, w, h, d, paint, grow=0.0):
        self.x, self.y, self.z, self.w, self.h, self.d = x, y, z, w, h, d
        self.paint = paint
        self.grow = grow
        self.u = self.v = 0


class Part:
    def __init__(self, name, parent, pivot=(0, 0, 0), rot=(0, 0, 0), cubes=()):
        self.name, self.parent, self.pivot, self.rot = name, parent, pivot, rot
        self.cubes = list(cubes)


class Model:
    def __init__(self, name):
        self.name = name
        self.parts = []

    def part(self, name, parent="root", pivot=(0, 0, 0), rot=(0, 0, 0), cubes=()):
        self.parts.append(Part(name, parent, pivot, rot, cubes))
        return name


C = Cube


# ------------------------------------------------------------------ face decorations
# Each takes (px, x0, y0, w, h, rnd) and paints inside that face rectangle.

def eyes(color, y=0.45, spread=0.25, size=1, glow=None):
    def f(px, x0, y0, w, h, rnd):
        cy = y0 + int(h * y)
        for cx in (x0 + int(w * (0.5 - spread)), x0 + int(w * (0.5 + spread)) - size):
            for dx in range(size):
                for dy in range(size):
                    px[cx + dx, cy + dy] = color
            if glow:
                for gx, gy in ((cx - 1, cy), (cx + size, cy)):
                    if x0 <= gx < x0 + w:
                        px[gx, gy] = glow
    return f


def many_eyes(color, n, rows=(0.3, 0.55)):
    def f(px, x0, y0, w, h, rnd):
        r = random.Random(n * 31 + w)
        for i in range(n):
            cx = x0 + 1 + r.randint(0, max(0, w - 3))
            cy = y0 + int(h * rows[i % len(rows)])
            px[cx, cy] = color
    return f


def mouth(color, teeth=(235, 230, 215, 255), y=0.65, width=0.7):
    def f(px, x0, y0, w, h, rnd):
        mw = max(2, int(w * width))
        mx = x0 + (w - mw) // 2
        my = y0 + int(h * y)
        for x in range(mx, mx + mw):
            px[x, my] = color
            px[x, min(y0 + h - 1, my + 1)] = color
            if (x - mx) % 2 == 0:
                px[x, my] = teeth
    return f


def big_eye(white, iris, pupil):
    def f(px, x0, y0, w, h, rnd):
        cx, cy = x0 + (w - 1) / 2, y0 + (h - 1) / 2
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                d = math.hypot(x - cx, y - cy) / (min(w, h) / 2)
                if d < 0.28:
                    px[x, y] = pupil
                elif d < 0.62:
                    px[x, y] = iris
                elif d < 1.05:
                    px[x, y] = white
                if 0.7 < d < 1.0 and rnd.random() < 0.12:
                    px[x, y] = (170, 30, 40, 255)
    return f


def rune(color):
    def f(px, x0, y0, w, h, rnd):
        cx, cy = x0 + w // 2, y0 + h // 2
        for i in range(-3, 4):
            if 0 <= cy + i - y0 < h:
                px[cx, cy + i] = color
            if 0 <= cx + i - x0 < w and abs(i) < 3:
                px[cx + i, cy - 1] = color
        for (dx, dy) in ((-2, 2), (2, 2), (-1, 3), (1, 3)):
            if x0 <= cx + dx < x0 + w and y0 <= cy + dy < y0 + h:
                px[cx + dx, cy + dy] = color
    return f


def slit(color, y=0.45):
    def f(px, x0, y0, w, h, rnd):
        yy = y0 + int(h * y)
        for x in range(x0 + 1, x0 + w - 1):
            px[x, yy] = color
    return f


def stripes(color, every=3, vertical=False):
    def f(px, x0, y0, w, h, rnd):
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                k = (x - x0) if vertical else (y - y0)
                if k % every == 0:
                    px[x, y] = color
    return f


def latch(color):
    def f(px, x0, y0, w, h, rnd):
        cx = x0 + w // 2
        for x in range(cx - 1, cx + 1):
            for y in range(y0, min(y0 + 3, y0 + h)):
                px[x, y] = color
    return f


def glyph(color):
    def f(px, x0, y0, w, h, rnd):
        r = random.Random(w * 7 + h)
        for _ in range(6):
            x = x0 + r.randint(1, w - 2)
            y = y0 + r.randint(1, h - 2)
            px[x, y] = color
            px[min(x0 + w - 1, x + 1), y] = color
        eyes(color, 0.4, 0.22)(px, x0, y0, w, h, rnd)
    return f


def combine(*fs):
    def f(px, x0, y0, w, h, rnd):
        for g in fs:
            g(px, x0, y0, w, h, rnd)
    return f


# ------------------------------------------------------------------ helpers for repeated shapes

def leg_pair(m, name, parent, at, side, length, thickness, paint, up=0.5, out=0.3, yaw=0.0, lower_len=None):
    """A two-segment arthropod leg. side = +1 (left, +x) or -1 (right, -x).

    The upper segment points sideways and is rolled up by `up` radians; the lower one hangs from its tip.
    Rolls add up (parent then child), so the lower segment's world tilt is up + child = -side * out:
    its foot splays outwards by `out` radians. With body pivot y0 the foot ends at about
    y0 - length*sin(up) + lower_len*cos(out); the creatures below are tuned so that lands on y = 24.
    """
    lower_len = lower_len or length + 2
    t = thickness
    upper = m.part(name + "_upper", parent, at, (0, yaw, -side * up),
                   [C(0 if side > 0 else -length, -t / 2, -t / 2, length, t, t, paint)])
    m.part(name + "_lower", upper, (side * length, 0, 0), (0, 0, side * (up - out)),
           [C(-t / 2 + (0 if side > 0 else 0), 0, -t / 2, t, lower_len, t, paint)])


# ------------------------------------------------------------------ the creatures

def crawler():
    m = Model("crawler")
    shell = P((34, 30, 38, 255), 8)
    skin = P((52, 44, 58, 255), 10)
    head = P((40, 34, 44, 255), 8, front=many_eyes((230, 60, 60, 255), 6))
    body = m.part("body", "root", (0, 15, 0), (0, 0, 0), [
        C(-4, -3, -4, 8, 5, 8, skin),            # thorax
        C(-5, -4, 4, 10, 6, 12, shell),          # abdomen behind
    ])
    m.part("head", body, (0, -1, -4), (0, 0, 0), [C(-3, -3, -6, 6, 5, 6, head)])
    leg = P((26, 22, 30, 255), 6)
    for i, z in enumerate((-3, 0, 3)):
        yaw = (-0.55, 0.0, 0.55)[i]
        leg_pair(m, f"leg_r{i}", body, (-4, 0, z), -1, 13, 2, leg, 0.55, 0.3, -yaw, 16)
        leg_pair(m, f"leg_l{i}", body, (4, 0, z), 1, 13, 2, leg, 0.55, 0.3, yaw, 16)
    # foot check: 15 - 13*sin(0.55) + 16*cos(0.3) = 23.5 -> on the ground
    return m


def blind():
    m = Model("blind")
    skin = P((160, 148, 140, 255), 12)
    dark = P((120, 108, 104, 255), 10)
    head = P((165, 152, 145, 255), 10, front=mouth((60, 10, 14, 255), y=0.55, width=0.8))
    m.part("leg_r", "root", (-2, 10, 0), (0, 0, 0), [C(-1.5, 0, -1.5, 3, 14, 3, dark)])
    m.part("leg_l", "root", (2, 10, 0), (0, 0, 0), [C(-1.5, 0, -1.5, 3, 14, 3, dark)])
    body = m.part("body", "root", (0, 10, 0), (0.25, 0, 0), [C(-3.5, -12, -2, 7, 12, 4, skin)])
    m.part("head", body, (0, -12, -1), (0.35, 0, 0), [C(-3, -9, -4, 6, 9, 6, head)])
    m.part("arm_r", body, (-4.5, -11, 0), (0, 0, 0.05), [C(-1, 0, -1, 2, 18, 2, skin)])
    m.part("arm_l", body, (4.5, -11, 0), (0, 0, -0.05), [C(-1, 0, -1, 2, 18, 2, skin)])
    return m


def light_eater():
    m = Model("light_eater")
    fur = P((228, 224, 214, 255), 10)
    wing = P((236, 232, 222, 255), 14, up=stripes((200, 190, 170, 255), 3), down=stripes((200, 190, 170, 255), 3))
    head = P((222, 218, 208, 255), 8, front=eyes((30, 20, 20, 255), 0.4, 0.28, 1))
    body = m.part("body", "root", (0, 17, 0), (0.15, 0, 0), [C(-2, -2, -4, 4, 4, 9, fur)])
    m.part("head", body, (0, -1, -4), (0, 0, 0), [C(-2, -2, -4, 4, 4, 4, head)])
    m.part("antenna_r", "head", (-1, -2, -3), (0.5, 0, -0.3), [C(-0.5, -6, -0.5, 1, 6, 1, fur)])
    m.part("antenna_l", "head", (1, -2, -3), (0.5, 0, 0.3), [C(-0.5, -6, -0.5, 1, 6, 1, fur)])
    m.part("wing_r", body, (-2, -2, -1), (0, 0, 0), [C(-14, 0, -4, 14, 1, 10, wing)])
    m.part("wing_l", body, (2, -2, -1), (0, 0, 0), [C(0, 0, -4, 14, 1, 10, wing)])
    m.part("wing_r2", body, (-2, -1, 4), (0, 0, 0), [C(-10, 0, -1, 10, 1, 7, wing)])
    m.part("wing_l2", body, (2, -1, 4), (0, 0, 0), [C(0, 0, -1, 10, 1, 7, wing)])
    return m


def shade():
    m = Model("shade")
    smoke = P((10, 8, 14, 255), 6)
    head = P((12, 10, 16, 255), 6, front=eyes((240, 240, 255, 255), 0.45, 0.25, 1, glow=(120, 120, 160, 255)))
    core = m.part("core", "root", (0, 2, 0), (0, 0, 0), [C(-4, -4, -4, 8, 8, 8, head)])
    t1 = m.part("tail1", core, (0, 4, 1), (0.4, 0, 0), [C(-3, 0, -3, 6, 6, 6, smoke)])
    t2 = m.part("tail2", t1, (0, 6, 0), (0.3, 0, 0), [C(-2, 0, -2, 4, 6, 4, smoke)])
    m.part("tail3", t2, (0, 6, 0), (0.3, 0, 0), [C(-1, 0, -1, 2, 5, 2, smoke)])
    m.part("arm_r", core, (-4, 1, 0), (0, 0, 0.6), [C(-1, 0, -1, 2, 8, 2, smoke)])
    m.part("arm_l", core, (4, 1, 0), (0, 0, -0.6), [C(-1, 0, -1, 2, 8, 2, smoke)])
    return m


def statue():
    m = Model("citadel_statue")
    stone = P((122, 122, 128, 255), 14)
    face = P((128, 128, 134, 255), 12, front=eyes((70, 70, 76, 255), 0.45, 0.22, 1))
    m.part("robe", "root", (0, 12, 0), (0, 0, 0), [C(-5, 0, -3, 10, 12, 6, stone)])
    body = m.part("body", "root", (0, 12, 0), (0, 0, 0), [C(-4, -12, -2, 8, 12, 4, stone)])
    m.part("head", body, (0, -12, 0), (0, 0, 0), [C(-4, -8, -4, 8, 8, 8, face)])
    m.part("arm_r", body, (-5, -10, 0), (0, 0, 0), [C(-2, -2, -2, 3, 12, 4, stone)])
    m.part("arm_l", body, (5, -10, 0), (0, 0, 0), [C(-1, -2, -2, 3, 12, 4, stone)])
    m.part("wing_r", body, (-2, -10, 2), (0.3, -0.5, 0), [C(-1, 0, 0, 1, 14, 8, stone)])
    m.part("wing_l", body, (2, -10, 2), (0.3, 0.5, 0), [C(0, 0, 0, 1, 14, 8, stone)])
    return m


def keeper():
    m = Model("citadel_keeper")
    steel = P((34, 38, 50, 255), 10)
    dark = P((24, 26, 34, 255), 8)
    chest = P((36, 40, 54, 255), 10, front=rune((110, 255, 230, 255)))
    helm = P((40, 44, 58, 255), 10, front=slit((110, 255, 230, 255), 0.45))
    m.part("leg_r", "root", (-3, 12, 0), (0, 0, 0), [C(-2.5, 0, -2.5, 5, 12, 5, dark)])
    m.part("leg_l", "root", (3, 12, 0), (0, 0, 0), [C(-2.5, 0, -2.5, 5, 12, 5, dark)])
    body = m.part("body", "root", (0, 12, 0), (0, 0, 0), [C(-6, -13, -3.5, 12, 13, 7, chest)])
    m.part("head", body, (0, -13, -0.5), (0, 0, 0), [C(-4, -8, -4, 8, 8, 8, helm),
                                                     C(-1, -11, -1, 2, 3, 2, steel)])
    m.part("arm_r", body, (-7, -11, 0), (0, 0, 0), [C(-4, -2, -2.5, 4, 15, 5, steel),
                                                    C(-5, -3, -3.5, 6, 4, 7, dark)])
    m.part("arm_l", body, (7, -11, 0), (0, 0, 0), [C(0, -2, -2.5, 4, 15, 5, steel),
                                                   C(-1, -3, -3.5, 6, 4, 7, dark)])
    return m


def watcher():
    m = Model("watcher")
    flesh = P((150, 120, 120, 255), 12)
    eye = P((220, 215, 205, 255), 8, front=big_eye((235, 232, 222, 255), (150, 40, 60, 255), (10, 6, 8, 255)))
    m.part("eye", "root", (0, 0, 0), (0, 0, 0), [C(-6, -6, -6, 12, 12, 12, eye),
                                                 C(-7, -7, 0, 14, 2, 7, flesh)])
    for i, (x, z) in enumerate(((-3, -3), (3, -3), (-3, 3), (3, 3))):
        a = m.part(f"tentacle{i}_a", "eye", (x, 5, z), (0, 0, 0), [C(-2, 0, -2, 4, 6, 4, flesh)])
        b = m.part(f"tentacle{i}_b", a, (0, 6, 0), (0, 0, 0), [C(-1.5, 0, -1.5, 3, 6, 3, flesh)])
        m.part(f"tentacle{i}_c", b, (0, 6, 0), (0, 0, 0), [C(-1, 0, -1, 2, 6, 2, flesh)])
    return m


def mimic_queen():
    m = Model("mimic_queen")
    wood = P((120, 84, 48, 255), 10, front=latch((180, 170, 120, 255)))
    lidp = P((126, 88, 50, 255), 10, front=latch((180, 170, 120, 255)))
    inside = P((120, 20, 34, 255), 10)
    gold = P((220, 180, 60, 255), 12)
    tooth = P((235, 230, 215, 255), 4)
    leg = P((60, 40, 26, 255), 8)
    body = m.part("body", "root", (0, 12, 0), (0, 0, 0), [C(-7, -4, -6, 14, 9, 12, wood)])
    m.part("tongue", body, (0, -4, 0), (0, 0, 0), [C(-2, -1, -7, 4, 1, 10, inside)])
    lid = m.part("lid", body, (0, -4, 6), (0, 0, 0), [C(-7, -5, -12, 14, 5, 12, lidp)])
    for i in range(5):
        m.part(f"tooth{i}", lid, (-6 + i * 3, 0, -11), (0, 0, 0), [C(-0.5, 0, -0.5, 1, 2, 1, tooth)])
    m.part("crown", lid, (0, -5, -6), (0, 0, 0), [C(-4, -2, -3, 8, 2, 6, gold),
                                                 C(-4, -4, -3, 1, 2, 1, gold), C(3, -4, -3, 1, 2, 1, gold),
                                                 C(-4, -4, 2, 1, 2, 1, gold), C(3, -4, 2, 1, 2, 1, gold)])
    for i, z in enumerate((-4, 4)):
        leg_pair(m, f"leg_r{i}", body, (-7, 2, z), -1, 8, 2, leg, 0.3, 0.5, 0.0, 14)
        leg_pair(m, f"leg_l{i}", body, (7, 2, z), 1, 8, 2, leg, 0.3, 0.5, 0.0, 14)
    return m


def shepherd():
    m = Model("shepherd")
    robe = P((26, 18, 36, 255), 8)
    hood = P((30, 22, 40, 255), 8, front=combine(lambda px, x0, y0, w, h, r: [px.__setitem__((x, y), (6, 4, 8, 255))
                                                                              for x in range(x0 + 1, x0 + w - 1)
                                                                              for y in range(y0 + 2, y0 + h)],
                                                 eyes((170, 120, 255, 255), 0.55, 0.22, 1)))
    wood = P((70, 52, 36, 255), 8)
    m.part("skirt", "root", (0, 8, 0), (0, 0, 0), [C(-5, 0, -4, 10, 16, 8, robe)])
    body = m.part("body", "root", (0, 8, 0), (0, 0, 0), [C(-4, -10, -2.5, 8, 10, 5, robe)])
    m.part("head", body, (0, -10, 0), (0, 0, 0), [C(-4.5, -9, -4.5, 9, 9, 9, hood)])
    m.part("arm_l", body, (5, -9, 0), (0, 0, -0.1), [C(-1.5, -1, -1.5, 3, 14, 3, robe)])
    arm = m.part("arm_r", body, (-5, -9, 0), (-0.4, 0, 0.1), [C(-1.5, -1, -1.5, 3, 14, 3, robe)])
    # the arm is raised by -0.4 rad; +0.4 on the staff keeps it upright in the hand
    m.part("staff", arm, (0, 12, -1), (0.4, 0, 0), [C(-0.5, -22, -0.5, 1, 30, 1, wood),
                                                    C(-0.5, -25, -0.5, 1, 3, 1, wood),
                                                    C(-0.5, -26, 0.5, 1, 1, 3, wood),
                                                    C(-0.5, -25, 3.5, 1, 3, 1, wood)])
    return m


def architect():
    m = Model("architect")
    stone = P((70, 66, 80, 255), 10)
    trim = P((95, 90, 110, 255), 10)
    face = P((84, 80, 96, 255), 8, front=glyph((255, 220, 120, 255)))
    block = P((60, 58, 72, 255), 10, front=stripes((255, 220, 120, 255), 4, True))
    m.part("leg_r", "root", (-2.5, 10, 0), (0, 0, 0), [C(-1.5, 0, -1.5, 3, 14, 3, stone)])
    m.part("leg_l", "root", (2.5, 10, 0), (0, 0, 0), [C(-1.5, 0, -1.5, 3, 14, 3, stone)])
    body = m.part("body", "root", (0, 10, 0), (0, 0, 0), [C(-4.5, -12, -2.5, 9, 12, 5, trim)])
    m.part("head", body, (0, -12, 0), (0, 0, 0), [C(-4, -8, -4, 8, 8, 8, face)])
    for s, name in ((-1, "arm_r"), (1, "arm_l")):
        a = m.part(name, body, (s * 6, -11, 0), (0, 0, 0), [C(-1.5, -1, -1.5, 3, 7, 3, stone)])
        m.part(name + "_block", a, (0, 9, 0), (0, 0, 0), [C(-3, -3, -3, 6, 6, 6, block)])
    m.part("orbit", body, (0, -6, 0), (0, 0, 0), [])
    for i in range(4):
        a = i * math.pi / 2
        m.part(f"cube{i}", "orbit", (round(math.cos(a) * 12, 2), 0, round(math.sin(a) * 12, 2)), (0.4, 0.3, 0),
               [C(-1.5, -1.5, -1.5, 3, 3, 3, block)])
    return m


def primordial_shadow():
    m = Model("primordial_shadow")
    void = P((5, 3, 9, 255), 5)
    smoke = P((14, 10, 22, 255), 8)
    head = P((8, 5, 14, 255), 5, front=combine(eyes((200, 130, 255, 255), 0.4, 0.26, 1, glow=(90, 50, 140, 255)),
                                               lambda px, x0, y0, w, h, r: px.__setitem__((x0 + w // 2, y0 + int(h * 0.25)), (230, 170, 255, 255))))
    m.part("leg_r", "root", (-2, 12, 0), (0, 0, 0), [C(-2, 0, -2, 4, 8, 4, void), C(-1.5, 8, -1.5, 3, 4, 3, smoke)])
    m.part("leg_l", "root", (2, 12, 0), (0, 0, 0), [C(-2, 0, -2, 4, 8, 4, void), C(-1.5, 8, -1.5, 3, 4, 3, smoke)])
    body = m.part("body", "root", (0, 12, 0), (0, 0, 0), [C(-5, -12, -3, 10, 12, 6, void)])
    m.part("head", body, (0, -12, 0), (0, 0, 0), [C(-4, -8, -4, 8, 8, 8, head),
                                                 C(-5, -10, -1, 2, 4, 2, void), C(3, -10, -1, 2, 4, 2, void)])
    m.part("chest", body, (0, -12, 0), (0, 0, 0), [C(-6, 0, -3.5, 12, 6, 7, void)])
    m.part("crown", "head", (0, -8, 0), (0, 0, 0), [C(-4, -3, -1, 1, 3, 1, smoke), C(3, -3, -1, 1, 3, 1, smoke),
                                                   C(-2, -4, -2, 1, 4, 1, smoke), C(1, -4, -2, 1, 4, 1, smoke), C(-0.5, -5, -3, 1, 5, 1, smoke)])
    for i, (x, y, yaw) in enumerate(((-3, -10, -0.6), (3, -10, 0.6), (-4, -6, -1.0), (4, -6, 1.0), (-2, -3, -0.3), (2, -3, 0.3))):
        a = m.part(f"tendril{i}_a", body, (x, y, 3), (0.9, yaw, 0), [C(-1.5, -1.5, 0, 3, 3, 7, smoke)])
        b = m.part(f"tendril{i}_b", a, (0, 0, 7), (0.3, 0, 0), [C(-1, -1, 0, 2, 2, 7, smoke)])
        m.part(f"tendril{i}_c", b, (0, 0, 7), (0.3, 0, 0), [C(-0.5, -0.5, 0, 1, 1, 6, void)])
    m.part("arm_r", body, (-6, -11, 0), (0, 0, 0.1), [C(-2, -1, -2, 3, 15, 4, void)])
    m.part("arm_l", body, (6, -11, 0), (0, 0, -0.1), [C(-1, -1, -2, 3, 15, 4, void)])
    for s, name in ((-1, "wing_r"), (1, "wing_l")):
        w1 = m.part(name, body, (s * 2, -10, 3), (0, s * 0.5, s * -0.3),
                    [C(0 if s > 0 else -14, -1, 0, 14, 2, 1, smoke)])
        m.part(name + "_tip", w1, (s * 14, 0, 0), (0, 0, s * -0.4),
               [C(0 if s > 0 else -12, -1, 0, 12, 1, 1, smoke),
                C(0 if s > 0 else -12, 0, 0, 12, 14, 1, smoke)])
        m.part(name + "_veil", w1, (0, 0, 0), (0, 0, 0), [C(0 if s > 0 else -14, 0, 0, 14, 16, 1, smoke)])
    return m


def shadow_villager():
    m = Model("shadow_villager")
    robe = P((52, 46, 60, 255), 10)
    face = P((32, 28, 38, 255), 8, front=eyes((150, 220, 210, 255), 0.4, 0.22, 1))
    m.part("legs", "root", (0, 12, 0), (0, 0, 0), [C(-4, 0, -3, 8, 12, 6, robe)])
    body = m.part("body", "root", (0, 12, 0), (0, 0, 0), [C(-4, -12, -3, 8, 12, 6, robe)])
    head = m.part("head", body, (0, -12, 0), (0, 0, 0), [C(-4, -10, -4, 8, 10, 8, face)])
    m.part("nose", head, (0, -3, -4), (0, 0, 0), [C(-1, -1, -2, 2, 4, 2, face)])
    m.part("arms", body, (0, -9, -1), (-0.75, 0, 0), [C(-4, 2, -2, 8, 4, 4, robe),
                                                      C(-8, -2, -2, 4, 8, 4, robe), C(4, -2, -2, 4, 8, 4, robe)])
    return m


def lurker():
    m = Model("lurker")
    skin = P((20, 24, 30, 255), 8, up=stripes((34, 44, 52, 255), 4))
    belly = P((52, 54, 60, 255), 8)
    head = P((22, 26, 32, 255), 8, front=combine(eyes((210, 245, 255, 255), 0.25, 0.34, 1, glow=(80, 140, 170, 255)),
                                                   mouth((70, 12, 16, 255), y=0.75, width=0.9)))
    lure = P((175, 245, 255, 255), 14)
    h = m.part("head", "root", (0, 18, -3), (0, 0, 0), [C(-4, -4, -7, 8, 6, 7, head)])
    m.part("jaw", h, (0, 2, -1), (0, 0, 0), [C(-3.5, 0, -6, 7, 2, 6, belly)])
    stalk = m.part("stalk", h, (0, -4, -5), (-0.7, 0, 0), [C(-0.5, -7, -0.5, 1, 7, 1, skin)])
    m.part("lure", stalk, (0, -7, 0), (0, 0, 0), [C(-1, -2, -1, 2, 2, 2, lure)])
    b1 = m.part("body1", "root", (0, 18, -3), (0, 0, 0), [C(-3, -3, 0, 6, 5, 8, skin)])
    b2 = m.part("body2", b1, (0, 0, 8), (0, 0, 0), [C(-2, -2, 0, 4, 4, 8, skin)])
    b3 = m.part("body3", b2, (0, 0, 8), (0, 0, 0), [C(-1.5, -1.5, 0, 3, 3, 7, skin)])
    m.part("fin", b3, (0, 0, 7), (0, 0, 0), [C(-0.5, -4, 0, 1, 8, 5, skin)])
    m.part("fin_r", b1, (-3, 1, 2), (0, 0, 0.5), [C(-5, 0, -1, 5, 1, 4, skin)])
    m.part("fin_l", b1, (3, 1, 2), (0, 0, -0.5), [C(0, 0, -1, 5, 1, 4, skin)])
    return m


def mimicker():
    """A chest, until it is not: legs folded away and lid shut while it waits (see Animations.mimicker)."""
    m = Model("mimicker")
    def planks(px, x0, y0, w, h, rnd):
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                    px[x, y] = (62, 40, 20, 255)
                elif (y - y0) % 4 == 0:
                    px[x, y] = (110, 74, 38, 255)
    wood = P((150, 104, 56, 255), 9, front=planks, back=planks, left=planks, right=planks, up=planks)
    lidp = P((156, 110, 60, 255), 9, front=combine(planks, latch((200, 200, 190, 255))), back=planks, left=planks, right=planks, up=planks)
    inside = P((120, 18, 30, 255), 10)
    tooth = P((236, 230, 214, 255), 4)
    leg = P((52, 34, 22, 255), 8)
    eye = P((255, 236, 150, 255), 6)
    body = m.part("body", "root", (0, 24, 0), (0, 0, 0), [C(-7, -10, -7, 14, 10, 14, wood)])
    m.part("tongue", body, (0, -10, 0), (0, 0, 0), [C(-2, -1, -6, 4, 1, 9, inside)])
    lid = m.part("lid", body, (0, -10, 7), (0, 0, 0), [C(-7, -5, -14, 14, 5, 14, lidp),
                                                       C(-1, -2, -15, 2, 4, 1, P((200, 200, 190, 255), 4))])
    for i in range(6):
        m.part(f"tooth{i}", lid, (-6 + i * 2.4, 0, -13), (0, 0, 0), [C(-0.5, 0, -0.5, 1, 2, 1, tooth)])
        m.part(f"fang{i}", body, (-6 + i * 2.4, -10, -6), (0, 0, 0), [C(-0.5, -2, -0.5, 1, 2, 1, tooth)])
    m.part("eyes", lid, (0, 0, -7), (0, 0, 0), [C(-4, -1, -4, 2, 1, 2, eye), C(2, -1, -4, 2, 1, 2, eye)])
    for i, z in enumerate((-4, 4)):
        leg_pair(m, f"leg_r{i}", body, (-7, -3, z), -1, 7, 2, leg, 0.3, 0.5, 0.0, 15)
        leg_pair(m, f"leg_l{i}", body, (7, -3, z), 1, 7, 2, leg, 0.3, 0.5, 0.0, 15)
    return m


def lamp_keeper():
    """Yan, the keeper of the lighthouse: an old man in a hood, a white beard, a lantern on a pole."""
    m = Model("lamp_keeper")
    coat = P((58, 66, 84, 255), 9, front=stripes((44, 50, 66, 255), 5, True))
    hood = P((50, 58, 74, 255), 8)
    face = P((196, 160, 132, 255), 6, front=combine(eyes((40, 60, 80, 255), 0.3, 0.2, 1),
                                                     lambda px, x0, y0, w, h, r: [px.__setitem__((x, y), (232, 232, 226, 255))
                                                                                  for x in range(x0, x0 + w) for y in range(y0 + h // 2 + 1, y0 + h)]))
    beard = P((230, 230, 224, 255), 6)
    boots = P((44, 34, 26, 255), 6)
    brass = P((190, 150, 70, 255), 10)
    pole = P((96, 70, 44, 255), 8)
    glow = P((215, 250, 255, 255), 6)
    m.part("legs", "root", (0, 12, 0), (0, 0, 0), [C(-4, 0, -3, 8, 10, 6, coat), C(-4, 10, -3.5, 8, 2, 7, boots)])
    body = m.part("body", "root", (0, 12, 0), (0, 0, 0), [C(-4.5, -12, -3, 9, 12, 6, coat)])
    head = m.part("head", body, (0, -12, 0), (0, 0, 0), [C(-4, -8, -4, 8, 8, 8, face), C(-4.5, -9, -4.5, 9, 4, 9, hood),
                                                        C(-4.5, -5, 0.5, 9, 6, 4, hood)])
    m.part("nose", head, (0, -3, -4), (0, 0, 0), [C(-1, -1, -2, 2, 3, 2, face)])
    m.part("beard", head, (0, 0, -4), (0, 0, 0), [C(-3, -1, -1, 6, 5, 1, beard), C(-2, 4, -1, 4, 2, 1, beard)])
    m.part("arm_l", body, (5.5, -11, 0), (0, 0, -0.05), [C(-1.5, -1, -1.5, 3, 11, 3, coat)])
    arm = m.part("arm_r", body, (-5.5, -11, 0), (-0.6, 0, 0.05), [C(-1.5, -1, -1.5, 3, 11, 3, coat)])
    staff = m.part("staff", arm, (0, 10, -0.5), (0.6, 0, 0), [C(-0.5, -18, -0.5, 1, 24, 1, pole), C(-0.5, -18, -4, 1, 1, 4, pole)])
    lamp = m.part("lantern", staff, (0, -17, -3.5), (0, 0, 0), [C(-1.5, 0, -1.5, 3, 1, 3, brass), C(-1, 1, -1, 2, 3, 2, glow),
                                                               C(-1.5, 4, -1.5, 3, 1, 3, brass)])
    return m


def ash_figure():
    """A person made of ash, crumbling: one arm broken off at the elbow, the head bowed, gaps in the chest."""
    m = Model("ash_figure")
    def cracks(px, x0, y0, w, h, rnd):
        r = random.Random(w * 13 + h * 7)
        for _ in range(max(2, w * h // 14)):
            x, y = x0 + r.randint(0, w - 1), y0 + r.randint(0, h - 1)
            px[x, y] = (255, 120, 40, 255) if r.random() < 0.25 else (34, 32, 32, 255)
    ash = P((112, 108, 104, 255), 14, front=cracks, back=cracks, left=cracks, right=cracks)
    dark = P((80, 76, 74, 255), 12, front=cracks)
    face = P((104, 100, 98, 255), 10, front=combine(eyes((28, 24, 24, 255), 0.45, 0.22, 1), mouth((40, 34, 32, 255), (60, 54, 50, 255), 0.75, 0.4)))
    m.part("leg_r", "root", (-2, 12, 0), (0, 0, 0), [C(-2, 0, -2, 4, 12, 4, ash)])
    m.part("leg_l", "root", (2, 12, 0), (0, 0, 0), [C(-2, 0, -2, 4, 9, 4, dark), C(-1.5, 9, -1.5, 3, 3, 3, ash)])
    body = m.part("body", "root", (0, 12, 0), (0, 0, 0), [C(-4, -12, -2, 8, 7, 4, ash), C(-4, -5, -2, 3, 5, 4, ash),
                                                         C(1, -5, -2, 3, 5, 4, dark)])
    m.part("head", body, (0, -12, 0), (0.35, 0, 0.1), [C(-4, -8, -4, 8, 8, 8, face)])
    m.part("arm_r", body, (-5, -11, 0), (0.1, 0, 0.08), [C(-2, -1, -2, 4, 12, 4, ash)])
    m.part("arm_l", body, (5, -11, 0), (-0.2, 0, -0.15), [C(-2, -1, -2, 4, 6, 4, dark)])
    m.part("crumbs", "root", (3, 24, -2), (0, 0.4, 0), [C(-1, -1, -1, 2, 1, 2, dark), C(1, -1, 1, 1, 1, 1, ash)])
    return m


def weeper():
    """The Weeper: a tall, thin woman under a long veil, bent forward, arms too long; tears that glow run down the veil."""
    m = Model("weeper")
    def tears(px, x0, y0, w, h, rnd):
        for x in range(x0 + 1, x0 + w - 1, 2):
            for y in range(y0 + h // 3, y0 + h):
                if (x * 7 + y) % 5 == 0:
                    px[x, y] = (150, 220, 255, 255)
    veil = P((58, 56, 70, 255), 8, front=tears)
    dress = P((40, 38, 50, 255), 8)
    skin = P((176, 170, 180, 255), 8)
    shard = P((90, 70, 130, 255), 10)
    m.part("dress", "root", (0, 10, 0), (0, 0, 0), [C(-4, 0, -3, 8, 14, 6, dress)])
    body = m.part("body", "root", (0, 10, 0), (0.3, 0, 0), [C(-3.5, -11, -2, 7, 11, 4, dress)])
    m.part("head", body, (0, -11, -1), (0.35, 0, 0), [C(-3.5, -8, -3.5, 7, 8, 7, skin), C(-4.5, -9, -4.5, 9, 13, 9, veil)])
    m.part("arm_r", body, (-4.5, -10, 0), (-0.4, 0, 0.1), [C(-1, 0, -1, 2, 19, 2, skin)])
    arm = m.part("arm_l", body, (4.5, -10, 0), (-0.4, 0, -0.1), [C(-1, 0, -1, 2, 19, 2, skin)])
    m.part("shard", arm, (0, 19, 0), (0, 0.6, 0), [C(-1, 0, -1, 2, 4, 2, shard)])
    return m


def bellringer():
    """The Bellringer of the shadow villages: a hunched figure in a sackcloth hood, a bronze hand bell bigger than its head."""
    m = Model("bellringer")
    sack = P((84, 72, 58, 255), 12, front=stripes((64, 54, 44, 255), 4, True))
    hood = P((70, 60, 48, 255), 10, front=eyes((255, 200, 90, 255), 0.55, 0.22, 1, glow=(150, 90, 30, 255)))
    rope = P((120, 96, 60, 255), 8)
    bronze = P((176, 128, 60, 255), 12)
    m.part("legs", "root", (0, 14, 0), (0, 0, 0), [C(-4, 0, -3, 8, 10, 6, sack)])
    body = m.part("body", "root", (0, 14, 0), (0.45, 0, 0), [C(-4.5, -11, -3, 9, 11, 6, sack)])
    m.part("head", body, (0, -11, -1), (0.2, 0, 0), [C(-4, -8, -5, 8, 8, 8, hood)])
    m.part("arm_l", body, (5.5, -10, 0), (-0.2, 0, -0.1), [C(-1.5, -1, -1.5, 3, 12, 3, sack)])
    arm = m.part("arm_r", body, (-5.5, -10, 0), (-1.2, 0, 0.1), [C(-1.5, -1, -1.5, 3, 12, 3, sack)])
    m.part("bell", arm, (0, 12, 0), (1.2, 0, 0), [C(-0.5, 0, -0.5, 1, 3, 1, rope), C(-3, 3, -3, 6, 2, 6, bronze), C(-4, 5, -4, 8, 4, 8, bronze)])
    return m


MODELS = [crawler, blind, light_eater, shade, statue, keeper, watcher, mimic_queen, shepherd, architect,
          primordial_shadow, shadow_villager, lurker, mimicker, lamp_keeper, ash_figure, weeper, bellringer]


# ------------------------------------------------------------------ UV packing + painting

def pack(model):
    """Shelf-packs every cube's UV footprint. Returns texture (width, height)."""
    cubes = [c for p in model.parts for c in p.cubes]
    width = 64
    while True:
        x = y = row = 0
        ok = True
        for c in sorted(cubes, key=lambda c: -(math.ceil(c.d) + math.ceil(c.h))):
            fw = 2 * (math.ceil(c.w) + math.ceil(c.d))
            fh = math.ceil(c.d) + math.ceil(c.h)
            if fw > width:
                ok = False
                break
            if x + fw > width:
                x, y, row = 0, y + row, 0
            c.u, c.v = x, y
            x += fw
            row = max(row, fh)
        if ok:
            height = 1 << max(5, math.ceil(math.log2(max(1, y + row))))
            if height <= width * 2:
                return width, height
        width *= 2


def shade_color(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


def paint(model, size):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(model.name)
    for p in model.parts:
        for c in p.cubes:
            w, h, d = math.ceil(c.w), math.ceil(c.h), math.ceil(c.d)
            u, v = c.u, c.v
            faces = {
                "up": (u + d, v, w, d, 1.12), "down": (u + d + w, v, w, d, 0.75),
                "right": (u, v + d, d, h, 0.9), "front": (u + d, v + d, w, h, 1.0),
                "left": (u + d + w, v + d, d, h, 0.9), "back": (u + 2 * d + w, v + d, w, h, 0.85),
            }
            for fname, (x0, y0, fw, fh, k) in faces.items():
                for x in range(x0, x0 + fw):
                    for y in range(y0, y0 + fh):
                        n = rnd.randint(-c.paint.noise, c.paint.noise)
                        # a soft vertical gradient: darker towards the bottom of side faces
                        g = 1.0 - (0.18 * (y - y0) / max(1, fh) if fname not in ("up", "down") else 0)
                        base = shade_color(c.paint.base, k * g)
                        px[x, y] = tuple(max(0, min(255, ch + n)) for ch in base[:3]) + (base[3],)
                deco = c.paint.faces.get(fname)
                if deco:
                    deco(px, x0, y0, fw, fh, rnd)
    return img


# ------------------------------------------------------------------ Java

def jf(v):
    s = f"{float(v):.4f}".rstrip("0").rstrip(".")
    if "." not in s:
        s += ".0"
    return s + "F"


def java_for(model, size):
    lines = [f"    /** {model.name}: {sum(len(p.cubes) for p in model.parts)} cubes, texture {size[0]}x{size[1]}. */",
             f"    public static LayerDefinition {camel(model.name)}() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    for p in model.parts:
        parent = "root" if p.parent == "root" else var(p.parent)
        cubes = "CubeListBuilder.create()"
        for c in p.cubes:
            cubes += f".texOffs({c.u}, {c.v}).addBox({jf(c.x)}, {jf(c.y)}, {jf(c.z)}, {jf(math.ceil(c.w))}, {jf(math.ceil(c.h))}, {jf(math.ceil(c.d))})"
        px, py, pz = p.pivot
        rx, ry, rz = p.rot
        lines.append(f"        PartDefinition {var(p.name)} = {parent}.addOrReplaceChild(\"{p.name}\", {cubes},")
        lines.append(f"                PartPose.offsetAndRotation({jf(px)}, {jf(py)}, {jf(pz)}, {jf(rx)}, {jf(ry)}, {jf(rz)}));")
    lines.append(f"        return LayerDefinition.create(mesh, {size[0]}, {size[1]});")
    lines.append("    }")
    return "\n".join(lines)


def camel(s):
    parts = s.split("_")
    return parts[0] + "".join(x.capitalize() for x in parts[1:])


def var(name):
    return "p_" + name


def main():
    blocks = []
    names = []
    for factory in MODELS:
        m = factory()
        size = pack(m)
        img = paint(m, size)
        os.makedirs(TEX, exist_ok=True)
        img.save(os.path.join(TEX, m.name + ".png"))
        blocks.append(java_for(m, size))
        names.append(m.name)
    src = ["package dev.umbra.client.model;", "",
           "import net.minecraft.client.model.geom.PartPose;",
           "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
           "import net.minecraft.client.model.geom.builders.LayerDefinition;",
           "import net.minecraft.client.model.geom.builders.MeshDefinition;",
           "import net.minecraft.client.model.geom.builders.PartDefinition;", "",
           "/** GENERATED by tools/models.py, do not edit by hand. Geometry of Umbra's creatures. */",
           "@SuppressWarnings(\"unused\")",
           "public final class GeneratedModels {",
           "    private GeneratedModels() {}", ""]
    src.append("\n\n".join(blocks))
    src.append("}")
    with open(JAVA, "w") as f:
        f.write("\n".join(src) + "\n")
    print("models:", ", ".join(names))


if __name__ == "__main__":
    main()
