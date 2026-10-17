#!/usr/bin/env python3
"""2.4 trophies: one relic from each boss (items), and the trophy shelf in Yan's house that holds all six
(a multipart block: the shelf, and a little likeness of each relic once it is set there).
Run from the repo root: python3 tools/gen_trophies.py"""
import json
import math
import random
import sys
import os

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
import gen_blocks23 as b  # noqa: E402

R = 'src/main/resources/'
A = R + 'assets/umbra/'
BOSSES = ["watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow"]
NAMES = {
    "watcher": ("Застывший глаз", "Frozen Eye"),
    "mimic_queen": ("Корона подмен", "Crown of Changelings"),
    "shepherd": ("Обломок посоха", "Broken Crook"),
    "doppelganger": ("Треснувшее лицо", "Cracked Face"),
    "architect": ("Краеугольный камень", "Cornerstone"),
    "primordial_shadow": ("Осколок первой тьмы", "Shard of the First Dark"),
}


def icon(name):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if name == "watcher":
        d.ellipse([2, 2, 13, 13], fill=(232, 226, 214, 255), outline=(120, 60, 60, 255))
        d.ellipse([5, 5, 10, 10], fill=(150, 40, 60, 255)); d.ellipse([7, 7, 8, 8], fill=(10, 6, 8, 255))
        d.point([(4, 6), (11, 9)], fill=(170, 30, 40, 255))
    elif name == "mimic_queen":
        d.polygon([(2, 12), (2, 6), (5, 9), (8, 4), (11, 9), (14, 6), (14, 12)], fill=(220, 180, 60, 255), outline=(120, 90, 20, 255))
        d.rectangle([2, 12, 14, 13], fill=(170, 130, 40, 255)); d.point([(8, 8)], fill=(200, 30, 60, 255))
    elif name == "shepherd":
        d.line([(4, 14), (9, 5)], fill=(90, 66, 42, 255), width=2)
        d.arc([8, 1, 14, 7], 180, 360, fill=(90, 66, 42, 255), width=2); d.point([(12, 3)], fill=(170, 120, 255, 255))
    elif name == "doppelganger":
        d.ellipse([3, 2, 12, 14], fill=(200, 196, 206, 255), outline=(90, 86, 100, 255))
        d.rectangle([5, 6, 6, 7], fill=(200, 20, 20, 255)); d.rectangle([9, 6, 10, 7], fill=(200, 20, 20, 255))
        d.line([(5, 11), (7, 10), (9, 11), (11, 10)], fill=(90, 10, 10, 255)); d.line([(8, 2), (6, 8), (9, 14)], fill=(60, 56, 70, 255))
    elif name == "architect":
        d.polygon([(8, 2), (14, 5), (14, 11), (8, 14), (2, 11), (2, 5)], fill=(84, 80, 96, 255), outline=(40, 38, 50, 255))
        d.line([(8, 2), (8, 8), (14, 5)], fill=(110, 106, 124, 255)); d.line([(8, 8), (2, 5)], fill=(110, 106, 124, 255))
        d.point([(5, 9), (11, 9), (8, 11)], fill=(255, 220, 120, 255))
    else:
        d.polygon([(8, 1), (13, 7), (9, 15), (4, 9)], fill=(16, 8, 24, 255), outline=(120, 60, 180, 255))
        d.line([(8, 3), (8, 12)], fill=(170, 90, 230, 255)); d.point([(7, 6), (9, 9)], fill=(235, 190, 255, 255))
    return im


