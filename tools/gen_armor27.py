#!/usr/bin/env python3
"""2.7: armour you can actually make. The shard of a seal (what each of the four mini-bosses leaves behind) is what
tier III asks for; tier II asks each set's own stuff; echo quartz and luminite are easier to find; shades drop their
shards more often. Writes the shard's icon, model, the recipes of tiers II and III, loot and worldgen numbers, lang.
Run from the repo root: python3 tools/gen_armor27.py"""
import json
import random

from PIL import Image

A = 'src/main/resources/assets/umbra/'
D = 'src/main/resources/data/umbra/'
SETS = ["gloom", "luminite", "shadow"]
PIECES = ["helmet", "chestplate", "leggings", "boots"]


def write(path, obj):
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def load(path):
    return json.load(open(path))


# ---------------------------------------------------------------- the shard of a seal: a split of black stone, a rune
def shard():
    rnd = random.Random(27)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    # a jagged wedge
    rows = {2: (8, 9), 3: (7, 10), 4: (6, 10), 5: (6, 11), 6: (5, 11), 7: (5, 12), 8: (4, 12), 9: (4, 11), 10: (4, 11),
            11: (5, 10), 12: (5, 10), 13: (6, 9), 14: (7, 8)}
    for y, (a, b) in rows.items():
        for x in range(a, b + 1):
            edge = x in (a, b) or y in (2, 14)
            base = (38, 34, 48) if not edge else (18, 16, 24)
            k = 1.0 + 0.25 * (1 - (x - a) / max(1, b - a)) - 0.1 * (y / 16) + rnd.uniform(-0.06, 0.06)
            px[x, y] = tuple(max(0, min(255, int(c * k))) for c in base) + (255,)
    # the rune of the seal, glowing
    for x, y in [(8, 4), (8, 5), (8, 6), (7, 7), (9, 7), (8, 8), (8, 9), (7, 10), (9, 10), (8, 11)]:
        px[x, y] = (150, 230, 255, 255) if y % 2 else (120, 190, 255, 255)
    for x, y in [(6, 6), (10, 8), (6, 11)]:
        px[x, y] = (95, 70, 150, 255)
    img.save(A + 'textures/item/seal_shard.png')
    write(A + 'models/item/seal_shard.json', {"parent": "minecraft:item/generated", "textures": {"layer0": "umbra:item/seal_shard"}})
    write(A + 'items/seal_shard.json', {"model": {"type": "minecraft:model", "model": "umbra:item/seal_shard"}})


# ---------------------------------------------------------------- recipes: II takes the set's own stuff, III a seal shard
ADD = {
    "gloom": {2: "umbra:gloom_ingot", 3: "umbra:seal_shard"},
    "luminite": {2: "umbra:luminite_block", 3: "umbra:seal_shard"},
    "shadow": {2: "umbra:shadow_shard", 3: "umbra:seal_shard"},
}


def recipes():
    for s in SETS:
        for piece in PIECES:
            for tier in (2, 3):
                prev = f"umbra:{s}_{piece}" + ("" if tier == 2 else "_2")
                write(D + f"recipe/{s}_{piece}_{tier}.json", {"type": "minecraft:smithing_transform", "template": "umbra:echo_quartz", "base": prev,
                                                              "addition": ADD[s][tier], "result": {"id": f"umbra:{s}_{piece}_{tier}"}})
    # four shards back into echo quartz, so a spare one is never wasted? No: they are the rare thing. Nothing else here.


