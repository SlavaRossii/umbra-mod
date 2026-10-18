#!/usr/bin/env python3
"""Umbra's armour in 3D: the vanilla armour shell plus pieces of its own for each set (luminite crystals, the shadow's
hood and cloak, gloom pauldrons and horns). Writes
  * src/main/java/dev/umbra/client/ArmorModels.java  (LayerDefinitions: the vanilla mesh + extra cubes, textures 64x96)
  * the equipment textures at 64x96: the old 64x32 layout on top, the extra cubes painted below
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
    "luminite": {"a": (175, 240, 255), "b": (90, 180, 225), "c": (230, 250, 255), "d": (70, 80, 96)},
    "shadow": {"a": (62, 48, 84), "b": (34, 26, 48), "c": (130, 80, 180), "d": (22, 18, 30)},
    "gloom": {"a": (78, 64, 100), "b": (44, 36, 60), "c": (170, 90, 230), "d": (30, 26, 38)},
}
H = 96   # texture height: the vanilla 64x32 layout on top, the extra pieces in the 64 rows below

# extras: set -> layer -> list of (part, name, (x, y, z, w, h, d), (px, py, pz, rx, ry, rz), colour, inflate)
# 2.5: each set has its own silhouette. Gloom is heavy plate with horns and spiked pauldrons; shadow a hood, a long
# cloak, a scarf and wraps; luminite a crown of crystals, crystal clusters on the shoulders, the back and the heels.
EXTRAS = {
    "gloom": {
        "outer": [
            ("head", "horn_r", (-1, -3, -1, 2, 3, 2), (-4.6, -6.6, -0.5, 0, 0, -0.6), "b", 0),
            ("head", "horn_r_tip", (-0.5, -4, -0.5, 1, 4, 1), (-6.3, -8.6, -0.5, 0.25, 0, -1.0), "d", 0),
            ("head", "horn_l", (-1, -3, -1, 2, 3, 2), (4.6, -6.6, -0.5, 0, 0, 0.6), "b", 0),
            ("head", "horn_l_tip", (-0.5, -4, -0.5, 1, 4, 1), (6.3, -8.6, -0.5, 0.25, 0, 1.0), "d", 0),
            ("head", "crest", (-0.5, -2, -5, 1, 2, 9), (0, -8.6, 0.5, 0, 0, 0), "c", 0),
            ("head", "brow", (-4, -1, -1, 8, 1, 1), (0, -5.2, -4.9, 0, 0, 0), "d", 0.2),
            ("right_arm", "pauldron_r", (-3, -2, -3.5, 6, 3, 7), (-1.6, -2.9, 0, 0, 0, 0.25), "a", 0),
            ("right_arm", "pauldron_r_spike", (-0.5, -3, -0.5, 1, 3, 1), (-2.8, -4.8, 0, 0, 0, -0.3), "d", 0),
            ("left_arm", "pauldron_l", (-3, -2, -3.5, 6, 3, 7), (1.6, -2.9, 0, 0, 0, -0.25), "a", 0),
            ("left_arm", "pauldron_l_spike", (-0.5, -3, -0.5, 1, 3, 1), (2.8, -4.8, 0, 0, 0, 0.3), "d", 0),
            ("right_arm", "gauntlet_r", (-2, 0, -2, 4, 3, 4), (-1, 7, 0, 0, 0, 0), "b", 1.2),
            ("left_arm", "gauntlet_l", (-2, 0, -2, 4, 3, 4), (1, 7, 0, 0, 0, 0), "b", 1.2),
            ("body", "gorget", (-4, -1, -2.5, 8, 2, 5), (0, 0.4, 0, 0, 0, 0), "b", 0.6),
            ("body", "ridge", (-1, 0, -1, 2, 7, 1), (0, 1.5, -3.3, 0, 0, 0), "c", 0),
            ("right_leg", "toe_r", (-2, -1, -1, 4, 2, 2), (0, 11.6, -2.9, 0, 0, 0), "a", 0),
            ("left_leg", "toe_l", (-2, -1, -1, 4, 2, 2), (0, 11.6, -2.9, 0, 0, 0), "a", 0),
            ("right_leg", "cuff_r", (-2, -1, -2, 4, 2, 4), (0, 9, 0, 0, 0, 0), "d", 1.3),
            ("left_leg", "cuff_l", (-2, -1, -2, 4, 2, 4), (0, 9, 0, 0, 0, 0), "d", 1.3),
        ],
        "inner": [
            ("body", "belt", (-4, -1, -2, 8, 2, 4), (0, 11, 0, 0, 0, 0), "d", 0.8),
            ("body", "tasset", (-3, 0, -0.5, 6, 4, 1), (0, 12, -3.1, -0.1, 0, 0), "a", 0),
            ("right_leg", "knee_r", (-2, 0, -1, 4, 3, 1), (0, 3, -2.6, 0, 0, 0), "a", 0),
            ("left_leg", "knee_l", (-2, 0, -1, 4, 3, 1), (0, 3, -2.6, 0, 0, 0), "a", 0),
        ],
    },
    "shadow": {
        "outer": [
            ("head", "hood", (-4, -8, -4, 8, 9, 8), (0, 0, 0, 0, 0, 0), "a", 1.4),
            ("head", "hood_peak", (-1.5, -2, -1.5, 3, 2, 3), (0, -9.2, -3.0, -0.6, 0, 0), "a", 0),
            ("body", "cloak", (-4.5, 0, 0, 9, 19, 1), (0, 0, 2.9, 0.16, 0, 0), "b", 0),
            ("body", "cloak_hem", (-4.5, 0, 0, 9, 2, 1), (0, 18.7, 6.0, 0.35, 0, 0), "d", 0),
            ("body", "scarf", (-4, -1, -2.5, 8, 2, 5), (0, 0.5, 0, 0, 0, 0), "c", 0.6),
            ("body", "scarf_tail", (-1, 0, -0.5, 2, 8, 1), (1.6, 1, -3.4, -0.12, 0, 0.12), "c", 0),
            ("right_arm", "mantle_r", (-3, -1, -3, 5, 2, 6), (-0.8, -2.4, 0, 0, 0, 0.4), "b", 0),
            ("left_arm", "mantle_l", (-2, -1, -3, 5, 2, 6), (0.8, -2.4, 0, 0, 0, -0.4), "b", 0),
            ("right_arm", "wrap_r", (-2, 0, -2, 4, 3, 4), (-1, 6, 0, 0, 0, 0), "d", 1.15),
            ("left_arm", "wrap_l", (-2, 0, -2, 4, 3, 4), (1, 6, 0, 0, 0, 0), "d", 1.15),
            ("right_leg", "bwrap_r", (-2, 0, -2, 4, 3, 4), (0, 8, 0, 0, 0, 0), "d", 1.2),
            ("left_leg", "bwrap_l", (-2, 0, -2, 4, 3, 4), (0, 8, 0, 0, 0, 0), "d", 1.2),
        ],
        "inner": [
            ("body", "sash", (-4, -1, -2, 8, 2, 4), (0, 11, 0, 0, 0, 0), "c", 0.7),
            ("body", "sash_end", (-1, 0, -0.5, 2, 6, 1), (2.6, 12, -2.9, -0.08, 0, -0.1), "c", 0),
            ("right_leg", "strap_r", (-2, 0, -2, 4, 1, 4), (0, 4, 0, 0, 0, 0), "d", 0.75),
            ("left_leg", "strap_l", (-2, 0, -2, 4, 1, 4), (0, 4, 0, 0, 0, 0), "d", 0.75),
        ],
    },
    "luminite": {
        "outer": [
            ("head", "circlet", (-4, -1, -4, 8, 1, 8), (0, -6.2, 0, 0, 0, 0), "d", 1.3),
            ("head", "spike0", (-1, -6, -1, 2, 6, 2), (0, -8.6, -1.5, 0, 0.785, 0), "a", 0),
            ("head", "spike1", (-1, -4, -1, 2, 4, 2), (-3, -8.4, -1, 0.2, 0.785, -0.35), "b", 0),
            ("head", "spike2", (-1, -4, -1, 2, 4, 2), (3, -8.4, -1, 0.2, 0.785, 0.35), "b", 0),
            ("head", "spike3", (-0.5, -3, -0.5, 1, 3, 1), (-4.8, -7.0, 1.5, 0, 0.785, -0.6), "c", 0),
            ("head", "spike4", (-0.5, -3, -0.5, 1, 3, 1), (4.8, -7.0, 1.5, 0, 0.785, 0.6), "c", 0),
            ("right_arm", "shard_r", (-1.5, -5, -1.5, 3, 5, 3), (-2.6, -2.6, 0, 0, 0.785, -0.45), "a", 0),
            ("right_arm", "shard_r2", (-1, -3, -1, 2, 3, 2), (-1.2, -2.8, 2.2, 0.35, 0.785, -0.2), "b", 0),
            ("left_arm", "shard_l", (-1.5, -5, -1.5, 3, 5, 3), (2.6, -2.6, 0, 0, 0.785, 0.45), "a", 0),
            ("left_arm", "shard_l2", (-1, -3, -1, 2, 3, 2), (1.2, -2.8, 2.2, 0.35, 0.785, 0.2), "b", 0),
            ("body", "core", (-1.5, -1.5, -1, 3, 3, 1), (0, 4, -3.2, 0, 0, 0.785), "c", 0),
            ("body", "back_r", (-1, -5, -1, 2, 6, 2), (-2, 3, 3.2, -0.45, 0.785, 0.25), "a", 0),
            ("body", "back_l", (-1, -5, -1, 2, 6, 2), (2, 3, 3.2, -0.45, 0.785, -0.25), "b", 0),
            ("right_leg", "spur_r", (-1, -1, -1, 2, 3, 2), (0, 10.5, 2.8, 0.6, 0.785, 0), "a", 0),
            ("left_leg", "spur_l", (-1, -1, -1, 2, 3, 2), (0, 10.5, 2.8, 0.6, 0.785, 0), "a", 0),
            ("right_leg", "anklet_r", (-2, -0.5, -2, 4, 1, 4), (0, 9.5, 0, 0, 0, 0), "d", 1.25),
            ("left_leg", "anklet_l", (-2, -0.5, -2, 4, 1, 4), (0, 9.5, 0, 0, 0, 0), "d", 1.25),
        ],
        "inner": [
            ("body", "buckle", (-1.5, -1.5, -1, 3, 3, 1), (0, 10.5, -2.7, 0, 0, 0.785), "c", 0),
            ("right_leg", "knee_r", (-1, -1.5, -1, 2, 3, 2), (0, 4, -2.6, 0, 0.785, 0), "a", 0),
            ("left_leg", "knee_l", (-1, -1.5, -1, 2, 3, 2), (0, 4, -2.6, 0, 0.785, 0), "a", 0),
        ],
    },
}


def pack(cubes):
    """Shelf-packs the UV islands of the extra cubes into the rows below the vanilla layout. Returns [(u, v)]."""
    sizes = [(2 * math.ceil(d) + 2 * math.ceil(w), math.ceil(d) + math.ceil(h)) for (x, y, z, w, h, d) in cubes]
    order = sorted(range(len(cubes)), key=lambda i: -sizes[i][1])
    out = [None] * len(cubes)
    u, v, row = 0, 32, 0
    for i in order:
        sw, sh = sizes[i]
        if u + sw > 64:
            u, v, row = 0, v + row, 0
        out[i] = (u, v)
        u += sw
        row = max(row, sh)
    assert v + row <= H, "the extra pieces do not fit in the texture"
    return out


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
            old = old.crop((0, 0, 64, 32))          # the vanilla layout; anything below is ours and is redrawn
            if layer == "outer":
                import os
                os.makedirs(T + 'humanoid_baby', exist_ok=True)
                old.save(T + 'humanoid_baby/' + s + '.png')
            img = Image.new('RGBA', (64, H), (0, 0, 0, 0))
            img.paste(old, (0, 0))
            px = img.load()
            items = layers[layer]
            uvs = pack([c[2] for c in items])
            for (part, name, (x, y, z, w, h, d), pose, ck, infl), (u, v) in zip(items, uvs):
                paint_cube(px, u, v, w, h, d, PALETTE[s][ck], rnd)
                if name == "hood":   # the face shows through
                    fw, fh, fd = math.ceil(w), math.ceil(h), math.ceil(d)
                    for xx in range(u + fd + 1, u + fd + fw - 1):
                        for yy in range(v + fd + 3, v + fd + fh - 1):
                            px[xx, yy] = (0, 0, 0, 0)
                    for xx in range(u + fd, u + fd + fw):     # a bright trim round the opening
                        px[xx, v + fd + 2] = PALETTE[s]["c"] + (255,)
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
           "    /**",
           "     * The vanilla shell with the parts not in {@code keep} emptied first, then the extra cubes, but only on kept parts.",
           "     * (addOrReplaceChild keeps the children of the part it replaces, so emptying a part afterwards left its horns,",
           "     * pauldrons and cloak behind: boots alone wore a helmet and a chestplate. Fixed in 2.6.2.)",
           "     */",
           "    public static MeshDefinition mesh(String set, boolean inner, java.util.Set<String> keep) {",
           "        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(inner ? 0.5F : 1.0F), 0.0F);",
           "        PartDefinition root = mesh.getRoot();",
           "        for (String part : new String[]{\"head\", \"body\", \"right_arm\", \"left_arm\", \"right_leg\", \"left_leg\"}) {",
           "            if (keep.contains(part)) continue;",
           "            PartDefinition empty = root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);",
           "            if (part.equals(\"head\")) empty.addOrReplaceChild(\"hat\", CubeListBuilder.create(), PartPose.ZERO);",
           "        }",
           "        switch (set + (inner ? \"/inner\" : \"/outer\")) {"]
    for s, layers in EXTRAS.items():
        for layer in ("outer", "inner"):
            out.append(f"            case \"{s}/{layer}\" -> {{")
            items = layers[layer]
            for (part, name, (x, y, z, w, h, d), (px, py, pz, rx, ry, rz), ck, infl), (u, v) in zip(items, pack([c[2] for c in items])):
                out.append(f"                if (keep.contains(\"{part}\")) root.getChild(\"{part}\").addOrReplaceChild(\"{name}\", CubeListBuilder.create().texOffs({u}, {v})"
                           f".addBox({jf(x)}, {jf(y)}, {jf(z)}, {jf(math.ceil(w))}, {jf(math.ceil(h))}, {jf(math.ceil(d))}, new CubeDeformation({jf(infl)})),")
                out.append(f"                        PartPose.offsetAndRotation({jf(px)}, {jf(py)}, {jf(pz)}, {jf(rx)}, {jf(ry)}, {jf(rz)}));")
            out.append("            }")
    out += ["            default -> { }",
            "        }",
            "        return mesh;",
            "    }", "",
            "    /** One piece only, like vanilla: the parts of other slots are emptied (a model never shows armour you do not wear). */",
            "    public static LayerDefinition create(String set, net.minecraft.world.entity.EquipmentSlot slot) {",
            "        boolean inner = slot == net.minecraft.world.entity.EquipmentSlot.LEGS;",
            "        java.util.Set<String> keep = switch (slot) {",
            "            case HEAD -> java.util.Set.of(\"head\");",
            "            case CHEST -> java.util.Set.of(\"body\", \"right_arm\", \"left_arm\");",
            "            case LEGS -> java.util.Set.of(\"body\", \"right_leg\", \"left_leg\");",
            "            default -> java.util.Set.of(\"right_leg\", \"left_leg\");",
            "        };",
            "        MeshDefinition mesh = mesh(set, inner, keep);",
            "        return LayerDefinition.create(mesh, 64, %d);" % H,
            "    }",
            "}"]
    open(JAVA, 'w').write("\n".join(out) + "\n")


if __name__ == '__main__':
    textures()
    java()
    print('ok')
