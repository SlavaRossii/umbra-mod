#!/usr/bin/env python3
"""Writes docs/RECIPES.md: every Umbra recipe with its grid, in Russian, and which journal entry teaches it.
Run from the repo root: python3 tools/gen_recipes_doc.py"""
import glob
import json
import os
import re

A = 'src/main/resources/assets/umbra/'
D = 'src/main/resources/data/umbra/recipe/'
ru = json.load(open(A + 'lang/ru_ru.json', encoding='utf-8'))
VANILLA = {
    "stick": "палка", "string": "нить", "iron_ingot": "железный слиток", "iron_nugget": "железный самородок",
    "gold_ingot": "золотой слиток", "gold_nugget": "золотой самородок", "copper_ingot": "медный слиток",
    "honeycomb": "соты", "diamond": "алмаз", "book": "книга", "glass": "стекло", "cobbled_deepslate": "глубинный булыжник",
    "torch": "факел", "glowstone_dust": "светопыль", "spruce_planks": "еловые доски", "spruce_slab": "еловая плита",
    "oak_planks": "дубовые доски", "amethyst_shard": "осколок аметиста", "netherite_ingot": "незеритовый слиток",
    "smithing_table": "кузнечный стол", "bone": "кость", "sugar": "сахар", "glass_bottle": "пузырёк", "paper": "бумага",
    "ender_pearl": "жемчуг Края", "redstone": "редстоун", "lantern": "фонарь", "coal": "уголь", "flint": "кремень",
    "feather": "перо", "arrow": "стрела", "potion": "зелье", "water_bucket": "ведро воды", "leather": "кожа",
    "crafting_table": "верстак", "pale_oak_planks": "доски бледного дуба", "ink_sac": "чернильный мешок", "glass_pane": "стеклянная панель", "compass": "компас", "sweet_berries": "сладкие ягоды", "ender_eye": "око Края", "amethyst_block": "аметистовый блок", "anvil": "наковальня", "candle": "свеча", "white_wool": "белая шерсть", "chest": "сундук", "deepslate": "глубинный сланец", "planks": "доски", "logs": "брёвна",
}


def name(ref):
    tag = ref.startswith('#')
    ref = ref.lstrip('#')
    ns, path = ref.split(':', 1) if ':' in ref else ('minecraft', ref)
    if ns == 'umbra':
        for k in ('item.umbra.' + path, 'block.umbra.' + path):
            if k in ru:
                return ru[k]
        return path
    n = VANILLA.get(path.split('/')[-1], path.replace('_', ' '))
    return ('любые ' + n) if tag else n


def ingredient(v):
    if isinstance(v, str):
        return name(v)
    if isinstance(v, list):
        return ' или '.join(ingredient(x) for x in v)
    if isinstance(v, dict):
        return name(v.get('item') or v.get('tag') or v.get('id') or '?')
    return str(v)


# which journal entry teaches which recipe
jsrc = open('src/main/java/dev/umbra/journal/Journal.java', encoding='utf-8').read()
teach = {}
block = jsrc[jsrc.index('RECIPES ='):jsrc.index(');', jsrc.index('RECIPES =')) + 2]
for entry, lst in re.findall(r'Map\.entry\("([a-z0-9_]+)", List\.of\(([^)]*)\)\)', block):
    for r in re.findall(r'"([a-z0-9_]+)"', lst):
        teach.setdefault(r, entry)

rows = []
for p in sorted(glob.glob(D + '*.json')):
    rid = os.path.basename(p)[:-5]
    r = json.load(open(p))
    t = r['type'].split(':')[-1]
    res = r.get('result', {})
    rname = name(res.get('id', '?')) + (f" ×{res['count']}" if res.get('count', 1) > 1 else '')
    if t == 'crafting_shaped':
        key = {k: ingredient(v) for k, v in r['key'].items()}
        grid = '<br>'.join('`' + ''.join(c if c != ' ' else '·' for c in row) + '`' for row in r['pattern'])
        legend = ', '.join(f"{k} — {v}" for k, v in key.items())
        how = f"{grid}<br>{legend}"
    elif t == 'crafting_shapeless':
        how = 'без формы: ' + ' + '.join(ingredient(i) for i in r['ingredients'])
    elif t in ('smelting', 'blasting', 'smoking'):
        how = ('переплавка' if t == 'smelting' else 'доменная печь') + ': ' + ingredient(r['ingredient'])
    elif t.startswith('smithing'):
        how = 'кузнечный стол: ' + ' + '.join(ingredient(r[k]) for k in ('template', 'base', 'addition') if k in r)
    else:
        how = t
    entry = teach.get(rid)
    opens = ru.get(f'journal.umbra.{entry}.title', entry) if entry else 'известен сразу'
    rows.append((rname, how, opens))

out = ["# Рецепты Umbra", "",
       "Сгенерировано `tools/gen_recipes_doc.py` из файлов рецептов мода. Сетка читается сверху вниз, `·` — пустая клетка.",
       "Колонка «Открывается» — запись дневника, после которой рецепт появляется в книге рецептов (скрафтить можно и раньше, если знаешь сетку).",
       "", "| Что получается | Как сделать | Открывается |", "|---|---|---|"]
for rname, how, opens in sorted(rows, key=lambda x: x[0].lower()):
    out.append(f"| {rname} | {how} | {opens} |")
open('docs/RECIPES.md', 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(len(rows), 'recipes')
