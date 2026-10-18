"""2.0 armour sets: recolours the gloom icons and layers into the luminite and shadow sets, adds tier pips,
and writes the model/item/recipe/tag/lang files for all 36 pieces. Run from the repo root."""
import colorsys
import json
import os
from PIL import Image

RES = "src/main/resources"
A = RES + "/assets/umbra"
D = RES + "/data/umbra"
PIECES = ["helmet", "chestplate", "leggings", "boots"]
SETS = {
    # name: (hue shift, saturation, value, ru adjective by piece, en name)
    "gloom": None,
    "luminite": (-0.25, 0.85, 1.45),
    "shadow": (0.0, 0.18, 0.62),
}
RU = {
    "gloom": ["Сумрачный шлем", "Сумрачный нагрудник", "Сумрачные поножи", "Сумрачные ботинки"],
    "luminite": ["Люминитовый шлем", "Люминитовый нагрудник", "Люминитовые поножи", "Люминитовые ботинки"],
    "shadow": ["Теневой шлем", "Теневой нагрудник", "Теневые поножи", "Теневые ботинки"],
}
EN = {
    "gloom": ["Gloom Helmet", "Gloom Chestplate", "Gloom Leggings", "Gloom Boots"],
    "luminite": ["Luminite Helmet", "Luminite Chestplate", "Luminite Leggings", "Luminite Boots"],
    "shadow": ["Shadow Helmet", "Shadow Chestplate", "Shadow Leggings", "Shadow Boots"],
}
ROMAN = {1: "", 2: " II", 3: " III"}


def recolor(img, spec):
    if spec is None:
        return img.copy()
    dh, ds, dv = spec
    out = img.copy()
    px = out.load()
    for x in range(out.width):
        for y in range(out.height):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h = (h + dh) % 1.0
            s = min(1.0, s * ds)
            v = min(1.0, v * dv)
            r2, g2, b2 = colorsys.hsv_to_rgb(h, s, v)
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    return out


def pips(img, tier):
    img = img.copy()
    px = img.load()
    colors = {2: (255, 214, 90, 255), 3: (120, 255, 255, 255)}
    if tier >= 2:
        for i in range(tier):
            px[14 - i * 2, 1] = colors[tier]
            px[14 - i * 2, 2] = colors[tier]
    return img


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(json.dumps(obj, indent=2, ensure_ascii=False) + "\n")


names = {}
for s, spec in SETS.items():
    # armour layers (same for all tiers)
    if s != "gloom":
        for sub in ("humanoid", "humanoid_leggings"):
            src = Image.open(f"{A}/textures/entity/equipment/{sub}/gloom.png").convert("RGBA")
            recolor(src, spec).save(f"{A}/textures/entity/equipment/{sub}/{s}.png")
        layer = {"humanoid": [{"texture": f"umbra:{s}"}], "humanoid_leggings": [{"texture": f"umbra:{s}"}], "humanoid_baby": [{"texture": f"umbra:{s}"}]}
        write(f"{A}/equipment/{s}.json", {"layers": layer})
    for pi, piece in enumerate(PIECES):
        base = Image.open(f"{A}/textures/item/gloom_{piece}.png").convert("RGBA")
        base = recolor(base, spec)
        for tier in (1, 2, 3):
            name = f"{s}_{piece}" + ("" if tier == 1 else f"_{tier}")
            if not (s == "gloom" and tier == 1):
                pips(base, tier).save(f"{A}/textures/item/{name}.png")
                write(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"umbra:item/{name}"}})
                write(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"umbra:item/{name}"}})
            names[name] = (RU[s][pi] + ROMAN[tier], EN[s][pi] + ROMAN[tier])

# recipes: tier I crafted, II and III by smithing (echo quartz as the template)
CRAFT = {"helmet": ["XXX", "X X"], "chestplate": ["X X", "XXX", "XXX"], "leggings": ["XXX", "X X", "X X"], "boots": ["X X", "X X"]}
BASE = {"luminite": "umbra:luminite_crystal", "shadow": "umbra:shadow_shard"}
# 2.7: tools/gen_armor27.py owns these numbers now (kept the same here so a rerun agrees)
ADD = {
    "gloom": {2: "umbra:gloom_ingot", 3: "umbra:seal_shard"},
    "luminite": {2: "umbra:luminite_block", 3: "umbra:seal_shard"},
    "shadow": {2: "umbra:shadow_shard", 3: "umbra:seal_shard"},
}
for s in SETS:
    for piece in PIECES:
        if s != "gloom":
            write(f"{D}/recipe/{s}_{piece}.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "key": {"X": BASE[s]},
                                                    "pattern": CRAFT[piece], "result": {"id": f"umbra:{s}_{piece}"}})
        for tier in (2, 3):
            prev = f"umbra:{s}_{piece}" + ("" if tier == 2 else "_2")
            write(f"{D}/recipe/{s}_{piece}_{tier}.json", {"type": "minecraft:smithing_transform", "template": "umbra:echo_quartz", "base": prev,
                                                          "addition": ADD[s][tier], "result": {"id": f"umbra:{s}_{piece}_{tier}"}})

# tags: repairing and enchanting
write(f"{D}/tags/item/luminite_armor_materials.json", {"values": ["umbra:luminite_crystal"]})
write(f"{D}/tags/item/shadow_armor_materials.json", {"values": ["umbra:shadow_shard"]})
for slot, piece in (("head_armor", "helmet"), ("chest_armor", "chestplate"), ("leg_armor", "leggings"), ("foot_armor", "boots")):
    path = f"{RES}/data/minecraft/tags/item/{slot}.json"
    cur = json.load(open(path)) if os.path.exists(path) else {"values": []}
    vals = [v for v in cur["values"] if not any(v.startswith(f"umbra:{x}_{piece}") for x in ("luminite", "shadow", "gloom"))]
    for s in SETS:
        for tier in (1, 2, 3):
            vals.append(f"umbra:{s}_{piece}" + ("" if tier == 1 else f"_{tier}"))
    write(path, {"values": vals})

# lang
for fn, idx in (("ru_ru", 0), ("en_us", 1)):
    path = f"{A}/lang/{fn}.json"
    cur = json.load(open(path, encoding="utf-8"))
    for name, pair in names.items():
        cur[f"item.umbra.{name}"] = pair[idx]
    write(path, cur)
json.dump(sorted(names), open("/tmp/armor_names.json", "w"))
print(len(names), "pieces")
