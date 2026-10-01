"""Minimal NBT writer (enough for structure templates). No dependencies."""
import gzip, struct

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE, TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = range(13)

class Int(int): pass
class Byte(int): pass
class Short(int): pass
class Long(int): pass
class Float(float): pass
class Double(float): pass

def _tag_type(v):
    if isinstance(v, Byte): return TAG_BYTE
    if isinstance(v, Short): return TAG_SHORT
    if isinstance(v, Long): return TAG_LONG
    if isinstance(v, Float): return TAG_FLOAT
    if isinstance(v, Double): return TAG_DOUBLE
    if isinstance(v, bool): return TAG_BYTE
    if isinstance(v, int): return TAG_INT
    if isinstance(v, float): return TAG_DOUBLE
    if isinstance(v, str): return TAG_STRING
    if isinstance(v, list): return TAG_LIST
    if isinstance(v, dict): return TAG_COMPOUND
    raise TypeError(type(v))

def _payload(v, out):
    t = _tag_type(v)
    if t == TAG_BYTE: out += struct.pack(">b", int(v))
    elif t == TAG_SHORT: out += struct.pack(">h", v)
    elif t == TAG_INT: out += struct.pack(">i", v)
    elif t == TAG_LONG: out += struct.pack(">q", v)
    elif t == TAG_FLOAT: out += struct.pack(">f", v)
    elif t == TAG_DOUBLE: out += struct.pack(">d", v)
    elif t == TAG_STRING:
        b = v.encode("utf-8"); out += struct.pack(">H", len(b)) + b
    elif t == TAG_LIST:
        et = _tag_type(v[0]) if v else TAG_END
        out += struct.pack(">bi", et, len(v))
        for x in v: _payload(x, out)
    elif t == TAG_COMPOUND:
        for k, x in v.items():
            out += struct.pack(">b", _tag_type(x)); _payload(k, out); _payload(x, out)
        out += b"\x00"

def write_gzip(path, root, name=""):
    out = bytearray()
    out += struct.pack(">b", TAG_COMPOUND); _payload(name, out); _payload(root, out)
    with gzip.open(path, "wb") as f:
        f.write(bytes(out))

DATA_VERSION_1_21_1 = 3955

def structure(size, palette, blocks):
    """size=(x,y,z); palette=list of block names; blocks=[(x,y,z,palette_index)]."""
    return {
        "DataVersion": Int(DATA_VERSION_1_21_1),
        "size": [Int(size[0]), Int(size[1]), Int(size[2])],
        "palette": [{"Name": n} for n in palette],
        "blocks": [{"pos": [Int(x), Int(y), Int(z)], "state": Int(s)} for x, y, z, s in blocks],
        "entities": [],
    }
