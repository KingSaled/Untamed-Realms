"""
3D item models from pixel art: a design is an upright picture of the item (tip at the top) drawn at
2 texels per model unit, plus a depth for every pixel. The picture is the texture; the depths become a
handful of cuboids (greedy-meshed by equal depth), so a blade can be a thin plate with a raised spine
while the guard and pommel stand out.

The whole model is rotated 45 degrees about the grip, so it sits in the hand exactly like vanilla's
diagonal sword sprite and can reuse vanilla's handheld display transforms; longer weapons simply
reach further past the top-right corner. Inventory uses a separate 32x32 icon
(NeoForge's `separate_transforms` model loader).
"""
import math

from PIL import Image

PX = 3                 # texels per model unit (48 per block: finer than vanilla, so blades can be slender)
GRIP = (3.5, 3.5)      # where vanilla's diagonal sword is held (model units)


class Design:
    def __init__(self, width, height, pivot):
        """pivot: (col, row) of the texel that sits in the hand."""
        self.img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.depth = [[0.0] * width for _ in range(height)]
        self.pivot = pivot

    @property
    def size(self):
        return self.img.size

    def put(self, x, y, color, depth):
        w, h = self.img.size
        if 0 <= x < w and 0 <= y < h:
            self.img.putpixel((x, y), color)
            self.depth[y][x] = depth

    def get(self, x, y):
        w, h = self.img.size
        if 0 <= x < w and 0 <= y < h:
            return self.img.getpixel((x, y))
        return (0, 0, 0, 0)


# ------------------------------------------------------------------------------- meshing

def _rects(design):
    """Greedy rectangles of opaque texels with equal depth: [(x0, y0, x1, y1, depth)] (x1/y1 exclusive)."""
    w, h = design.size
    used = [[False] * w for _ in range(h)]
    out = []
    for y in range(h):
        for x in range(w):
            if used[y][x] or design.get(x, y)[3] == 0:
                continue
            d = design.depth[y][x]
            x1 = x
            while x1 < w and not used[y][x1] and design.get(x1, y)[3] > 0 and design.depth[y][x1] == d:
                x1 += 1
            y1 = y + 1
            while y1 < h and all(not used[y1][i] and design.get(i, y1)[3] > 0 and design.depth[y1][i] == d for i in range(x, x1)):
                y1 += 1
            for yy in range(y, y1):
                for xx in range(x, x1):
                    used[yy][xx] = True
            out.append((x, y, x1, y1, d))
    return out


def model_json(design, texture, angle=-45.0, display=None, grip=GRIP):
    """The 3D model (Minecraft JSON). texture: e.g. 'urarsenal:item/3d/iron_sword'.

    The design is placed upright and centred in the model box (so even a greatsword fits the
    -16..32 limit), then every element is rotated by `angle` about an origin chosen so that the
    pivot texel lands exactly on GRIP - where vanilla's sword is held.
    """
    w, h = design.size
    px, py = design.pivot
    assert h / PX <= 48 and w / PX <= 48, "design too big for one model"
    ox = 8 - w / PX / 2
    oy = 8 + h / PX / 2

    def mx(col):
        return round(ox + col / PX, 4)

    def my(row):
        return round(oy - row / PX, 4)

    def u(col):
        return round(16 * col / w, 4)

    def v(row):
        return round(16 * row / h, 4)

    # rotation origin O with O + R(G - O) = GRIP, i.e. (I - R) O = GRIP - R G
    g = (ox + (px + 0.5) / PX, oy - (py + 0.5) / PX)
    a = math.radians(angle)
    c, s_ = math.cos(a), math.sin(a)
    rg = (g[0] * c - g[1] * s_, g[0] * s_ + g[1] * c)
    bx, by = grip[0] - rg[0], grip[1] - rg[1]
    m00, m01, m10, m11 = 1 - c, s_, -s_, 1 - c          # I - R
    det = m00 * m11 - m01 * m10
    origin = [round((bx * m11 - m01 * by) / det, 4), round((m00 * by - m10 * bx) / det, 4), 8]

    elements = []
    edge_u = 0.5 / PX
    for x0, y0, x1, y1, d in _rects(design):
        z0, z1 = round(8 - d / 2, 4), round(8 + d / 2, 4)
        frm = [mx(x0), my(y1), z0]
        to = [mx(x1), my(y0), z1]
        half_u = 16 * 0.5 / w
        half_v = 16 * 0.5 / h
        faces = {
            "south": {"uv": [u(x0), v(y0), u(x1), v(y1)], "texture": "#0"},
            "north": {"uv": [u(x1), v(y0), u(x0), v(y1)], "texture": "#0"},
            # sides sample the outermost texels of the rectangle (like vanilla's extruded items)
            "east": {"uv": [u(x1) - half_u, v(y0), u(x1), v(y1)], "texture": "#0"},
            "west": {"uv": [u(x0), v(y0), u(x0) + half_u, v(y1)], "texture": "#0"},
            "up": {"uv": [u(x0), v(y0), u(x1), v(y0) + half_v], "texture": "#0"},
            "down": {"uv": [u(x0), v(y1) - half_v, u(x1), v(y1)], "texture": "#0"},
        }
        el = {"from": frm, "to": to, "faces": faces}
        if angle:
            el["rotation"] = {"angle": angle, "axis": "z", "origin": origin}
        elements.append(el)
    for e in elements:
        for k in range(3):
            assert -16 <= e["from"][k] <= 32 and -16 <= e["to"][k] <= 32, f"element outside -16..32: {e['from']} {e['to']}"
    return {
        "textures": {"0": texture, "particle": texture},
        "elements": elements,
        "display": display or HANDHELD,
    }


