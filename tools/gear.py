"""Gloom gear: shaded item icons and armour layers (vanilla 64x32 humanoid UV layout).
Run from the repo root (gen_textures.py runs it at the end)."""
import math
import os
import random
from PIL import Image

ROOT = "src/main/resources/assets/umbra/textures"

PAL = {
    "o": (16, 10, 24, 255),     # outline
    "d": (44, 34, 64, 255),     # dark plate
    "m": (76, 60, 110, 255),    # mid plate
    "l": (128, 102, 178, 255),  # light edge
    "g": (196, 144, 255, 255),  # rune glow
    "w": (238, 222, 255, 255),  # hot glow
    "h": (58, 40, 30, 255),     # handle
    "H": (96, 70, 48, 255),     # handle highlight
    "s": (30, 22, 40, 255),     # wrap
}


def save(img, path):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def outline(px, n=16):
    filled = {(x, y) for x in range(n) for y in range(n) if px[x, y][3] > 0}
    for (x, y) in filled:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < n and 0 <= q[1] < n and q not in filled and px[q][3] == 0:
                px[q] = PAL["o"]


def icon_from(cells, name):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for (x, y), c in cells.items():
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = PAL[c]
    outline(px)
    save(img, f"item/{name}.png")


def mask(rows, name):
    cells = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                cells[(x, y)] = ch
    icon_from(cells, name)


def sp(x, y):
    """Diagonal coordinates: s runs along the handle (bottom-left -> top-right), p across it."""
    return x - y, x + y - 15


