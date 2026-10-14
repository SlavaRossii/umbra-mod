"""2.0 data: enchantments, their tags, artifact recipes and icons, enchanted books in loot, lang. Run from the repo root."""
import json
import os
from PIL import Image

RES = "src/main/resources"
A = RES + "/assets/umbra"
D = RES + "/data/umbra"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(json.dumps(obj, indent=2, ensure_ascii=False) + "\n")


# --- item tags for what the enchantments fit
write(f"{D}/tags/item/enchantable/melee.json", {"values": ["#minecraft:swords", "#minecraft:axes"]})
write(f"{D}/tags/item/enchantable/boots.json", {"values": ["#minecraft:foot_armor"]})
write(f"{D}/tags/item/enchantable/helmets.json", {"values": ["#minecraft:head_armor"]})
write(f"{D}/tags/item/enchantable/leggings.json", {"values": ["#minecraft:leg_armor"]})
write(f"{D}/tags/item/enchantable/bows.json", {"values": ["minecraft:bow", "minecraft:crossbow"]})
write(f"{D}/tags/item/enchantable/armor.json", {"values": ["#minecraft:head_armor", "#minecraft:chest_armor", "#minecraft:leg_armor", "#minecraft:foot_armor"]})
write(f"{D}/tags/item/enchantable/cursable.json", {"values": ["#umbra:enchantable/armor", "#umbra:enchantable/melee", "#umbra:enchantable/bows"]})

# name: (tag, slots, max level, weight, base cost, per level, anvil, ru, en, ru effect, en effect)
E = {
    "echo": ("melee", ["mainhand"], 3, 5, 12, 9, 4, "Эхо", "Echo",
             "Удар повторяется через полсекунды ослабленным эхом.", "A blow sounds again half a second later, weaker."),
    "silence": ("boots", ["feet"], 2, 5, 8, 10, 4, "Тишина", "Silence",
                "Шаги тише: слепцы и шаги за спиной замечают тебя хуже.", "Quieter steps: the blind and the steps behind you notice you less."),
    "void": ("armor", ["armor"], 3, 4, 10, 10, 4, "Пустота", "Void",
             "Тьма медленнее истощает рассудок.", "The dark drains your mind more slowly."),
    "harvest": ("melee", ["mainhand"], 3, 5, 10, 9, 4, "Жатва", "Harvest",
                "Существа Изнанки оставляют больше осколков, кварца и кристаллов.", "Creatures of the Underside leave more shards, quartz and crystals."),
    "taste_of_darkness": ("melee", ["mainhand"], 2, 3, 15, 12, 6, "Вкус тьмы", "Taste of Darkness",
                          "Убийство существа Изнанки лечит, но отнимает немного рассудка.", "Killing a creature of the Underside heals you, at a small cost to your mind."),
    "flash": ("bows", ["mainhand"], 3, 5, 12, 9, 4, "Вспышка", "Flash",
              "Стрела может вспыхнуть при попадании и оглушить существо тьмы.", "An arrow may flare on impact and stun a creature of the dark."),
    "anchor": ("leggings", ["legs"], 2, 4, 10, 12, 4, "Якорь", "Anchor",
               "Тебя труднее оттолкнуть: боссы и глубинники не сдвинут с места.", "Harder to push around: bosses and lurkers cannot shift you."),
    "instinct": ("helmets", ["head"], 2, 4, 14, 12, 4, "Чутьё", "Instinct",
                 "Существа тьмы светятся сквозь стены, подражатели тоже.", "Creatures of the dark glow through walls, mimickers too."),
    "whisper_curse": ("cursable", ["any"], 1, 1, 25, 25, 8, "Проклятие шёпота", "Curse of Whispers",
                      "Шёпот шепчет всё время и понемногу отнимает рассудок.", "The whisper never stops and slowly drains your mind."),
}
for name, (tag, slots, mx, wt, base, per, anvil, ru, en, rue, ene) in E.items():
    effects = {}
    if name == "anchor":
        effects["minecraft:attributes"] = [{
            "id": "umbra:enchantment.anchor", "attribute": "minecraft:knockback_resistance", "operation": "add_value",
            "amount": {"type": "minecraft:linear", "base": 0.3, "per_level_above_first": 0.3}}]
    write(f"{D}/enchantment/{name}.json", {
        "anvil_cost": anvil,
        "description": {"translate": f"enchantment.umbra.{name}"},
        "effects": effects,
        "max_cost": {"base": base + 30, "per_level_above_first": per},
        "max_level": mx,
        "min_cost": {"base": base, "per_level_above_first": per},
        "slots": slots,
        "supported_items": f"#umbra:enchantable/{tag}",
        "weight": wt,
    })

curses = ["umbra:whisper_curse"]
normal = [f"umbra:{n}" for n in E if n != "whisper_curse"]
write(f"{RES}/data/minecraft/tags/enchantment/curse.json", {"values": curses})
write(f"{RES}/data/minecraft/tags/enchantment/in_enchanting_table.json", {"values": normal})
write(f"{RES}/data/minecraft/tags/enchantment/tradeable.json", {"values": normal})

