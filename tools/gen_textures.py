"""Procedural pixel art for Umbra. Run from the repo root: python3 tools/gen_textures.py
Deterministic (fixed seeds), so re-running only changes what you edit here."""
import math
import os
import random
from PIL import Image

ROOT = "src/main/resources/assets/umbra/textures"


def save(img, path):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def noise_tile(base, spread, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for x in range(16):
        for y in range(16):
            d = rnd.randint(-spread, spread)
            px[x, y] = tuple(max(0, min(255, c + d)) for c in base) + (255,)
    return img


def ore(base, spread, seed, name):
    img = noise_tile(base, spread, seed)
    px = img.load()
    rnd = random.Random(seed + 1)
    for _ in range(5):  # crystal clusters
        cx, cy = rnd.randint(2, 13), rnd.randint(2, 13)
        for dx, dy in [(0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, 1)]:
            if rnd.random() < 0.85:
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16:
                    bright = (dx, dy) == (0, 0)
                    px[x, y] = (200, 250, 255, 255) if bright else (90, 200, 230, 255)
    save(img, f"block/{name}.png")


def lamp():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            inner = 3 <= x <= 12 and 3 <= y <= 12
            if edge:
                px[x, y] = (40, 70, 90, 255)
            elif inner:
                d = math.hypot(x - 7.5, y - 7.5)
                v = int(255 - d * 12)
                px[x, y] = (max(120, v - 40), max(200, v), 255, 255)
            else:
                px[x, y] = (110, 190, 220, 255)
    save(img, "block/luminite_lamp.png")


def eternal_torch():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(6, 16):
        for x in (7, 8):
            if y <= 7:
                px[x, y] = (210, 250, 255, 255) if y == 6 else (120, 220, 245, 255)
            else:
                px[x, y] = (90, 66, 40, 255) if x == 7 else (70, 50, 30, 255)
    save(img, "block/eternal_torch.png")


def item_crystal():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    shape = [(7, 2), (8, 2), (6, 3), (7, 3), (8, 3), (9, 3)]
    for y in range(4, 12):
        w = 3 if y < 9 else 2
        for x in range(8 - w, 8 + w):
            shape.append((x, y))
    for (x, y) in shape:
        light = x < 8
        px[x, y] = (190, 245, 255, 255) if light else (80, 180, 220, 255)
    for (x, y) in [(7, 12), (8, 12), (7, 13)]:
        px[x, y] = (60, 140, 190, 255)
    save(img, "item/luminite_crystal.png")


def item_shard():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(9)
    for y in range(2, 15):
        half = max(0, 4 - abs(y - 7) // 2)
        cx = 8 + (y - 8) // 4
        for x in range(cx - half, cx + half + 1):
            v = rnd.randint(10, 30)
            px[x, y] = (v + 25, v, v + 45, 255)
    for (x, y) in [(8, 5), (8, 6), (9, 7)]:
        px[x, y] = (150, 110, 210, 255)
    save(img, "item/shadow_shard.png")


def item_pill(color, name):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            # a capsule lying diagonally
            u = (x - y) / math.sqrt(2)
            v = (x + y - 15) / math.sqrt(2)
            if abs(v) <= 2.6 and abs(u) <= 6.0 or math.hypot(abs(u) - 6.0, v) <= 2.6 and abs(u) > 6.0:
                inside = abs(u) <= 8.6
                if not inside:
                    continue
                c = (240, 240, 235) if u < 0 else color
                shade = 25 if v > 1.2 else 0
                px[x, y] = tuple(max(0, ch - shade) for ch in c) + (255,)
    save(img, f"item/{name}.png")


def item_mirror():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 5.5)
            if d <= 5.2:
                if d > 4.2:
                    px[x, y] = (150, 150, 160, 255)
                else:
                    t = (x + y) % 5
                    px[x, y] = (200, 225, 240, 255) if t == 0 else (140, 175, 200, 255)
    for y in range(11, 16):
        for x in (7, 8):
            px[x, y] = (110, 80, 50, 255) if x == 7 else (90, 64, 40, 255)
    save(img, "item/mirror.png")


def item_dispeller():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    frame = (120, 115, 105, 255)
    for x in range(5, 11):
        px[x, 3] = frame
        px[x, 13] = frame
    for y in range(3, 14):
        px[4, y] = frame
        px[11, y] = frame
    for y in range(4, 13):
        for x in range(5, 11):
            d = math.hypot(x - 7.5, y - 8)
            v = int(255 - d * 18)
            px[x, y] = (max(100, v - 60), max(180, v), 255, 255)
    for x in (7, 8):
        px[x, 1] = frame
        px[x, 2] = frame
    px[6, 1] = frame
    px[9, 1] = frame
    save(img, "item/dispeller.png")


def humanoid_skin(path, body, face, eyes, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    regions = [(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32), (32, 48, 48, 64), (16, 48, 32, 64)]
    for (x0, y0, x1, y1) in regions:
        for x in range(x0, x1):
            for y in range(y0, y1):
                d = rnd.randint(-4, 4)
                px[x, y] = tuple(max(0, min(255, c + d)) for c in body) + (255,)
    if face:
        for x in range(8, 16):
            for y in range(8, 16):
                d = rnd.randint(-6, 6)
                px[x, y] = tuple(max(0, min(255, c + d)) for c in face) + (255,)
    for (ex, ey) in [(9, 12), (10, 12), (13, 12), (14, 12)]:
        px[ex, ey] = eyes + (255,)
    save(img, path)


# ------------------------------------------------------------------ stage 2: the Underside

def plain(base, spread, seed, name, speckle=None):
    img = noise_tile(base, spread, seed)
    if speckle:
        px = img.load()
        rnd = random.Random(seed + 7)
        for _ in range(speckle[1]):
            px[rnd.randint(0, 15), rnd.randint(0, 15)] = speckle[0] + (255,)
    save(img, f"block/{name}.png")


def ore_on(base, spread, seed, name, color, bright, clusters=4):
    img = noise_tile(base, spread, seed)
    px = img.load()
    rnd = random.Random(seed + 1)
    for _ in range(clusters):
        cx, cy = rnd.randint(2, 13), rnd.randint(2, 13)
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0)]:
            if rnd.random() < 0.8:
                px[cx + dx, cy + dy] = (bright if (dx, dy) == (0, 0) else color) + (255,)
    save(img, f"block/{name}.png")


