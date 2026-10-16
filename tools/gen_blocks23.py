#!/usr/bin/env python3
"""2.3 blocks and item models with real volume: the echo altar, the gloom anvil, luminite candles, the 3D aegis and the
pulsing lighthouse lamp. Writes textures (16x16 painted with light from the top left: bevels, gradients, glints),
block/item models built from elements, blockstates and item definitions.
Run from the repo root: python3 tools/gen_blocks23.py   (preview: python3 tools/gen_blocks23.py --preview out.png)
"""
import json
import math
import os
import random
import sys

from PIL import Image

R = 'src/main/resources/assets/umbra/'
TB, TI = R + 'textures/block/', R + 'textures/item/'
MB, MI = R + 'models/block/', R + 'models/item/'
BS, ITEMS = R + 'blockstates/', R + 'items/'


def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def tex(name, fn, size=16, folder=TB, seed=None):
    rnd = random.Random(seed if seed is not None else name)
    im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = im.load()
    for y in range(size):
        for x in range(size):
            c = fn(x, y, rnd)
            if c is not None:
                px[x, y] = tuple(clamp(v) for v in c) if len(c) == 4 else tuple(clamp(v) for v in c) + (255,)
    im.save(folder + name + '.png')
    return im


def bevel(x, y, base, size=16, k=26, edge=1):
    """Light from the top left: brighter rim up and left, darker down and right, a soft gradient across."""
    c = list(base)
    g = (size - 1 - (x + y) / 2) / (size - 1) - 0.5
    c = [v + g * k * 0.8 for v in c]
    if x < edge or y < edge:
        c = [v + k for v in c]
    if x >= size - edge or y >= size - edge:
        c = [v - k for v in c]
    return c


def noise(c, rnd, n=8):
    d = rnd.randint(-n, n)
    return [v + d for v in c[:3]] + list(c[3:])


# ------------------------------------------------------------------ palette
STONE = (46, 34, 60)        # dusk stone
STONE_D = (28, 19, 38)
ECHO = (110, 235, 220)       # echo quartz
ECHO_D = (40, 140, 150)
LUMI = (175, 240, 255)
LUMI_D = (70, 160, 210)
GLOOM = (58, 47, 78)
GLOOM_D = (30, 24, 42)
VIOLET = (170, 90, 230)
WAX = (196, 214, 226)


def bricks(x, y, rnd, base=STONE, dark=STONE_D):
    row = y // 4
    off = 4 if row % 2 else 0
    mortar = y % 4 == 3 or (x + off) % 8 == 7
    if mortar:
        return noise(dark, rnd, 3)
    bx, by = (x + off) % 8, y % 4
    c = bevel(bx, by, base, size=8, k=14)
    return noise(c, rnd, 6)


