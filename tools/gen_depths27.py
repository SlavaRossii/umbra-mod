#!/usr/bin/env python3
"""2.7: the drowned halls. Models and textures of the valve, the bell and the Keepers' lamp; the acolyte and the
Drowned Keeper (recoloured from the weeper and the citadel keeper), their eggs and loot; the caches' loot; lang.
Run from the repo root: python3 tools/gen_depths27.py"""
import colorsys
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import gen_blocks23 as g  # noqa: E402

A = 'src/main/resources/assets/umbra/'
D = 'src/main/resources/data/umbra/'
TB = A + 'textures/block/'
MB = A + 'models/block/'


def write(path, obj):
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def load(path):
    return json.load(open(path))


# ---------------------------------------------------------------- textures
def textures():
    rust = (92, 74, 62)
    g.tex('depths_iron', lambda x, y, r: g.noise(g.bevel(x, y, rust, k=22), r, 10) if (x * 7 + y * 3) % 11 else (70, 110, 100, 255))
    verd = (70, 128, 112)
    g.tex('depths_bronze', lambda x, y, r: g.noise(g.bevel(x, y, verd if (x + y * 2) % 9 else (150, 110, 60), k=26), r, 8))
    g.tex('depths_lamp_dark', lambda x, y, r: (30, 40, 48, 255) if 3 <= x <= 12 and 3 <= y <= 12 else g.noise(g.bevel(x, y, (60, 64, 70), k=20), r, 6))
    g.tex('depths_lamp_lit', lambda x, y, r: (list(g.mix((255, 255, 255), (120, 230, 255), (abs(x - 7.5) + abs(y - 7.5)) / 9)) + [255])
          if 3 <= x <= 12 and 3 <= y <= 12 else g.noise(g.bevel(x, y, (60, 64, 70), k=20), r, 6))


# ---------------------------------------------------------------- models
def models():
    iron = {"particle": "umbra:block/depths_iron", "iron": "umbra:block/depths_iron"}
    for on in (False, True):
        a = 45 if on else 0
        els = [g.box((7, 0, 7), (9, 8, 9), "iron"),                                             # the pipe
               g.box((2, 7, 7), (14, 9, 9), "iron", rot={"angle": a, "axis": "z", "origin": [8, 8, 8]} if a else None),
               g.box((7, 2, 7), (9, 14, 9), "iron", rot={"angle": a, "axis": "z", "origin": [8, 8, 8]} if a else None),
               g.box((3, 3, 7.5), (13, 4, 8.5), "iron"), g.box((3, 12, 7.5), (13, 13, 8.5), "iron"),
               g.box((3, 4, 7.5), (4, 12, 8.5), "iron"), g.box((12, 4, 7.5), (13, 12, 8.5), "iron")]
        g.model(MB + f'depths_valve{"_on" if on else ""}.json', "minecraft:block/block", iron, els)
    bronze = {"particle": "umbra:block/depths_bronze", "b": "umbra:block/depths_bronze"}
    for on in (False, True):
        els = [g.box((4, 4, 4), (12, 13, 12), "b"), g.box((3, 2, 3), (13, 4, 13), "b"), g.box((6, 13, 6), (10, 15, 10), "b"),
               g.box((7, 15, 7), (9, 16, 9), "b"), g.box((7, 0.5 if on else 1, 7), (9, 2, 9), "b", light=8 if on else 0)]
        g.model(MB + f'depths_bell{"_on" if on else ""}.json', "minecraft:block/block", bronze, els)
    for on in (False, True):
        t = {"particle": "umbra:block/depths_iron", "frame": "umbra:block/depths_iron", "glass": f"umbra:block/depths_lamp_{'lit' if on else 'dark'}"}
        els = [g.box((3, 0, 3), (13, 2, 13), "frame"), g.box((3, 12, 3), (13, 14, 13), "frame"),
               g.box((4, 2, 4), (12, 12, 12), "glass", light=15 if on else 0, shade=not on),
               g.box((3, 2, 3), (4, 12, 4), "frame"), g.box((12, 2, 3), (13, 12, 4), "frame"),
               g.box((3, 2, 12), (4, 12, 13), "frame"), g.box((12, 2, 12), (13, 12, 13), "frame")]
        g.model(MB + f'depths_lamp{"_on" if on else ""}.json', "minecraft:block/block", t, els)
    for name in ("depths_valve", "depths_bell", "depths_lamp"):
        variants = {}
        for on in ("false", "true"):
            for wl in ("false", "true"):
                variants[f"on={on},waterlogged={wl}"] = {"model": f"umbra:block/{name}{'_on' if on == 'true' else ''}"}
        write(A + f'blockstates/{name}.json', {"variants": variants})