# books in the loot of the Underside
entries = [{"type": "minecraft:empty", "weight": 7}]
for n, v in E.items():
    lvl = 1 if v[2] == 1 else 2
    entries.append({"type": "minecraft:item", "name": "minecraft:enchanted_book", "weight": 2 if n != "whisper_curse" else 1,
                    "modifier": [{"type": "minecraft:set_enchantments", "enchantments": {f"umbra:{n}": lvl}}]})
write(f"{D}/loot_table/chests/umbra_books.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": entries}]})
write(f"{D}/loot_modifiers/umbra_books.json", {
    "type": "neoforge:add_table",
    "condition": {"type": "minecraft:any_of", "terms": [{"type": "neoforge:loot_table_id", "loot_table_id": t} for t in (
        "umbra:chests/citadel_library", "umbra:chests/citadel_secret", "umbra:chests/maze_prize", "umbra:chests/sunken_temple",
        "umbra:chests/lighthouse_house", "umbra:chests/mimic_hoard")]},
    "table": "umbra:chests/umbra_books"})

# artifacts
write(f"{D}/recipe/ash_clasp.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
      "key": {"S": "umbra:shadow_shard", "G": "umbra:gloom_ingot", "Q": "umbra:echo_quartz"},
      "pattern": [" S ", "GQG", " S "], "result": {"id": "umbra:ash_clasp"}})
for n in ("ash_clasp", "architect_seal"):
    write(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"umbra:item/{n}"}})
    write(f"{A}/items/{n}.json", {"model": {"type": "minecraft:model", "model": f"umbra:item/{n}"}})


def icon(name, cells, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(cells):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = palette[ch]
    img.save(f"{A}/textures/item/{name}.png")


icon("ash_clasp", [
    "................",
    "................",
    "....oooooooo....",
    "...ogggggggro...",
    "..ogllllllllgo..",
    "..oglooooooglo..",
    "..ogloaaaaoglo..",
    "..ogloaggaoglo..",
    "..ogloaaaaoglo..",
    "..oglooooooglo..",
    "..ogllllllllgo..",
    "...ogggggggro...",
    "....oooooooo....",
    ".......pp.......",
    "......pppp......",
    "................"],
     {"o": (24, 22, 28, 255), "g": (92, 88, 100, 255), "l": (150, 146, 156, 255), "r": (70, 66, 78, 255), "a": (60, 56, 66, 255), "p": (120, 90, 60, 255)})
icon("architect_seal", [
    "................",
    ".....oooooo.....",
    "...ooddddddoo...",
    "..odddmmmmdddo..",
    ".oddmmwwwwmmddo.",
    ".odmmwggggwmmdo.",
    ".odmwgllllgwmdo.",
    ".odmwglwwlgwmdo.",
    ".odmwglwwlgwmdo.",
    ".odmwgllllgwmdo.",
    ".odmmwggggwmmdo.",
    ".oddmmwwwwmmddo.",
    "..odddmmmmdddo..",
    "...ooddddddoo...",
    ".....oooooo.....",
    "................"],
     {"o": (14, 8, 20, 255), "d": (44, 30, 66, 255), "m": (84, 56, 128, 255), "w": (150, 110, 210, 255), "g": (196, 150, 255, 255), "l": (238, 222, 255, 255)})

# lang
ru = {}
en = {}
for n, v in E.items():
    ru[f"enchantment.umbra.{n}"] = v[7]
    en[f"enchantment.umbra.{n}"] = v[8]
    ru[f"enchantment.umbra.{n}.desc"] = v[9]
    en[f"enchantment.umbra.{n}.desc"] = v[10]
ru.update({
    "item.umbra.ash_clasp": "Пепельная застёжка", "item.umbra.architect_seal": "Печать Архитектора",
    "artifact.umbra.ash_clasp": "Артефакт: пепельная застёжка", "artifact.umbra.architect_seal": "Артефакт: печать Архитектора",
    "artifact.umbra.ash_clasp.effect": "На нагруднике: тихие шаги, нет урона от падения, буря не слепит.",
    "artifact.umbra.architect_seal.effect": "На нагруднике: видишь сквозь призрачные стены, лабиринт не перестраивается.",
    "artifact.umbra.how": "Скрепляется с нагрудником на наковальне.",
})
en.update({
    "item.umbra.ash_clasp": "Ash Clasp", "item.umbra.architect_seal": "Architect's Seal",
    "artifact.umbra.ash_clasp": "Artifact: Ash Clasp", "artifact.umbra.architect_seal": "Artifact: Architect's Seal",
    "artifact.umbra.ash_clasp.effect": "On a chestplate: quiet steps, no fall damage, storms do not blind.",
    "artifact.umbra.architect_seal.effect": "On a chestplate: see through phantom walls; the maze stops reshuffling.",
    "artifact.umbra.how": "Pin it to a chestplate at an anvil.",
})
for fn, d in (("ru_ru", ru), ("en_us", en)):
    path = f"{A}/lang/{fn}.json"
    cur = json.load(open(path, encoding="utf-8"))
    cur.update(d)
    write(path, cur)
print("ok")
