#!/usr/bin/env python3
"""2.7: how Yan's story ends. The Fallen Keeper (Yan's own model, burnt and gone dark), his spawn egg, the Keeper's
staff (icon and a held model), the boss's loot, and the lang for all of it.
Run from the repo root: python3 tools/gen_ending27.py"""
import json
import colorsys

from PIL import Image

A = 'src/main/resources/assets/umbra/'
D = 'src/main/resources/data/umbra/'


def write(path, obj):
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def load(path):
    return json.load(open(path))


def fallen():
    """Yan's skin, gone dark: the coat near black with violet, the skin ash, the beard grey, the lantern's glow gold,
    the eyes burning violet."""
    src = Image.open(A + 'textures/entity/lamp_keeper.png').convert('RGBA')
    px = src.load()
    out = src.copy()
    po = out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if v > 0.9 and b > 240 and g > 230:          # the lantern's glow
                po[x, y] = (255, 200, 90, a)
            elif s < 0.08 and v > 0.8:                   # beard
                po[x, y] = (120, 116, 120, a)
            elif 0.02 < h < 0.12 and s > 0.2 and v > 0.6:  # skin
                k = v * 0.55
                po[x, y] = (int(150 * k), int(140 * k), int(150 * k), a)
            elif abs(r - 40) < 6 and abs(g - 60) < 6 and abs(b - 80) < 6:   # eyes
                po[x, y] = (210, 130, 255, a)
            elif 0.5 < h < 0.75:                         # coat and hood
                po[x, y] = (int(r * 0.45 + 12), int(g * 0.3), int(b * 0.55 + 10), a)
            elif 0.08 < h < 0.2 and v > 0.5:             # brass
                po[x, y] = (int(r * 0.6), int(g * 0.5), int(b * 0.4), a)
            else:
                po[x, y] = (int(r * 0.6), int(g * 0.55), int(b * 0.65), a)
    # 2.7.1: the Fallen Keeper has his own model and texture now (tools/models.py)
    egg = Image.open(A + 'textures/item/lamp_keeper_spawn_egg.png').convert('RGBA')
    ep = egg.load()
    for y in range(egg.height):
        for x in range(egg.width):
            r, g, b, a = ep[x, y]
            if a:
                ep[x, y] = (int(r * 0.45 + 30), int(g * 0.3 + 10), int(b * 0.5 + 40), a) if (x + y) % 3 else (230, 180, 80, a)
    egg.save(A + 'textures/item/fallen_keeper_spawn_egg.png')
    write(A + 'models/item/fallen_keeper_spawn_egg.json', {"parent": "minecraft:item/generated", "textures": {"layer0": "umbra:item/fallen_keeper_spawn_egg"}})
    write(A + 'items/fallen_keeper_spawn_egg.json', {"model": {"type": "minecraft:model", "model": "umbra:item/fallen_keeper_spawn_egg"}})


