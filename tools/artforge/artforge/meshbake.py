"""
Bakes a hand-made weapon mesh (artforge.gltf) into a NeoForge OBJ item model that sits in the hand
like vanilla's sword:

* the weapon is modelled upright (blade or head up, +Y), at any size;
* it is scaled so its length matches its type (LENGTHS, in pixels; a vanilla sword is ~19), so a dagger
  is always shorter than a sword and a greatsword longer, whatever size it was modelled at;
* it is turned 45 degrees and moved so the middle of the part named "handle" (or "grip") lands where
  vanilla's sword is held, so vanilla's handheld poses fit it; without such a part, a point 20% up
  from the bottom is used.
"""
import math

from PIL import Image, ImageDraw

# overall length in pixels (1/16 block) per weapon type
LENGTHS = {"dagger": 14, "sword": 20, "war_axe": 19, "mace": 18, "greatsword": 27, "battleaxe": 26, "warhammer": 26, "bow": 20}
GRIP = (3.5, 3.5)     # vanilla's sword grip in the 16x16 item frame
BOW_GRIP = (4.5, 11.5)


def kind_of(name):
    for kind in sorted(LENGTHS, key=len, reverse=True):
        if name.endswith(kind) or f"{kind}_pulling" in name:
            return kind
    return "sword"


def _bounds(points):
    xs, ys, zs = zip(*points)
    return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))


def grip_point(parts):
    named = [p for part in parts if any(k in part.name.lower() for k in ("handle", "grip")) for p in part.positions]
    allp = [p for part in parts for p in part.positions]
    if named:
        lo, hi = _bounds(named)
        return tuple((a + b) / 2 for a, b in zip(lo, hi))
    lo, hi = _bounds(allp)
    return ((lo[0] + hi[0]) / 2, lo[1] + 0.2 * (hi[1] - lo[1]), (lo[2] + hi[2]) / 2)


def bake(parts, kind):
    """Moves the parts into item-model pixel space in place. Returns (scale, grip in model, length before)."""
    allp = [p for part in parts for p in part.positions]
    lo, hi = _bounds(allp)
    length = hi[1] - lo[1]
    scale = LENGTHS[kind] / length
    gx, gy, gz = grip_point(parts)
    tx, ty = BOW_GRIP if kind == "bow" else GRIP
    c, s = math.cos(math.radians(-45)), math.sin(math.radians(-45))
    for part in parts:
        moved = []
        for x, y, z in part.positions:
            x, y, z = (x - gx) * scale, (y - gy) * scale, (z - gz) * scale
            moved.append((x * c - y * s + tx, x * s + y * c + ty, z + 8))
        part.positions = moved
        if part.normals:
            part.normals = [(x * c - y * s, x * s + y * c, z) for x, y, z in part.normals]
    return scale, (gx, gy, gz), length


def gui_transform(parts):
    """Fits the baked model in an inventory slot, centred."""
    lo, hi = _bounds([p for part in parts for p in part.positions])
    span = max(hi[0] - lo[0], hi[1] - lo[1])
    scale = round(min(1.0, 15.0 / span), 3)
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    return {"rotation": [0, 0, 0], "translation": [round(-scale * (cx - 8), 3), round(-scale * (cy - 8), 3), 0],
            "scale": [scale, scale, scale]}


def to_obj(name, parts, material_names):
    """OBJ + MTL text. Positions are written in blocks; UVs flipped to OBJ's bottom-left origin."""
    obj = [f"# {name}: baked by artforge.meshbake from art/blockbench", f"mtllib {name}.mtl"]
    v_off = 0
    for part in parts:
        obj.append(f"o {part.name}")
        for x, y, z in part.positions:
            obj.append(f"v {x / 16:.6f} {y / 16:.6f} {z / 16:.6f}")
        for u, v in part.uvs:
            obj.append(f"vt {u:.6f} {1 - v:.6f}")
        normals = part.normals or [(0, 1, 0)] * len(part.positions)
        for x, y, z in normals:
            obj.append(f"vn {x:.6f} {y:.6f} {z:.6f}")
        obj.append(f"usemtl {material_names[part.material]}")
        for a, b, c in part.triangles:
            a, b, c = a + 1 + v_off, b + 1 + v_off, c + 1 + v_off
            obj.append(f"f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}")
        v_off += len(part.positions)
    mtl = []
    for m in sorted(set(material_names[p.material] for p in parts)):
        mtl += [f"newmtl {m}", "Kd 1.000000 1.000000 1.000000", "d 1.000000", f"map_Kd #{m}", ""]
    return "\n".join(obj) + "\n", "\n".join(mtl)


def render(parts, textures, yaw=0, pitch=0, scale=12, size=(240, 240), grid=False):
    """A quick software preview: flat-shaded triangles coloured from their texture, painter-sorted."""
    img = Image.new("RGBA", size, (32, 28, 36, 255))
    d = ImageDraw.Draw(img)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def view(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, z = x * cy + z * sy, -x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z

    if grid:
        for i in range(17):
            a = [size[0] / 2 + (i - 8) * scale, size[1] / 2 - 8 * scale]
            d.line([(a[0], a[1]), (a[0], a[1] + 16 * scale)], fill=(60, 56, 66, 255))
            b = [size[0] / 2 - 8 * scale, size[1] / 2 + (i - 8) * scale]
            d.line([(b[0], b[1]), (b[0] + 16 * scale, b[1])], fill=(60, 56, 66, 255))
    light = (-0.4, 0.7, 0.6)
    tris = []
    for part in parts:
        tex = textures[part.material]
        for t in part.triangles:
            pts = [view(part.positions[i]) for i in t]
            u = sum(part.uvs[i][0] for i in t) / 3
            v = sum(part.uvs[i][1] for i in t) / 3
            col = tex.getpixel((min(tex.width - 1, max(0, int(u * tex.width))), min(tex.height - 1, max(0, int(v * tex.height)))))
            if col[3] < 20:
                continue
            ax, ay, az = (pts[1][k] - pts[0][k] for k in range(3))
            bx, by, bz = (pts[2][k] - pts[0][k] for k in range(3))
            n = (ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx)
            ln = math.sqrt(sum(c * c for c in n)) or 1
            shade = 0.55 + 0.45 * abs(sum(n[k] / ln * light[k] for k in range(3)))
            tris.append((sum(p[2] for p in pts) / 3, pts, tuple(int(c * shade) for c in col[:3]) + (255,)))
    for _, pts, col in sorted(tris, key=lambda t: -t[0]):
        d.polygon([(size[0] / 2 + p[0] * scale, size[1] / 2 - p[1] * scale) for p in pts], fill=col)
    return img