def main():
    for name in BOSSES:
        im = icon(name)
        im.save(A + 'textures/item/trophy_%s.png' % name)
        im.save(A + 'textures/block/trophy_%s.png' % name)      # block models only see the blocks atlas
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "umbra:item/trophy_" + name}}, open(A + 'models/item/trophy_%s.json' % name, 'w'), indent=2)
        json.dump({"model": {"type": "minecraft:model", "model": "umbra:item/trophy_" + name}}, open(A + 'items/trophy_%s.json' % name, 'w'), indent=2)
        # every boss leaves its relic
        p = R + 'data/umbra/loot_table/entities/%s.json' % name
        j = json.load(open(p))
        if not any(e.get("name") == "umbra:trophy_" + name for pool in j["pools"] for e in pool["entries"]):
            j["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": "umbra:trophy_" + name}]})
        json.dump(j, open(p, 'w'), indent=2)

    # the shelf: two boards, sides, a back; slots three across, two high
    b.tex('trophy_shelf', lambda x, y, r: b.noise(b.bevel(x, y, (92, 66, 44), k=22), r, 6))
    t = {"particle": "umbra:block/trophy_shelf", "wood": "umbra:block/trophy_shelf"}
    els = [b.box((0, 0, 13), (16, 16, 16), "wood"),
           b.box((0, 1, 9), (16, 2, 13), "wood"), b.box((0, 8, 9), (16, 9, 13), "wood"), b.box((0, 15, 9), (16, 16, 13), "wood"),
           b.box((0, 0, 9), (1, 16, 13), "wood"), b.box((15, 0, 9), (16, 16, 13), "wood")]
    b.model(A + 'models/block/trophy_shelf.json', "minecraft:block/block", t, els)
    slots = [(3, 2), (8, 2), (13, 2), (3, 9), (8, 9), (13, 9)]
    for i, name in enumerate(BOSSES):
        x, y = slots[i]
        # the relic stands on the board like a little plaque: its picture on the front, a thin edge round it
        el = b.box((x - 2, y, 10.5), (x + 2, y + 5, 11.5), "relic", light=6 if name in ("watcher", "primordial_shadow") else 0)
        el["faces"]["north"]["uv"] = [0, 0, 16, 16]
        el["faces"]["south"]["uv"] = [16, 0, 0, 16]
        for side in ("east", "west"):
            el["faces"][side]["uv"] = [0, 0, 1, 16]
        for side in ("up", "down"):
            el["faces"][side]["uv"] = [0, 0, 16, 1]
        b.model(A + 'models/block/trophy_shelf_%s.json' % name, "minecraft:block/block",
                {"particle": "umbra:block/trophy_" + name, "relic": "umbra:block/trophy_" + name}, [el])
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    parts = []
    for f, y in rot.items():
        parts.append({"when": {"facing": f}, "apply": {"model": "umbra:block/trophy_shelf", "y": y}})
        for name in BOSSES:
            parts.append({"when": {"facing": f, name: "true"}, "apply": {"model": "umbra:block/trophy_shelf_" + name, "y": y}})
    json.dump({"multipart": parts}, open(A + 'blockstates/trophy_shelf.json', 'w'), indent=2)
    json.dump({"model": {"type": "minecraft:model", "model": "umbra:block/trophy_shelf"}}, open(A + 'items/trophy_shelf.json', 'w'), indent=2)
    json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "umbra:trophy_shelf"}],
                                                     "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": "umbra:blocks/trophy_shelf"},
              open(R + 'data/umbra/loot_table/blocks/trophy_shelf.json', 'w'), indent=2)
    json.dump({"type": "minecraft:crafting_shaped", "category": "misc", "key": {"P": "minecraft:spruce_planks", "S": "minecraft:spruce_slab"},
               "pattern": ["SSS", "P P", "SSS"], "result": {"id": "umbra:trophy_shelf"}}, open(R + 'data/umbra/recipe/trophy_shelf.json', 'w'), indent=2)
    for f, k in (('ru_ru', 0), ('en_us', 1)):
        p = A + 'lang/%s.json' % f
        j = json.load(open(p))
        for name in BOSSES:
            j['item.umbra.trophy_' + name] = NAMES[name][k]
        j['block.umbra.trophy_shelf'] = ["Полка трофеев", "Trophy Shelf"][k]
        json.dump(j, open(p, 'w'), ensure_ascii=False, indent=2)
    print('trophies ok')


if __name__ == '__main__':
    main()
