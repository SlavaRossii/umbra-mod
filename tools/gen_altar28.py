#!/usr/bin/env python3
"""2.8: the sacrificial altar (Жертвенник) and the four seals in 3D.

The altar: a plinth of dusk-stone bricks, a step, a body with an arched niche on each side and a rune burning in it,
four fluted pillars at the corners crowned with horns, and a table with a bronze rim and a summoning circle in its
dish. Three looks, by its block state glow=0..2: asleep (the runes and the circle dark), a seal laid on it (they
smoulder), the rite under way (they blaze, at full brightness).

The seals: each its own little object, built of boxes like the 3D weapons: the seal of watching is a medallion with an
open eye on both faces; the seal of mimicry a little chest with gold corners, a crown on its lid and an eye in its lock;
the seal of herding a bronze bell on a shepherd's crook with soul fire in its mouth; the seal of reflection a hand
mirror. In the inventory they stay flat pictures; everywhere else (in the hand, on the ground, on the altar) 3D.

Writes textures/block/altar_*.png, textures/item/mat_*.png (the new materials), models/block/summoning_altar_{0,1,2}.json,
blockstates/summoning_altar.json, items/summoning_altar.json, models/item/seal_of_*_3d.json and items/seal_of_*.json.
Run from the repo root: python3 tools/gen_altar28.py [--preview out.png]"""
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
import gen_blocks23 as g  # noqa: E402

A = 'src/main/resources/assets/umbra/'
TB, TI = A + 'textures/block/', A + 'textures/item/'
MB, MI = A + 'models/block/', A + 'models/item/'
BS, ITEMS = A + 'blockstates/', A + 'items/'

STONE = (58, 48, 74)
STONE_D = (30, 24, 42)
NICHE = (18, 13, 26)
BRONZE = (118, 88, 52)
HORN = (92, 80, 86)
# the glow of the runes, the circle and the horn tips: asleep, smouldering, blazing
GLOW = [(66, 50, 92), (168, 104, 236), (236, 196, 255)]
GLOW_D = [(46, 36, 64), (120, 70, 180), (200, 150, 255)]
LIGHT = [0, 7, 15]


