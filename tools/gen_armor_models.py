#!/usr/bin/env python3
"""Umbra's armour in 3D: the vanilla armour shell plus pieces of its own for each set (luminite crystals, the shadow's
hood and cloak, gloom pauldrons and horns). Writes
  * src/main/java/dev/umbra/client/ArmorModels.java  (LayerDefinitions: the vanilla mesh + extra cubes, textures 64x64)
  * the equipment textures at 64x64: the old 64x32 layout on top, the extra cubes painted below
  * 64x32 copies for baby mobs in humanoid_baby/ (their model keeps the vanilla layout)
Run from the repo root: python3 tools/gen_armor_models.py
"""
import json
import math
import random

from PIL import Image

T = 'src/main/resources/assets/umbra/textures/entity/equipment/'
EQ = 'src/main/resources/assets/umbra/equipment/'
JAVA = 'src/main/java/dev/umbra/client/ArmorModels.java'

PALETTE = {
    "luminite": {"a": (175, 240, 255), "b": (90, 180, 225), "c": (230, 250, 255)},
    "shadow": {"a": (34, 24, 48), "b": (22, 14, 32), "c": (120, 70, 170)},
    "gloom": {"a": (70, 58, 92), "b": (44, 36, 60), "c": (170, 90, 230)},
}

# extras: set -> layer ("outer"/"inner") -> list of (part, name, (u, v), (x, y, z, w, h, d), (px, py, pz, rx, ry, rz), colour key)
EXTRAS = {
    "luminite": {
        "outer": [
            ("head", "spike0", (0, 32), (-1, -6, -1, 2, 6, 2), (0, -8, 0, 0, 0.785, 0), "a"),
            ("head", "spike1", (8, 32), (-1, -4, -1, 2, 4, 2), (-3, -8, 1, 0.25, 0.785, -0.35), "b"),
            ("head", "spike2", (16, 32), (-1, -4, -1, 2, 4, 2), (3, -8, 1, 0.25, 0.785, 0.35), "b"),
            ("right_arm", "shard_r", (0, 40), (-2, -4, -2, 4, 4, 4), (-2, -2, 0, 0, 0.785, -0.3), "a"),
            ("left_arm", "shard_l", (16, 40), (-2, -4, -2, 4, 4, 4), (2, -2, 0, 0, 0.785, 0.3), "a"),
            ("body", "core", (32, 32), (-1.5, -1.5, -1, 3, 3, 1), (0, 4, -3, 0, 0, 0.785), "c"),
        ],
        "inner": [
            ("body", "buckle", (0, 32), (-1.5, -1.5, -1, 3, 3, 1), (0, 10, -2.6, 0, 0, 0.785), "c"),
        ],
    },
    "shadow": {
        "outer": [
            ("head", "hood", (0, 32), (-5.5, -10, -5.5, 11, 11, 11), (0, 0, 0, 0, 0, 0), "a"),
            ("body", "cloak", (44, 32), (-4.5, 0, 0, 9, 16, 1), (0, 0, 2.8, 0.12, 0, 0), "b"),
            ("body", "scarf", (0, 54), (-4.5, -1, -3, 9, 2, 6), (0, 0, 0, 0, 0, 0), "c"),
        ],
        "inner": [
            ("body", "sash", (0, 32), (-4.5, -1, -2.5, 9, 2, 5), (0, 11, 0, 0, 0, 0), "c"),
        ],
    },
    "gloom": {
        "outer": [
            ("right_arm", "pauldron_r", (0, 32), (-3.5, -2, -3, 6, 3, 6), (-1, -2.5, 0, 0, 0, 0.2), "a"),
            ("left_arm", "pauldron_l", (24, 32), (-2.5, -2, -3, 6, 3, 6), (1, -2.5, 0, 0, 0, -0.2), "a"),
            ("head", "horn_r", (48, 32), (-0.5, -5, -0.5, 1, 5, 1), (-4, -7, 0, 0, 0, -0.5), "b"),
            ("head", "horn_l", (52, 32), (-0.5, -5, -0.5, 1, 5, 1), (4, -7, 0, 0, 0, 0.5), "b"),
            ("body", "ridge", (0, 44), (-3, 0, -1, 6, 6, 1), (0, 1, -2.6, 0, 0, 0), "c"),
        ],
        "inner": [
            ("right_leg", "knee_r", (0, 32), (-2, 0, -1, 4, 3, 1), (0, 3, -2.4, 0, 0, 0), "a"),
            ("left_leg", "knee_l", (12, 32), (-2, 0, -1, 4, 3, 1), (0, 3, -2.4, 0, 0, 0), "a"),
        ],
    },
}


