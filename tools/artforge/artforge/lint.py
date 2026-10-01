"""
Style checks for generated sprites - the rules of docs/ART_STYLE.md as code. `python3 -m artforge check`
runs them on every sprite, so CI rejects art that breaks the design language.

Each sprite is checked against its family's rules:
  size      exact canvas size
  alpha     only fully transparent or fully opaque pixels (no anti-aliasing)
  palette   every colour comes from an approved ramp (artforge.palette)
  outline   every silhouette edge pixel is an outline colour
  centered  the silhouette's bounding box is centred on the canvas
  colors    at most N distinct colours
"""
from PIL import Image

from . import palette as P


def approved_colors():
    colors = {P.OUTLINE, P.OUTLINE_SOFT, P.INK, (255, 255, 255, 255)}
    for value in vars(P).values():
        if isinstance(value, list) and value and isinstance(value[0], tuple) and len(value[0]) == 4:
            colors.update(value)
        elif isinstance(value, dict):
            for ramp in value.values():
                if isinstance(ramp, list):
                    colors.update(ramp)
    for ramp in P.SKIN_TONES:
        colors.update(ramp)
    return colors


APPROVED = approved_colors()
OUTLINES = {P.OUTLINE, P.OUTLINE_SOFT}

FAMILIES = {
    # name: size, centered, max colours
    "item": dict(size=(16, 16), centered=False, colors=16),
    "icon": dict(size=(16, 16), centered=True, colors=12),
    "weapon": dict(size=(16, 16), centered=False, colors=14),
}


def bbox(img):
    box = img.getbbox()
    return box  # (x0, y0, x1, y1) with x1/y1 exclusive


def check_sprite(name, img, family):
    """Returns a list of human-readable problems (empty = passes)."""
    rules = FAMILIES[family]
    problems = []
    img = img.convert("RGBA")
    if img.size != rules["size"]:
        return [f"{name}: size {img.size}, expected {rules['size']}"]
    w, h = img.size
    px = img.load()
    colors = set()
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] == 0:
                continue
            if c[3] != 255:
                problems.append(f"{name}: semi-transparent pixel at {x},{y}")
                break
            colors.add(c)
            if c not in APPROVED:
                problems.append(f"{name}: colour #{c[0]:02x}{c[1]:02x}{c[2]:02x} at {x},{y} is not in the palette")
            edge = x in (0, w - 1) or y in (0, h - 1) or any(px[x + dx, y + dy][3] == 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge and c not in OUTLINES:
                problems.append(f"{name}: edge pixel at {x},{y} is not outlined")
    if len(colors) > rules["colors"]:
        problems.append(f"{name}: {len(colors)} colours (max {rules['colors']})")
    if rules["centered"]:
        box = bbox(img)
        if box:
            cx = (box[0] + box[2] - 1) / 2
            cy = (box[1] + box[3] - 1) / 2
            if abs(cx - (w - 1) / 2) > 0.01 or abs(cy - (h - 1) / 2) > 0.01:
                problems.append(f"{name}: not centred (bbox centre {cx},{cy}, canvas centre {(w - 1) / 2})")
    # de-duplicate repeated messages of the same kind per sprite
    seen, out = set(), []
    for p in problems:
        key = p.split(" at ")[0]
        if key not in seen:
            seen.add(key)
            out.append(p)
    return out


def centered_glyph(rows, name):
    """Asserts a glyph's drawn pixels span a box centred in its grid (even-sized grids only)."""
    h, w = len(rows), len(rows[0])
    assert w % 2 == 0 and h % 2 == 0, f"glyph {name}: grid must be even-sized so it can sit on the medallion centre"
    xs = [x for row in rows for x, ch in enumerate(row) if ch != "."]
    ys = [y for y, row in enumerate(rows) for ch in row if ch != "."]
    assert min(xs) + max(xs) == w - 1, f"glyph {name}: not horizontally centred ({min(xs)}..{max(xs)} in {w})"
    assert min(ys) + max(ys) == h - 1, f"glyph {name}: not vertically centred ({min(ys)}..{max(ys)} in {h})"