# ------------------------------------------------------------------ textures
def textures():
    g.tex('altar_stone', lambda x, y, r: g.bricks(x, y, r, base=STONE, dark=STONE_D))

    def base_side(x, y, r):
        if y in (5, 6):                                   # a carved groove round the plinth
            return g.noise((STONE_D[0] - 6, STONE_D[1] - 6, STONE_D[2] - 4), r, 2)
        if y < 5:
            return g.bricks(x, y, r, base=STONE, dark=STONE_D)
        c = g.bevel(x % 8, y - 7, STONE, size=9, k=16)    # big blocks below the groove
        return g.noise(c if x % 8 != 7 else STONE_D, r, 5)
    g.tex('altar_base_side', base_side)

    def pillar(x, y, r):
        flute = x % 4                                     # fluted: grooves running up
        c = g.bevel(flute, y, STONE, size=4, k=18)
        if flute == 3:
            c = STONE_D
        if y in (0, 15):
            c = [v + 14 for v in STONE]
        return g.noise(c, r, 4)
    g.tex('altar_pillar', pillar)

    def body(x, y, r):
        # an arched niche: the rune burns in it (a separate glowing plane in front)
        inside = 3 <= x <= 12 and 4 <= y <= 14 and not (y == 4 and x in (3, 12)) and not (y == 5 and x in (3, 12) and False)
        if y < 4 and 3 <= x <= 12:
            arch = abs(x - 7.5) ** 2 / 20.0 + (3.5 - y)
            inside = arch < 2.6
        if inside:
            return g.noise(NICHE, r, 3)
        edge = (2 <= x <= 13 and 1 <= y <= 15) and not (3 <= x <= 12 and 2 <= y)
        c = g.bevel(x, y, STONE, k=20)
        if edge:
            c = [v + 10 for v in c]
        return g.noise(c, r, 5)
    g.tex('altar_body', body)

    def table_side(x, y, r):
        c = g.bevel(x, y, STONE, k=18)
        if y % 8 in (3, 4):
            c = STONE_D if x % 4 != 1 else [v + 20 for v in STONE]     # a band of little carved squares
        return g.noise(c, r, 4)
    g.tex('altar_table_side', table_side)

    def dish(x, y, r):
        c = g.mix((40, 32, 48), (26, 20, 32), math.hypot(x - 7.5, y - 7.5) / 10)
        if r.random() < 0.06:
            c = (70, 58, 64)                                 # ash
        return g.noise(c, r, 3)
    g.tex('altar_dish', dish)

    def metal(x, y, r):
        c = g.bevel(x, y, BRONZE, k=28)
        if (x % 5 == 2 and y % 5 == 2):
            c = [v + 46 for v in BRONZE]                     # rivets
        return g.noise(c, r, 6)
    g.tex('altar_metal', metal)

    def horn(x, y, r):
        c = g.bevel(x, y % 4, HORN, size=4, k=20)
        if y % 4 == 3:
            c = [v - 26 for v in HORN]                       # ribs
        return g.noise(c, r, 4)
    g.tex('altar_horn', horn)

    for k in range(3):
        col, dark = GLOW[k], GLOW_D[k]

        def rune(x, y, r, col=col, dark=dark):
            cx, cy = 7.5, 8.0
            d = math.hypot(x - cx, (y - cy) * 1.1)
            on = (abs(d - 3.4) < 0.6) or (x in (7, 8) and 2 <= y <= 14) or (abs((x - cx) - (y - 3)) < 0.5 and 1 <= y <= 5) \
                or (abs((cx - x) - (y - 3)) < 0.5 and 1 <= y <= 5) or (y == 12 and 4 <= x <= 11) or (y in (13, 14) and x in (4, 11))
            if not on:
                return None
            return list(col) + [255] if (x + y) % 5 else list(dark) + [255]
        g.tex('altar_rune_%d' % k, rune)

        def circle(x, y, r, col=col, dark=dark):
            d = math.hypot(x - 7.5, y - 7.5)
            on = abs(d - 7.0) < 0.55 or abs(d - 4.6) < 0.5
            a = math.atan2(y - 7.5, x - 7.5)
            spoke = 4.6 < d < 7.0 and min(abs(((a / (math.pi / 4)) % 1) - 0.5), 1) > 0.42
            tick = d < 2.0 and (x in (7, 8) or y in (7, 8))
            if not (on or spoke or tick):
                return None
            return list(col if on or tick else dark) + [255]
        g.tex('altar_circle_%d' % k, circle)

        g.tex('altar_tip_%d' % k, lambda x, y, r, col=col, dark=dark: g.noise(g.mix(dark, col, 1 - y / 15), r, 3))
        g.tex('altar_flame_%d' % k, lambda x, y, r, col=col: list(g.mix((255, 255, 255), col, y / 15)) + [255])
    g.tex('altar_wax', lambda x, y, r: g.noise(g.bevel(x % 4, y, (70, 58, 76), size=4, k=14) if y > 1 else (36, 30, 40), r, 3))


# the new materials of the seals (the rest are the 3D weapons' own)
MATS = {
    "seal_stone": ((60, 52, 78), 18), "silver": ((178, 184, 198), 22), "mirror": ((196, 236, 250), 10), "soul": ((90, 230, 230), 10),
    "iris": ((206, 44, 56), 10), "eye_green": ((176, 255, 90), 8), "ink": ((14, 10, 16), 4), "gold": ((214, 172, 64), 22),
    "bronze": ((140, 104, 56), 20), "chest": ((112, 76, 44), 14), "chest_lid": ((126, 88, 52), 14), "wool": ((226, 224, 218), 8),
}


def materials():
    for name, (base, k) in MATS.items():
        def fn(x, y, r, base=base, k=k, name=name):
            c = g.bevel(x, y, base, k=k)
            if name in ("chest", "chest_lid") and y % 4 == 3:
                c = [v - 18 for v in c]
            if name == "mirror" and (x - y) % 7 == 0:
                c = [v + 30 for v in c]                      # a glint across the glass
            return g.noise(c, r, 5)
        g.tex('mat_' + name, fn, folder=TI)


