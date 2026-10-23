#!/usr/bin/env python3
"""Static audit of Umbra's resources against its Java registrations. Run from the repo root: python3 tools/audit.py
Exit code 1 if anything is wrong. Checks:
  * every registered item/block: lang name, item definition, models it points at, textures they use, blockstate, loot
  * every model JSON: parent and textures exist; every blockstate's models exist
  * recipes, loot tables, tags: every umbra:/minecraft: id they name exists (umbra ids against the registrations)
  * biome spawns and spawners: registered entities; entity textures for renderers; sounds.json files
  * lang: ru and en have the same keys; translatable keys used in Java exist; journal entries have title/text
  * advancements: icons exist
"""
import glob
import json
import os
import re
import sys

J = 'src/main/java/dev/umbra/'
A = 'src/main/resources/assets/umbra/'
D = 'src/main/resources/data/'
problems = []


def bad(msg):
    problems.append(msg)


def read(p):
    return open(p, encoding='utf-8').read()


java = {p: read(p) for p in glob.glob(J + '**/*.java', recursive=True)}
items_src = read(J + 'registry/ModItems.java')
blocks_src = read(J + 'registry/ModBlocks.java')
ents_src = read(J + 'registry/ModEntities.java')

items = set(i for i in re.findall(r'ITEMS\.register\w*\("([a-z0-9_]+)"', items_src) if not i.endswith('_'))
# items registered in loops: armour pieces, trophies
for set_ in ("gloom", "luminite", "shadow"):
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        for t in ("", "_2", "_3"):
            items.add(f"{set_}_{piece}{t}")
for b in ("watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow"):
    items.add("trophy_" + b)
# 2.9: the discs, one per name in ModItems.DISC_TRACKS
m = re.search(r'DISC_TRACKS = java\.util\.List\.of\(([^)]*)\)', items_src, re.S)
for t in re.findall(r'"([a-z_]+)"', m.group(1) if m else ""):
    items.add("music_disc_" + t)
blocks = set(i for i in re.findall(r'BLOCKS\.register\w*\("([a-z0-9_]+)"', blocks_src) if not i.endswith('_'))
# blocks registered in loops (the mimic blocks): take their names from the blockstates the generator wrote
for p in glob.glob(A + 'blockstates/mimic_*.json'):
    blocks.add(os.path.basename(p)[:-5])
    no_loot_mimic = True
entities = set(re.findall(r'registerEntityType\("([a-z0-9_]+)"', ents_src))
no_loot = set()
for m in re.finditer(r'BLOCKS\.register\w*\("([a-z0-9_]+)"(.*?)\);\n', blocks_src, re.S):
    if 'noLootTable' in m.group(2):
        no_loot.add(m.group(1))
no_loot |= {b for b in blocks if b.startswith('mimic_')}   # mimic blocks drop nothing (noLootTable in mimic())
all_ids = items | blocks
print(f"{len(items)} items, {len(blocks)} blocks, {len(entities)} entities")

ru = json.load(open(A + 'lang/ru_ru.json', encoding='utf-8'))
en = json.load(open(A + 'lang/en_us.json', encoding='utf-8'))
for k in sorted(set(ru) ^ set(en)):
    bad(f"lang: key only in {'ru' if k in ru else 'en'}: {k}")


def tex_exists(ref):
    ns, path = ref.split(':', 1) if ':' in ref else ('minecraft', ref)
    if ns != 'umbra':
        return True
    return os.path.exists(A + 'textures/' + path + '.png')


def model_exists(ref):
    ns, path = ref.split(':', 1) if ':' in ref else ('minecraft', ref)
    if ns != 'umbra':
        return True
    return os.path.exists(A + 'models/' + path + '.json')


# models
for p in glob.glob(A + 'models/**/*.json', recursive=True):
    m = json.load(open(p))
    if 'parent' in m and not model_exists(m['parent']):
        bad(f"model {p}: parent {m['parent']} missing")
    for k, v in m.get('textures', {}).items():
        if not v.startswith('#') and not tex_exists(v):
            bad(f"model {p}: texture {k}={v} missing")
    tex = m.get('textures', {})
    for e in m.get('elements', []):
        for side, f in e.get('faces', {}).items():
            t = f.get('texture', '')
            if t.startswith('#') and t[1:] not in tex and not m.get('parent'):
                bad(f"model {p}: face {side} uses {t} which is not defined")
            uv = f.get('uv')
            if uv and (min(uv) < 0 or max(uv) > 16):
                bad(f"model {p}: uv out of range {uv}")
        for c in e['from'] + e['to']:
            if c < -16 or c > 32:
                bad(f"model {p}: element out of range {c}")


