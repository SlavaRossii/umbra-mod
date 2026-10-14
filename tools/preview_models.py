import importlib.util, math, sys
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
from PIL import Image
spec = importlib.util.spec_from_file_location("m", "tools/models.py"); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]]); Ry = np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]]); Rz = np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])
    return Rz @ Ry @ Rx

models = [f() for f in m.MODELS]
cols = 4
rows = (len(models) + cols - 1) // cols
fig = plt.figure(figsize=(16, 4 * rows))
for idx, model in enumerate(models):
    size = m.pack(model)
    tex = np.array(m.paint(model, size)).astype(float) / 255
    parts = {p.name: p for p in model.parts}
    def world(pname):
        p = parts[pname]; M = rot(*p.rot); t = np.array(p.pivot, float)
        if p.parent != "root":
            PM, Pt = world(p.parent); return PM @ M, PM @ t + Pt
        return M, t
    ax = fig.add_subplot(rows, cols, idx + 1, projection="3d")
    polys, colors = [], []
    for p in model.parts:
        M, t = world(p.name)
        for c in p.cubes:
            w, h, d = math.ceil(c.w), math.ceil(c.h), math.ceil(c.d)
            x0, y0, z0 = c.x, c.y, c.z
            corners = lambda xs, ys, zs: [M @ np.array([x, y, z]) + t for x, y, z in zip(xs, ys, zs)]
            u, v = c.u, c.v
            faces = {
                "front": ([x0, x0+w, x0+w, x0], [y0, y0, y0+h, y0+h], [z0]*4, (u+d, v+d, w, h)),
                "back": ([x0, x0+w, x0+w, x0], [y0, y0, y0+h, y0+h], [z0+d]*4, (u+2*d+w, v+d, w, h)),
                "right": ([x0]*4, [y0, y0, y0+h, y0+h], [z0, z0+d, z0+d, z0], (u, v+d, d, h)),
                "left": ([x0+w]*4, [y0, y0, y0+h, y0+h], [z0, z0+d, z0+d, z0], (u+d+w, v+d, d, h)),
                "top": ([x0, x0+w, x0+w, x0], [y0]*4, [z0, z0, z0+d, z0+d], (u+d, v, w, d)),
                "bottom": ([x0, x0+w, x0+w, x0], [y0+h]*4, [z0, z0, z0+d, z0+d], (u+d+w, v, w, d)),
            }
            for name, (xs, ys, zs, (fu, fv, fw, fh)) in faces.items():
                pts = corners(xs, ys, zs)
                # model space -> plot space: x, -z (front towards viewer), -y (up)
                polys.append([(q[0], -q[2], -q[1]) for q in pts])
                region = tex[int(fv):int(fv+max(1,fh)), int(fu):int(fu+max(1,fw))]
                col = region.reshape(-1, 4).mean(axis=0) if region.size else np.array([1, 0, 1, 1])
                col[3] = 1
                colors.append(col)
    pc = Poly3DCollection(polys, facecolors=colors, edgecolors=(0, 0, 0, 0.25), linewidths=0.3)
    ax.add_collection3d(pc)
    allp = np.array([q for poly in polys for q in poly])
    mid = (allp.max(0) + allp.min(0)) / 2; r = (allp.max(0) - allp.min(0)).max() / 2 + 2
    ax.set_xlim(mid[0]-r, mid[0]+r); ax.set_ylim(mid[1]-r, mid[1]+r); ax.set_zlim(-24-0, -24 + 2*r)
    ax.view_init(elev=15, azim=-60)
    ax.set_title(model.name); ax.set_axis_off()
    # ground plane line
    ax.plot([mid[0]-r, mid[0]+r], [mid[1], mid[1]], [-24, -24], color="green", lw=0.8)
plt.tight_layout()
plt.savefig(sys.argv[1], dpi=80)