# ------------------------------------------------------------------ the altar
def altar_elements(k):
    box = g.box
    L = LIGHT[k]
    side4 = ("north", "south", "east", "west")
    els = [
        box((0, 0, 0), (16, 2, 16), faces={s: "base_side" for s in side4} | {"up": "stone", "down": "stone"}),
        box((1, 2, 1), (15, 4, 15), "stone"),
        box((4, 4, 4), (12, 11, 12), faces={s: "body" for s in side4} | {"up": "stone", "down": "stone"}),
        box((0.5, 11, 0.5), (15.5, 13, 15.5), faces={s: "table" for s in side4} | {"up": "dish", "down": "stone"}),
        box((0.5, 13, 0.5), (15.5, 14, 1.5), "metal"), box((0.5, 13, 14.5), (15.5, 14, 15.5), "metal"),
        box((0.5, 13, 1.5), (1.5, 14, 14.5), "metal"), box((14.5, 13, 1.5), (15.5, 14, 14.5), "metal"),
    ]
    for x, z in ((1.5, 1.5), (12, 1.5), (1.5, 12), (12, 12)):
        # a slim fluted pillar under each corner of the table, with a capital
        els.append(box((x, 4, z), (x + 2.5, 10.5, z + 2.5), faces={s: "pillar" for s in side4} | {"up": "stone", "down": "stone"}))
        els.append(box((x - 0.5, 10, z - 0.5), (x + 3, 11, z + 3), "stone"))
        # a horn rising from the corner of the table and curling outwards, its tip burning
        hx, hz = (x - 0.5, z - 0.5) if x < 8 else (x + 0.5, z - 0.5)
        hz = hz if z < 8 else z + 0.5
        sgn = 1 if x < 8 else -1
        o1 = [hx + 1, 14, hz + 1]
        els.append(box((hx, 14, hz), (hx + 2, 17, hz + 2), "horn", rot={"origin": o1, "axis": "z", "angle": 22.5 * sgn}))
        a1 = math.radians(22.5 * sgn)
        tx, ty = o1[0] - 3 * math.sin(a1), 14 + 3 * math.cos(a1)          # the top of the first piece, turned
        o2 = [round(tx, 3), round(ty, 3), hz + 1]
        els.append(box((tx - 0.75, ty, hz + 0.25), (tx + 0.75, ty + 2.5, hz + 1.75), "horn", rot={"origin": o2, "axis": "z", "angle": 45 * sgn}))
        a2 = math.radians(45 * sgn)
        ux, uy = tx - 2.5 * math.sin(a2), ty + 2.5 * math.cos(a2)
        o3 = [round(ux, 3), round(uy, 3), hz + 1]
        els.append(box((ux - 0.5, uy, hz + 0.5), (ux + 0.5, uy + 2, hz + 1.5), "tip", rot={"origin": o3, "axis": "z", "angle": 22.5 * sgn},
                       light=L, shade=not L))
    # the runes in the four niches: planes just in front of the body
    sides = {"north": ((5, 5, 3.98), (11, 10.5, 3.98)), "south": ((5, 5, 12.02), (11, 10.5, 12.02)),
             "west": ((3.98, 5, 5), (3.98, 10.5, 11)), "east": ((12.02, 5, 5), (12.02, 10.5, 11))}
    for side, (frm, to) in sides.items():
        e = box(frm, to, faces={side: "rune"}, light=L, shade=not L)
        e["faces"][side]["uv"] = [3, 3, 13, 13]
        els.append(e)
    # candles on the step, two to a side, flanking the niche: lit while a seal lies on the altar
    for cx, cz in ((5, 2), (10, 2), (5, 13), (10, 13), (2, 5), (2, 10), (13, 5), (13, 10)):
        h = 2.5 if (cx + cz) % 2 else 1.8
        els.append(box((cx, 4, cz), (cx + 1, 4 + h, cz + 1), "wax"))
        if k:
            els.append(box((cx + 0.25, 4 + h, cz + 0.25), (cx + 0.75, 5.2 + h, cz + 0.75), "flame", light=L, shade=False))
    e = box((1.5, 13.02, 1.5), (14.5, 13.02, 14.5), faces={"up": "circle"}, light=L, shade=not L)
    e["faces"]["up"]["uv"] = [1.5, 1.5, 14.5, 14.5]
    els.append(e)
    return els