# ---------------------------------------------------------------- loot
def loot():
    for boss in ("watcher", "mimic_queen", "shepherd", "doppelganger"):
        path = D + f"loot_table/entities/{boss}.json"
        t = load(path)
        t["pools"] = [p for p in t["pools"] if not any(e.get("name") == "umbra:seal_shard" for e in p["entries"])]
        t["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": "umbra:seal_shard",
                                                    "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 4}}]}]})
        write(path, t)
    # shades drop their shard far more often
    path = D + "loot_table/entities/shade.json"
    t = load(path)
    t["pools"][0]["condition"] = {"type": "minecraft:random_chance", "chance": 0.45}
    write(path, t)
    # luminite ore gives one or two crystals (fortune on top)
    path = D + "loot_table/blocks/luminite_ore.json"
    for name in ("luminite_ore", "deepslate_luminite_ore"):
        path = D + f"loot_table/blocks/{name}.json"
        t = load(path)
        alt = t["pools"][0]["entries"][0]["children"][1]
        fns = [f for f in alt.get("modifier", []) if f.get("type") != "minecraft:set_count"]
        alt["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}] + fns
        write(path, t)


# ---------------------------------------------------------------- worldgen: more echo quartz, more luminite
def worldgen():
    f = D + "worldgen/feature/ore_echo_quartz.json"
    t = load(f)
    t["size"] = 5
    t["discard_chance_on_air_exposure"] = 0.15
    write(f, t)
    for name, count in (("ore_echo_quartz", 6), ("ore_echo_quartz_ruins", 9)):
        f = D + f"worldgen/placed_feature/{name}.json"
        t = load(f)
        for p in t["placement"]:
            if p["type"] == "minecraft:count": p["count"] = count
        write(f, t)
    f = D + "worldgen/feature/ore_luminite.json"
    t = load(f)
    t["size"] = 7
    t["discard_chance_on_air_exposure"] = 0.15
    write(f, t)
    f = D + "worldgen/placed_feature/ore_luminite.json"
    t = load(f)
    for p in t["placement"]:
        if p["type"] == "minecraft:count": p["count"] = 7
        if p["type"] == "minecraft:height_range": p["height"]["max_inclusive"] = {"absolute": 32}
    write(f, t)


# ---------------------------------------------------------------- lang
LANG = {
    "ru_ru": {
        "item.umbra.seal_shard": "Осколок печати",
        "tip.umbra.seal_shard": "Печать раскололась, но держит. Её сила ляжет в доспех.",
        "set.umbra.gloom": "Полный комплект: стойкость в Изнанке, удары тварей слабее.",
        "set.umbra.shadow": "Полный комплект: быстрее на ногах, видишь в темноте, присев, теряешься из виду.",
        "set.umbra.luminite": "Полный комплект: удары тварей слабее, пожиратели света горят рядом.",
        "set.umbra.gloom.master": "Ступень III целиком: «Бастион» — на краю гибели стальная стена (раз в 90 с).",
        "set.umbra.shadow.master": "Ступень III целиком: «Ускользание» — каждый пятый удар твари проходит сквозь тень.",
        "set.umbra.luminite.master": "Ступень III целиком: «Ореол» — светишь сам, тьма рядом слабеет.",
        "set.umbra.gloom.bastion": "Сталь встала стеной.",
    },
    "en_us": {
        "item.umbra.seal_shard": "Seal Shard",
        "tip.umbra.seal_shard": "The seal broke, yet it holds. Its strength will go into armour.",
        "set.umbra.gloom": "Full set: resistance in the Underside, their blows weaker.",
        "set.umbra.shadow": "Full set: quicker on your feet, sight in the dark, crouch to slip from view.",
        "set.umbra.luminite": "Full set: their blows weaker, light eaters burn near you.",
        "set.umbra.gloom.master": "All tier III: \"Bastion\" - at death's edge a wall of steel (once in 90 s).",
        "set.umbra.shadow.master": "All tier III: \"Slip\" - one blow in five of theirs goes through your shadow.",
        "set.umbra.luminite.master": "All tier III: \"Halo\" - you shine yourself, the dark near you weakens.",
        "set.umbra.gloom.bastion": "The steel stood up like a wall.",
    },
}


def lang():
    for code, entries in LANG.items():
        path = A + f"lang/{code}.json"
        t = load(path)
        t.update(entries)
        write(path, t)


if __name__ == '__main__':
    shard()
    recipes()
    loot()
    worldgen()
    lang()
    print("ok")
