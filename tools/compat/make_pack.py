#!/usr/bin/env python3
"""Builds the Umbra pack (CI): a Modrinth .mrpack (opens in the Modrinth App, Prism Launcher, ATLauncher) and a plain
.zip of the same files for putting into an instance by hand. The mods are the ones listed in tools/compat/pack.json,
at their newest build for 26.3 / NeoForge, with what they need; Umbra itself goes in from build/libs.
usage: make_pack.py <umbra jar> <out dir>"""
import hashlib
import json
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(__file__))
import fetch_mods as fm  # noqa: E402

NEOFORGE = "26.3.0.76"


def main():
    umbra, out = sys.argv[1], sys.argv[2]
    cfg = json.load(open(os.path.join(os.path.dirname(__file__), "pack.json")))
    os.makedirs(out, exist_ok=True)
    stage = out + "/stage"
    os.makedirs(stage + "/mods", exist_ok=True)
    os.makedirs(stage + "/shaderpacks", exist_ok=True)
    os.makedirs(stage + "/config", exist_ok=True)
    files, lines, done, queue = [], [], set(), [(m, None) for m in cfg["mods"]]
    while queue:
        slug, why = queue.pop(0)
        if slug in done:
            continue
        done.add(slug)
        vs = fm.versions(slug, ["neoforge"])
        if not vs:
            if why is None:
                raise SystemExit(f"no 26.3 NeoForge build of {slug}")
            print(f"skipped {slug}, asked for by {why}: no NeoForge build (a Fabric-only dependency in its listing)")
            continue
        v = vs[0]
        f = next((x for x in v["files"] if x["primary"]), v["files"][0])
        proj = fm.get(f"/project/{v['project_id']}")
        fm.download(f["url"], f"{stage}/mods/{f['filename']}")
        files.append({"path": f"mods/{f['filename']}", "hashes": {"sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"]},
                      "env": {"client": "required", "server": "unsupported"}, "downloads": [f["url"]], "fileSize": f["size"]})
        lines.append(f"- {proj['title']} {v['version_number']}" + (f" (нужен для {why})" if why else ""))
        for d in v.get("dependencies", []):
            if d["dependency_type"] == "required" and d.get("project_id"):
                queue.append((d["project_id"], proj["title"]))
    shader_file = None
    for s in cfg["shaders"]:
        vs = fm.versions(s, ["iris"]) or fm.versions(s, ["iris"], game=False)
        v = vs[0]
        f = next((x for x in v["files"] if x["primary"]), v["files"][0])
        proj = fm.get(f"/project/{v['project_id']}")
        fm.download(f["url"], f"{stage}/shaderpacks/{f['filename']}")
        files.append({"path": f"shaderpacks/{f['filename']}", "hashes": {"sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"]},
                      "env": {"client": "required", "server": "unsupported"}, "downloads": [f["url"]], "fileSize": f["size"]})
        lines.append(f"- шейдер {proj['title']} {v['version_number']}" + (" (включён)" if s == cfg["default_shader"] else " (можно выбрать в настройках шейдеров)"))
        if s == cfg["default_shader"]:
            shader_file = f["filename"]
    iris = f"enableShaders=true\nshaderPack={shader_file}\n"
    open(stage + "/config/iris.properties", "w").write(iris)
    umbra_name = os.path.basename(umbra)
    version = umbra_name.removeprefix("umbra-").removesuffix(".jar")
    index = {"formatVersion": 1, "game": "minecraft", "versionId": version, "name": cfg["name"], "summary": cfg["summary"],
             "files": files, "dependencies": {"minecraft": "26.3", "neoforge": NEOFORGE}}
    name = f"umbra-pack-{version}"
    with zipfile.ZipFile(f"{out}/{name}.mrpack", "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("modrinth.index.json", json.dumps(index, ensure_ascii=False, indent=1))
        z.write(umbra, f"overrides/mods/{umbra_name}")
        z.writestr("overrides/config/iris.properties", iris)
    readme = (f"{cfg['name']} ({version})\n\nMinecraft 26.3, NeoForge {NEOFORGE}.\n\n"
              "Что внутри:\n- Umbra " + version + "\n" + "\n".join(lines) + "\n\n"
              "Как поставить вручную: создай в лаунчере профиль Minecraft 26.3 с NeoForge " + NEOFORGE + " и положи в его папку\n"
              "содержимое этого архива (папки mods, shaderpacks, config) с заменой.\n"
              "Шейдеры выключаются и меняются: Настройки → Видео → Шейдеры (или клавиша O).\n")
    with zipfile.ZipFile(f"{out}/{name}.zip", "w", zipfile.ZIP_DEFLATED) as z:
        z.write(umbra, f"mods/{umbra_name}")
        for root, _, fs in os.walk(stage):
            for fn in fs:
                p = os.path.join(root, fn)
                z.write(p, os.path.relpath(p, stage))
        z.writestr("README.txt", readme)
    open(f"{out}/pack.md", "w").write(readme)
    print(readme)


if __name__ == "__main__":
    main()