def altar():
    for k in range(3):
        g.model(MB + 'summoning_altar_%d.json' % k, "minecraft:block/block", {
            "particle": "umbra:block/altar_stone", "stone": "umbra:block/altar_stone", "base_side": "umbra:block/altar_base_side",
            "body": "umbra:block/altar_body", "table": "umbra:block/altar_table_side", "dish": "umbra:block/altar_dish",
            "metal": "umbra:block/altar_metal", "pillar": "umbra:block/altar_pillar", "horn": "umbra:block/altar_horn",
            "tip": "umbra:block/altar_tip_%d" % k, "rune": "umbra:block/altar_rune_%d" % k, "circle": "umbra:block/altar_circle_%d" % k,
            "wax": "umbra:block/altar_wax", "flame": "umbra:block/altar_flame_%d" % max(1, k)},
            altar_elements(k), {"render_type": "minecraft:cutout", "ambientocclusion": False})
    json.dump({"variants": {"glow=%d" % k: {"model": "umbra:block/summoning_altar_%d" % k} for k in range(3)}},
              open(BS + 'summoning_altar.json', 'w'), indent=2)
    json.dump({"model": {"type": "minecraft:model", "model": "umbra:block/summoning_altar_0"}}, open(ITEMS + 'summoning_altar.json', 'w'), indent=2)


# ------------------------------------------------------------------ the seals
class S:
    def __init__(self, name):
        self.name, self.els, self.used = name, [], set()

    def box(self, frm, to, mat, light=0, rot=None):
        self.els.append(g.box(frm, to, mat, rot=rot, light=light, shade=not light))
        self.used.add(mat)
        return self


def watching():
    s = S("seal_of_watching")
    for frm, to in (((5, 2.5, 7.4), (11, 13.5, 8.6)), ((3.5, 4, 7.4), (12.5, 12, 8.6)), ((4, 3, 7.4), (12, 13, 8.6))):
        s.box(frm, to, "seal_stone")
    s.box((4.5, 12.5, 7.2), (11.5, 13.2, 8.8), "bronze").box((4.5, 2.8, 7.2), (11.5, 3.5, 8.8), "bronze")   # bronze bands
    s.box((7.2, 13.5, 7.6), (8.8, 15.2, 8.4), "bronze")                                                   # the loop
    for z0, z1, zi0, zi1, zp0, zp1 in ((7.15, 7.4, 6.95, 7.15, 6.85, 6.95), (8.6, 8.85, 8.85, 9.05, 9.05, 9.15)):
        s.box((5, 6.3, z0), (11, 9.7, z1), "bone")                         # the white of the eye
        s.box((5.8, 5.6, z0), (10.2, 6.3, z1), "bone").box((5.8, 9.7, z0), (10.2, 10.4, z1), "bone")
        s.box((6.8, 6.4, zi0), (9.2, 9.6, zi1), "iris", light=8)           # the iris burns
        s.box((7.5, 7.1, zp0), (8.5, 8.9, zp1), "ink")                     # the pupil
    for x in (5.5, 7.0, 8.5, 10.0):                                        # lashes on both faces
        s.box((x, 10.4, 7.25), (x + 0.5, 11.6, 7.4), "ink").box((x, 10.4, 8.6), (x + 0.5, 11.6, 8.75), "ink")
    return s