# ---------------------------------------------------------------- creatures
def recolour(src, dst, fn):
    img = Image.open(src).convert('RGBA')
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, gg, b, a = px[x, y]
            if a:
                px[x, y] = fn(r, gg, b, a)
    img.save(dst)


def drowned(r, gg, b, a):
    h, s, v = colorsys.rgb_to_hsv(r / 255, gg / 255, b / 255)
    if v > 0.85 and s < 0.3:                          # pale eyes and highlights: a dead green glow
        return (150, 240, 210, a)
    nh = 0.47 + (h - 0.5) * 0.1
    nr, ng, nb = colorsys.hsv_to_rgb(nh % 1.0, min(1.0, s * 0.8 + 0.25), v * 0.85)
    return (int(nr * 255), int(ng * 255), int(nb * 255), a)


def creatures():
    # 2.7.1: the acolyte and the Drowned Keeper have their own models and textures now (tools/models.py)
    for name, src in (("acolyte", "weeper"), ("drowned_keeper", "citadel_keeper")):
        egg = A + f'textures/item/{src}_spawn_egg.png'
        if not os.path.exists(egg):
            egg = A + 'textures/item/weeper_spawn_egg.png'
        recolour(egg, A + f'textures/item/{name}_spawn_egg.png', drowned)
        write(A + f'models/item/{name}_spawn_egg.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"umbra:item/{name}_spawn_egg"}})
        write(A + f'items/{name}_spawn_egg.json', {"model": {"type": "minecraft:model", "model": f"umbra:item/{name}_spawn_egg"}})


# ---------------------------------------------------------------- loot
def item(name, lo=1, hi=1, weight=None):
    e = {"type": "minecraft:item", "name": name}
    if weight:
        e["weight"] = weight
    if hi > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def potion(weight):
    return {"type": "minecraft:item", "name": "minecraft:potion", "weight": weight,
            "modifier": [{"type": "minecraft:set_potion", "id": "minecraft:long_water_breathing"}]}


def loot():
    write(D + 'loot_table/entities/acolyte.json', {"type": "minecraft:entity", "random_sequence": "umbra:entities/acolyte", "pools": [
        {"rolls": 1, "entries": [item("umbra:shadow_shard")], "condition": {"type": "minecraft:random_chance", "chance": 0.35}},
        {"rolls": 1, "entries": [item("umbra:echo_quartz")], "condition": {"type": "minecraft:random_chance", "chance": 0.2}}]})
    write(D + 'loot_table/entities/drowned_keeper.json', {"type": "minecraft:entity", "random_sequence": "umbra:entities/drowned_keeper", "pools": [
        {"rolls": 1, "entries": [item("umbra:seal_shard", 2, 3)]},
        {"rolls": 1, "entries": [item("umbra:echo_quartz", 6, 10)]},
        {"rolls": 1, "entries": [item("umbra:luminite_block", 2, 4)]},
        {"rolls": 1, "entries": [potion(1)]}]})
    write(D + 'loot_table/chests/depths_cache.json', {"type": "minecraft:chest", "random_sequence": "umbra:chests/depths_cache", "pools": [
        {"rolls": 1, "entries": [potion(1)]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            item("umbra:echo_quartz", 2, 5, 5), item("umbra:luminite_crystal", 3, 8, 5), item("umbra:gloom_ingot", 2, 4, 4),
            item("umbra:seal_shard", 1, 1, 1), potion(3)]}]})
    t = load(D + 'loot_table/chests/depths.json')
    if not any(e.get("name") == "minecraft:potion" for p in t["pools"] for e in p["entries"]):
        t["pools"].append({"rolls": 1, "entries": [potion(1)]})
    write(D + 'loot_table/chests/depths.json', t)


