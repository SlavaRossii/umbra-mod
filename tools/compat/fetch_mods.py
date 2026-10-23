#!/usr/bin/env python3
"""Compatibility check (CI): fetch mods from Modrinth for Minecraft 26.3 / NeoForge into a client's mods folder.
usage: fetch_mods.py <set name> <out dir>    (sets in tools/compat/sets.json)
Writes <out>/mods/*.jar, <out>/shaderpacks/*.zip (+ config/iris.properties), and <out>/fetch.md: what was found, at
which version, and what was not (no build for 26.3 yet). Required dependencies are fetched too."""
import json
import os
import sys
import urllib.parse
import urllib.request

API = "https://api.modrinth.com/v2"
UA = {"User-Agent": "SlavaRossii/umbra-mod compat-check (github.com/SlavaRossii/umbra-mod)"}
GAME = "26.3"


def get(path, **q):
    url = API + path + ("?" + urllib.parse.urlencode({k: json.dumps(v) if isinstance(v, list) else v for k, v in q.items()}) if q else "")
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30) as r:
        return json.load(r)


def download(url, to):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120) as r, open(to, "wb") as f:
        f.write(r.read())


def versions(project, loaders, game=True):
    q = {"loaders": loaders}
    if game:
        q["game_versions"] = [GAME]
    try:
        return get(f"/project/{project}/version", **q)
    except Exception as e:  # unknown slug
        return None


def main():
    name, out = sys.argv[1], sys.argv[2]
    sets = json.load(open(os.path.join(os.path.dirname(__file__), "sets.json")))
    s = sets[name]
    os.makedirs(out + "/mods", exist_ok=True)
    report = [f"# {name}", ""]
    done, queue = set(), [(m, "asked") for m in s.get("mods", [])]
    manifest = []
    while queue:
        slug, why = queue.pop(0)
        if slug in done:
            continue
        done.add(slug)
        vs = versions(slug, ["neoforge"])
        if vs is None:
            report.append(f"- **{slug}**: no such project on Modrinth")
            continue
        if not vs:
            other = versions(slug, ["neoforge"], game=False) or []
            last = sorted({g for v in other for g in v["game_versions"]})[-3:] if other else []
            report.append(f"- **{slug}**: NO build for {GAME} NeoForge" + (f" (latest NeoForge builds for {', '.join(last)})" if last else " (no NeoForge builds at all)"))
            continue
        v = vs[0]
        f = next((x for x in v["files"] if x["primary"]), v["files"][0])
        download(f["url"], f"{out}/mods/{f['filename']}")
        proj = get(f"/project/{v['project_id']}")
        report.append(f"- {proj['title']} (`{proj['slug']}`) {v['version_number']} — {f['filename']}" + ("" if why == "asked" else f" (needed by {why})"))
        manifest.append({"slug": proj["slug"], "title": proj["title"], "version": v["version_number"], "file": f["filename"], "url": f["url"],
                         "sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"], "size": f["size"], "client": proj["client_side"], "server": proj["server_side"]})
        for d in v.get("dependencies", []):
            if d["dependency_type"] == "required" and d.get("project_id"):
                queue.append((d["project_id"], proj["slug"]))
    if s.get("shader"):
        os.makedirs(out + "/shaderpacks", exist_ok=True)
        vs = versions(s["shader"], ["iris"]) or versions(s["shader"], ["iris"], game=False) or []
        if vs:
            v = vs[0]
            f = next((x for x in v["files"] if x["primary"]), v["files"][0])
            download(f["url"], f"{out}/shaderpacks/{f['filename']}")
            os.makedirs(out + "/config", exist_ok=True)
            open(out + "/config/iris.properties", "w").write(f"enableShaders=true\nshaderPack={f['filename']}\n")
            report.append(f"- shader pack {s['shader']} {v['version_number']} — {f['filename']}")
            manifest.append({"slug": s["shader"], "version": v["version_number"], "file": f["filename"], "url": f["url"], "sha1": f["hashes"]["sha1"],
                             "sha512": f["hashes"]["sha512"], "size": f["size"], "shader": True})
        else:
            report.append(f"- shader pack {s['shader']}: not found")
    open(out + "/fetch.md", "w").write("\n".join(report) + "\n")
    json.dump(manifest, open(out + "/manifest.json", "w"), indent=1)
    print("\n".join(report))


if __name__ == "__main__":
    main()
