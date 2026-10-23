#!/usr/bin/env python3
"""Compatibility check (CI): what is out for Minecraft 26.3 on NeoForge, most downloaded first, in the categories that
matter here (sounds, looks, shaders). Writes the list to the file given."""
import json
import sys
import urllib.parse
import urllib.request

UA = {"User-Agent": "SlavaRossii/umbra-mod compat-check (github.com/SlavaRossii/umbra-mod)"}


def search(facets, limit=60):
    url = "https://api.modrinth.com/v2/search?" + urllib.parse.urlencode({"facets": json.dumps(facets), "limit": limit, "index": "downloads"})
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30) as r:
        return json.load(r)["hits"]


out = []
for title, facets in [
    ("Mods for 26.3 NeoForge, client side, most downloaded", [["versions:26.3"], ["categories:neoforge"], ["project_type:mod"], ["client_side:required", "client_side:optional"]]),
    ("Shader packs for 26.3", [["versions:26.3"], ["project_type:shader"]]),
]:
    out.append(f"## {title}")
    for h in search(facets):
        out.append(f"- `{h['slug']}` {h['title']} — {h['downloads']} dl — {', '.join(h['categories'][:6])} — {h['description'][:110]}")
    out.append("")
open(sys.argv[1], "w").write("\n".join(out))
print("\n".join(out))
