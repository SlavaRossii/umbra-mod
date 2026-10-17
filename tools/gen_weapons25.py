#!/usr/bin/env python3
"""2.5: three weapons (luminite spear, shepherd's scythe, shadow dagger) and three weapon enchantments (Radiance,
Clarity, Shadow Cut). Writes icons, item models, recipes, enchantment JSON, tags and lang.
Run from the repo root: python3 tools/gen_weapons25.py"""
import json
import os
import random

from PIL import Image

A = 'src/main/resources/assets/umbra/'
D = 'src/main/resources/data/'
TI = A + 'textures/item/'


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, k):
    return tuple(clamp(x * k) for x in c) + (255,)


def line(px, pts, col, rnd, width=1, light=1.15):
    for i, (x, y) in enumerate(pts):
        k = 0.8 + 0.4 * (i / max(1, len(pts) - 1))
        px[x, y] = shade(col, k * (1 + rnd.uniform(-0.05, 0.05)))
        if width > 1 and 0 <= x + 1 < 16:
            px[x + 1, y] = shade(col, k * 0.75)


def diag(x0, y0, n, dx=1, dy=-1):
    return [(x0 + i * dx, y0 + i * dy) for i in range(n)]


def outline(img):
    """A dark outline around the drawn pixels, like vanilla icons."""
    px = img.load()
    out = img.copy()
    po = out.load()
    for x in range(16):
        for y in range(16):
            if px[x, y][3] == 0:
                for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + ax, y + ay
                    if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3] > 0 and px[nx, ny][:3] != (20, 16, 26):
                        po[x, y] = (20, 16, 26, 255)
                        break
    return out


def spear():
    rnd = random.Random(1)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    line(px, diag(1, 14, 10), (92, 66, 44), rnd)                   # the shaft
    line(px, diag(4, 12, 2), (60, 58, 70), rnd)                    # a grip band
    line(px, diag(10, 5, 2), (70, 58, 92), rnd, 2)                 # the gloom socket
    for (x, y) in diag(11, 4, 4):                                  # the crystal head
        px[x, y] = shade((175, 240, 255), 1.1)
        if y + 1 < 16:
            px[x, y + 1] = shade((90, 180, 225), 1.0)
        if x - 1 >= 0:
            px[x - 1, y] = shade((120, 205, 240), 1.0)
    px[14, 1] = (240, 255, 255, 255)
    return outline(img)


def scythe():
    rnd = random.Random(2)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    line(px, diag(2, 14, 11), (74, 60, 50), rnd)                   # the crook's old shaft
    blade = [(12, 3), (11, 2), (10, 2), (9, 2), (8, 2), (7, 3), (6, 3), (5, 4), (4, 5), (3, 6), (3, 7)]
    for i, (x, y) in enumerate(blade):
        px[x, y] = shade((170, 160, 190), 1.15 - i * 0.03)
        px[x, y + 1] = shade((90, 70, 120), 0.9)
    px[2, 8] = shade((170, 90, 230), 1.0)                          # a soul still caught on its tip
    px[13, 2] = shade((60, 50, 80), 1.0)
    return outline(img)


def dagger():
    rnd = random.Random(3)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    line(px, diag(3, 12, 3), (40, 30, 56), rnd)                    # the wrapped grip
    for x, y in ((5, 11), (6, 10), (4, 9), (7, 12)):               # the guard
        px[x, y] = shade((130, 80, 180), 1.0)
    for i, (x, y) in enumerate(diag(6, 9, 6)):                     # the blade of shadow
        px[x, y] = shade((62, 48, 84), 1.3 - i * 0.05)
        if y + 1 < 16:
            px[x, y + 1] = shade((34, 26, 48), 1.0)
    px[12, 3] = shade((180, 140, 230), 1.0)
    return outline(img)


