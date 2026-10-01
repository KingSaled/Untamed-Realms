"""
Cuboid 3D models: one Python definition -> Minecraft block/item model JSON, a Blockbench .bbmodel
(so artists can keep refining by hand) and an isometric PNG preview rendered in pure Python.

    board = Model("notice_board", textures={"wood": wood_png, "paper": paper_png})
    board.cube((1, 0, 7), (3, 16, 9), "wood")
    board.cube((3, 8, 7.75), (7, 13, 8), "paper", faces=("north", "south"))
"""
import base64, io, json, math, uuid
from dataclasses import dataclass, field
from PIL import Image, ImageDraw

FACE_NAMES = ("north", "south", "east", "west", "up", "down")


@dataclass
class Cube:
    frm: tuple
    to: tuple
    texture: str
    faces: tuple = FACE_NAMES
    face_textures: dict = field(default_factory=dict)
    rotation: dict = None  # {"angle": 22.5, "axis": "y", "origin": [8, 8, 8]}
    name: str = "cube"

    def uv(self, face):
        """Blockbench-style auto UV derived from the cube's position."""
        (x1, y1, z1), (x2, y2, z2) = self.frm, self.to
        if face in ("north", "south"):
            u1, u2 = (16 - x2, 16 - x1) if face == "north" else (x1, x2)
            return [u1, 16 - y2, u2, 16 - y1]
        if face in ("east", "west"):
            u1, u2 = (16 - z2, 16 - z1) if face == "east" else (z1, z2)
            return [u1, 16 - y2, u2, 16 - y1]
        return [x1, z1, x2, z2]