LANG = {
    "ru_ru": {
        "block.umbra.depths_valve": "Вентиль шлюза",
        "block.umbra.depths_bell": "Колокол Глубин",
        "block.umbra.depths_lamp": "Лампа Хранителей",
        "block.umbra.depths_valve.turned": "Вентиль повернулся. Где-то поднялась решётка (%s из %s).",
        "block.umbra.depths_valve.all": "Последняя решётка поднялась. Путь в святилище открыт (%s из %s).",
        "block.umbra.depths_valve.done": "Вентиль уже повёрнут до упора.",
        "block.umbra.depths_bell.rung": "Колокол гудит сквозь воду (%s из %s).",
        "block.umbra.depths_lamp.lit": "Лампа вспыхнула. Под водой стало светло и спокойнее.",
        "block.umbra.depths_lamp.dark": "Лампа погасла давно. Ей нужен кристалл люминита.",
        "message.umbra.depths.enter": "Вода смыкается над головой. Ключ даёт один долгий вдох — дальше дыши, как сумеешь.",
        "message.umbra.depths.wake": "Четвёртый колокол затих. В святилище что-то открыло глаза.",
        "entity.umbra.acolyte": "Утопленник-послушник",
        "entity.umbra.drowned_keeper": "Утопленный Хранитель",
        "item.umbra.acolyte_spawn_egg": "Яйцо призыва утопленника-послушника",
        "item.umbra.drowned_keeper_spawn_egg": "Яйцо призыва утопленного Хранителя",
        "journal.umbra.depths.text": "Под чёрной водой — залы Хранителей, затопленные до самого свода. Воздуха там нет: ключ даёт один вдох, а дальше — зелья дыхания. Лампы погасли, решётки опущены, а в конце спит тот, кто остался сторожить.",
        "journal.umbra.acolyte.title": "Утопленник-послушник",
        "journal.umbra.acolyte.text": "Они остались с Хранителями, когда залы затопили, и так и не ушли. Хватают и тянут на дно.",
        "journal.umbra.acolyte.look": "Тонкая фигура в мокром балахоне, глаза светятся мёртвой зеленью.",
        "journal.umbra.acolyte.weak": "Свет заставляет их отпустить и отплыть. Держи фонарь или зажги лампу рядом.",
        "journal.umbra.drowned_keeper.title": "Утопленный Хранитель",
        "journal.umbra.drowned_keeper.text": "Последний из Хранителей. Он затопил залы, чтобы запереть тьму, и запер с ней себя. Четыре колокола будят его.",
        "journal.umbra.drowned_keeper.look": "Огромная фигура в позеленевшей броне; вокруг неё вода закручивается воронкой.",
        "journal.umbra.drowned_keeper.weak": "Он отказался от света и не выносит его: пока в святилище не горят хотя бы две лампы, удары проходят наполовину. Зажги их кристаллами до того, как звонить.",
        "journal.umbra.weak_drowned_keeper.title": "Как одолеть Утопленного Хранителя",
        "journal.umbra.weak_drowned_keeper.text": "Зажги лампы святилища — в темноте он почти неуязвим. Воронка тянет к нему, вода взрывается там, где стоишь. На второй трети он мутит воду и зовёт послушников, на последней — бросается.",
        "journal.umbra.depths_sluice.title": "Шлюзы",
        "journal.umbra.depths_sluice.text": "Три вентиля, три решётки перед святилищем. Каждый поворот поднимает одну. Вентили спрятаны в боковых залах — у колонн и за северным перекрёстком.",
        "journal.umbra.depths_bells.title": "Колокола Глубин",
        "journal.umbra.depths_bells.text": "Четыре колокола: два у галереи, один на севере, один в склепе. Когда прозвонят все — проснётся тот, кто спит в святилище.",
        "journal.umbra.depths_lamps.title": "Лампы Хранителей",
        "journal.umbra.depths_lamps.text": "Лампы погасли, но кристалл зажигает их снова. Рядом с горящей лампой рассудок возвращается, а под водой видно дальше.",
        "journal.umbra.depths_note_1.title": "Надпись у колокола",
        "journal.umbra.depths_note_1.text": "«Мы звоним, чтобы он спал. Если колокола замолчат все разом — бегите».",
        "journal.umbra.depths_note_2.title": "Надпись у вентиля",
        "journal.umbra.depths_note_2.text": "«Открыли шлюзы на себя. Вода пришла быстрее, чем тьма. Это и был план».",
        "journal.umbra.depths_note_3.title": "Надпись в святилище",
        "journal.umbra.depths_note_3.text": "«Я погасил свои лампы, чтобы тьма приняла меня за своего. Зажги их — и я вспомню, кем был».",
    },
    "en_us": {
        "block.umbra.depths_valve": "Sluice Valve",
        "block.umbra.depths_bell": "Bell of the Depths",
        "block.umbra.depths_lamp": "Keepers' Lamp",
        "block.umbra.depths_valve.turned": "The valve turns. Somewhere a grate rises (%s of %s).",
        "block.umbra.depths_valve.all": "The last grate rises. The way to the sanctum is open (%s of %s).",
        "block.umbra.depths_valve.done": "The valve is already turned as far as it goes.",
        "block.umbra.depths_bell.rung": "The bell hums through the water (%s of %s).",
        "block.umbra.depths_lamp.lit": "The lamp flares. The water is lighter, and calmer.",
        "block.umbra.depths_lamp.dark": "The lamp went out long ago. It wants a luminite crystal.",
        "message.umbra.depths.enter": "The water closes over your head. The key gives one long breath; after that, breathe as you can.",
        "message.umbra.depths.wake": "The fourth bell falls silent. In the sanctum something opens its eyes.",
        "entity.umbra.acolyte": "Drowned Acolyte",
        "entity.umbra.drowned_keeper": "Drowned Keeper",
        "item.umbra.acolyte_spawn_egg": "Drowned Acolyte Spawn Egg",
        "item.umbra.drowned_keeper_spawn_egg": "Drowned Keeper Spawn Egg",
        "journal.umbra.depths.text": "Under the black water lie the Keepers' halls, drowned to the vault. There is no air there: the key gives one breath, then it is potions of water breathing. The lamps are out, the grates down, and at the end sleeps the one who stayed on guard.",
        "journal.umbra.acolyte.title": "Drowned Acolyte",
        "journal.umbra.acolyte.text": "They stayed with the Keepers when the halls were flooded, and never left. They take hold and drag you down.",
        "journal.umbra.acolyte.look": "A thin figure in a wet robe, eyes lit a dead green.",
        "journal.umbra.acolyte.weak": "Light makes them let go and swim off. Hold a lantern, or light a lamp near you.",
        "journal.umbra.drowned_keeper.title": "Drowned Keeper",
        "journal.umbra.drowned_keeper.text": "The last of the Keepers. He flooded the halls to lock the dark in, and locked himself in with it. Four bells wake him.",
        "journal.umbra.drowned_keeper.look": "A huge figure in armour gone green; the water twists into a funnel round it.",
        "journal.umbra.drowned_keeper.weak": "He gave up the light and cannot bear it: until at least two lamps burn in the sanctum, blows land by half. Light them with crystals before you ring.",
        "journal.umbra.weak_drowned_keeper.title": "How to beat the Drowned Keeper",
        "journal.umbra.weak_drowned_keeper.text": "Light the sanctum's lamps: in the dark he is all but untouchable. The whirlpool draws you in, the water bursts where you stand. In his second third he clouds the water and calls acolytes, in the last he rushes.",
        "journal.umbra.depths_sluice.title": "The Sluices",
        "journal.umbra.depths_sluice.text": "Three valves, three grates before the sanctum. Each turn lifts one. The valves are in the side halls: by the pillars and past the northern crossing.",
        "journal.umbra.depths_bells.title": "Bells of the Depths",
        "journal.umbra.depths_bells.text": "Four bells: two by the gallery, one to the north, one in the crypt. When all have rung, the one asleep in the sanctum wakes.",
        "journal.umbra.depths_lamps.title": "The Keepers' Lamps",
        "journal.umbra.depths_lamps.text": "The lamps are out, but a crystal lights them again. Near a burning lamp the mind comes back, and under water you see further.",
        "journal.umbra.depths_note_1.title": "Words by the Bell",
        "journal.umbra.depths_note_1.text": "\"We ring so that he sleeps. If all the bells go quiet at once, run.\"",
        "journal.umbra.depths_note_2.title": "Words by the Valve",
        "journal.umbra.depths_note_2.text": "\"We opened the sluices on ourselves. The water came faster than the dark. That was the plan.\"",
        "journal.umbra.depths_note_3.title": "Words in the Sanctum",
        "journal.umbra.depths_note_3.text": "\"I put out my lamps so the dark would take me for its own. Light them, and I will remember who I was.\"",
    },
}


def lang():
    for code, entries in LANG.items():
        path = A + f"lang/{code}.json"
        t = load(path)
        t.update(entries)
        write(path, t)


if __name__ == '__main__':
    textures()
    models()
    creatures()
    loot()
    lang()
    print("ok")