def write_textures():
    # echo altar
    tex('echo_altar_side', lambda x, y, r: bricks(x, y, r))
    tex('echo_altar_bottom', lambda x, y, r: noise(bevel(x, y, STONE_D, k=10), r, 5))

    def altar_top(x, y, r):
        cx, cy = x - 7.5, y - 7.5
        d = math.hypot(cx, cy)
        c = noise(bevel(x, y, (40, 30, 54), k=18), r, 4)
        if 5.2 < d < 6.6:                      # rune ring
            a = math.atan2(cy, cx)
            if int((a + math.pi) / (math.pi / 6)) % 2 == 0:
                return mix(ECHO, (255, 255, 255), 0.25 if d < 5.9 else 0.0)
            return ECHO_D
        if d < 2.2:
            return mix(ECHO, (255, 255, 255), 0.5 - d / 5)
        return c
    tex('echo_altar_top', altar_top)

    def pillar(x, y, r):
        c = noise(bevel(x, y, (52, 38, 70), k=22), r, 5)
        if x in (7, 8) and 3 <= y <= 12 and y % 3 != 0:
            return ECHO if y % 3 == 1 else ECHO_D
        return c
    tex('echo_altar_pillar', pillar)

    def crystal(x, y, r):
        t = (x + (15 - y)) / 30
        c = mix(ECHO_D, (220, 255, 250), t)
        if (x + y) % 5 == 0:
            c = mix(c, (255, 255, 255), 0.4)
        return noise(list(c), r, 4)
    tex('echo_crystal', crystal)

    # gloom anvil: dark metal, violet seams
    def metal(x, y, r, base=GLOOM):
        c = bevel(x, y, base, k=30)
        if (x * 7 + y * 3) % 23 == 0:
            c = mix(c, VIOLET, 0.6)
        return noise(c, r, 5)
    tex('gloom_anvil_side', metal)

    def anvil_top(x, y, r):
        c = metal(x, y, r, base=(70, 58, 92))
        if 2 <= x <= 13 and y in (7, 8):
            return mix(VIOLET, (230, 190, 255), 0.3 if y == 7 else 0.0)
        return c
    tex('gloom_anvil_top', anvil_top)

    # candles
    def wax(x, y, r):
        c = bevel(x, y, WAX, k=24)
        if y < 2:
            c = mix(c, LUMI, 0.35)
        return noise(c, r, 3)
    tex('luminite_candle', wax)

    def flame(x, y, r):
        d = math.hypot(x - 7.5, (y - 9) * 0.7)
        if d > 7.5:
            return None
        return list(mix((255, 255, 255), LUMI, d / 7.5)) + [255]
    tex('luminite_candle_flame', flame)

    # aegis: rim of gloom metal, a face of luminite
    def aegis_face(x, y, r):
        cx, cy = x - 7.5, y - 7.5
        d = max(abs(cx), abs(cy) * 0.9)
        if d > 6.5:
            return noise(bevel(x, y, GLOOM, k=30), r, 4)
        c = mix(LUMI_D, LUMI, 1 - (x + y) / 30)
        if abs(cx) < 1.1 or abs(cy + 1) < 1.1:
            c = mix(c, (255, 255, 255), 0.55)
        return noise(list(c), r, 4)
    tex('luminite_shield_face', aegis_face, folder=TI)
    tex('luminite_shield_rim', lambda x, y, r: noise(bevel(x, y, GLOOM, k=26), r, 4), folder=TI)

    # lighthouse lamp: a luminite core glowing by level; each level its own animated strip (pulse)
    for lvl in range(5):
        frames = 8
        strip = Image.new('RGBA', (16, 16 * frames))
        for f in range(frames):
            glow = 0.0 if lvl == 0 else (0.35 + 0.15 * lvl) * (0.75 + 0.25 * math.sin(f / frames * 2 * math.pi))
            rnd = random.Random(lvl * 31 + f)
            for y in range(16):
                for x in range(16):
                    cx, cy = x - 7.5, y - 7.5
                    d = math.hypot(cx, cy)
                    if x in (0, 15) or y in (0, 15):
                        c = bevel(x, y, (90, 82, 70), k=30)                 # brass frame
                    elif x in (5, 10) or y in (5, 10):
                        c = (60, 54, 48)                                     # mullions
                    else:
                        dead = mix((40, 50, 60), (70, 80, 90), 1 - d / 10)
                        lit = mix(LUMI, (255, 255, 255), max(0, 1 - d / 6))
                        c = mix(dead, lit, min(1.0, glow * (1.2 - d / 12)))
                    strip.putpixel((x, y + 16 * f), tuple(clamp(v + rnd.randint(-3, 3)) for v in c[:3]) + (255,))
        strip.save(TB + 'lighthouse_lamp_glow_%d.png' % lvl)
        if lvl > 0:
            json.dump({"animation": {"frametime": 4 - min(3, lvl // 2), "interpolate": True}}, open(TB + 'lighthouse_lamp_glow_%d.png.mcmeta' % lvl, 'w'))


# ------------------------------------------------------------------ models

def face(t, uv=None, cull=None, tint=None):
    f = {"texture": "#" + t}
    if uv:
        f["uv"] = uv
    if cull:
        f["cullface"] = cull
    return f


def box(frm, to, tex_all=None, faces=None, rot=None, light=0, shade=True):
    fs = {}
    for side in ("north", "south", "east", "west", "up", "down"):
        t = (faces or {}).get(side, tex_all)
        if t:
            fs[side] = face(t)
    e = {"from": list(frm), "to": list(to), "faces": fs}
    if rot:
        e["rotation"] = rot
    if light:
        e["light_emission"] = light
    if not shade:
        e["shade"] = False
    return e


def model(path, parent, textures, elements, extra=None):
    m = {"parent": parent, "textures": textures, "elements": elements}
    if extra:
        m.update(extra)
    json.dump(m, open(path, 'w'), indent=2)
    return m


ALTAR = None


def write_models():
    global ALTAR
    # echo altar: a plinth, a fluted pillar, a rune table and a crystal crown
    els = [
        box((0, 0, 0), (16, 4, 16), faces={"north": "side", "south": "side", "east": "side", "west": "side", "up": "side", "down": "bottom"}),
        box((3, 4, 3), (13, 10, 13), "pillar"),
        box((1, 10, 1), (15, 13, 15), faces={"north": "side", "south": "side", "east": "side", "west": "side", "up": "top", "down": "bottom"}),
        box((6.5, 13, 6.5), (9.5, 20, 9.5), "crystal", rot={"origin": [8, 13, 8], "axis": "y", "angle": 45}, light=10),
        box((4, 13, 6), (6, 17, 8), "crystal", rot={"origin": [6, 13, 7], "axis": "z", "angle": 22.5}, light=8),
        box((10, 13, 8), (12, 17, 10), "crystal", rot={"origin": [10, 13, 9], "axis": "z", "angle": -22.5}, light=8),
        box((7, 13, 10), (9, 16, 12), "crystal", rot={"origin": [8, 13, 5.5], "axis": "x", "angle": -22.5}, light=8),
    ]
    ALTAR = model(MB + 'echo_altar.json', "minecraft:block/block",
                  {"particle": "umbra:block/echo_altar_side", "side": "umbra:block/echo_altar_side", "bottom": "umbra:block/echo_altar_bottom",
                   "top": "umbra:block/echo_altar_top", "pillar": "umbra:block/echo_altar_pillar", "crystal": "umbra:block/echo_crystal"}, els)
    json.dump({"variants": {"": {"model": "umbra:block/echo_altar"}}}, open(BS + 'echo_altar.json', 'w'), indent=2)

    # gloom anvil (faces north by default: the long side runs east-west)
    els = [
        box((2, 0, 2), (14, 4, 14), "side"),
        box((4, 4, 3), (12, 5, 13), "side"),
        box((6, 5, 4), (10, 10, 12), "side"),
        box((3, 10, 0), (13, 16, 16), faces={"north": "side", "south": "side", "east": "side", "west": "side", "up": "top", "down": "side"}),
        box((6.5, 16, 7), (9.5, 17, 9), "side", light=4),
    ]
    model(MB + 'gloom_anvil.json', "minecraft:block/block",
          {"particle": "umbra:block/gloom_anvil_side", "side": "umbra:block/gloom_anvil_side", "top": "umbra:block/gloom_anvil_top"}, els)
    json.dump({"variants": {"facing=north": {"model": "umbra:block/gloom_anvil", "y": 90},
                            "facing=east": {"model": "umbra:block/gloom_anvil", "y": 180},
                            "facing=south": {"model": "umbra:block/gloom_anvil", "y": 270},
                            "facing=west": {"model": "umbra:block/gloom_anvil"}}}, open(BS + 'gloom_anvil.json', 'w'), indent=2)

    # candles: 1..4
    spots = [(7, 7, 6), (4, 9, 5), (10, 5, 7), (9, 10, 4)]
    for n in range(1, 5):
        els = []
        for i in range(n):
            x, z, h = spots[i]
            els.append(box((x, 0, z), (x + 2, h, z + 2), "wax"))
            els.append(box((x + 0.5, h, z + 0.5), (x + 1.5, h + 2, z + 1.5), "flame", light=15, shade=False))
        model(MB + 'luminite_candle_%d.json' % n, "minecraft:block/block",
              {"particle": "umbra:block/luminite_candle", "wax": "umbra:block/luminite_candle", "flame": "umbra:block/luminite_candle_flame"}, els,
              {"ambientocclusion": False})
    json.dump({"variants": {"candles=%d" % n: {"model": "umbra:block/luminite_candle_%d" % n} for n in range(1, 5)}}, open(BS + 'luminite_candle.json', 'w'), indent=2)

    for name, mdl in (("echo_altar", "umbra:block/echo_altar"), ("gloom_anvil", "umbra:block/gloom_anvil")):
        json.dump({"model": {"type": "minecraft:model", "model": mdl}}, open(ITEMS + name + '.json', 'w'), indent=2)
    # the candle item is a flat picture (like vanilla candles)
    tex('luminite_candle_item', lambda x, y, r: (None if not (6 <= x <= 9 and 3 <= y <= 15) else
                                                (list(mix((255, 255, 255), LUMI, (y - 3) / 3)) + [255] if y < 6 and 7 <= x <= 8 else
                                                 None if y < 6 else bevel(x - 6, y, WAX, size=4, k=30))), folder=TI)
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "umbra:item/luminite_candle_item"}}, open(MI + 'luminite_candle.json', 'w'), indent=2)
    json.dump({"model": {"type": "minecraft:model", "model": "umbra:item/luminite_candle"}}, open(ITEMS + 'luminite_candle.json', 'w'), indent=2)

    # the aegis in 3D: a kite shield, a rim, a boss and a strap; the flat icon stays for the inventory
    els = [
        box((3, 1, 7), (13, 14, 8), faces={"north": "face", "south": "rim", "east": "rim", "west": "rim", "up": "rim", "down": "rim"}),
        box((2, 4, 7.5), (3, 13, 8.5), "rim"), box((13, 4, 7.5), (14, 13, 8.5), "rim"),
        box((3, 14, 7.5), (13, 15, 8.5), "rim"), box((5, 0, 7.5), (11, 1, 8.5), "rim"),
        box((7, 7, 6), (9, 9, 7), "face", light=12),
        box((7, 3, 8), (9, 12, 9), "rim"),
    ]
    display = {
        "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]},
        "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [10, 6, 12], "scale": [1, 1, 1]},
        "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-10, 2, -10], "scale": [1.25, 1.25, 1.25]},
        "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [10, 0, -10], "scale": [1.25, 1.25, 1.25]},
        "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
        "ground": {"translation": [4, 4, 2], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.5, 0.5, 0.5]},
    }
    model(MI + 'luminite_shield_3d.json', "minecraft:item/handheld",
          {"particle": "umbra:item/luminite_shield_rim", "face": "umbra:item/luminite_shield_face", "rim": "umbra:item/luminite_shield_rim"},
          els, {"display": display, "gui_light": "front"})
    # in the hand the 3D model, in the inventory the picture
    json.dump({"model": {"type": "minecraft:select", "property": "minecraft:display_context",
                         "cases": [{"when": ["gui", "ground", "fixed"], "model": {"type": "minecraft:model", "model": "umbra:item/luminite_shield"}}],
                         "fallback": {"type": "minecraft:model", "model": "umbra:item/luminite_shield_3d"}}},
              open(ITEMS + 'luminite_shield.json', 'w'), indent=2)

    # the lighthouse lamp: a cage of brass round a core whose glow pulses faster as it grows
    for lvl in range(5):
        light = [0, 6, 9, 12, 15][lvl]
        els = [
            box((1, 0, 1), (15, 2, 15), "frame"),
            box((1, 14, 1), (15, 16, 15), "frame"),
            box((1, 2, 1), (3, 14, 3), "frame"), box((13, 2, 1), (15, 14, 3), "frame"),
            box((1, 2, 13), (3, 14, 15), "frame"), box((13, 2, 13), (15, 14, 15), "frame"),
            box((3, 2, 3), (13, 14, 13), "glow", light=light, shade=lvl == 0),
        ]
        model(MB + 'lighthouse_lamp_%d.json' % lvl, "minecraft:block/block",
              {"particle": "umbra:block/lighthouse_lamp_bottom", "frame": "umbra:block/lighthouse_lamp_bottom", "glow": "umbra:block/lighthouse_lamp_glow_%d" % lvl}, els)
    variants = {}
    for lvl in range(5):
        for fill in range(4):
            variants["fill=%d,level=%d" % (fill, lvl)] = {"model": "umbra:block/lighthouse_lamp_%d" % lvl}
    json.dump({"variants": variants}, open(BS + 'lighthouse_lamp.json', 'w'), indent=2)
    json.dump({"model": {"type": "minecraft:model", "model": "umbra:block/lighthouse_lamp_2"}}, open(ITEMS + 'lighthouse_lamp.json', 'w'), indent=2)


