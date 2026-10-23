#!/usr/bin/env python3
"""2.9: the Depths in the middle of the game, and the Keepers' treasure; the boss tracks on discs.
Writes: the icons of the lurker's scale, the Keeper's key and the eight discs; the Keepers' chest (textures, a shut
and an open model, blockstate); item models and definitions; the jukebox songs; loot (the lurker's scale, the key
and each boss's disc); the new recipe of the Key of the Depths; lang (ru, en).
Run from the repo root: python3 tools/gen_depths29.py [--preview out.png]"""
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import gen_blocks23 as g  # noqa: E402

A = 'src/main/resources/assets/umbra/'
D = 'src/main/resources/data/umbra/'
TB = A + 'textures/block/'
TI = A + 'textures/item/'
MB = A + 'models/block/'
MI = A + 'models/item/'

# the tracks, their length in seconds (as tools/gen_music28.py renders them) and the colour of the disc's label
DISCS = {
    "watcher": (83.6, (205, 40, 44)), "mimic_queen": (72.9, (226, 176, 64)), "shepherd": (93.5, (150, 98, 226)),
    "doppelganger": (65.6, (222, 224, 236)), "architect": (68.0, (80, 206, 222)), "primordial_shadow": (76.7, (118, 40, 170)),
    "drowned_keeper": (68.5, (38, 156, 150)), "fallen_keeper": (79.6, (244, 192, 92)),
}
BOSS_LOOT = {"watcher": "watcher", "mimic_queen": "mimic_queen", "shepherd": "shepherd", "doppelganger": "doppelganger",
             "architect": "architect", "primordial_shadow": "primordial_shadow", "drowned_keeper": "drowned_keeper",
             "fallen_keeper": "fallen_keeper"}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def load(path):
    return json.load(open(path))


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


# ---------------------------------------------------------------- item icons
def icon(name, fn):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c is not None:
                px[x, y] = clamp(c[:3]) + ((c[3],) if len(c) > 3 else (255,))
    img.save(TI + name + '.png')
    write(MI + name + '.json', {"parent": "minecraft:item/generated", "textures": {"layer0": "umbra:item/" + name}})
    write(A + 'items/' + name + '.json', {"model": {"type": "minecraft:model", "model": "umbra:item/" + name}})


def scale_icon(x, y):
    """A long scale of the lurker: dark sea-green, a sheen across it, a pale rim, ridged."""
    cx, cy = 7.5, 8.0
    dx, dy = (x - cx) / 5.2, (y - cy) / 6.8
    r = dx * dx + dy * dy
    if r > 1.0 or y < 2:
        return None
    if r > 0.78:
        return (24, 44, 46)
    sheen = max(0.0, 1.0 - abs((x - y) + 1) / 3.5)
    base = g.mix((30, 74, 80), (70, 150, 150), 1 - (y - 2) / 13)
    if (y % 3 == 0) and r < 0.6:
        base = [v - 18 for v in base]
    c = g.mix(base, (170, 230, 220), sheen * 0.7)
    if (x, y) in ((6, 5), (7, 5)):
        c = (230, 255, 245)
    return c


def key_icon(x, y):
    """The Keeper's key: a ring of bronze round a pearl at the top left, the shaft down to the bit at the bottom right."""
    # the bow: a ring centred at (4.5, 4.5)
    d = math.hypot(x - 4.5, y - 4.5)
    if d <= 1.4:
        return g.mix((250, 246, 255), (190, 205, 235), d / 1.4) if d < 1.0 else (210, 196, 230)
    if 1.4 < d <= 3.4:
        return (64, 120, 104) if d > 2.9 else (98, 168, 140) if (x + y) % 3 else (150, 110, 60)
    # the shaft along the diagonal
    t = (x + y) / 2.0
    off = abs(x - y)
    if 6.0 <= t <= 13.5 and off <= 1:
        return (84, 148, 124) if off == 0 else (52, 104, 90)
    # the bit: teeth below the end of the shaft
    if (x, y) in ((11, 13), (12, 14), (13, 12), (10, 14), (13, 14)):
        return (70, 130, 110)
    return None