def handle(cells, s_from, s_to):
    for x in range(16):
        for y in range(16):
            s, p = sp(x, y)
            if p == 0 and s_from <= s <= s_to:
                cells[(x, y)] = "H" if (s // 2) % 3 == 0 else "h"
            elif p == 1 and s_from <= s <= s_to and (s // 2) % 3 == 1:
                cells[(x, y)] = "s"


def sword():
    cells = {}
    handle(cells, -13, -7)
    for x in range(16):
        for y in range(16):
            s, p = sp(x, y)
            if -5 <= s <= 13:
                width = 1 if s < 10 else 0
                if abs(p) <= width:
                    cells[(x, y)] = "l" if p < 0 else ("m" if p > 0 else ("g" if s % 4 == 0 else "w" if s >= 11 else "l"))
                elif p == width + 1 and s < 11:
                    cells[(x, y)] = "d"
            if -7 <= s <= -5 and abs(p) <= 4:  # guard
                cells[(x, y)] = "g" if abs(p) == 4 else ("m" if s == -6 else "d")
            if s <= -14 and abs(p) <= 1:  # pommel gem
                cells[(x, y)] = "g" if p == 0 else "m"
    icon_from(cells, "gloom_sword")


def pickaxe():
    cells = {}
    handle(cells, -12, 8)
    for x in range(16):
        for y in range(16):
            s, p = sp(x, y)
            centre = 10 - p * p / 14.0
            if abs(p) <= 9 and centre - 1.2 <= s <= centre + 1.2:
                tip = abs(p) >= 8
                cells[(x, y)] = "w" if tip else ("l" if s > centre else ("g" if abs(p) == 4 else "m"))
            elif abs(p) <= 7 and centre - 2.2 <= s < centre - 1.2:
                cells[(x, y)] = "d"
    icon_from(cells, "gloom_pickaxe")


def axe():
    cells = {}
    handle(cells, -12, 9)
    for x in range(16):
        for y in range(16):
            s, p = sp(x, y)
            if 1 <= s <= 12 and -8 <= p <= -1:
                bulge = abs(s - 6.5)
                if -p <= 8.2 - bulge * 0.75:
                    edge = -p >= 7.2 - bulge * 0.75
                    cells[(x, y)] = "w" if edge and bulge < 2 else ("l" if edge else ("g" if s == 6 and p == -3 else "m" if p < -2 else "d"))
            if 5 <= s <= 8 and 1 <= p <= 2:  # back spike
                cells[(x, y)] = "d" if p == 2 else "m"
    icon_from(cells, "gloom_axe")


def shovel():
    cells = {}
    handle(cells, -12, 4)
    for x in range(16):
        for y in range(16):
            s, p = sp(x, y)
            if 5 <= s <= 14 and abs(p) <= 3:
                if s >= 12 and abs(p) > 14 - s:
                    continue
                cells[(x, y)] = "w" if s >= 13 else ("l" if p < 0 else ("g" if s == 8 and p == 1 else "m" if p == 0 else "d"))
    icon_from(cells, "gloom_shovel")


def armour_icons():
    mask(["................",
          "................",
          "....dmmmmmmd....",
          "...dmllllllmd...",
          "..dmlmmmmmmlmd..",
          "..dmmddddddmmd..",
          "..dmdwgddgwdmd..",
          "..dmddddddddmd..",
          "..dmd......dmd..",
          "..ddd......ddd..",
          "................"], "gloom_helmet")
    mask(["................",
          "..mmm......mmm..",
          ".mllmd....dmllm.",
          ".mlmmddddddmmlm.",
          ".mmdddmllmdddmm.",
          "..ddddlggldddd..",
          "...dddmwwmddd...",
          "...ddddmmdddd...",
          "...dmddddddmd...",
          "...dmmmmmmmmd...",
          "...dddddddddd...",
          "................"], "gloom_chestplate")
    mask(["................",
          "................",
          "...mmmmmmmmmm...",
          "...ddddggdddd...",
          "...dmmmmmmmmd...",
          "...dmmd..dmmd...",
          "...dlmd..dlmd...",
          "...dgmd..dgmd...",
          "...dmmd..dmmd...",
          "...dmmd..dmmd...",
          "...dddd..dddd...",
          "................"], "gloom_leggings")
    mask(["................",
          "................",
          "................",
          "................",
          "................",
          "................",
          "................",
          "...dmmd..dmmd...",
          "...dlmd..dlmd...",
          "...dgmd..dgmd...",
          "..ddmmd..dmmdd..",
          "..dllld..dllld..",
          "..ddddd..ddddd..",
          "................"], "gloom_boots")


# ---------- armour layers ----------

def faces(u, v, w, h, d):
    """UV rectangles of a box: name -> (x0, y0, width, height)."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


class Layer:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def plate(self, rect, rows=None, trim=True):
        x0, y0, w, h = rect
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                ly = y - y0
                if rows is not None and ly not in rows:
                    continue
                v = self.rnd.randint(-5, 5)
                base = PAL["d"] if (x + y) % 5 else PAL["m"]
                c = tuple(max(0, min(255, base[i] + v)) for i in range(3)) + (255,)
                if trim and (x in (x0, x0 + w - 1) or ly in ((rows[0] if rows else 0), (rows[-1] if rows else h - 1))):
                    c = PAL["l"] if ly == (rows[0] if rows else 0) else PAL["o"] if x in (x0, x0 + w - 1) else PAL["m"]
                self.px[x, y] = c

    def put(self, x, y, c):
        self.px[x, y] = PAL[c]


def humanoid_layer():
    L = Layer(55)
    # helmet: head box 8x8x8 at (0, 0)
    head = faces(0, 0, 8, 8, 8)
    for f in head.values():
        L.plate(f)
    fx, fy, _, _ = head["front"]
    for x in range(fx + 1, fx + 7):  # open face below the visor
        for y in range(fy + 5, fy + 8):
            if 2 <= x - fx <= 5:
                L.px[x, y] = (0, 0, 0, 0)
    for x in (fx + 1, fx + 2, fx + 5, fx + 6):  # glowing eye slits
        L.put(x, fy + 3, "g")
    L.put(fx + 1, fy + 3, "w")
    L.put(fx + 6, fy + 3, "w")
    for x in range(fx, fx + 8):
        L.put(x, fy + 2, "l")
    tx, ty, _, _ = head["top"]
    for y in range(ty, ty + 8):  # crest
        L.put(tx + 3, y, "g" if y % 2 else "l")
        L.put(tx + 4, y, "l")
    # hat overlay (32, 0): only the crest, standing out of the helmet
    hat = faces(32, 0, 8, 8, 8)
    hx, hy, _, _ = hat["top"]
    for y in range(hy + 1, hy + 7):
        L.put(hx + 3, y, "m")
        L.put(hx + 4, y, "g" if y % 2 else "m")
    bx, by, _, _ = hat["back"]
    for y in range(by, by + 4):
        L.put(bx + 3, y, "m")
        L.put(bx + 4, y, "m")

    # chestplate: body 8x12x4 at (16, 16)
    body = faces(16, 16, 8, 12, 4)
    for f in body.values():
        L.plate(f)
    cx, cy, _, _ = body["front"]
    eye = {(3, 3): "l", (4, 3): "l", (2, 4): "l", (3, 4): "g", (4, 4): "g", (5, 4): "l", (3, 5): "w", (4, 5): "w",
           (2, 5): "g", (5, 5): "g", (3, 6): "g", (4, 6): "g"}
    for (x, y), c in eye.items():
        L.put(cx + x, cy + y, c)
    for x in range(cx, cx + 8):
        L.put(x, cy + 9, "l")  # belt line
    L.put(cx + 3, cy + 10, "g")
    L.put(cx + 4, cy + 10, "g")
    kx, ky, _, _ = body["back"]
    for y in range(ky + 1, ky + 10):  # spine rune
        L.put(kx + 3, y, "g" if y % 3 == 0 else "m")
        L.put(kx + 4, y, "m")
    # arms 4x12x4 at (40, 16): pauldrons and a rune down the forearm
    arm = faces(40, 16, 4, 12, 4)
    for name, f in arm.items():
        L.plate(f, rows=list(range(0, f[3])) if name in ("top", "bottom") else list(range(0, 9)))
    for name in ("front", "right", "left", "back"):
        x0, y0, w, _ = arm[name]
        for x in range(x0, x0 + w):
            L.put(x, y0, "l")
            L.put(x, y0 + 1, "l")
            L.put(x, y0 + 2, "m")
    ax, ay, _, _ = arm["front"]
    for y in range(ay + 4, ay + 8):
        L.put(ax + 1, y, "g" if y % 2 else "m")
    # boots: legs 4x12x4 at (0, 16), only the lower 5 rows
    leg = faces(0, 16, 4, 12, 4)
    for name, f in leg.items():
        if name in ("top",):
            continue
        L.plate(f, rows=list(range(0, f[3])) if name == "bottom" else list(range(7, 12)))
    lx, ly, _, _ = leg["front"]
    for x in range(lx, lx + 4):
        L.put(x, ly + 7, "l")
        L.put(x, ly + 11, "o")
    L.put(lx + 1, ly + 9, "g")
    L.put(lx + 2, ly + 9, "g")
    save(L.img, "entity/equipment/humanoid/gloom.png")


def leggings_layer():
    L = Layer(56)
    body = faces(16, 16, 8, 12, 4)
    for name, f in body.items():
        if name in ("top",):
            continue
        L.plate(f, rows=list(range(0, f[3])) if name == "bottom" else list(range(8, 12)))
    cx, cy, _, _ = body["front"]
    for x in range(cx, cx + 8):
        L.put(x, cy + 8, "l")
    L.put(cx + 3, cy + 9, "w")
    L.put(cx + 4, cy + 9, "g")
    L.put(cx + 3, cy + 10, "g")
    L.put(cx + 4, cy + 10, "g")
    leg = faces(0, 16, 4, 12, 4)
    for name, f in leg.items():
        if name == "top":
            continue
        L.plate(f, rows=list(range(0, f[3])) if name == "bottom" else list(range(0, 10)))
    lx, ly, _, _ = leg["front"]
    for y in range(ly + 3, ly + 6):  # knee guard
        for x in range(lx, lx + 4):
            L.put(x, y, "m" if y != ly + 4 else "l")
    L.put(lx + 1, ly + 4, "g")
    L.put(lx + 2, ly + 4, "g")
    for name in ("right", "left", "back"):
        x0, y0, w, _ = leg[name]
        L.put(x0 + w // 2, y0 + 6, "g")
    save(L.img, "entity/equipment/humanoid_leggings/gloom.png")


def preview():
    """A 4x preview sheet of all of the above, for checking by eye."""
    names = ["gloom_sword", "gloom_pickaxe", "gloom_axe", "gloom_shovel", "gloom_helmet", "gloom_chestplate", "gloom_leggings", "gloom_boots"]
    sheet = Image.new("RGBA", (8 * 72 + 8, 72 + 8 + 2 * 136), (40, 40, 40, 255))
    for i, n in enumerate(names):
        im = Image.open(os.path.join(ROOT, f"item/{n}.png")).resize((64, 64), Image.NEAREST)
        sheet.paste(im, (8 + i * 72, 8), im)
    for j, layer in enumerate(("humanoid", "humanoid_leggings")):
        im = Image.open(os.path.join(ROOT, f"entity/equipment/{layer}/gloom.png")).resize((256, 128), Image.NEAREST)
        sheet.paste(im, (8 + j * 280, 80), im)
    return sheet


if __name__ == "__main__":
    sword()
    pickaxe()
    axe()
    shovel()
    armour_icons()
    humanoid_layer()
    leggings_layer()
    import sys
    if len(sys.argv) > 1:
        preview().save(sys.argv[1])
    print("gear written")