def write_candle_kinds():
    """2.4: more candles. Thick ones, melted stubs with wax run down, and an iron candelabrum."""
    tex('luminite_candle_iron', lambda x, y, r: noise(bevel(x, y, (60, 58, 66), k=30), r, 4))
    kinds = {
        "thick_luminite_candle": [(6, 6, 7, 4), (3, 9, 5, 4), (10, 4, 6, 4), (9, 10, 4, 4)],
        "melted_luminite_candle": [(7, 7, 3, 3), (4, 9, 2, 3), (10, 5, 4, 3), (9, 10, 2, 3)],
        "luminite_candelabrum": [(7, 7, 10, 2), (3, 7, 8, 2), (11, 7, 8, 2), (7, 3, 8, 2)],
    }
    for kind, spots in kinds.items():
        for n in range(1, 5):
            els = []
            if kind == "luminite_candelabrum":
                els += [box((6.5, 0, 6.5), (9.5, 1, 9.5), "iron"), box((7.5, 1, 7.5), (8.5, 6, 8.5), "iron"),
                        box((3, 6, 7.5), (13, 7, 8.5), "iron"), box((7.5, 6, 3), (8.5, 7, 13), "iron")]
            for i in range(n):
                x, z, h, w = spots[i]
                base = 7 if kind == "luminite_candelabrum" and i > 0 else (1 if kind == "luminite_candelabrum" else 0)
                if kind == "luminite_candelabrum" and i == 0:
                    base = 6
                els.append(box((x, base, z), (x + w, base + h - (w - 2), z + w), "wax"))
                top = base + h - (w - 2)
                if kind == "melted_luminite_candle":
                    els.append(box((x - 1, 0, z - 1), (x + w + 1, 1, z + w + 1), "wax"))      # the puddle of wax
                fx = x + w / 2
                els.append(box((fx - 0.5, top, z + w / 2 - 0.5), (fx + 0.5, top + 2, z + w / 2 + 0.5), "flame", light=15, shade=False))
            model(MB + '%s_%d.json' % (kind, n), "minecraft:block/block",
                  {"particle": "umbra:block/luminite_candle", "wax": "umbra:block/luminite_candle", "flame": "umbra:block/luminite_candle_flame",
                   "iron": "umbra:block/luminite_candle_iron"}, els, {"ambientocclusion": False})
        json.dump({"variants": {"candles=%d" % n: {"model": "umbra:block/%s_%d" % (kind, n)} for n in range(1, 5)}}, open(BS + kind + '.json', 'w'), indent=2)
        json.dump({"model": {"type": "minecraft:model", "model": "umbra:block/%s_1" % kind}}, open(ITEMS + kind + '.json', 'w'), indent=2)


