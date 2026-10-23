#!/usr/bin/env python3
"""2.9: the bosses' music on discs, found in the mod's chests too, so it can be heard without fighting for it.
Adds (or replaces) one pool per chest table: a chance of one disc, any of the eight.
Run from the repo root: python3 tools/gen_disc_loot.py"""
import json

D = 'src/main/resources/data/umbra/loot_table/chests/'
TRACKS = ["watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow", "drowned_keeper", "fallen_keeper"]
CHANCE = {"maze_common": 0.05, "maze_common_underside": 0.06, "maze_prize": 0.35, "citadel_library": 0.15, "citadel_secret": 0.3,
          "lighthouse_house": 0.12, "explorer_camp": 0.1, "sunken_temple": 0.2, "shadow_village": 0.05, "keepers_workshop": 0.15,
          "depths": 0.5, "mimic_hoard": 0.25}

for name, chance in CHANCE.items():
    p = D + name + '.json'
    m = json.load(open(p))
    m["pools"] = [q for q in m["pools"] if not any(e.get("name", "").startswith("umbra:music_disc_") for e in q.get("entries", []))]
    m["pools"].append({"rolls": 1, "condition": {"type": "minecraft:random_chance", "chance": chance},
                       "entries": [{"type": "minecraft:item", "name": "umbra:music_disc_" + t} for t in TRACKS]})
    json.dump(m, open(p, 'w'), indent=2)
    open(p, 'a').write('\n')
