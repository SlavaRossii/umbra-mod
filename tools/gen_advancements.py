#!/usr/bin/env python3
"""Advancements from journal entries: one per entry, granted by Journal.unlock. Run once, commit the output."""
import json, os, re
R = 'src/main/resources/'
lang = json.load(open(R + 'assets/umbra/lang/en_us.json'))
ids = [m.group(1) for k in lang for m in [re.fullmatch(r'journal\.umbra\.([a-z0-9_]+)\.title', k)] if m]
BOSS = {"watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow", "freed"}
d = R + 'data/umbra/advancement/'
os.makedirs(d, exist_ok=True)
def write(name, obj): json.dump(obj, open(d + name + '.json', 'w'), indent=2)
write('root', {"display": {"icon": {"id": "umbra:journal"}, "title": {"translate": "advancement.umbra.root.title"},
    "description": {"translate": "advancement.umbra.root.desc"}, "show_toast": False, "announce_to_chat": False},
    "criteria": {"entry": {"trigger": "minecraft:impossible"}}})
for i in ids:
    frame = "challenge" if i in ("freed", "primordial_shadow") else "goal" if i in BOSS else "task"
    write('entry/' + i if False else 'e_' + i, {"parent": "umbra:root", "display": {"icon": {"id": "umbra:journal"},
        "title": {"translate": "journal.umbra.%s.title" % i}, "description": {"translate": "advancement.umbra.entry.desc"},
        "frame": frame, "show_toast": not i.startswith("insight"), "announce_to_chat": False, "hidden": False},
        "criteria": {"entry": {"trigger": "minecraft:impossible"}}})
for f, root, rd, ed in (('en_us', 'Umbra', 'The journal writes itself.', 'An entry in your journal.'),
                       ('ru_ru', 'Умбра', 'Дневник пишется сам.', 'Запись в вашем дневнике.')):
    p = R + 'assets/umbra/lang/%s.json' % f
    j = json.load(open(p))
    j.update({'advancement.umbra.root.title': root, 'advancement.umbra.root.desc': rd, 'advancement.umbra.entry.desc': ed})
    json.dump(j, open(p, 'w'), ensure_ascii=False, indent=2)
print(len(ids), 'entries')