# vanilla's bow sprite is held at the middle of its arc, top-left of centre
BOW_GRIP = (4.5, 11.5)

# vanilla item/bow transforms (measured from 1.21.1)
BOW = {
    "thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.75, 0.75, 0.75]},
}

# vanilla item/handheld transforms (measured from 1.21.1 by tools/research/inspect.py), plus ground/fixed
HANDHELD = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.75, 0.75, 0.75]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def item_json(model_3d, icon_texture, overrides=None):
    """The item model: the 3D model everywhere except the inventory, which shows the 2D icon."""
    out = {
        "loader": "neoforge:separate_transforms",
        "base": {"parent": model_3d},
        "perspectives": {
            "gui": {"parent": "minecraft:item/generated", "textures": {"layer0": icon_texture}},
        },
    }
    if overrides:
        out["overrides"] = overrides
    return out


# ------------------------------------------------------------------------------- inventory icon

def icon(design, size=32, outline=None):
    """The design laid diagonally (tip top-right, like vanilla) and shrunk into a size x size icon."""
    big = design.img.resize((design.size[0] * 4, design.size[1] * 4), Image.NEAREST)
    rot = big.rotate(-45, resample=Image.NEAREST, expand=True)
    box = rot.getbbox()
    rot = rot.crop(box)
    margin = 1
    s = (size - 2 * margin) / max(rot.size)
    tw, th = max(1, round(rot.size[0] * s)), max(1, round(rot.size[1] * s))
    small = _majority_resize(rot, tw, th)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # sit the grip end in the bottom-left like vanilla
    out.paste(small, (margin, size - margin - th), small)
    if outline:
        out = _outline(out, outline)
    return out


def _majority_resize(img, w, h):
    """Downscale keeping hard pixels: each output pixel takes the most common opaque colour of its block
    (transparent if most of the block is empty)."""
    src = img.load()
    sw, sh = img.size
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(w):
            x0, x1 = int(x * sw / w), max(int(x * sw / w) + 1, int((x + 1) * sw / w))
            y0, y1 = int(y * sh / h), max(int(y * sh / h) + 1, int((y + 1) * sh / h))
            counts, filled, total = {}, 0, 0
            for yy in range(y0, y1):
                for xx in range(x0, x1):
                    c = src[xx, yy]
                    total += 1
                    if c[3] > 0:
                        filled += 1
                        counts[c] = counts.get(c, 0) + 1
            if filled * 2.5 >= total and counts:
                out.putpixel((x, y), max(counts.items(), key=lambda kv: kv[1])[0])
    return out


def _outline(img, color):
    w, h = img.size
    src = img.copy()
    px = src.load()
    out = img.copy()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0 and any(0 <= x + dx < w and 0 <= y + dy < h and px[x + dx, y + dy][3] > 0
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), color)
    return out


# ------------------------------------------------------------------------------- preview renderer

SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


def _rot(p, axis, deg, origin):
    a = math.radians(deg)
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    c, s = math.cos(a), math.sin(a)
    if axis == "z":
        x, y = x * c - y * s, x * s + y * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        y, z = y * c - z * s, y * s + z * c
    return (x + origin[0], y + origin[1], z + origin[2])