def write_lantern():
    """The Underside's lamp: a dark iron cage round a luminite core; standing or hanging from a short chain."""
    tex('luminite_lantern_cage', lambda x, y, r: noise(bevel(x, y, (52, 50, 62), k=26), r, 4))
    tex('luminite_lantern_core', lambda x, y, r: list(mix((255, 255, 255), LUMI, min(1.0, math.hypot(x - 7.5, y - 7.5) / 9))) + [255])
    t = {"particle": "umbra:block/luminite_lantern_cage", "cage": "umbra:block/luminite_lantern_cage", "core": "umbra:block/luminite_lantern_core"}
    for hanging in (False, True):
        dy = 1 if hanging else 0
        els = [box((5, dy, 5), (11, dy + 1, 11), "cage"),
               box((5.5, dy + 1, 5.5), (10.5, dy + 7, 10.5), "core", light=15, shade=False),
               box((5, dy + 7, 5), (11, dy + 8, 11), "cage"),
               box((6.5, dy + 8, 6.5), (9.5, dy + 9, 9.5), "cage"),
               box((5, dy + 1, 5), (6, dy + 7, 6), "cage"), box((10, dy + 1, 5), (11, dy + 7, 6), "cage"),
               box((5, dy + 1, 10), (6, dy + 7, 11), "cage"), box((10, dy + 1, 10), (11, dy + 7, 11), "cage")]
        if hanging:
            els.append(box((7.5, dy + 9, 7.5), (8.5, 16, 8.5), "cage"))
        model(MB + 'luminite_lantern_block%s.json' % ('_hanging' if hanging else ''), "minecraft:block/block", t, els, {"ambientocclusion": False})
    json.dump({"variants": {"hanging=false": {"model": "umbra:block/luminite_lantern_block"},
                            "hanging=true": {"model": "umbra:block/luminite_lantern_block_hanging"}}}, open(BS + 'luminite_lantern_block.json', 'w'), indent=2)
    json.dump({"model": {"type": "minecraft:model", "model": "umbra:block/luminite_lantern_block"}}, open(ITEMS + 'luminite_lantern_block.json', 'w'), indent=2)


