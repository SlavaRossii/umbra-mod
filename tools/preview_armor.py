"""Preview of Umbra's 3D armour on a standing figure: python3 tools/preview_armor.py out.png"""
import importlib.util, math, sys
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
from PIL import Image
spec = importlib.util.spec_from_file_location("g", "tools/gen_armor_models.py"); g = importlib.util.module_from_spec(spec); spec.loader.exec_module(g)

def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]]); Ry = np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]]); Rz = np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])
    return Rz @ Ry @ Rx

# vanilla humanoid parts: name -> pivot, cube (x,y,z,w,h,d), uv
BASE = {"head": ((0, 0, 0), (-4, -8, -4, 8, 8, 8), (0, 0)), "body": ((0, 0, 0), (-4, 0, -2, 8, 12, 4), (16, 16)),
        "right_arm": ((-5, 2, 0), (-3, -2, -2, 4, 12, 4), (40, 16)), "left_arm": ((5, 2, 0), (-1, -2, -2, 4, 12, 4), (40, 16)),
        "right_leg": ((-1.9, 12, 0), (-2, 0, -2, 4, 12, 4), (0, 16)), "left_leg": ((1.9, 12, 0), (-2, 0, -2, 4, 12, 4), (0, 16))}

def faces(c, uv, grow):
    x0, y0, z0, w, h, d = c
    u, v = uv
    x0 -= grow; y0 -= grow; z0 -= grow
    W, H, D = w + 2 * grow, h + 2 * grow, d + 2 * grow
    return {
        "front": ([x0, x0+W, x0+W, x0], [y0, y0, y0+H, y0+H], [z0]*4, (u+d, v+d, w, h)),
        "back": ([x0, x0+W, x0+W, x0], [y0, y0, y0+H, y0+H], [z0+D]*4, (u+2*d+w, v+d, w, h)),
        "right": ([x0]*4, [y0, y0, y0+H, y0+H], [z0, z0+D, z0+D, z0], (u, v+d, d, h)),
        "left": ([x0+W]*4, [y0, y0, y0+H, y0+H], [z0, z0+D, z0+D, z0], (u+d+w, v+d, d, h)),
        "top": ([x0, x0+W, x0+W, x0], [y0]*4, [z0, z0, z0+D, z0+D], (u+d, v, w, d)),
    }

fig = plt.figure(figsize=(15, 6))
for i, (s, layers) in enumerate(g.EXTRAS.items()):
    ax = fig.add_subplot(1, 3, i + 1, projection="3d")
    polys, colors = [], []
    for layer, folder, grow, parts in (("outer", "humanoid", 1.0, ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg"]),
                                       ("inner", "humanoid_leggings", 0.5, ["body", "right_leg", "left_leg"])):
        tex = np.array(Image.open(g.T + folder + "/" + s + ".png").convert("RGBA")).astype(float) / 255
        cubes = [(BASE[p][0], (0, 0, 0), BASE[p][1], BASE[p][2], grow) for p in parts if not (layer == "outer" and p.endswith("leg") and False)]
        items = layers[layer]
        for (part, name, c, pose, ck, infl), uv in zip(items, g.pack([it[2] for it in items])):
            px, py, pz, rx, ry, rz = pose
            pp = BASE[part][0]
            cubes.append(((pp[0] + px, pp[1] + py, pp[2] + pz), (rx, ry, rz), c, uv, infl))
        for piv, r, c, uv, grow_ in cubes:
            M = rot(*r); t = np.array(piv, float)
            for name, (xs, ys, zs, (fu, fv, fw, fh)) in faces(c, uv, grow_).items():
                pts = [M @ np.array([x, y, z]) + t for x, y, z in zip(xs, ys, zs)]
                polys.append([(q[0], -q[2], -q[1]) for q in pts])
                region = tex[int(fv):int(fv + max(1, math.ceil(fh))), int(fu):int(fu + max(1, math.ceil(fw)))]
                region = region.reshape(-1, 4)
                region = region[region[:, 3] > 0.1] if len(region) else region
                col = region.mean(axis=0) if len(region) else np.array([1, 0, 1, 1])
                col[3] = 1
                colors.append(col)
    ax.add_collection3d(Poly3DCollection(polys, facecolors=colors, edgecolors=(0, 0, 0, 0.3), linewidths=0.3))
    ax.set_xlim(-14, 14); ax.set_ylim(-14, 14); ax.set_zlim(-26, 4)
    ax.view_init(elev=12, azim=float(sys.argv[2]) if len(sys.argv) > 2 else -55); ax.set_title(s); ax.set_axis_off()
plt.tight_layout(); plt.savefig(sys.argv[1], dpi=80)