def disc_icon(label):
    def fn(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d > 7.4:
            return None
        if d > 6.8:
            return (12, 12, 14)
        if d <= 3.2:
            if d < 0.8:
                return (8, 8, 10)
            return g.mix(label, (255, 255, 255), 0.25) if (x + y) % 4 == 0 else label
        c = (26, 26, 30) if int(d * 2) % 2 else (34, 34, 40)
        if abs((x - 7.5) + (y - 7.5) * 0.4 + 2.5) < 0.7 and d > 3.2:        # the shine
            c = (92, 92, 104)
        return c
    return fn


# ---------------------------------------------------------------- the Keepers' chest
def chest_textures():
    wood, wood_d = (68, 62, 46), (40, 38, 28)

    def planks(x, y, r, across=True):
        u, v = (y, x) if across else (x, y)
        c = wood if u % 4 else wood_d
        c = g.mix(c, (52, 74, 62), 0.25 + 0.15 * ((v * 7 + u * 3) % 5) / 4)       # gone green in the water
        if u % 4 and (v * 5 + u) % 11 == 0:
            c = [k - 12 for k in c]
        if u % 4 == 1 and v in (1, 14):
            c = (120, 120, 112)                                                     # nails
        return g.noise(c, r, 6)
    g.tex('kc_wood', lambda x, y, r: planks(x, y, r))
    g.tex('kc_wood_top', lambda x, y, r: planks(x, y, r, across=False))
    g.tex('kc_band', lambda x, y, r: g.noise(g.bevel(x, y, (72, 128, 110) if (x * 3 + y) % 7 else (150, 112, 62), k=24), r, 8)
          if (x % 5 != 2 or y % 5 != 2) else (176, 150, 96))
    g.tex('kc_barnacle', lambda x, y, r: g.noise((214, 214, 204) if (x + y) % 3 else (150, 152, 140), r, 14)
          if math.hypot(x - 7.5, y - 7.5) < 7.6 else None)
    g.tex('kc_coral', lambda x, y, r: g.noise(g.mix((236, 112, 92), (255, 170, 140), ((x * 7 + y * 3) % 6) / 6), r, 12))
    g.tex('kc_weed', lambda x, y, r: g.noise(g.mix((40, 90, 50), (80, 140, 70), y / 16), r, 10))
    g.tex('kc_pearl', lambda x, y, r: (40, 34, 44) if 6 <= x <= 9 and 6 <= y <= 11 and (x in (7, 8) or y in (7, 8)) else
          g.mix((252, 248, 255), (196, 210, 240), math.hypot(x - 5, y - 4) / 12))
    g.tex('kc_gold', lambda x, y, r: g.noise((255, 236, 150) if (x * 3 + y * 5) % 9 == 0 else
                                             g.mix((236, 186, 70), (190, 136, 40), ((x - 7.5) ** 2 + (y - 7.5) ** 2) % 9 / 9), r, 10))
    g.tex('kc_inside', lambda x, y, r: g.noise((30, 26, 22) if x % 4 else (20, 18, 16), r, 4))


def chest_models():
    t = {"particle": "umbra:block/kc_wood", "wood": "umbra:block/kc_wood", "top": "umbra:block/kc_wood_top", "band": "umbra:block/kc_band",
         "barn": "umbra:block/kc_barnacle", "coral": "umbra:block/kc_coral", "weed": "umbra:block/kc_weed", "pearl": "umbra:block/kc_pearl",
         "gold": "umbra:block/kc_gold", "inside": "umbra:block/kc_inside"}

    def body():
        els = [g.box((1, 1, 2), (15, 9, 14), faces={"north": "wood", "south": "wood", "east": "wood", "west": "wood", "down": "wood", "up": "inside"}),
               g.box((0.5, 0, 1.5), (15.5, 1.2, 14.5), "band")]                                   # the plinth
        for x0 in (2.5, 12.0):                                                                    # bands round the body
            els.append(g.box((x0, 0.5, 1.6), (x0 + 1.5, 9.2, 14.4), "band"))
        els.append(g.box((0.6, 4.0, 1.6), (15.4, 5.0, 14.4), faces={"north": "band", "south": "band", "east": "band", "west": "band"}))
        # barnacles and a shell or two, a strand of weed, all on the sides
        for frm, to in (((15.0, 2, 5), (15.7, 3.2, 6.2)), ((15.0, 6, 9), (15.6, 6.8, 10)), ((15.0, 1.5, 11), (15.5, 2.3, 11.8)),
                        ((0.3, 2.5, 8), (1.0, 3.6, 9.2)), ((0.4, 6.5, 4), (1.0, 7.3, 4.8)), ((5, 1.2, 14.0), (6.2, 2.2, 14.6)),
                        ((10.2, 6.2, 14.0), (11.0, 7.0, 14.5))):
            els.append(g.box(frm, to, "barn"))
        els.append(g.box((14.9, 1.2, 3.0), (15.4, 7.5, 3.6), "weed"))
        els.append(g.box((0.6, 1.2, 12.0), (1.1, 5.5, 12.6), "weed"))
        return els

    def lid(rot):
        els = [g.box((1, 9, 2), (15, 11, 14), faces={"north": "wood", "south": "wood", "east": "wood", "west": "wood", "up": "top", "down": "inside"}, rot=rot),
               g.box((1.5, 11, 2.5), (14.5, 12.4, 13.5), faces={"north": "wood", "south": "wood", "east": "wood", "west": "wood", "up": "top"}, rot=rot),
               g.box((2.6, 12.4, 3.6), (13.4, 13.4, 12.4), faces={"north": "wood", "south": "wood", "east": "wood", "west": "wood", "up": "top"}, rot=rot)]
        for x0 in (2.5, 12.0):
            els += [g.box((x0, 9, 1.6), (x0 + 1.5, 11.2, 14.4), "band", rot=rot),
                    g.box((x0, 11.2, 2.2), (x0 + 1.5, 12.6, 13.8), "band", rot=rot),
                    g.box((x0, 12.6, 3.3), (x0 + 1.5, 13.6, 12.7), "band", rot=rot)]
        els.append(g.box((6.6, 7.0, 14.0), (9.4, 10.4, 14.9), "pearl", rot=None, light=4))           # the lock of pearl (on the body)
        els += [g.box((11.6, 13.4, 9.5), (13.2, 15.6, 11.0), "coral", rot=rot),                       # coral on the lid
                g.box((12.8, 13.4, 10.6), (13.8, 14.8, 11.8), "coral", rot=rot),
                g.box((3.0, 13.4, 4.0), (4.0, 14.2, 5.0), "barn", rot=rot)]
        return els

    shut = body() + lid(None)
    g.model(MB + 'keepers_chest.json', "minecraft:block/block", t, shut, {"render_type": "minecraft:cutout"})
    hinge = {"angle": -45.0, "axis": "x", "origin": [8, 9, 2]}
    opened = body() + lid(hinge)
    opened += [g.box((2, 8.6, 3), (14, 9.6, 13), "gold", light=3),                                    # the hoard
               g.box((5, 9.6, 5), (8, 10.6, 8), "gold", light=3), g.box((9, 9.6, 7), (11, 10.4, 10), "gold", light=3),
               g.box((6.5, 10.6, 6), (7.5, 11.4, 7), "pearl", light=6)]
    g.model(MB + 'keepers_chest_open.json', "minecraft:block/block", t, opened, {"render_type": "minecraft:cutout"})
    variants = {}
    for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for op in ("false", "true"):
            for wl in ("false", "true"):
                v = {"model": "umbra:block/keepers_chest" + ("_open" if op == "true" else "")}
                if y:
                    v["y"] = y
                variants[f"facing={facing},open={op},waterlogged={wl}"] = v
    write(A + 'blockstates/keepers_chest.json', {"variants": variants})


# ---------------------------------------------------------------- data
def data():
    # the lurker leaves a scale or two
    write(D + 'loot_table/entities/lurker.json', {"type": "minecraft:entity", "random_sequence": "umbra:entities/lurker", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "umbra:lurker_scale",
                                   "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}]})
    # the Key of the Depths, now in the middle of the game: four scales of the lurker, echo quartz, a seal's shard
    write(D + 'recipe/depths_key.json', {"type": "minecraft:crafting_shaped", "category": "misc",
                                         "key": {"S": "umbra:lurker_scale", "Q": "umbra:echo_quartz", "H": "umbra:seal_shard"},
                                         "pattern": ["QSQ", "SHS", "QSQ"], "result": {"id": "umbra:depths_key", "count": 1}})
    # each boss leaves its own track on a disc; the Drowned Keeper leaves the key to his chest
    for boss, track in BOSS_LOOT.items():
        p = D + f'loot_table/entities/{boss}.json'
        t = load(p)
        t["pools"] = [pool for pool in t["pools"] if not any(e.get("name", "").startswith("umbra:music_disc_") or e.get("name") == "umbra:keepers_key"
                                                            for e in pool["entries"])]
        t["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"umbra:music_disc_{track}"}]})
        if boss == "drowned_keeper":
            t["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": "umbra:keepers_key"}]})
        write(p, t)
    for track, (length, _) in DISCS.items():
        write(D + f'jukebox_song/{track}.json', {"sound_event": f"umbra:music.boss_{track}",
                                                 "description": {"translate": f"jukebox_song.umbra.{track}"},
                                                 "length_in_seconds": length, "comparator_output": 1 + list(DISCS).index(track)})


LANG = {
    "ru_ru": {
        "item.umbra.lurker_scale": "Чешуя Глубинника",
        "tip.umbra.lurker_scale": "Скользкая и холодная. Четыре таких, осколок печати и эхо-кварц — и вода Изнанки откроется вниз.",
        "item.umbra.keepers_key": "Ключ Хранителя",
        "tip.umbra.keepers_key": "Бронза и жемчуг. Открывает сундук Хранителя в святилище Глубин.",
        "block.umbra.keepers_chest": "Сундук Хранителя",
        "container.umbra.keepers_chest": "Сундук Хранителя",
        "block.umbra.keepers_chest.locked": "Замок из жемчуга не поддаётся. Ключ — у того, кто спит в святилище.",
        "block.umbra.keepers_chest.unlocked": "Ключ провернулся в жемчуге. Крышка поднялась.",
        "journal.umbra.depths_treasure.title": "Сундук Хранителя",
        "journal.umbra.depths_treasure.text": "На помосте святилища — сундук, обросший ракушками, с замком из жемчуга. Руками не открыть и не разбить. Ключ был у Хранителя: после боя он лежал в иле.\n\nКаждый раз, как Хранитель снова падает, сундук снова полон и снова заперт.",
        "message.umbra.depths.enter_hint": "Наверх — тем же ключом: ПКМ в любом месте Глубин.",
        "message.umbra.chest_sealed": "Сундук не открывается: его держит печать.",
    },
    "en_us": {
        "item.umbra.lurker_scale": "Lurker Scale",
        "tip.umbra.lurker_scale": "Slick and cold. Four of them, a seal shard and echo quartz, and the Underside's water opens downwards.",
        "item.umbra.keepers_key": "Keeper's Key",
        "tip.umbra.keepers_key": "Bronze and pearl. Opens the Keeper's chest in the sanctum of the Depths.",
        "block.umbra.keepers_chest": "Keeper's Chest",
        "container.umbra.keepers_chest": "Keeper's Chest",
        "block.umbra.keepers_chest.locked": "The pearl lock will not give. The key is with the one who sleeps in the sanctum.",
        "block.umbra.keepers_chest.unlocked": "The key turns in the pearl. The lid lifts.",
        "journal.umbra.depths_treasure.title": "The Keeper's Chest",
        "journal.umbra.depths_treasure.text": "On the dais of the sanctum stands a chest grown over with shells, its lock a pearl. It will not open by hand and will not break. The Keeper had the key: after the fight it lay in the silt.\n\nEach time the Keeper falls again, the chest is full again, and locked again.",
        "message.umbra.depths.enter_hint": "The way up is the same key: use it anywhere in the Depths.",
        "message.umbra.chest_sealed": "The chest will not open: the seal holds it.",
    },
}
DISC_NAMES = {
    "ru_ru": {"watcher": "Не моргай", "mimic_queen": "Пляска подмен", "shepherd": "Стадо во тьме", "doppelganger": "Зеркальная дуэль",
              "architect": "Механизм лабиринта", "primordial_shadow": "Первая тьма", "drowned_keeper": "Утопленный колокол",
              "fallen_keeper": "Свет, который погас"},
    "en_us": {"watcher": "Do Not Blink", "mimic_queen": "Dance of the Changelings", "shepherd": "A Flock in the Dark",
              "doppelganger": "Mirror Duel", "architect": "Clockwork of the Maze", "primordial_shadow": "The First Dark",
              "drowned_keeper": "The Drowned Bell", "fallen_keeper": "The Light That Went Out"},
}


def lang():
    for code, entries in LANG.items():
        p = A + f'lang/{code}.json'
        t = load(p)
        t.update(entries)
        disc = "Пластинка" if code == "ru_ru" else "Music Disc"
        for track, title in DISC_NAMES[code].items():
            t[f"item.umbra.music_disc_{track}"] = disc
            t[f"jukebox_song.umbra.{track}"] = "Umbra — " + title
        write(p, t)


def main():
    icon('lurker_scale', scale_icon)
    icon('keepers_key', key_icon)
    for track, (_, colour) in DISCS.items():
        icon('music_disc_' + track, disc_icon(colour))
    chest_textures()
    chest_models()
    data()
    lang()
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        tiles = [g.render(MB + 'keepers_chest.json'), g.render(MB + 'keepers_chest_open.json')]
        sheet = Image.new('RGBA', (sum(t.width for t in tiles), tiles[0].height), (40, 44, 52, 255))
        x = 0
        for t in tiles:
            sheet.paste(t, (x, 0), t)
            x += t.width
        icons = ['lurker_scale', 'keepers_key'] + ['music_disc_' + k for k in DISCS]
        strip = Image.new('RGBA', (len(icons) * 72, 72), (40, 44, 52, 255))
        for i, n in enumerate(icons):
            strip.paste(Image.open(TI + n + '.png').resize((64, 64), Image.NEAREST), (i * 72 + 4, 4))
        out = Image.new('RGBA', (max(sheet.width, strip.width), sheet.height + strip.height), (40, 44, 52, 255))
        out.paste(sheet, (0, 0))
        out.paste(strip, (0, sheet.height))
        out.save(sys.argv[2])
    print('ok')


if __name__ == '__main__':
    main()