# ------------------------------------------------------------------ preview: a little isometric renderer for element models

def load_tex(ref):
    ns, path = ref.split(':')
    base = 'src/main/resources/assets/%s/textures/%s.png' % (ns, path) if ns == 'umbra' else None
    if base and os.path.exists(base):
        im = Image.open(base).convert('RGBA')
        if im.height > im.width:
            im = im.crop((0, im.height - im.width, im.width, im.height))   # last frame of a strip: the brightest
        return im
    return Image.new('RGBA', (16, 16), (255, 0, 255, 255))


def render(model_path, scale=8):
    m = json.load(open(model_path))
    texs = {k: v for k, v in m["textures"].items()}

    def resolve(t):
        while t.startswith('#'):
            t = texs[t[1:]]
        return t
    W = 40 * scale
    img = Image.new('RGBA', (W, W), (0, 0, 0, 0))
    from PIL import ImageDraw
    d = ImageDraw.Draw(img)

    def iso(x, y, z):
        return (W / 2 + (x - z) * scale * 0.87, W * 0.62 - y * scale + (x + z) * scale * 0.5 - 8 * scale)

    def rotate(p, rot):
        if not rot:
            return p
        o = rot["origin"]
        a = math.radians(rot["angle"])
        x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
        if rot["axis"] == "y":
            x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
        elif rot["axis"] == "x":
            y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
        else:
            x, y = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
        return (x + o[0], y + o[1], z + o[2])

    quads = []
    for e in m["elements"]:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        rot = e.get("rotation")
        for side, f in e["faces"].items():
            if side not in ("up", "south", "east"):
                continue
            im = load_tex(resolve(f["texture"]))
            if side == "up":
                corners = [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
                shade = 1.0
            elif side == "south":
                corners = [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)]
                shade = 0.8
            else:
                corners = [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)]
                shade = 0.6
            if e.get("light_emission"):
                shade = 1.0
            corners = [rotate(c, rot) for c in corners]
            depth = sum(c[0] + c[2] + c[1] * 0.5 for c in corners) / 4
            quads.append((depth, corners, im, shade))
    quads.sort(key=lambda q: q[0])
    for _, (a, b, c, dd), im, shade in quads:
        n = 16
        for j in range(n):
            for i in range(n):
                def lerp(u, v):
                    top = [a[k] + (b[k] - a[k]) * u for k in range(3)]
                    bot = [dd[k] + (c[k] - dd[k]) * u for k in range(3)]
                    return [top[k] + (bot[k] - top[k]) * v for k in range(3)]
                pts = [iso(*lerp(i / n, j / n)), iso(*lerp((i + 1) / n, j / n)), iso(*lerp((i + 1) / n, (j + 1) / n)), iso(*lerp(i / n, (j + 1) / n))]
                col = im.getpixel((int(i * im.width / n), int(j * im.height / n)))
                if col[3] < 10:
                    continue
                d.polygon(pts, fill=(int(col[0] * shade), int(col[1] * shade), int(col[2] * shade), 255))
    return img


def preview(out):
    names = [MB + 'echo_altar.json', MB + 'gloom_anvil.json', MB + 'luminite_candle_4.json', MB + 'lighthouse_lamp_4.json',
             MB + 'lighthouse_lamp_0.json', MI + 'luminite_shield_3d.json']
    tiles = [render(n) for n in names]
    sheet = Image.new('RGBA', (tiles[0].width * 3, tiles[0].height * 2), (24, 22, 30, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 3) * t.width, (i // 3) * t.height))
    sheet.save(out)


if __name__ == '__main__':
    write_textures()
    write_models()
    write_lantern()
    write_candle_kinds()
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    print('ok')
