#!/usr/bin/env python3
"""2.7.1: weapons and tools with real volume in the hand. Each is built upright in its own frame (x across, y up the
handle, z through the blade, the middle of the block at 8, 8, 8) and then every element is turned the same 45 degrees
about z, so it lies on the diagonal where a flat sprite lies: the vanilla handheld/bow display transforms hold it just
as they hold the sprite. In the inventory, on the ground and in frames the flat picture stays.
Writes textures/item/mat_*.png, models/item/<weapon>_3d.json (the bow: four of them) and items/<weapon>.json.
Run from the repo root: python3 tools/gen_weapons3d.py [--preview out.png]"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_blocks23 as g  # noqa: E402

A = 'src/main/resources/assets/umbra/'
TI = A + 'textures/item/'
MI = A + 'models/item/'
ITEMS = A + 'items/'
ANGLE = -45.0     # the upright weapon tips over to the right: its top goes to the top right corner, like a sprite

# ---------------------------------------------------------------- materials
MATS = {
    "wood_dark": ((58, 42, 30), 14), "wood": ((98, 72, 46), 14), "leather": ((72, 48, 40), 10),
    "gloom": ((104, 94, 140), 22), "gloom_dark": ((54, 46, 74), 16), "shadow": ((42, 32, 58), 14),
    "luminite": ((176, 238, 255), 18), "violet": ((196, 126, 255), 20), "echo": ((70, 214, 196), 18),
    "brass": ((192, 150, 70), 20), "bone": ((214, 204, 222), 14), "crimson": ((112, 26, 58), 14),
    "string": ((232, 236, 244), 6), "glow_gold": ((255, 226, 140), 10),
}


def materials():
    for name, (base, k) in MATS.items():
        def fn(x, y, r, base=base, k=k):
            c = g.bevel(x, y, base, k=k)
            if name.startswith("wood") and (x * 3 + y) % 7 == 0:
                c = [v - 14 for v in c]
            if name in ("gloom", "gloom_dark", "shadow") and (x + y * 5) % 13 == 0:
                c = [v + 26 for v in c]
            if name == "leather" and y % 3 == 0:
                c = [v - 16 for v in c]
            return g.noise(c, r, 6)
        g.tex('mat_' + name, fn, folder=TI)


# ---------------------------------------------------------------- building
class W:
    """One weapon: boxes in the upright frame, then laid on the diagonal."""

    def __init__(self, name, parent="minecraft:item/handheld"):
        self.name, self.parent, self.els, self.used = name, parent, [], set()

    def box(self, x1, y1, x2, y2, mat, t=1.0, z=None, light=0):
        """A box from (x1, y1) to (x2, y2), t thick through z (centred on 8, or from z[0] to z[1])."""
        z1, z2 = z if z else (8 - t / 2, 8 + t / 2)
        e = g.box((x1, y1, z1), (x2, y2, z2), mat, rot={"angle": ANGLE, "axis": "z", "origin": [8, 8, 8]}, light=light)
        self.els.append(e)
        self.used.add(mat)
        return self

    def write(self, suffix="_3d"):
        tex = {m: "umbra:item/mat_" + m for m in sorted(self.used)}
        tex["particle"] = "umbra:item/" + self.name
        g.model(MI + self.name + suffix + '.json', self.parent, tex, self.els, {"display": display(self.parent, self.scale)})

    scale = 0.85


# vanilla's own transforms for a held sprite (and a bow), shrunk a little: a 3D weapon reaches past its 16 x 16 square
HANDHELD = {"thirdperson_righthand": ([0, -90, 55], [0, 4.0, 0.5], 0.85), "thirdperson_lefthand": ([0, 90, -55], [0, 4.0, 0.5], 0.85),
            "firstperson_righthand": ([0, -90, 25], [1.13, 3.2, 1.13], 0.68), "firstperson_lefthand": ([0, 90, -25], [1.13, 3.2, 1.13], 0.68)}
BOW = {"thirdperson_righthand": ([-80, 260, -40], [-1, -2, 2.5], 0.9), "thirdperson_lefthand": ([-80, -280, 40], [-1, -2, 2.5], 0.9),
       "firstperson_righthand": ([0, -90, 25], [1.13, 3.2, 1.13], 0.68), "firstperson_lefthand": ([0, 90, -25], [1.13, 3.2, 1.13], 0.68)}


def display(parent, k):
    base = BOW if parent.endswith("bow") else HANDHELD
    return {ctx: {"rotation": r, "translation": t, "scale": [round(s * k, 3)] * 3} for ctx, (r, t, s) in base.items()}


def hilt(w, top=4.0, bottom=-2.5, wrap="leather", pommel="gloom"):
    w.box(7.25, bottom, 8.75, top, wrap, t=1.5)
    for y in range(int(bottom) + 1, int(top), 2):
        w.box(7.1, y, 8.9, y + 0.5, "wood_dark", t=1.7)
    w.box(6.75, bottom - 1.5, 9.25, bottom, pommel, t=2.5)


def sword():
    w = W("gloom_sword")
    hilt(w)
    w.box(4, 4, 12, 5.5, "gloom", t=2.2)                    # the guard
    w.box(3.2, 4.5, 4.2, 6.5, "gloom", t=1.6).box(11.8, 4.5, 12.8, 6.5, "gloom", t=1.6)
    w.box(7.25, 4.2, 8.75, 5.3, "violet", t=2.6, light=10)  # a gem in the guard
    w.box(6.6, 5.5, 9.4, 17.5, "gloom", t=0.9)              # the blade
    w.box(7.6, 5.5, 8.4, 16, "gloom_dark", t=1.1)           # the fuller
    w.box(7.0, 17.5, 9.0, 19, "gloom", t=0.8).box(7.6, 19, 8.4, 20, "gloom", t=0.6)
    return w


def despair():
    w = W("despair_blade")
    hilt(w, wrap="crimson", pommel="shadow")
    w.box(3.5, 4, 12.5, 5.2, "shadow", t=1.8)
    w.box(2.8, 5.2, 3.8, 7.2, "shadow", t=1.2).box(12.2, 5.2, 13.2, 7.2, "shadow", t=1.2)
    w.box(2.8, 2.2, 3.8, 4, "shadow", t=1.2).box(12.2, 2.2, 13.2, 4, "shadow", t=1.2)
    # a jagged blade that leans: each piece a little further over
    for i, (x1, x2) in enumerate(((6.4, 9.6), (6.8, 10.0), (7.2, 10.4), (7.6, 10.6), (8.0, 10.6))):
        y = 5.2 + i * 2.6
        w.box(x1, y, x2, y + 2.6, "shadow", t=0.9)
        w.box(x2, y + 0.6, x2 + 0.8, y + 1.6, "crimson", t=0.7)     # teeth on the edge
    w.box(8.6, 18.2, 10.2, 20.2, "shadow", t=0.7)
    w.box(7.7, 5.2, 8.5, 17.5, "violet", t=1.1, light=8)    # the vein that glows
    return w


def dagger():
    w = W("shadow_dagger")
    hilt(w, top=3.5, bottom=-1.5, wrap="shadow", pommel="violet")
    w.box(5.5, 3.5, 10.5, 4.5, "gloom_dark", t=1.8)
    w.box(7.0, 4.5, 9.2, 11.5, "shadow", t=0.7)
    w.box(7.8, 4.5, 8.4, 11, "violet", t=0.9, light=6)
    w.box(7.6, 11.5, 9.6, 13, "shadow", t=0.6).box(9.0, 12.6, 10.2, 14.2, "shadow", t=0.5)   # the hooked tip
    return w


def knife():
    w = W("shadow_knife")
    w.box(7.4, 0, 8.6, 5, "leather", t=1.2).box(7.0, -1.2, 9.0, 0, "gloom_dark", t=1.6)
    w.box(6.8, 5, 9.2, 6, "gloom_dark", t=1.4)
    w.box(7.2, 6, 8.8, 12, "shadow", t=0.6).box(7.6, 12, 8.4, 13.6, "shadow", t=0.5)
    w.box(7.85, 6, 8.15, 12, "violet", t=0.8, light=6)
    return w


def axe():
    w = W("gloom_axe")
    w.box(7.3, -3, 8.7, 17, "wood", t=1.4)
    w.box(7.1, -1, 8.9, 0, "leather", t=1.6).box(7.1, 1, 8.9, 2, "leather", t=1.6)
    w.box(6.5, 11, 9.5, 16.5, "gloom_dark", t=2.4)                       # the socket
    w.box(2.6, 10.5, 6.5, 17, "gloom", t=1.2)                            # the blade, on the left
    w.box(1.6, 9.5, 2.6, 18, "gloom", t=0.8)                             # its edge, wider
    w.box(1.2, 10, 1.6, 17.5, "bone", t=0.6)                             # honed bright
    w.box(9.5, 12.5, 12.5, 14.5, "gloom", t=1.2).box(12.5, 13, 13.5, 14, "gloom", t=0.8)   # a spike behind
    w.box(7.0, 16.5, 9.0, 18, "gloom_dark", t=1.8)
    return w


def pickaxe():
    w = W("gloom_pickaxe")
    w.box(7.3, -3, 8.7, 15, "wood", t=1.4)
    w.box(7.1, -1, 8.9, 0, "leather", t=1.6).box(7.1, 1, 8.9, 2, "leather", t=1.6)
    w.box(6.2, 13.5, 9.8, 17.5, "gloom_dark", t=2.4)
    w.box(2.5, 14.5, 6.2, 16.8, "gloom", t=1.6).box(9.8, 14.5, 13.5, 16.8, "gloom", t=1.6)
    w.box(0.5, 13.5, 2.5, 16, "gloom", t=1.2).box(13.5, 13.5, 15.5, 16, "gloom", t=1.2)
    w.box(-0.3, 12.2, 1.2, 14.2, "bone", t=0.8).box(14.8, 12.2, 16.3, 14.2, "bone", t=0.8)
    return w


def shovel():
    w = W("gloom_shovel")
    w.box(7.3, -3, 8.7, 12, "wood", t=1.4)
    w.box(6.0, -4, 10.0, -3, "wood_dark", t=1.4)                         # a grip across the end
    w.box(7.0, 11, 9.0, 13, "gloom_dark", t=1.8)
    w.box(5.8, 13, 10.2, 18.5, "gloom", t=0.8)
    w.box(6.4, 18.5, 9.6, 19.6, "gloom", t=0.7).box(7.2, 19.6, 8.8, 20.4, "bone", t=0.6)
    w.box(7.6, 13.5, 8.4, 18, "gloom_dark", t=1.0)                      # a ridge down the spade
    return w


def spear():
    w = W("luminite_spear")
    w.box(7.4, -5, 8.6, 15, "wood", t=1.2)
    w.box(7.2, -5.6, 8.8, -4.4, "brass", t=1.4)
    for y in (2.0, 8.0):
        w.box(7.2, y, 8.8, y + 1, "leather", t=1.4)
    w.box(7.0, 14.2, 9.0, 16, "brass", t=1.6)
    w.box(6.4, 16, 9.6, 18, "luminite", t=1.2, light=12)   # a leaf of crystal
    w.box(6.9, 18, 9.1, 20.5, "luminite", t=1.0, light=12)
    w.box(7.5, 20.5, 8.5, 22, "luminite", t=0.8, light=12)
    w.box(5.4, 16.2, 6.4, 17.2, "luminite", t=0.8, light=12).box(9.6, 16.2, 10.6, 17.2, "luminite", t=0.8, light=12)
    return w


def scythe():
    w = W("shepherd_scythe")
    w.box(7.4, -4, 8.6, 18, "wood_dark", t=1.2)
    w.box(7.2, 6, 8.8, 8, "leather", t=1.4)
    w.box(8.6, 6.5, 11, 7.5, "wood_dark", t=1.0)                          # the second grip
    w.box(7.0, 17, 9.0, 19, "shadow", t=1.6)
    # the blade sweeps out to the left and down, pale as bone
    # (2.7.2: the glowing line now runs along the inner, cutting edge of each piece instead of straight across)
    for x1, y1, x2, y2 in ((2.5, 17.2, 7.0, 19.2), (-0.5, 16.2, 2.5, 18.6), (-2.6, 14.4, -0.5, 17.4), (-3.6, 12.0, -1.8, 14.4),
                           (-3.8, 10.2, -2.6, 12.0)):
        w.box(x1, y1, x2, y2, "bone", t=0.6)
    for x1, y1, x2, y2 in ((2.5, 16.9, 7.0, 17.3), (-0.5, 15.9, 2.5, 16.3), (-0.9, 14.4, -0.5, 16.2), (-1.8, 12.0, -1.4, 14.4)):
        w.box(x1, y1, x2, y2, "violet", t=0.7, light=5)
    return w


def echo_staff():
    w = W("echo_staff")
    w.box(7.4, -5, 8.6, 14, "shadow", t=1.2)
    for y in (0.0, 5.0, 10.0):
        w.box(7.2, y, 8.8, y + 0.8, "gloom", t=1.4)
    w.box(6.5, 13.5, 9.5, 15, "gloom", t=2.4)
    w.box(5.0, 14, 6.0, 19, "gloom", t=1.0).box(10.0, 14, 11.0, 19, "gloom", t=1.0)     # two prongs
    w.box(5.6, 18.4, 6.8, 19.6, "gloom", t=0.8).box(9.2, 18.4, 10.4, 19.6, "gloom", t=0.8)
    w.box(6.5, 15, 9.5, 19.5, "echo", t=2.6, light=14)      # the crystal between them
    w.box(7.2, 19.5, 8.8, 21, "echo", t=1.6, light=14)
    return w


def keeper_staff():
    w = W("keeper_staff")
    w.box(7.4, -5, 8.6, 13, "wood", t=1.2)
    for y in (-2.0, 3.0, 8.0):
        w.box(7.2, y, 8.8, y + 0.8, "brass", t=1.4)
    w.box(5.0, 13, 11.0, 14, "brass", t=6.0)                 # the lantern: a base,
    for x in (5.0, 10.0):
        w.box(x, 14, x + 1, 18.5, "brass", z=(5.0, 6.0)).box(x, 14, x + 1, 18.5, "brass", z=(10.0, 11.0))
    w.box(6.2, 14.2, 9.8, 18.3, "glow_gold", t=3.6, light=15)  # the flame,
    w.box(5.0, 18.5, 11.0, 19.5, "brass", t=6.0)             # a roof,
    w.box(7.4, 19.5, 8.6, 21, "brass", t=1.2)                # a ring to hang it by
    return w


# ---------------------------------------------------------------- the bow: four draws
def bow(draw):
    """draw 0 = at rest; 1..3 the string further back with an arrow on it."""
    w = W("luminite_bow", parent="minecraft:item/bow")
    w.box(6.8, 6, 9.2, 10, "leather", t=1.8)                       # the grip
    # limbs: the riser on the left, the tips curve over to the right (where the string is)
    limbs = ((7.0, 10, 8.6, 13), (7.8, 13, 9.4, 15.5), (8.8, 15.5, 10.2, 17.5), (9.8, 17.5, 11.0, 19))
    for x1, y1, x2, y2 in limbs:
        w.box(x1, y1, x2, y2, "gloom", t=1.2)
        w.box(x1, 16 - y2, x2, 16 - y1, "gloom", t=1.2)
    w.box(10.4, 18.6, 11.6, 19.8, "luminite", t=1.0, light=10).box(10.4, -3.8, 11.6, -2.6, "luminite", t=1.0, light=10)
    w.box(7.6, 7.2, 8.4, 8.8, "luminite", t=2.0, light=8)
    pull = (0.0, 1.6, 2.8, 4.0)[draw]
    if draw == 0:
        w.box(10.9, -2.8, 11.2, 18.8, "string", t=0.3)
    else:
        # the string bent back to the nock in three steps each way
        nx = 11.0 + pull
        for i in range(3):
            xa = 11.0 + pull * i / 3
            w.box(xa, 18.8 - (i + 1) * 3.5, xa + 0.4, 18.8 - i * 3.5, "string", t=0.3)
            w.box(xa, -2.8 + i * 3.5, xa + 0.4, -2.8 + (i + 1) * 3.5, "string", t=0.3)
        w.box(nx - 0.2, 7.9, nx + 0.4, 8.1, "string", t=0.3)
        # the arrow: from the nock across the grip, its crystal head beyond the riser
        w.box(nx - 13.5, 7.7, nx, 8.3, "wood", t=0.6)
        w.box(nx - 15.5, 7.4, nx - 13.5, 8.6, "luminite", t=0.9, light=12)
        w.box(nx - 1.6, 8.3, nx - 0.2, 9.3, "bone", t=0.2).box(nx - 1.6, 6.7, nx - 0.2, 7.7, "bone", t=0.2)
    w.write("_3d" if draw == 0 else "_pulling_%d_3d" % (draw - 1))
    return w


SIMPLE = [sword, despair, dagger, knife, axe, pickaxe, shovel, spear, scythe, echo_staff, keeper_staff]


def item_def(name):
    """Flat in the inventory, on the ground and in frames; the 3D model everywhere else."""
    return {"model": {"type": "minecraft:select", "property": "minecraft:display_context",
                      "cases": [{"when": ["gui", "ground", "fixed"], "model": {"type": "minecraft:model", "model": "umbra:item/" + name}}],
                      "fallback": {"type": "minecraft:model", "model": "umbra:item/" + name + "_3d"}}}


def bow_def():
    def cond(suffix):
        return {"type": "minecraft:condition", "property": "minecraft:using_item",
                "on_false": {"type": "minecraft:model", "model": "umbra:item/luminite_bow" + ("" if not suffix else "_3d")},
                "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
                            "entries": [{"threshold": 0.65, "model": {"type": "minecraft:model", "model": "umbra:item/luminite_bow_pulling_1" + suffix}},
                                        {"threshold": 0.9, "model": {"type": "minecraft:model", "model": "umbra:item/luminite_bow_pulling_2" + suffix}}],
                            "fallback": {"type": "minecraft:model", "model": "umbra:item/luminite_bow_pulling_0" + suffix}}}
    return {"model": {"type": "minecraft:select", "property": "minecraft:display_context",
                      "cases": [{"when": ["gui", "ground", "fixed"], "model": cond("")}], "fallback": cond("_3d")}}


def write_all():
    materials()
    built = []
    for f in SIMPLE:
        w = f()
        w.write()
        json.dump(item_def(w.name), open(ITEMS + w.name + '.json', 'w'), indent=2)
        built.append(w)
    for d in range(4):
        built.append(bow(d))
    json.dump(bow_def(), open(ITEMS + 'luminite_bow.json', 'w'), indent=2)
    return built


def preview(built, out):
    """Front view of each 3D model (as it lies in the 16 x 16 square), coloured by material."""
    import math
    from PIL import Image, ImageDraw
    cell = 160
    img = Image.new('RGB', (cell * 5, cell * ((len(built) + 4) // 5)), (30, 30, 36))
    d = ImageDraw.Draw(img)
    for i, w in enumerate(built):
        ox, oy = (i % 5) * cell, (i // 5) * cell
        d.rectangle([ox + 32, oy + 32, ox + 128, oy + 128], outline=(70, 70, 80))
        for e in sorted(w.els, key=lambda e: e['to'][2]):
            mat = next(iter(e['faces'].values()))['texture'][1:]
            col = MATS[mat][0]
            a = math.radians(e.get('rotation', {}).get('angle', 0))
            pts = []
            for x, y in ((e['from'][0], e['from'][1]), (e['to'][0], e['from'][1]), (e['to'][0], e['to'][1]), (e['from'][0], e['to'][1])):
                x, y = x - 8, y - 8
                xr, yr = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
                pts.append((ox + 80 + xr * 6, oy + 80 - yr * 6))
            d.polygon(pts, fill=col, outline=tuple(max(0, c - 40) for c in col))
        d.text((ox + 4, oy + 4), w.name[:18], fill=(200, 200, 200))
    img.save(out)


if __name__ == '__main__':
    b = write_all()
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(b, sys.argv[2])
    print("ok", len(b))