class Model:
    def __init__(self, name, textures, namespace="urcore", folder="block", parent="minecraft:block/block"):
        self.name = name
        self.textures = textures          # key -> PIL image
        self.namespace = namespace
        self.folder = folder
        self.parent = parent
        self.cubes = []
        self.particle = next(iter(textures))

    def cube(self, frm, to, texture, faces=FACE_NAMES, rotation=None, name=None, **face_textures):
        c = Cube(tuple(frm), tuple(to), texture, tuple(faces), face_textures, rotation, name or f"cube{len(self.cubes)}")
        self.cubes.append(c)
        return c

    def texture_path(self, key):
        return f"{self.namespace}:{self.folder}/{self.name}_{key}"

    # ------------------------------------------------------------------ Minecraft JSON
    def to_minecraft_json(self):
        elements = []
        for c in self.cubes:
            el = {"from": list(c.frm), "to": list(c.to), "faces": {}}
            if c.rotation:
                el["rotation"] = c.rotation
            for f in c.faces:
                tex = c.face_textures.get(f, c.texture)
                face = {"uv": c.uv(f), "texture": f"#{tex}"}
                if _touches_edge(c, f):
                    face["cullface"] = f
                el["faces"][f] = face
            elements.append(el)
        textures = {k: self.texture_path(k) for k in self.textures}
        textures["particle"] = self.texture_path(self.particle)
        return {"parent": self.parent, "textures": textures, "elements": elements}

    # ------------------------------------------------------------------ Blockbench
    def to_bbmodel(self):
        tex_keys = list(self.textures)
        textures = []
        for key in tex_keys:
            img = self.textures[key]
            buf = io.BytesIO()
            img.save(buf, "PNG")
            textures.append({
                "path": "", "name": f"{self.name}_{key}.png", "folder": self.folder, "namespace": self.namespace,
                "id": str(len(textures)), "width": img.width, "height": img.height,
                "uv_width": 16, "uv_height": 16, "particle": key == self.particle,
                "render_mode": "default", "visible": True, "mode": "bitmap", "saved": True,
                "uuid": str(uuid.uuid4()), "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode(),
            })
        elements = []
        for c in self.cubes:
            faces = {}
            for f in FACE_NAMES:
                if f in c.faces:
                    faces[f] = {"uv": c.uv(f), "texture": tex_keys.index(c.face_textures.get(f, c.texture))}
                else:
                    faces[f] = {"uv": [0, 0, 0, 0], "texture": None}
            el = {"name": c.name, "box_uv": False, "rescale": False, "locked": False, "render_order": "default",
                  "from": list(c.frm), "to": list(c.to), "autouv": 0, "color": 0,
                  "origin": list(c.rotation["origin"]) if c.rotation else [8, 8, 8],
                  "faces": faces, "type": "cube", "uuid": str(uuid.uuid4())}
            if c.rotation:
                rot = [0, 0, 0]
                rot["xyz".index(c.rotation["axis"])] = c.rotation["angle"]
                el["rotation"] = rot
            elements.append(el)
        return {
            "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
            "name": self.name, "model_identifier": "", "parent": self.parent,
            "ambientocclusion": True, "front_gui_light": False,
            "resolution": {"width": 16, "height": 16},
            "elements": elements, "outliner": [e["uuid"] for e in elements], "textures": textures,
        }

    def _mirrored_z(self):
        m = Model(self.name, self.textures, self.namespace, self.folder, self.parent)
        m.cubes = [_mirror_cube(c) for c in self.cubes]
        return m

    # ------------------------------------------------------------------ preview
    def render_iso(self, scale=12, front="north"):
        """Isometric preview showing the model's front (default north) face, top lit."""
        if front == "north":
            return self._mirrored_z().render_iso(scale, front="south")
        size = int(scale * 16 * 2.2)
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        cos30 = math.cos(math.radians(30))
        ox, oy = size / 2, size * 0.62

        def project(x, y, z):
            # x to the right-down, z to the left-down, y up
            sx = (x - z) * cos30 * scale
            sy = (x + z) * 0.5 * scale - y * scale
            return ox + sx - 0 * scale, oy + sy - 8 * scale

        polys = []
        for c in self.cubes:
            (x1, y1, z1), (x2, y2, z2) = c.frm, c.to
            depth_base = (x1 + x2) / 2 + (z1 + z2) / 2 + (y1 + y2) / 2
            for f, shade in (("up", 1.0), ("south", 0.82), ("east", 0.64)):
                if f not in c.faces:
                    continue
                tex = self.textures[c.face_textures.get(f, c.texture)]
                u1, v1, u2, v2 = c.uv(f)
                # corners of the face in 3D and how texture axes map onto it
                if f == "up":
                    origin, du, dv = (x1, y2, z1), (x2 - x1, 0, 0), (0, 0, z2 - z1)
                    depth = depth_base + 0.5
                elif f == "south":
                    origin, du, dv = (x1, y2, z2), (x2 - x1, 0, 0), (0, -(y2 - y1), 0)
                    depth = depth_base + 0.3
                else:
                    origin, du, dv = (x2, y2, z2), (0, 0, -(z2 - z1)), (0, -(y2 - y1), 0)
                    depth = depth_base + 0.3
                nu = max(1, int(round(abs(u2 - u1))))
                nv = max(1, int(round(abs(v2 - v1))))
                for i in range(nu):
                    for j in range(nv):
                        tu = u1 + (u2 - u1) * (i + 0.5) / nu
                        tv = v1 + (v2 - v1) * (j + 0.5) / nv
                        px = tex.getpixel((int(tu / 16 * tex.width) % tex.width, int(tv / 16 * tex.height) % tex.height))
                        if px[3] < 8:
                            continue
                        color = tuple(int(ch * shade) for ch in px[:3]) + (255,)
                        corners = []
                        for a, b in ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)):
                            p = [origin[k] + du[k] * a / nu + dv[k] * b / nv for k in range(3)]
                            corners.append(project(*p))
                        # depth along the view axis (1,1,1) of the texel centre: larger = nearer
                        centre = [origin[k] + du[k] * (i + 0.5) / nu + dv[k] * (j + 0.5) / nv for k in range(3)]
                        polys.append((sum(centre) + depth * 1e-3, corners, color))
        polys.sort(key=lambda t: t[0])
        for _, corners, color in polys:
            draw.polygon(corners, fill=color)
        bbox = img.getbbox()
        return img.crop(bbox) if bbox else img


def _mirror_cube(c):
    (x1, y1, z1), (x2, y2, z2) = c.frm, c.to
    swap = {"north": "south", "south": "north"}
    faces = tuple(swap.get(f, f) for f in c.faces)
    face_textures = {swap.get(k, k): v for k, v in c.face_textures.items()}
    return Cube((x1, y1, 16 - z2), (x2, y2, 16 - z1), c.texture, faces, face_textures, None, c.name)


def _touches_edge(c, face):
    (x1, y1, z1), (x2, y2, z2) = c.frm, c.to
    return {"north": z1 == 0, "south": z2 == 16, "west": x1 == 0, "east": x2 == 16,
            "down": y1 == 0, "up": y2 == 16}[face]


def blockstate_horizontal(model_id):
    """Blockstate JSON rotating a north-facing model to the block's `facing` property."""
    return {"variants": {
        "facing=north": {"model": model_id},
        "facing=east": {"model": model_id, "y": 90},
        "facing=south": {"model": model_id, "y": 180},
        "facing=west": {"model": model_id, "y": 270},
    }}


def write_json(path, obj):
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")