def walk_item_model(node, where):
    if isinstance(node, dict):
        if node.get('type') in ('minecraft:model', 'model') and 'model' in node and isinstance(node['model'], str):
            if not model_exists(node['model']):
                bad(f"item def {where}: model {node['model']} missing")
        for v in node.values():
            walk_item_model(v, where)
    elif isinstance(node, list):
        for v in node:
            walk_item_model(v, where)


# items: definition, name
for i in sorted(items | blocks):
    defn = A + f'items/{i}.json'
    is_block_only = i in blocks and i not in items
    if not os.path.exists(defn):
        if not is_block_only:
            bad(f"item {i}: no items/{i}.json")
    else:
        walk_item_model(json.load(open(defn)), i)
    key = ('block' if i in blocks else 'item') + '.umbra.' + i
    if key not in ru and ('item.umbra.' + i) not in ru and ('block.umbra.' + i) not in ru:
        bad(f"{i}: no name in lang")

# blocks: blockstate, loot
for b in sorted(blocks):
    bs = A + f'blockstates/{b}.json'
    if not os.path.exists(bs):
        bad(f"block {b}: no blockstate")
    else:
        s = json.load(open(bs))
        refs = re.findall(r'"model":\s*"([^"]+)"', json.dumps(s))
        for r in refs:
            if not model_exists(r):
                bad(f"block {b}: blockstate model {r} missing")
    if b not in no_loot and not os.path.exists(D + f'umbra/loot_table/blocks/{b}.json'):
        bad(f"block {b}: no loot table (and not noLootTable)")

# ids in data files
def check_ids(path, text):
    for ns, i in re.findall(r'"#?(umbra|minecraft):([a-z0-9_/]+)"', text):
        if ns == 'umbra' and '/' not in i and i not in all_ids and i not in entities:
            # enchantments, loot tables and the like are fine if they exist as files
            if os.path.exists(D + f'umbra/enchantment/{i}.json'):
                continue
            bad(f"{path}: umbra:{i} is not a registered item/block/entity")


for p in glob.glob(D + 'umbra/recipe/*.json') + glob.glob(D + 'umbra/loot_table/**/*.json', recursive=True):
    check_ids(p, read(p))
for p in glob.glob(D + '*/tags/**/*.json', recursive=True):
    t = json.load(open(p))
    for v in t.get('values', []):
        v = v if isinstance(v, str) else v.get('id', '')
        if v.startswith('umbra:'):
            i = v[6:]
            kind = p.split('/tags/')[1].split('/')[0]
            if kind == 'item' and i not in all_ids:
                bad(f"tag {p}: item {v} missing")
            if kind == 'block' and i not in blocks:
                bad(f"tag {p}: block {v} missing")
            if kind == 'enchantment' and not os.path.exists(D + f'umbra/enchantment/{i}.json'):
                bad(f"tag {p}: enchantment {v} missing")
        elif v.startswith('#umbra:'):
            kind = p.split('/tags/')[1].split('/')[0]
            if not os.path.exists(D + f'umbra/tags/{kind}/{v[7:]}.json'):
                bad(f"tag {p}: tag {v} missing")

# recipe results and ingredients that are tags
for p in glob.glob(D + 'umbra/recipe/*.json'):
    r = json.load(open(p))
    res = r.get('result', {}).get('id', '')
    if res.startswith('umbra:') and res[6:] not in all_ids:
        bad(f"recipe {p}: result {res} missing")

# spawns in biomes and biome modifiers
for p in glob.glob(D + 'umbra/worldgen/biome/*.json') + glob.glob(D + 'umbra/neoforge/biome_modifier/*.json'):
    for e in re.findall(r'"type":\s*"umbra:([a-z0-9_]+)"', read(p)):
        if e not in entities and not os.path.exists(D + f'umbra/worldgen/placed_feature/{e}.json'):
            bad(f"{p}: spawns umbra:{e} which is not registered")

# entity textures used by renderers
client = read(J + 'client/UmbraClient.java')
for tex in re.findall(r'Renderer<[^>]*>\(ctx, (?:ModLayers\.\w+, )?"([a-z0-9_]+)"', client):
    if not os.path.exists(A + f'textures/entity/{tex}.png'):
        bad(f"renderer texture textures/entity/{tex}.png missing")
for e in entities:
    if f'ModEntities.{e.upper()}.get()' not in client:
        bad(f"entity {e}: no renderer registered")