def mimicry():
    s = S("seal_of_mimicry")
    s.box((4, 2.5, 5), (12, 7.5, 11), "chest").box((4, 7.5, 5), (12, 10, 11), "chest_lid")
    for x in (3.7, 11.3):
        for z in (4.7, 10.3):
            s.box((x, 2.3, z), (x + 1, 3.5, z + 1), "gold").box((x, 8.8, z), (x + 1, 10.2, z + 1), "gold")
    for z0, z1, ze0, ze1 in ((4.6, 5, 4.4, 4.6), (11, 11.4, 11.4, 11.6)):
        s.box((6.8, 5.8, z0), (9.2, 8.6, z1), "gold")                      # the lock plate, on both sides
        s.box((7.5, 6.6, ze0), (8.5, 7.8, ze1), "eye_green", light=10)     # and the eye in it
    s.box((5.5, 10, 6.5), (10.5, 11, 9.5), "gold")                         # the crown
    for x in (5.5, 9.5):
        for z in (6.5, 8.5):
            s.box((x, 11, z), (x + 1, 12.6, z + 1), "gold")
    s.box((7.5, 11, 7.5), (8.5, 13, 8.5), "gold")
    return s


def herding():
    s = S("seal_of_herding")
    s.box((4, 2.5, 4), (12, 4, 12), "bronze")                              # the lip of the bell
    s.box((5, 4, 5), (11, 9.5, 11), "bronze").box((5.6, 9.5, 5.6), (10.4, 11, 10.4), "bronze")
    s.box((4.8, 6.2, 4.8), (11.2, 7, 11.2), "gold")                        # a band
    s.box((5, 2.6, 5), (11, 3, 11), "soul", light=12)                      # soul fire in its mouth
    s.box((7.5, 11, 7.5), (8.5, 15, 8.5), "wood_dark")                     # the crook
    s.box((8.5, 14.2, 7.5), (10.6, 15.2, 8.5), "wood_dark").box((9.6, 12.4, 7.5), (10.6, 14.2, 8.5), "wood_dark")
    return s


def reflection():
    s = S("seal_of_reflection")
    s.box((4.5, 6.5, 7.4), (11.5, 14.5, 8.6), "silver").box((5.5, 5.5, 7.4), (10.5, 15.5, 8.6), "silver")
    for z0, z1 in ((7.2, 7.4), (8.6, 8.8)):
        s.box((5.4, 7, z0), (10.6, 14, z1), "mirror", light=7).box((6.2, 6.4, z0), (9.8, 14.6, z1), "mirror", light=7)
    s.box((7.3, 1.2, 7.5), (8.7, 5.5, 8.5), "wood_dark")                   # the handle
    s.box((6.9, 0.2, 7.2), (9.1, 1.2, 8.8), "silver").box((6.9, 4.8, 7.2), (9.1, 5.6, 8.8), "silver")
    return s