def staff():
    """A long staff of dark wood bound in brass, and on top a small lantern cage with a white-gold flame."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    wood = [(78, 56, 36), (96, 70, 44), (62, 44, 28)]
    # shaft: from the bottom-left corner up to the cage
    for i in range(10):
        x, y = 2 + i, 15 - i
        px[x, y] = wood[i % 3] + (255,)
        if i % 3 == 1:
            px[x, y] = (190, 150, 70, 255)         # brass bands
        if 0 <= x - 1 and i < 9:
            px[x - 1, y] = (40, 28, 18, 255)       # outline
    # the cage
    for x in range(10, 15):
        for y in range(1, 7):
            edge = x in (10, 14) or y in (1, 6)
            if edge:
                px[x, y] = (170, 130, 60, 255) if (x + y) % 2 else (120, 90, 40, 255)
    for x, y, c in [(11, 2, (255, 250, 220)), (12, 2, (255, 240, 170)), (13, 2, (255, 250, 220)),
                    (11, 3, (255, 230, 140)), (12, 3, (255, 255, 255)), (13, 3, (255, 230, 140)),
                    (11, 4, (255, 210, 110)), (12, 4, (255, 245, 200)), (13, 4, (255, 210, 110)),
                    (11, 5, (230, 170, 80)), (12, 5, (255, 220, 130)), (13, 5, (230, 170, 80))]:
        px[x, y] = c + (255,)
    px[12, 0] = (170, 130, 60, 255)
    img.save(A + 'textures/item/keeper_staff.png')
    write(A + 'models/item/keeper_staff.json', {"parent": "minecraft:item/handheld", "textures": {"layer0": "umbra:item/keeper_staff"}})
    write(A + 'items/keeper_staff.json', {"model": {"type": "minecraft:model", "model": "umbra:item/keeper_staff"}})


def loot():
    write(D + 'loot_table/entities/fallen_keeper.json', {
        "type": "minecraft:entity",
        "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "umbra:echo_quartz",
                                      "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 8, "max": 12}}]}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "umbra:luminite_crystal",
                                      "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 12, "max": 20}}]}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "umbra:keeper_staff",
                                      "modifier": [{"type": "minecraft:set_damage", "damage": 0.3}]}]},
        ],
        "random_sequence": "umbra:entities/fallen_keeper"})


LANG = {
    "ru_ru": {
        "entity.umbra.fallen_keeper": "Павший смотритель",
        "entity.umbra.marta_echo": "Эхо Марты",
        "item.umbra.fallen_keeper_spawn_egg": "Яйцо призыва павшего смотрителя",
        "item.umbra.keeper_staff": "Посох Хранителя",
        "tip.umbra.keeper_staff": "Свет маяка в руке. Присев — круг Хранителя. Заряжается у горящей лампы.",
        "item.umbra.keeper_staff.empty": "Посох потускнел. Отнеси его к горящей лампе маяка.",
        "item.umbra.keeper_staff.lamp_dead": "Лампа мертва — посоху нечем напиться.",
        "item.umbra.keeper_staff.filled": "Посох напился света.",
        "block.umbra.lighthouse_lamp.staff": "У лампы стоит посох. Ян оставил его тебе.",
        "dialogue.umbra.yan.fall.1": "Ян (опуская трубку): Я узнал тебя по шагам. Так ходит то, что сидело в бездне.",
        "dialogue.umbra.yan.fall.2": "Ян: Марта тоже думала, что тьму можно взять в себя и остаться собой.",
        "dialogue.umbra.yan.fall.3": "Ян: Пока горит этот фонарь, к маяку ты не пройдёшь.",
        "dialogue.umbra.yan.fall.lamp": "Ян: Пусть гаснет. Свет — не в лампе.",
        "dialogue.umbra.yan.fall.marta": "Ян: Марта... ты всё-таки пришла.",
        "dialogue.umbra.yan.farewell.1": "Ян: Вот и всё. Лампа будет гореть и без меня — если её кормить.",
        "dialogue.umbra.yan.farewell.2": "Ян: Возьми посох. Он старше маяка, и он помнит, как держать круг.",
        "dialogue.umbra.yan.farewell.3": "Ян: Теперь смотритель — ты. А мне пора к Марте.",
        "ending.umbra.dark.yan": "Далеко, у маяка, старик поднял голову. Он знает, что ты сделал.",
        "ending.umbra.light.yan": "У маяка тебя ждёт Ян. Ему есть что тебе отдать.",
        "ending.umbra.fallen.title": "Последний смотритель",
        "ending.umbra.fallen.subtitle": "Маяк больше не горит",
        "ending.umbra.fallen.1": "Фонарь упал и не разбился. Он просто погас.",
        "ending.umbra.fallen.2": "Над Изнанкой больше нет огня, и никто не ждёт у воды.",
        "ending.umbra.fallen.3": "Тьма теперь знает только одного хранителя. Тебя.",
        "journal.umbra.fallen_keeper.title": "Павший смотритель",
        "journal.umbra.fallen_keeper.text": "Ян встал против меня с тем же фонарём, с которым ждал меня у воды. Он не злится. Он просто не пускает.",
        "journal.umbra.fallen_keeper.look": "Старик в обгоревшем плаще, фонарь горит золотом. Глаза — уже не его.",
        "journal.umbra.fallen_keeper.weak": "Когда фонарь вспыхивает, Ян на миг слепнет сам — бей в эту секунду. Лампа отмечает, где ты стоишь: не стой. Эхо Марты бережёт его — сначала они.",
        "journal.umbra.weak_fallen_keeper.title": "Как одолеть Яна",
        "journal.umbra.weak_fallen_keeper.text": "Вспышка — и он слепнет на две секунды. Свет сверху бьёт туда, где ты стоял. Когда придёт эхо Марты, сначала развей его.",
        "journal.umbra.ending_fallen.title": "Конец смотрителя",
        "journal.umbra.ending_fallen.text": "Маяк погас. Ян лежит у воды, а фонарь рядом с ним холодный. Я последний, кто помнит, что здесь был свет.",
        "journal.umbra.keeper_staff.title": "Посох Хранителя",
        "journal.umbra.keeper_staff.text": "Ян отдал мне свой посох. Он бьёт светом маяка и умеет держать круг, куда тьма не заходит. Пить ему нужно у горящей лампы.",
        "journal.umbra.yan_farewell.title": "Ян ушёл",
        "journal.umbra.yan_farewell.text": "Ян ушёл в туман над водой. Сказал, что ему пора к Марте. Маяк теперь мой.",
    },
    "en_us": {
        "entity.umbra.fallen_keeper": "Fallen Keeper",
        "entity.umbra.marta_echo": "Echo of Marta",
        "item.umbra.fallen_keeper_spawn_egg": "Fallen Keeper Spawn Egg",
        "item.umbra.keeper_staff": "Keeper's Staff",
        "tip.umbra.keeper_staff": "The lighthouse's light in your hand. Crouch: the Keeper's circle. Refill at the burning lamp.",
        "item.umbra.keeper_staff.empty": "The staff has gone dim. Take it to the lighthouse's burning lamp.",
        "item.umbra.keeper_staff.lamp_dead": "The lamp is dead: there is nothing for the staff to drink.",
        "item.umbra.keeper_staff.filled": "The staff has drunk its fill of light.",
        "block.umbra.lighthouse_lamp.staff": "A staff leans against the lamp. Yan left it for you.",
        "dialogue.umbra.yan.fall.1": "Yan (setting down his pipe): I knew you by your step. That is how the thing in the abyss walked.",
        "dialogue.umbra.yan.fall.2": "Yan: Marta too thought the dark could be taken in and you stay yourself.",
        "dialogue.umbra.yan.fall.3": "Yan: While this lantern burns, you do not pass to the lighthouse.",
        "dialogue.umbra.yan.fall.lamp": "Yan: Let it go out. The light is not in the lamp.",
        "dialogue.umbra.yan.fall.marta": "Yan: Marta... you came after all.",
        "dialogue.umbra.yan.farewell.1": "Yan: That is all. The lamp will burn without me, if you feed it.",
        "dialogue.umbra.yan.farewell.2": "Yan: Take the staff. It is older than the lighthouse, and it remembers how to hold a circle.",
        "dialogue.umbra.yan.farewell.3": "Yan: You are the keeper now. And it is time I went to Marta.",
        "ending.umbra.dark.yan": "Far away, at the lighthouse, an old man lifts his head. He knows what you have done.",
        "ending.umbra.light.yan": "Yan waits for you at the lighthouse. He has something to give you.",
        "ending.umbra.fallen.title": "The Last Keeper",
        "ending.umbra.fallen.subtitle": "The lighthouse burns no more",
        "ending.umbra.fallen.1": "The lantern fell and did not break. It simply went out.",
        "ending.umbra.fallen.2": "There is no fire over the Underside now, and no one waits by the water.",
        "ending.umbra.fallen.3": "The dark knows only one keeper now. You.",
        "journal.umbra.fallen_keeper.title": "The Fallen Keeper",
        "journal.umbra.fallen_keeper.text": "Yan stood against me with the same lantern he waited for me with by the water. He is not angry. He just will not let me pass.",
        "journal.umbra.fallen_keeper.look": "An old man in a burnt cloak, the lantern burning gold. The eyes are no longer his.",
        "journal.umbra.fallen_keeper.weak": "When the lantern flares, Yan is blind for a moment himself: strike then. The lamp marks where you stand: do not stand. Marta's echoes guard him: they go first.",
        "journal.umbra.weak_fallen_keeper.title": "How to beat Yan",
        "journal.umbra.weak_fallen_keeper.text": "A flare, and he is blind for two seconds. The light from above strikes where you stood. When Marta's echo comes, scatter it first.",
        "journal.umbra.ending_fallen.title": "The Keeper's End",
        "journal.umbra.ending_fallen.text": "The lighthouse is dark. Yan lies by the water, the lantern cold beside him. I am the last who remembers there was light here.",
        "journal.umbra.keeper_staff.title": "The Keeper's Staff",
        "journal.umbra.keeper_staff.text": "Yan gave me his staff. It strikes with the lighthouse's light and can hold a circle the dark does not enter. It drinks at the burning lamp.",
        "journal.umbra.yan_farewell.title": "Yan Has Gone",
        "journal.umbra.yan_farewell.text": "Yan went into the fog over the water. He said it was time he went to Marta. The lighthouse is mine now.",
    },
}


def lang():
    for code, entries in LANG.items():
        path = A + f"lang/{code}.json"
        t = load(path)
        for k in [k for k in t if k.startswith(("journal.umbra.new_cycle", "dialogue.umbra.yan.cycle", "dialogue.umbra.yan.offer"))]:
            del t[k]
        t.update(entries)
        write(path, t)


if __name__ == '__main__':
    fallen()
    staff()
    loot()
    lang()
    print("ok")
