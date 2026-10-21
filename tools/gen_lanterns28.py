#!/usr/bin/env python3
"""2.8: the three lanterns in 3D, and in first person held out on the arm.

Each lantern (luminite, Marta's, the Dawn) is built of boxes: a base, a cage of posts, a glowing core, a cap and a
handle. In the inventory the flat picture stays. Everywhere else the 3D lantern; in first person the lantern hangs from
a gloved fist at the end of an arm reaching out ahead of you (a sleeve and a glove: drawn as part of the model, since
the game draws no arm while something is held). The luminite lantern has a lit and an unlit look (it is lit when it
carries the glint component, as LanternItem sets it).

Writes textures/item/mat_*.png (new materials), models/item/<lantern>_3d.json and _fp.json (luminite: also _off_),
items/<lantern>.json.
Run from the repo root: python3 tools/gen_lanterns28.py [--preview out.png]"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
import gen_blocks23 as g  # noqa: E402

A = 'src/main/resources/assets/umbra/'
TI, MI, ITEMS = A + 'textures/item/', A + 'models/item/', A + 'items/'

MATS = {
    "iron_dark": ((64, 66, 78), 22), "glow_blue": ((120, 196, 255), 10), "glow_core": ((214, 246, 255), 6),
    "glow_pale": ((206, 246, 250), 8), "glow_warm": ((255, 206, 128), 8), "glow_sun": ((255, 236, 178), 6),
    "lamp_dark": ((40, 52, 70), 8), "sleeve": ((52, 44, 58), 14), "cuff": ((70, 58, 74), 14), "glove": ((86, 60, 44), 16),
    "brass": ((192, 150, 70), 20), "gold": ((214, 172, 64), 22), "white_gold": ((236, 222, 176), 14),
}


def materials():
    for name, (base, k) in MATS.items():
        def fn(x, y, r, base=base, k=k, name=name):
            c = g.bevel(x, y, base, k=k)
            if name == "sleeve" and (x + y * 3) % 5 == 0:
                c = [v - 10 for v in c]                    # the weave of the cloth
            if name == "glove" and y % 5 == 4:
                c = [v - 16 for v in c]                    # stitches
            if name.startswith("glow") and (x * 7 + y * 3) % 11 == 0:
                c = [v + 12 for v in c]
            return g.noise(c, r, 4)
        g.tex('mat_' + name, fn, folder=TI)


class M:
    def __init__(self):
        self.els, self.used = [], set()

    def box(self, frm, to, mat, light=0, rot=None):
        self.els.append(g.box(frm, to, mat, rot=rot, light=light, shade=not light))
        self.used.add(mat)
        return self


def cage(m, metal, glow, light, x=8.0, y=0.0, z=8.0, tall=8.5):
    """Base, core, four posts, a band, the top rim (centred on x, z, standing on y). Returns the top of the rim."""
    def B(a, b, mat, **kw):
        m.box((x + a[0], y + a[1], z + a[2]), (x + b[0], y + b[1], z + b[2]), mat, **kw)
    B((-3, 0, -3), (3, 1, 3), metal)
    B((-2.6, 1, -2.6), (2.6, 1.5, 2.6), metal)
    B((-2, 1.5, -2), (2, tall, 2), glow, light=light)
    for sx in (-1, 1):
        for sz in (-1, 1):
            B((sx * 2.6 - 0.45, 1.5, sz * 2.6 - 0.45), (sx * 2.6 + 0.45, tall, sz * 2.6 + 0.45), metal)
    for yy in (4.6,):
        B((-2.6, yy, -2.25), (2.6, yy + 0.5, -2.05), metal)
        B((-2.6, yy, 2.05), (2.6, yy + 0.5, 2.25), metal)
        B((-2.25, yy, -2.6), (-2.05, yy + 0.5, 2.6), metal)
        B((2.05, yy, -2.6), (2.25, yy + 0.5, 2.6), metal)
    B((-3, tall, -3), (3, tall + 1, 3), metal)
    return y + tall + 1


def handle(m, metal, x=8.0, y=0.0, z=8.0):
    m.box((x - 1.25, y, z - 0.25), (x - 0.75, y + 1.8, z + 0.25), metal)
    m.box((x + 0.75, y, z - 0.25), (x + 1.25, y + 1.8, z + 0.25), metal)
    m.box((x - 1.25, y + 1.8, z - 0.25), (x + 1.25, y + 2.3, z + 0.25), metal)
    return y + 2.3


def luminite(m, lit=True, x=8.0, y=0.0, z=8.0):
    top = cage(m, "iron_dark", "glow_blue" if lit else "lamp_dark", 14 if lit else 0, x, y, z)
    if lit:
        m.box((x - 1, y + 3, z - 1), (x + 1, y + 7, z + 1), "glow_core", light=15)
    m.box((x - 2, top, z - 2), (x + 2, top + 1, z + 2), "iron_dark")
    m.box((x - 1, top + 1, z - 1), (x + 1, top + 1.7, z + 1), "iron_dark")
    return handle(m, "iron_dark", x, top + 1.7, z)


def marta(m, x=8.0, y=0.0, z=8.0):
    top = cage(m, "brass", "glow_pale", 12, x, y, z, tall=9.0)
    m.box((x - 0.6, y + 3.5, z - 0.6), (x + 0.6, y + 6.5, z + 0.6), "glow_warm", light=15)   # the flame inside
    m.box((x - 2.5, top, z - 2.5), (x + 2.5, top + 0.8, z + 2.5), "brass")
    m.box((x - 1.6, top + 0.8, z - 1.6), (x + 1.6, top + 1.6, z + 1.6), "brass")
    m.box((x - 0.7, top + 1.6, z - 0.7), (x + 0.7, top + 2.4, z + 0.7), "brass")
    return handle(m, "brass", x, top + 2.4, z)


def dawn(m, x=8.0, y=0.0, z=8.0):
    top = cage(m, "gold", "glow_warm", 15, x, y, z)
    m.box((x - 1, y + 3, z - 1), (x + 1, y + 7, z + 1), "glow_sun", light=15)
    m.box((x - 2.2, top, z - 2.2), (x + 2.2, top + 1, z + 2.2), "white_gold")
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):            # little rays round the cap
        cx, cz = x + dx * 3.2, z + dz * 3.2
        m.box((cx - (0.4 if dx == 0 else 0.9), top + 0.2, cz - (0.4 if dz == 0 else 0.9)),
              (cx + (0.4 if dx == 0 else 0.9), top + 0.7, cz + (0.4 if dz == 0 else 0.9)), "glow_sun", light=15)
    m.box((x - 1, top + 1, z - 1), (x + 1, top + 1.8, z + 1), "gold")
    return handle(m, "gold", x, top + 1.8, z)


def arm(m, hand_y, x=8.0, z=8.0):
    """A gloved fist round the handle, a cuff, and a sleeve running back and down towards you."""
    m.box((x - 1.8, hand_y - 0.6, z - 1.4), (x + 1.8, hand_y + 2.6, z + 1.4), "glove")      # the fist
    m.box((x - 1.4, hand_y - 1.0, z - 1.6), (x + 1.4, hand_y - 0.4, z - 0.6), "glove")      # fingers under the handle
    o = [x, hand_y + 1.0, z + 1.4]
    rot = {"origin": o, "axis": "x", "angle": 22.5}
    m.box((x - 2.0, hand_y - 0.6, z + 1.4), (x + 2.0, hand_y + 2.8, z + 3.2), "cuff", rot=rot)
    m.box((x - 1.8, hand_y - 0.4, z + 3.2), (x + 1.8, hand_y + 2.6, min(32.0, z + 27.5)), "sleeve", rot=rot)


LANTERNS = {"luminite_lantern": lambda m, **kw: luminite(m, True, **kw), "luminite_lantern_off": lambda m, **kw: luminite(m, False, **kw),
            "marta_lantern": marta, "dawn_lantern": dawn}

# the lantern alone, held out in the hand (third person), on the ground and in a frame
DISPLAY_3D = {
    # held out on a raised arm (LanternHold: the arm at -1.25 rad): turned 90 degrees more than the arm, it hangs upright,
    # its handle in the fist (see the derivation in docs/ARCHITECTURE.md, "Lanterns held out")
    "thirdperson_righthand": {"rotation": [18, 0, 0], "translation": [0, -4.0, -1.3], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_lefthand": {"rotation": [18, 0, 0], "translation": [0, -4.0, -1.3], "scale": [0.6, 0.6, 0.6]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 10, 0], "scale": [0.8, 0.8, 0.8]},
}
# first person: the arm reaches out from the bottom right; the lantern hangs ahead of you
FP_YAW = 28.0
DISPLAY_FP = {
    "firstperson_righthand": {"rotation": [0, FP_YAW, 0], "translation": [-4.5, 5.5, -1.5], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [0, FP_YAW, 0], "translation": [-4.5, 5.5, -1.5], "scale": [0.5, 0.5, 0.5]},
}


def build(name, with_arm):
    """In first person the lantern stands nearer the front of the box (z 4.5), so the sleeve fits in it (to z 32)."""
    m = M()
    z = 4.5 if with_arm else 8.0
    hand_y = LANTERNS[name](m, x=8.0, y=1.5, z=z)
    if with_arm:
        arm(m, hand_y, z=z)
    return m


def write():
    materials()
    for name in LANTERNS:
        for fp in (False, True):
            m = build(name, fp)
            tex = {k: "umbra:item/mat_" + k for k in sorted(m.used)}
            base = name.replace("_off", "")
            tex["particle"] = "umbra:item/" + base
            g.model(MI + name + ('_fp' if fp else '_3d') + '.json', "minecraft:block/block", tex, m.els,
                    {"display": DISPLAY_FP if fp else DISPLAY_3D, "gui_light": "front"})

    def pick(base):
        return {"type": "minecraft:select", "property": "minecraft:display_context",
                "cases": [{"when": ["gui"], "model": {"type": "minecraft:model", "model": "umbra:item/" + base.replace("_off", "")}},
                          {"when": ["firstperson_righthand", "firstperson_lefthand"], "model": {"type": "minecraft:model", "model": "umbra:item/" + base + "_fp"}}],
                "fallback": {"type": "minecraft:model", "model": "umbra:item/" + base + "_3d"}}
    json.dump({"model": {"type": "minecraft:condition", "property": "minecraft:has_component", "component": "minecraft:enchantment_glint_override",
                         "on_true": pick("luminite_lantern"), "on_false": pick("luminite_lantern_off")}},
              open(ITEMS + 'luminite_lantern.json', 'w'), indent=2)
    for base in ("marta_lantern", "dawn_lantern"):
        json.dump({"model": pick(base)}, open(ITEMS + base + '.json', 'w'), indent=2)


# ------------------------------------------------------------------ preview: the first-person view, as the game builds it
def first_person(model, W=854, H=480, right=True, bg=(96, 120, 150)):
    """The vanilla first-person transform: (+-0.56, -0.52, -0.72), then the display transform (translate, rotate XYZ,
    scale), then -0.5; a 70 degree vertical field of view."""
    disp = model["display"]["firstperson_righthand" if right else "firstperson_lefthand"]
    rx, ry, rz = (math.radians(a) for a in disp["rotation"])
    if not right:
        ry, rz = -ry, -rz
    tx, ty, tz = (v / 16 for v in disp["translation"])
    if not right:
        tx = -tx
    sc = disp["scale"]
    f = (H / 2) / math.tan(math.radians(35))

    def R(p):
        x, y, z = p
        x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
        x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
        y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
        return x, y, z

    def to_view(p):
        x, y, z = p[0] / 16 - 0.5, p[1] / 16 - 0.5, p[2] / 16 - 0.5
        x, y, z = x * sc[0], y * sc[1], z * sc[2]
        x, y, z = R((x, y, z))
        return ((0.56 if right else -0.56) + tx + x, -0.52 + ty + y, -0.72 + tz + z)

    def elrot(p, r):
        if not r:
            return p
        o, a = r["origin"], math.radians(r["angle"])
        x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
        if r["axis"] == "y":
            x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
        elif r["axis"] == "x":
            y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
        else:
            x, y = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
        return (x + o[0], y + o[1], z + o[2])
    img = Image.new('RGB', (W, H), bg)
    d = ImageDraw.Draw(img)
    pieces = []
    for e in model["elements"]:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        mat = next(iter(e["faces"].values()))["texture"][1:]
        col = MATS.get(mat, ((150, 150, 150), 0))[0]
        faces = {"up": ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], 1.0),
                 "down": ([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], 0.5),
                 "north": ([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], 0.8),
                 "south": ([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], 0.8),
                 "west": ([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], 0.65),
                 "east": ([(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], 0.65)}
        for side, (cs, k) in faces.items():
            vs = [to_view(elrot(c, e.get("rotation"))) for c in cs]
            if any(v[2] > -0.05 for v in vs):
                continue
            pts = [(W / 2 + v[0] / -v[2] * f, H / 2 - v[1] / -v[2] * f) for v in vs]
            depth = sum(v[2] for v in vs) / 4
            kk = 1.0 if e.get("light_emission") else k
            pieces.append((depth, pts, tuple(int(c * kk) for c in col)))
    pieces.sort(key=lambda q: q[0])
    for _, pts, col in pieces:
        d.polygon(pts, fill=col)
    d.line([(W / 2 - 6, H / 2), (W / 2 + 6, H / 2)], fill=(255, 255, 255))
    d.line([(W / 2, H / 2 - 6), (W / 2, H / 2 + 6)], fill=(255, 255, 255))
    return img


def preview(out):
    tiles = [first_person(json.load(open(MI + n + '_fp.json'))) for n in ("luminite_lantern", "marta_lantern", "dawn_lantern")]
    tiles.append(first_person(json.load(open(MI + 'luminite_lantern_fp.json')), right=False))
    sheet = Image.new('RGB', (854 * 2, 480 * 2))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 2) * 854, (i // 2) * 480))
    sheet.save(out)


if __name__ == '__main__':
    write()
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    print('ok')