SEALS = [watching, mimicry, herding, reflection]
# held like a small thing in the fingers (the flat-item transforms, a little larger: they are thick)
DISPLAY = {
    # upright in the fist (the arm hangs at about -18 degrees: turned 72 more, it stands up), its face forwards
    "thirdperson_righthand": {"rotation": [72, 0, 0], "translation": [0, 2.5, 0.5], "scale": [0.42, 0.42, 0.42]},
    "thirdperson_lefthand": {"rotation": [72, 0, 0], "translation": [0, 2.5, 0.5], "scale": [0.42, 0.42, 0.42]},
    # held up before you, its face to you, a little to the right and below the middle of the view
    "firstperson_righthand": {"rotation": [10, -15, 0], "translation": [-1.1, 4.3, -2.9], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [10, -15, 0], "translation": [-1.1, 4.3, -2.9], "scale": [0.5, 0.5, 0.5]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def seals():
    built = []
    for f in SEALS:
        s = f()
        tex = {m: "umbra:item/mat_" + m for m in sorted(s.used)}
        tex["particle"] = "umbra:item/" + s.name
        g.model(MI + s.name + '_3d.json', "minecraft:item/generated" if False else "minecraft:block/block", tex, s.els,
                {"display": DISPLAY, "gui_light": "front"})
        json.dump({"model": {"type": "minecraft:select", "property": "minecraft:display_context",
                             "cases": [{"when": ["gui"], "model": {"type": "minecraft:model", "model": "umbra:item/" + s.name}}],
                             "fallback": {"type": "minecraft:model", "model": "umbra:item/" + s.name + "_3d"}}},
                  open(ITEMS + s.name + '.json', 'w'), indent=2)
        built.append(s)
    return built


# ------------------------------------------------------------------ preview: a small painter's renderer that honours uvs
def tex_of(ref):
    ns, path = ref.split(':')
    p = 'src/main/resources/assets/%s/textures/%s.png' % (ns, path)
    if os.path.exists(p):
        return Image.open(p).convert('RGBA')
    return Image.new('RGBA', (16, 16), (255, 0, 255, 255))


def draw(model, yaw=35, pitch=28, size=360, zoom=9.0, bg=(24, 22, 30)):
    texs = model["textures"]

    def res(t):
        while t.startswith('#'):
            t = texs[t[1:]]
        return t
    img = Image.new('RGB', (size, size), bg)
    d = ImageDraw.Draw(img)
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def view(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp + z * sp, -y * sp + z * cp
        return (size / 2 + x * zoom, size * 0.56 - y * zoom), z

    def rot(p, r):
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
    quads = []
    for e in model["elements"]:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        shade = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
        corners = {"up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
                   "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
                   "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
                   "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
                   "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
                   "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)]}
        auto = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
                "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
                "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
        for side, f in e["faces"].items():
            im = tex_of(res(f["texture"]))
            uv = f.get("uv", auto[side])
            pts = [view(rot(c, e.get("rotation"))) for c in corners[side]]
            depth = sum(p[1] for p in pts) / 4
            k = 1.0 if e.get("light_emission") else shade[side]
            quads.append((depth, [(p[0][0], p[0][1], p[1]) for p in pts], im, uv, k))
    # painter's algorithm on small pieces of each face (big faces sorted whole would cover what stands on them)
    pieces = []
    for _, (a, b, c, dd), im, uv, k in quads:
        n = 8
        for j in range(n):
            for i in range(n):
                tu = uv[0] + (uv[2] - uv[0]) * (i + 0.5) / n
                tv = uv[1] + (uv[3] - uv[1]) * (j + 0.5) / n
                col = im.getpixel((min(im.width - 1, max(0, int(tu * im.width / 16))), min(im.height - 1, max(0, int(tv * im.height / 16)))))
                if col[3] < 10:
                    continue
                pieces.append((i, j, n, a, b, c, dd, col, k))
    pieces_d = []
    for i, j, n, a, b, c, dd, col, k in pieces:
        def lerp(u, v, a=a, b=b, c=c, dd=dd):
            top = (a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u, a[2] + (b[2] - a[2]) * u)
            bot = (dd[0] + (c[0] - dd[0]) * u, dd[1] + (c[1] - dd[1]) * u, dd[2] + (c[2] - dd[2]) * u)
            return (top[0] + (bot[0] - top[0]) * v, top[1] + (bot[1] - top[1]) * v, top[2] + (bot[2] - top[2]) * v)
        pts = [lerp(i / n, j / n), lerp((i + 1) / n, j / n), lerp((i + 1) / n, (j + 1) / n), lerp(i / n, (j + 1) / n)]
        pieces_d.append((sum(p[2] for p in pts) / 4, [(p[0], p[1]) for p in pts], col, k))
    pieces_d.sort(key=lambda q: -q[0])
    for _, pts, col, k in pieces_d:
        d.polygon(pts, fill=(int(col[0] * k), int(col[1] * k), int(col[2] * k)))
    return img


def preview(out):
    tiles = []
    for k in range(3):
        tiles.append(draw(json.load(open(MB + 'summoning_altar_%d.json' % k))))
    tiles.append(draw(json.load(open(MB + 'summoning_altar_2.json')), yaw=-125, pitch=20))
    for f in SEALS:
        name = f().name
        tiles.append(draw(json.load(open(MI + name + '_3d.json')), yaw=25, pitch=15, zoom=16))
        tiles.append(draw(json.load(open(MI + name + '_3d.json')), yaw=200, pitch=15, zoom=16))
    cols = 4
    sheet = Image.new('RGB', (360 * cols, 360 * ((len(tiles) + cols - 1) // cols)), (24, 22, 30))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % cols) * 360, (i // cols) * 360))
    sheet.save(out)


if __name__ == '__main__':
    textures()
    materials()
    altar()
    seals()
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    print('ok')