def clamp(v):
    return max(0, min(255, int(v)))


def paint_cube(px, u, v, w, h, d, col, rnd):
    w, h, d = math.ceil(w), math.ceil(h), math.ceil(d)
    faces = {"up": (u + d, v, w, d, 1.15), "down": (u + d + w, v, w, d, 0.7),
             "right": (u, v + d, d, h, 0.85), "front": (u + d, v + d, w, h, 1.0),
             "left": (u + d + w, v + d, d, h, 0.85), "back": (u + 2 * d + w, v + d, w, h, 0.8)}
    for name, (x0, y0, fw, fh, k) in faces.items():
        for x in range(x0, x0 + fw):
            for y in range(y0, y0 + fh):
                g = 1.0 - 0.25 * (y - y0) / max(1, fh)
                edge = x in (x0, x0 + fw - 1) or y in (y0, y0 + fh - 1)
                kk = k * g * (1.18 if edge and name in ("front", "up") else 1.0)
                n = rnd.randint(-8, 8)
                px[x, y] = tuple(clamp(c * kk + n) for c in col) + (255,)


def textures():
    for s, layers in EXTRAS.items():
        rnd = random.Random(s)
        for layer, folder in (("outer", "humanoid"), ("inner", "humanoid_leggings")):
            old = Image.open(T + folder + '/' + s + '.png').convert('RGBA')
            if old.size == (64, 64):          # already converted: keep the top half as it is
                old = old.crop((0, 0, 64, 32))
            if layer == "outer":
                import os
                os.makedirs(T + 'humanoid_baby', exist_ok=True)
                old.save(T + 'humanoid_baby/' + s + '.png')
            img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
            img.paste(old, (0, 0))
            px = img.load()
            for part, name, (u, v), (x, y, z, w, h, d), pose, ck in layers[layer]:
                paint_cube(px, u, v, w, h, d, PALETTE[s][ck], rnd)
            img.save(T + folder + '/' + s + '.png')


def jf(v):
    s = f"{float(v):.4f}".rstrip("0").rstrip(".")
    if "." not in s:
        s += ".0"
    return s + "F"


def java():
    out = ["package dev.umbra.client;", "",
           "import net.minecraft.client.model.HumanoidModel;",
           "import net.minecraft.client.model.geom.ModelLayerLocation;",
           "import net.minecraft.client.model.geom.PartPose;",
           "import net.minecraft.client.model.geom.builders.CubeDeformation;",
           "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
           "import net.minecraft.client.model.geom.builders.LayerDefinition;",
           "import net.minecraft.client.model.geom.builders.MeshDefinition;",
           "import net.minecraft.client.model.geom.builders.PartDefinition;",
           "import net.minecraft.resources.Identifier;", "",
           "/** GENERATED by tools/gen_armor_models.py, do not edit by hand. Umbra's armour: the vanilla shell plus pieces of its own. */",
           "public final class ArmorModels {",
           "    private ArmorModels() {}", "",
           "    public static final String[] SETS = {%s};" % ", ".join('"%s"' % s for s in EXTRAS), "",
           "    public static ModelLayerLocation layer(String set, boolean inner) {",
           "        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, \"armor_\" + set), inner ? \"inner\" : \"outer\");",
           "    }", "",
           "    public static LayerDefinition create(String set, boolean inner) {",
           "        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(inner ? 0.5F : 1.0F), 0.0F);",
           "        PartDefinition root = mesh.getRoot();",
           "        switch (set + (inner ? \"/inner\" : \"/outer\")) {"]
    for s, layers in EXTRAS.items():
        for layer in ("outer", "inner"):
            out.append(f"            case \"{s}/{layer}\" -> {{")
            for part, name, (u, v), (x, y, z, w, h, d), (px, py, pz, rx, ry, rz), ck in layers[layer]:
                out.append(f"                root.getChild(\"{part}\").addOrReplaceChild(\"{name}\", CubeListBuilder.create().texOffs({u}, {v})"
                           f".addBox({jf(x)}, {jf(y)}, {jf(z)}, {jf(math.ceil(w))}, {jf(math.ceil(h))}, {jf(math.ceil(d))}),")
                out.append(f"                        PartPose.offsetAndRotation({jf(px)}, {jf(py)}, {jf(pz)}, {jf(rx)}, {jf(ry)}, {jf(rz)}));")
            out.append("            }")
    out += ["            default -> { }",
            "        }",
            "        return LayerDefinition.create(mesh, 64, 64);",
            "    }",
            "}"]
    open(JAVA, 'w').write("\n".join(out) + "\n")


if __name__ == '__main__':
    textures()
    java()
    print('ok')