# sounds
sounds = json.load(open(A + 'sounds.json'))
for ev, s in sounds.items():
    for snd in s['sounds']:
        name = snd if isinstance(snd, str) else snd['name']
        if name.startswith('umbra:') and not os.path.exists(A + 'sounds/' + name[6:] + '.ogg'):
            bad(f"sound {ev}: file {name} missing")
    if 'subtitle' in s and s['subtitle'] not in ru:
        bad(f"sound {ev}: subtitle {s['subtitle']} not in lang")
reg_sounds = set(re.findall(r'reg\("([a-z0-9_.]+)"\)', read(J + 'registry/ModSounds.java')))
for s in reg_sounds:
    if s not in sounds:
        bad(f"sound event {s} registered but not in sounds.json")

# translatable keys used literally in Java
for p, src in java.items():
    if p.endswith('SmokeTest.java') or p.endswith('SmokeLife.java') or p.endswith('SmokeMulti.java'):
        continue
    for k in re.findall(r'translatable\("([a-z_]+\.umbra\.[a-z0-9_.]+)"(?!\s*\+)', src):
        if k.endswith('.'):
            continue
        if k not in ru:
            bad(f"{p}: lang key {k} missing")

# journal entries: title and text (creatures: look/text/weak)
jsrc = read(J + 'journal/Journal.java')
block = jsrc[jsrc.index('ENTRIES = '):jsrc.index('.toList();', jsrc.index('ENTRIES = '))]
entries = re.findall(r'"([a-z0-9_]+)"', block)
for e in entries:
    for part in ('title', 'text'):
        if f'journal.umbra.{e}.{part}' not in ru:
            bad(f"journal entry {e}: no .{part}")
# 2.11: pages with a name of their own (not their first entry's) have a title and a line too
for pg in re.findall(r'm\.put\("([a-z0-9_]+)"', jsrc):
    if pg not in entries:
        for part in ('title', 'text'):
            if f'journal.umbra.{pg}.{part}' not in ru:
                bad(f"journal page {pg}: no .{part}")
for q in re.findall(r'new Quest\("([a-z0-9_]+)"', read(J + 'quest/Quests.java')):
    for part in ('title', 'text'):
        if f'journal.umbra.quest_{q}.{part}' not in ru:
            bad(f"errand quest_{q}: no .{part}")
best = read(J + 'journal/Bestiary.java')
for c in re.findall(r'"([a-z_]+)"', best[best.index('CREATURES = '):best.index(';', best.index('CREATURES = '))]):
    for part in ('look', 'weak'):
        if f'journal.umbra.{c}.{part}' not in ru:
            bad(f"bestiary {c}: no .{part}")
# recipes the journal teaches exist
for r in set(re.findall(r'List\.of\(([^)]*)\)', jsrc[jsrc.index('RECIPES ='):jsrc.index(');', jsrc.index('RECIPES ='))])):
    for i in re.findall(r'"([a-z0-9_]+)"', r):
        if not os.path.exists(D + f'umbra/recipe/{i}.json'):
            bad(f"journal teaches recipe {i} which does not exist")
# every umbra recipe is taught by some entry (or is an ordinary vanilla-book recipe on purpose)
taught = set(re.findall(r'"([a-z0-9_]+)"', jsrc[jsrc.index('RECIPES ='):jsrc.index(');', jsrc.index('RECIPES =')) + 2]))
for p in glob.glob(D + 'umbra/recipe/*.json'):
    n = os.path.basename(p)[:-5]
    if n not in taught:
        if not re.match(r'(gloom|luminite|shadow)_(helmet|chestplate|leggings|boots)(_[23])?$', n):
            print(f"note: recipe {n} is not taught by the journal (learned the vanilla way)")

# advancements
for p in glob.glob(D + 'umbra/advancement/*.json'):
    a = json.load(open(p))
    icon = a.get('display', {}).get('icon', {}).get('id', '')
    if icon.startswith('umbra:') and icon[6:] not in all_ids:
        bad(f"advancement {p}: icon {icon} missing")
    for k in re.findall(r'"translate":\s*"([^"]+)"', json.dumps(a)):
        if k not in ru:
            bad(f"advancement {p}: {k} not in lang")

# textures: sizes sane (power of two-ish widths for items/blocks)
from PIL import Image
for p in glob.glob(A + 'textures/item/*.png') + glob.glob(A + 'textures/block/*.png'):
    w, h = Image.open(p).size
    if w != 16 and w != 32 and not (w == 16 and h % 16 == 0):
        print(f"note: {p} is {w}x{h}")

print(f"{len(problems)} problems")
for x in problems:
    print("  -", x)
sys.exit(1 if problems else 0)
