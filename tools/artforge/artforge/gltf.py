"""
Blockbench glTF (.glb) exports -> meshes we can bake into NeoForge OBJ item models.

Only what Blockbench writes is supported: triangle primitives with POSITION / NORMAL / TEXCOORD_0,
one base-colour texture per material, and nodes with translation / rotation / scale (any nesting).
"""
import io
import json
import math
import struct

from PIL import Image

_COMPONENTS = {5126: ("f", 4), 5125: ("I", 4), 5123: ("H", 2), 5121: ("B", 1)}
_WIDTH = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}


class Part:
    """One primitive in model space: triangles with positions, normals, uvs and a material index."""
    def __init__(self, name, positions, normals, uvs, triangles, material):
        self.name, self.positions, self.normals, self.uvs = name, positions, normals, uvs
        self.triangles, self.material = triangles, material


def _read(js, binary, index):
    acc = js["accessors"][index]
    view = js["bufferViews"][acc["bufferView"]]
    fmt, size = _COMPONENTS[acc["componentType"]]
    width = _WIDTH[acc["type"]]
    stride = view.get("byteStride") or size * width
    base = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
    out = []
    for i in range(acc["count"]):
        vals = struct.unpack_from("<" + fmt * width, binary, base + i * stride)
        out.append(vals if width > 1 else vals[0])
    return out


def _quat_matrix(q):
    x, y, z, w = q
    return [[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
            [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
            [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]]


def _node_matrix(node):
    """4x4 row-major local transform."""
    if "matrix" in node:
        m = node["matrix"]   # column-major
        return [[m[c * 4 + r] for c in range(4)] for r in range(4)]
    t = node.get("translation", [0, 0, 0])
    r = _quat_matrix(node.get("rotation", [0, 0, 0, 1]))
    s = node.get("scale", [1, 1, 1])
    return [[r[i][0] * s[0], r[i][1] * s[1], r[i][2] * s[2], t[i]] for i in range(3)] + [[0, 0, 0, 1]]


def _mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def _apply(m, p, w=1.0):
    return tuple(m[i][0] * p[0] + m[i][1] * p[1] + m[i][2] * p[2] + m[i][3] * w for i in range(3))


def load_glb(path):
    """Returns (parts, textures): parts in model space, textures as RGBA images indexed by material."""
    data = open(path, "rb").read()
    magic, _, _ = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67:
        raise ValueError(f"{path} is not a .glb file")
    offset, js, binary = 12, None, b""
    while offset < len(data):
        length, kind = struct.unpack_from("<II", data, offset)
        chunk = data[offset + 8: offset + 8 + length]
        if kind == 0x4E4F534A:
            js = json.loads(chunk)
        elif kind == 0x004E4942:
            binary = chunk
        offset += 8 + length

    images = []
    for im in js.get("images", []):
        view = js["bufferViews"][im["bufferView"]]
        raw = binary[view.get("byteOffset", 0): view.get("byteOffset", 0) + view["byteLength"]]
        images.append(Image.open(io.BytesIO(raw)).convert("RGBA"))
    textures = []
    for mat in js.get("materials", []):
        tex = mat.get("pbrMetallicRoughness", {}).get("baseColorTexture")
        textures.append(images[js["textures"][tex["index"]]["source"]] if tex else Image.new("RGBA", (1, 1), (255, 255, 255, 255)))

    parts = []

    def visit(index, parent):
        node = js["nodes"][index]
        world = _mul(parent, _node_matrix(node))
        if "mesh" in node:
            for prim in js["meshes"][node["mesh"]]["primitives"]:
                if prim.get("mode", 4) != 4:
                    continue
                attrs = prim["attributes"]
                pos = [_apply(world, p) for p in _read(js, binary, attrs["POSITION"])]
                nrm = [_apply(world, n, 0.0) for n in _read(js, binary, attrs["NORMAL"])] if "NORMAL" in attrs else None
                if nrm:
                    nrm = [tuple(c / (math.sqrt(sum(v * v for v in n)) or 1) for c in n) for n in nrm]
                uv = _read(js, binary, attrs["TEXCOORD_0"]) if "TEXCOORD_0" in attrs else [(0, 0)] * len(pos)
                idx = _read(js, binary, prim["indices"]) if "indices" in prim else list(range(len(pos)))
                tris = [tuple(idx[i:i + 3]) for i in range(0, len(idx) - 2, 3)]
                parts.append(Part(node.get("name", f"node{index}"), pos, nrm, uv, tris, prim.get("material", 0)))
        for child in node.get("children", []):
            visit(child, world)

    identity = [[1 if i == j else 0 for j in range(4)] for i in range(4)]
    scene = js["scenes"][js.get("scene", 0)]
    for root in scene["nodes"]:
        visit(root, identity)
    return parts, textures