FACE_CORNERS = {
    # corners as (x,y,z) picks from (from,to), ordered: top-left, top-right, bottom-right, bottom-left of the texture
    "south": [("f", "t", "t"), ("t", "t", "t"), ("t", "f", "t"), ("f", "f", "t")],
    "north": [("t", "t", "f"), ("f", "t", "f"), ("f", "f", "f"), ("t", "f", "f")],
    "east": [("t", "t", "t"), ("t", "t", "f"), ("t", "f", "f"), ("t", "f", "t")],
    "west": [("f", "t", "f"), ("f", "t", "t"), ("f", "f", "t"), ("f", "f", "f")],
    "up": [("f", "t", "f"), ("t", "t", "f"), ("t", "t", "t"), ("f", "t", "t")],
    "down": [("f", "f", "t"), ("t", "f", "t"), ("t", "f", "f"), ("f", "f", "f")],
}


def render(model, texture, yaw=0.0, pitch=0.0, scale=12, size=None, bg=(32, 28, 36, 255)):
    """Orthographic software render of a JSON model (elements with z/y/x rotations), MC-style face shading."""
    tex = texture.convert("RGBA")
    tw, th = tex.size
    tpx = tex.load()
    quads = []
    for el in model["elements"]:
        f, t = el["from"], el["to"]
        pick = {"f": f, "t": t}
        rot = el.get("rotation")
        for face, spec in el["faces"].items():
            pts = []
            for cx, cy, cz in FACE_CORNERS[face]:
                p = (pick[cx][0], pick[cy][1], pick[cz][2])
                if rot:
                    p = _rot(p, rot["axis"], rot["angle"], rot["origin"])
                p = _rot(p, "y", yaw, (8, 8, 8))
                p = _rot(p, "x", pitch, (8, 8, 8))
                pts.append(p)
            # back-face cull: the face normal must point towards the viewer (+z)
            ax, ay = pts[1][0] - pts[0][0], pts[1][1] - pts[0][1]
            bx, by = pts[3][0] - pts[0][0], pts[3][1] - pts[0][1]
            if ax * by - ay * bx > -1e-9:
                continue
            depth = sum(p[2] for p in pts) / 4
            quads.append((depth, pts, spec["uv"], SHADE[face]))
    quads.sort(key=lambda q: q[0])
    xs = [p[0] for q in quads for p in q[1]] or [0]
    ys = [p[1] for q in quads for p in q[1]] or [0]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    W = size[0] if size else int((maxx - minx) * scale) + 8
    H = size[1] if size else int((maxy - miny) * scale) + 8
    img = Image.new("RGBA", (W, H), bg)
    out = img.load()
    cx = W / 2 - (minx + maxx) / 2 * scale
    cy = H / 2 + (miny + maxy) / 2 * scale

    def scr(p):
        return (cx + p[0] * scale, cy - p[1] * scale)

    for _, pts, uv, shade in quads:
        s = [scr(p) for p in pts]
        # parallelogram: P = s0 + a*(s1-s0) + b*(s3-s0)
        ex, ey = s[1][0] - s[0][0], s[1][1] - s[0][1]
        fx, fy = s[3][0] - s[0][0], s[3][1] - s[0][1]
        det = ex * fy - ey * fx
        if abs(det) < 1e-6:
            continue
        x0, x1 = int(min(q[0] for q in s)), int(max(q[0] for q in s)) + 1
        y0, y1 = int(min(q[1] for q in s)), int(max(q[1] for q in s)) + 1
        u0, v0, u1, v1 = uv
        for yy in range(max(0, y0), min(H, y1)):
            for xx in range(max(0, x0), min(W, x1)):
                dx, dy = xx + 0.5 - s[0][0], yy + 0.5 - s[0][1]
                a = (dx * fy - dy * fx) / det
                b = (ex * dy - ey * dx) / det
                if not (0 <= a <= 1 and 0 <= b <= 1):
                    continue
                tu = (u0 + (u1 - u0) * a) / 16 * tw
                tv = (v0 + (v1 - v0) * b) / 16 * th
                c = tpx[min(tw - 1, max(0, int(tu))), min(th - 1, max(0, int(tv)))]
                if c[3] < 128:
                    continue
                out[xx, yy] = (int(c[0] * shade), int(c[1] * shade), int(c[2] * shade), 255)
    return img
