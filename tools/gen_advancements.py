#!/usr/bin/env python3
"""Advancements: about thirty milestones of the journal, as a tree (the small entries stay in the journal only).
Each is granted by Journal.unlock when its entry is written (Journal.award looks up umbra:e_<entry>).
Run once from the repo root, commit the output: python3 tools/gen_advancements.py
"""
import glob
import json
import os

R = 'src/main/resources/'
D = R + 'data/umbra/advancement/'

# entry, parent entry (None = root), icon, frame
TREE = [
    ("start", None, "umbra:journal", "task"),
    ("sanity", "start", "umbra:sedative", "task"),
    ("luminite", "start", "umbra:luminite_crystal", "task"),
    ("shadow_dispelled", "luminite", "umbra:shadow_shard", "task"),
    ("steps", "sanity", "minecraft:leather_boots", "task"),
    ("mimic", "start", "minecraft:chest", "task"),
    ("maze", "start", "umbra:quantum_block", "task"),
    ("nightmare", "sanity", "minecraft:red_bed", "task"),
    ("insight_3", "nightmare", "umbra:madman_mask", "goal"),
    ("darkness", "luminite", "umbra:dispeller", "task"),
    ("underside", "darkness", "umbra:underside_key", "goal"),
    ("shadow_village", "underside", "umbra:shadow_villager_spawn_egg", "task"),
    ("sunken_temple", "underside", "umbra:echo_quartz", "task"),
    ("whispering_forest", "underside", "umbra:dead_log", "task"),
    ("ash_storm", "underside", "umbra:ash_soil", "task"),
    ("lurker", "sunken_temple", "umbra:lurker_spawn_egg", "task"),
    ("watcher", "underside", "umbra:watcher_eye", "goal"),
    ("mimic_queen", "underside", "umbra:mimic_crown", "goal"),
    ("shepherd", "underside", "umbra:shepherd_crook", "goal"),
    ("doppelganger", "underside", "umbra:mirror_mask", "goal"),
    ("citadel", "underside", "umbra:maze_wall", "goal"),
    ("architect", "citadel", "umbra:architect_seal", "challenge"),
    ("keeper_7", "luminite", "umbra:keeper_page", "goal"),
    ("note_8", "underside", "umbra:explorer_note", "task"),
    ("lighthouse", "underside", "umbra:lighthouse_lamp", "task"),
    ("yan_4", "lighthouse", "umbra:luminite_lantern", "goal"),
    ("onslaught_won", "lighthouse", "umbra:keeper_horn", "goal"),
    ("primordial_shadow", "architect", "umbra:abyss_heart", "challenge"),
    ("freed", "primordial_shadow", "umbra:shadow_shard", "goal"),
    ("ending_light", "primordial_shadow", "umbra:dawn_lantern", "challenge"),
    ("ending_dark", "primordial_shadow", "umbra:primordial_heart", "challenge"),
]


def write(name, obj):
    json.dump(obj, open(D + name + '.json', 'w'), indent=2)


def main():
    os.makedirs(D, exist_ok=True)
    for old in glob.glob(D + 'e_*.json'):
        os.remove(old)
    write('root', {"display": {"icon": {"id": "umbra:journal"}, "title": {"translate": "advancement.umbra.root.title"},
                               "description": {"translate": "advancement.umbra.root.desc"},
                               "background": "minecraft:gui/advancements/backgrounds/stone",
                               "show_toast": False, "announce_to_chat": False},
                   "criteria": {"entry": {"trigger": "minecraft:impossible"}}})
    for entry, parent, icon, frame in TREE:
        write('e_' + entry, {
            "parent": "umbra:root" if parent is None else "umbra:e_" + parent,
            "display": {"icon": {"id": icon}, "title": {"translate": "journal.umbra.%s.title" % entry},
                        "description": {"translate": "advancement.umbra.%s.desc" % entry},
                        "frame": frame, "show_toast": True, "announce_to_chat": False, "hidden": entry.startswith("ending")},
            "criteria": {"entry": {"trigger": "minecraft:impossible"}}})
    # the list Java checks against
    with open(R + 'data/umbra/advancement_entries.txt', 'w') as f:
        f.write('\n'.join(e for e, *_ in TREE) + '\n')
    print(len(TREE), 'advancements')


if __name__ == '__main__':
    main()