WEAPONS = {
    "luminite_spear": (spear, ("Люминитовое копьё", "Luminite Spear"),
                       ("Достаёт дальше. Твари Изнанки боятся его жала.", "Reaches further. The things of the Underside fear its sting."),
                       {"type": "minecraft:crafting_shaped", "category": "equipment",
                        "key": {"L": "umbra:luminite_crystal", "G": "umbra:gloom_ingot", "S": "minecraft:stick"},
                        "pattern": ["  L", " G ", "S  "], "result": {"id": "umbra:luminite_spear", "count": 1}}),
    "shepherd_scythe": (scythe, ("Коса Пастуха", "Shepherd's Scythe"),
                        ("Пастух собирал ей стадо. Теперь она жнёт его.", "The Shepherd gathered his flock with it. Now it reaps them."),
                        {"type": "minecraft:crafting_shaped", "category": "equipment",
                         "key": {"G": "umbra:gloom_ingot", "Q": "umbra:echo_quartz", "C": "umbra:shepherd_crook"},
                         "pattern": ["GGQ", " C "], "result": {"id": "umbra:shepherd_scythe", "count": 1}}),
    "shadow_dagger": (dagger, ("Теневой кинжал", "Shadow Dagger"),
                      ("Быстр. Тот, кто не видит удара, получает его дважды.", "Quick. The one who does not see it coming takes it twice."),
                      {"type": "minecraft:crafting_shaped", "category": "equipment",
                       "key": {"S": "umbra:shadow_shard", "I": "minecraft:stick"},
                       "pattern": ["S", "S", "I"], "result": {"id": "umbra:shadow_dagger", "count": 1}}),
}

ENCHANTS = {
    # id: (max level, weight, min base, min per, max base, max per, anvil, ru name, en name, ru desc, en desc)
    "radiance": (3, 5, 10, 10, 40, 10, 2, "Сияние", "Radiance",
                 "Клинок жжёт существ Изнанки сильнее.", "The blade burns the things of the Underside harder."),
    "clarity": (2, 3, 14, 12, 44, 12, 4, "Ясность", "Clarity",
                "Каждое убитое существо Изнанки возвращает немного рассудка.", "Every creature of the Underside you kill gives back a little of your mind."),
    "shadow_cut": (2, 2, 18, 14, 50, 14, 4, "Теневой разрез", "Shadow Cut",
                   "Иногда твоя тень бьёт следом из-за спины цели.", "Sometimes your shadow strikes again from behind the target."),
}


def merge_tag(path, values):
    j = json.load(open(path)) if os.path.exists(path) else {"values": []}
    for v in values:
        if v not in j["values"]:
            j["values"].append(v)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(j, open(path, 'w'), indent=2)


def main():
    lang = {"ru_ru.json": {}, "en_us.json": {}}
    for name, (draw, (ru, en), (tru, ten), recipe) in WEAPONS.items():
        draw().save(TI + name + '.png')
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "umbra:item/" + name}}, open(A + 'models/item/%s.json' % name, 'w'), indent=2)
        json.dump({"model": {"type": "minecraft:model", "model": "umbra:item/" + name}}, open(A + 'items/%s.json' % name, 'w'), indent=2)
        json.dump(recipe, open(D + 'umbra/recipe/%s.json' % name, 'w'), indent=2)
        lang["ru_ru.json"]["item.umbra." + name] = ru
        lang["en_us.json"]["item.umbra." + name] = en
        lang["ru_ru.json"]["tip.umbra." + name] = tru
        lang["en_us.json"]["tip.umbra." + name] = ten
    ids = ["umbra:" + n for n in WEAPONS]
    for t in ("minecraft/tags/item/swords", "minecraft/tags/item/enchantable/sword", "minecraft/tags/item/enchantable/weapon",
              "minecraft/tags/item/enchantable/sharp_weapon", "minecraft/tags/item/enchantable/durability",
              "minecraft/tags/item/enchantable/fire_aspect", "umbra/tags/item/enchantable/melee"):
        merge_tag(D + t + '.json', ids)
    for e, (mx, w, mb, mp, xb, xp, anvil, ru, en, dru, den) in ENCHANTS.items():
        json.dump({"anvil_cost": anvil, "description": {"translate": "enchantment.umbra." + e},
                   "effects": {}, "max_cost": {"base": xb, "per_level_above_first": xp}, "max_level": mx,
                   "min_cost": {"base": mb, "per_level_above_first": mp}, "slots": ["mainhand"],
                   "supported_items": "#umbra:enchantable/melee", "weight": w},
                  open(D + 'umbra/enchantment/%s.json' % e, 'w'), indent=2)
        lang["ru_ru.json"]["enchantment.umbra." + e] = ru
        lang["en_us.json"]["enchantment.umbra." + e] = en
        lang["ru_ru.json"]["enchantment.umbra.%s.desc" % e] = dru
        lang["en_us.json"]["enchantment.umbra.%s.desc" % e] = den
    for t in ("minecraft/tags/enchantment/in_enchanting_table", "minecraft/tags/enchantment/tradeable"):
        merge_tag(D + t + '.json', ["umbra:" + e for e in ENCHANTS])
    for f, add in lang.items():
        p = A + 'lang/' + f
        d = json.load(open(p))
        d.update(add)
        json.dump(d, open(p, 'w'), ensure_ascii=False, indent=2)
    print('weapons ok')


if __name__ == '__main__':
    main()