def dusk_stone():
    img = noise_tile((38, 26, 52), 8, 21)
    px = img.load()
    for i in range(16):  # faint runes
        if i % 4 == 1:
            for j in range(3, 13, 3):
                px[i, j] = (110, 70, 160, 255)
    for x in range(16):
        px[x, 0] = px[x, 15] = (25, 16, 36, 255)
        px[0, x] = px[15, x] = (25, 16, 36, 255)
    save(img, "block/dusk_stone.png")


def dead_log():
    rnd = random.Random(31)
    side = Image.new("RGBA", (16, 16))
    px = side.load()
    for x in range(16):
        stripe = rnd.randint(-10, 10)
        for y in range(16):
            v = 92 + stripe + rnd.randint(-6, 6)
            px[x, y] = (v, v - 4, v - 10, 255)
    save(side, "block/dead_log.png")
    top = Image.new("RGBA", (16, 16))
    px = top.load()
    for x in range(16):
        for y in range(16):
            d = int(math.hypot(x - 7.5, y - 7.5))
            v = 120 - (d % 3) * 14 if d < 7 else 80
            px[x, y] = (v, v - 6, v - 14, 255)
    save(top, "block/dead_log_top.png")


def rift():
    frames = 16
    img = Image.new("RGBA", (16, 16 * frames))
    px = img.load()
    for f in range(frames):
        for x in range(16):
            for y in range(16):
                a = math.sin(x * 0.6 + f * 0.4) + math.cos(y * 0.5 - f * 0.3) + math.sin((x + y) * 0.3 + f * 0.2)
                v = int(40 + 30 * a)
                px[x, y + 16 * f] = (max(0, v + 40), max(0, v - 20), min(255, v + 120), 190)
    save(img, "block/rift.png")
    with open(os.path.join(ROOT, "block/rift.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 2,\n    "interpolate": true\n  }\n}\n')


def ingot(name, light, dark):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(6, 11):
        for x in range(2 + (10 - y), 14 - (y - 6)):
            px[x, y] = light if y < 8 else dark
    save(img, f"item/{name}.png")


def nugget(name, light, dark, seed):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(seed)
    for y in range(4, 13):
        for x in range(4, 12):
            if math.hypot(x - 7.5, y - 8) < 4.3 + rnd.random() * 0.8:
                px[x, y] = light if rnd.random() < 0.4 else dark
    save(img, f"item/{name}.png")


def tool(name, head_pixels, color, handle=(80, 58, 36, 255)):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i in range(3, 14):  # diagonal handle from bottom-left
        px[i - 2, 17 - i] = handle
    for (x, y) in head_pixels:
        px[x, y] = color
    save(img, f"item/{name}.png")


def armor_icon(name, mask, color, edge):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch == "#":
                px[x, y] = color
            elif ch == "+":
                px[x, y] = edge
    save(img, f"item/{name}.png")


def armor_layers():
    rnd = random.Random(55)
    for layer in ("humanoid", "humanoid_leggings"):
        img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        px = img.load()
        regions = ([(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32)] if layer == "humanoid"
                   else [(16, 16, 40, 32), (0, 16, 16, 32)])
        for (x0, y0, x1, y1) in regions:
            for x in range(x0, x1):
                for y in range(y0, y1):
                    v = rnd.randint(28, 44)
                    trim = (x + y) % 9 == 0
                    px[x, y] = (110, 70, 170, 255) if trim else (v, v - 6, v + 14, 255)
        save(img, f"entity/equipment/{layer}/gloom.png")


GLOOM = (70, 58, 96, 255)
GLOOM_EDGE = (150, 120, 200, 255)


def stage2():
    plain((92, 90, 96), 10, 11, "ash_stone")
    plain((150, 146, 150), 12, 12, "ash_soil", ((200, 198, 200), 14))
    dusk_stone()
    dead_log()
    ore_on((92, 90, 96), 10, 11, "gloom_ore", (60, 40, 90), (130, 90, 190))
    ore_on((92, 90, 96), 10, 13, "echo_quartz_ore", (40, 140, 140), (150, 255, 240), 3)
    plain((52, 42, 70), 6, 14, "gloom_block", ((120, 90, 170), 10))
    rift()
    nugget("raw_gloom", (110, 80, 150, 255), (60, 44, 84, 255), 3)
    ingot("gloom_ingot", (140, 112, 190, 255), (82, 64, 116, 255))
    item_quartz = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = item_quartz.load()
    for y in range(3, 14):
        for x in range(7 - (y - 3) // 4, 9 + (y - 3) // 4):
            px[x, y] = (170, 255, 240, 255) if x < 8 else (60, 170, 170, 255)
    save(item_quartz, "item/echo_quartz.png")
    head_sword = [(i, 13 - i + 1) for i in range(6, 14)] + [(i + 1, 13 - i + 1) for i in range(6, 13)]
    tool("gloom_sword", head_sword, GLOOM_EDGE)
    tool("gloom_pickaxe", [(x, 3 + abs(x - 9) // 3) for x in range(4, 15)] + [(9, 4), (10, 4)], GLOOM_EDGE)
    tool("gloom_axe", [(x, y) for x in range(8, 13) for y in range(2, 7) if x + y > 10], GLOOM_EDGE)
    tool("gloom_shovel", [(x, y) for x in range(10, 14) for y in range(2, 6)], GLOOM_EDGE)
    armor_icon("gloom_helmet", ["", "", "", "    ++++++++", "   +########+", "   +########+", "   +##+  +##+", "   +##+  +##+"], GLOOM, GLOOM_EDGE)
    armor_icon("gloom_chestplate", ["", "  +++    +++", "  +##+  +##+", "  +########+", "   +######+", "   +######+", "   +######+", "   +######+", "   +######+", "   ++++++++"], GLOOM, GLOOM_EDGE)
    armor_icon("gloom_leggings", ["", "", "   ++++++++", "   +######+", "   +##++##+", "   +##++##+", "   +##++##+", "   +##++##+", "   +##++##+", "   ++++++++"], GLOOM, GLOOM_EDGE)
    armor_icon("gloom_boots", ["", "", "", "", "", "", "", "", "   +##+ +##+", "   +##+ +##+", "  +###+ +###+", "  +++++ +++++"], GLOOM, GLOOM_EDGE)
    armor_layers()


# ------------------------------------------------------------------ stage 3: creatures

def fill_tex(size, regions, color_fn, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    px = img.load()
    for (x0, y0, x1, y1) in regions:
        for x in range(x0, x1):
            for y in range(y0, y1):
                px[x, y] = color_fn(x, y, rnd)
    return img, px


def crawler():
    # spider layout 64x32: head 32,4..56,20 ; body0 0,0..24,12 ; body1 0,12..40,24 ; legs 18,0..50,4
    regions = [(0, 0, 64, 32)]
    img, px = fill_tex((64, 32), regions, lambda x, y, r: (lambda v: (v, v, v - 6, 255))(r.randint(170, 200)), 61)
    for (ex, ey) in [(40, 12), (41, 12), (45, 12), (46, 12), (42, 14), (44, 14)]:
        px[ex, ey] = (20, 20, 20, 255)
    save(img, "entity/crawler.png")


def light_eater():
    # vex layout 32x32
    img, px = fill_tex((32, 32), [(0, 0, 32, 32)], lambda x, y, r: (lambda v: (v, v, v + 8, 210))(r.randint(200, 240)), 62)
    for (ex, ey) in [(2, 3), (5, 3)]:
        px[ex, ey] = (0, 0, 0, 255)
    for x in range(16, 32):  # wings: dusty with dark eye-spots
        for y in range(14, 30):
            if (x - 23) ** 2 + (y - 21) ** 2 < 6:
                px[x, y] = (40, 30, 30, 255)
    save(img, "entity/light_eater.png")


def stage3():
    crawler()
    light_eater()
    humanoid_skin("entity/blind.png", (150, 140, 135), (175, 165, 160), (120, 40, 40), 63)
    img = Image.open(os.path.join(ROOT, "entity/blind.png"))
    px = img.load()
    for x in range(8, 16):  # stitched eyes
        px[x, 12] = (60, 20, 20, 255)
    save(img, "entity/blind.png")
    rnd = random.Random(64)
    patches = [(120, 90, 60), (40, 40, 40), (220, 220, 210), (160, 110, 80), (90, 120, 70)]
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for (x0, y0, x1, y1) in [(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32), (32, 48, 48, 64), (16, 48, 32, 64)]:
        for x in range(x0, x1):
            for y in range(y0, y1):
                c = patches[((x // 4) + (y // 4) * 3 + rnd.randint(0, 1)) % len(patches)]
                px[x, y] = c + (255,)
    for (ex, ey) in [(9, 11), (10, 11), (13, 13), (14, 13)]:
        px[ex, ey] = (0, 0, 0, 255)
    save(img, "entity/mimicker.png")
    humanoid_skin("entity/shade.png", (8, 8, 12), None, (240, 240, 255), 65)
    eggs = {"crawler": ((190, 188, 180), (30, 30, 30)), "blind": ((150, 140, 135), (120, 40, 40)),
            "light_eater": ((230, 230, 238), (60, 50, 50)), "mimicker": ((120, 90, 60), (220, 220, 210)),
            "shade": ((15, 15, 20), (230, 230, 255))}
    for name, (base, spot) in eggs.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = img.load()
        rnd = random.Random(name)
        for y in range(16):
            for x in range(16):
                if ((x - 7.5) / 5.2) ** 2 + ((y - 8.5) / 6.8) ** 2 <= 1:
                    px[x, y] = (spot if rnd.random() < 0.18 else base) + (255,)
        save(img, f"item/{name}_spawn_egg.png")


# ------------------------------------------------------------------ stage 4: minibosses

def icon(name, draw, seed=0):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    draw(px, random.Random(seed))
    save(img, f"item/{name}.png")


def disc(px, cx, cy, r, color):
    for x in range(16):
        for y in range(16):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px[x, y] = color


def seal(color):
    def d(px, rnd):
        disc(px, 7.5, 7.5, 6.5, (40, 30, 50, 255))
        disc(px, 7.5, 7.5, 5.2, color)
        for (x, y) in [(7, 4), (8, 4), (5, 7), (10, 7), (7, 11), (8, 11), (7, 7), (8, 8)]:
            px[x, y] = (30, 20, 40, 255)
    return d


def stage4():
    humanoid_skin("entity/watcher.png", (225, 225, 220), (245, 245, 240), (10, 10, 10), 71)
    img = Image.open(os.path.join(ROOT, "entity/watcher.png"))
    px = img.load()
    for (ex, ey) in [(9, 11), (10, 11), (9, 12), (10, 12), (13, 11), (14, 11), (13, 12), (14, 12)]:
        px[ex, ey] = (0, 0, 0, 255)
    save(img, "entity/watcher.png")
    src = Image.open(os.path.join(ROOT, "entity/mimicker.png")).convert("RGBA")
    px = src.load()
    for x in range(8, 16):  # golden crown band on the forehead
        px[x, 8] = (230, 190, 60, 255)
        if x % 2 == 0:
            px[x, 7] = (230, 190, 60, 255) if 7 >= 0 else px[x, 7]
    save(src, "entity/mimic_queen.png")
    humanoid_skin("entity/shepherd.png", (25, 18, 35), (60, 50, 70), (170, 120, 255), 72)
    humanoid_skin("entity/doppelganger.png", (150, 160, 175), (190, 200, 210), (40, 60, 90), 73)
    icon("seal_of_watching", seal((235, 235, 230, 255)))
    icon("seal_of_mimicry", seal((120, 170, 80, 255)))
    icon("seal_of_herding", seal((110, 70, 170, 255)))
    icon("seal_of_reflection", seal((120, 160, 220, 255)))

    def eye(px, rnd):
        disc(px, 7.5, 7.5, 6, (235, 235, 228, 255))
        disc(px, 7.5, 7.5, 3, (20, 20, 20, 255))
        px[6, 6] = (255, 255, 255, 255)
    icon("watcher_eye", eye)

    def crown(px, rnd):
        for x in range(2, 14):
            for y in range(8, 12):
                px[x, y] = (220, 180, 60, 255)
        for x in (2, 5, 8, 11, 13):
            for y in range(4, 8):
                if y >= 4 + abs(x % 3):
                    px[x, y] = (220, 180, 60, 255)
        for x in (4, 8, 12):
            px[x, 10] = (110, 160, 80, 255)
    icon("mimic_crown", crown)

    def crook(px, rnd):
        for i in range(4, 15):
            px[16 - i, i] = (80, 50, 110, 255)
        for (x, y) in [(11, 2), (12, 2), (13, 3), (13, 4), (12, 5), (10, 3)]:
            px[x, y] = (80, 50, 110, 255)
        px[12, 3] = (190, 140, 255, 255)
    icon("shepherd_crook", crook)

    def mask(px, rnd):
        for x in range(3, 13):
            for y in range(3, 13):
                if ((x - 7.5) / 5) ** 2 + ((y - 7.5) / 5.5) ** 2 <= 1:
                    px[x, y] = (190, 205, 220, 255) if (x + y) % 4 else (230, 240, 250, 255)
        for (x, y) in [(5, 7), (6, 7), (9, 7), (10, 7), (7, 10), (8, 10)]:
            px[x, y] = (20, 25, 35, 255)
    icon("mirror_mask", mask)

    def key(px, rnd):
        disc(px, 4.5, 4.5, 3.2, (70, 50, 100, 255))
        disc(px, 4.5, 4.5, 1.5, (0, 0, 0, 0))
        for i in range(6, 14):
            px[i, i] = (70, 50, 100, 255)
        for (x, y) in [(11, 13), (12, 12), (13, 11), (10, 12)]:
            px[x, y] = (170, 130, 230, 255)
    icon("underside_key", key)


# ------------------------------------------------------------------ stage 5: bosses

def stage5():
    humanoid_skin("entity/architect.png", (70, 66, 80), (95, 90, 110), (255, 220, 120), 81)
    img = Image.open(os.path.join(ROOT, "entity/architect.png"))
    px = img.load()
    for x in range(16, 40):  # glowing blueprint lines across the body
        for y in range(16, 32):
            if (x + y) % 6 == 0:
                px[x, y] = (255, 210, 110, 255)
    save(img, "entity/architect.png")
    humanoid_skin("entity/primordial_shadow.png", (4, 2, 8), (10, 6, 16), (190, 120, 255), 82)

    def heart(px, rnd):
        for y in range(16):
            for x in range(16):
                u, v = (x - 7.5) / 6.0, (y - 7.0) / 6.0
                if (u * u + v * v - 1) ** 3 - u * u * v ** 3 * -1 <= 0:
                    px[x, y] = (40, 10, 60, 255) if rnd.random() < 0.7 else (120, 60, 190, 255)
        px[6, 6] = (220, 180, 255, 255)
    icon("abyss_heart", heart, 5)

    def lantern(px, rnd):
        frame = (230, 200, 120, 255)
        for x in range(5, 11):
            px[x, 3] = frame
            px[x, 13] = frame
        for y in range(3, 14):
            px[4, y] = frame
            px[11, y] = frame
        for y in range(4, 13):
            for x in range(5, 11):
                d = math.hypot(x - 7.5, y - 8)
                v = int(255 - d * 14)
                px[x, y] = (255, max(200, v), max(120, v - 60), 255)
        for x in (7, 8):
            px[x, 1] = frame
            px[x, 2] = frame
    icon("dawn_lantern", lantern)


if __name__ == "__main__":
    ore((125, 125, 125), 14, 1, "luminite_ore")
    ore((60, 60, 66), 10, 2, "deepslate_luminite_ore")
    lamp()
    eternal_torch()
    item_crystal()
    item_shard()
    item_pill((150, 120, 230), "sedative")
    item_pill((210, 50, 60), "antipsychotic")
    item_mirror()
    item_dispeller()
    humanoid_skin("entity/hallucination.png", (18, 18, 22), (205, 205, 200), (5, 5, 5), 4)
    stage2()
    stage3()
    stage4()
    stage5()
    print("textures written")
