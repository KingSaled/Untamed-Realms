"""
Sprite templates: ASCII grids where each character is a palette slot.

  '.'        transparent
  'o' / 'O'  outline / soft outline
  '1'..'5'   primary ramp (dark -> light)
  'a'..'e'   secondary ramp
  'A'..'E'   tertiary ramp
  'w'        pure white highlight, 'k' ink (palette INK, never pure black)
  anything else -> looked up in the `extra` legend

One template + different ramps = a whole tier set (iron / steel / elven / glass / ...).
"""
from PIL import Image
from . import palette as P

SLOTS_PRIMARY = "12345"
SLOTS_SECONDARY = "abcde"
SLOTS_TERTIARY = "ABCDE"


class Template:
    def __init__(self, text, extra=None):
        rows = [r for r in text.strip("\n").split("\n")]
        rows = [r.rstrip() for r in rows]
        width = max(len(r) for r in rows)
        self.rows = [r.ljust(width, ".") for r in rows]
        self.width, self.height = width, len(self.rows)
        self.extra = extra or {}

    def render(self, primary=None, secondary=None, tertiary=None, outline=P.OUTLINE, extra=None):
        legend = {".": P.TRANSPARENT, " ": P.TRANSPARENT, "o": outline, "O": P.OUTLINE_SOFT,
                  "w": (255, 255, 255, 255), "k": P.INK}
        for ramp, slots in ((primary, SLOTS_PRIMARY), (secondary, SLOTS_SECONDARY), (tertiary, SLOTS_TERTIARY)):
            if ramp:
                for i, ch in enumerate(slots):
                    legend[ch] = ramp[i]
        legend.update(self.extra)
        legend.update(extra or {})
        img = Image.new("RGBA", (self.width, self.height))
        px = img.load()
        for y, row in enumerate(self.rows):
            for x, ch in enumerate(row):
                if ch not in legend:
                    raise KeyError(f"no colour for '{ch}' at {x},{y}")
                px[x, y] = legend[ch]
        return img


def canvas(w=16, h=16):
    return Image.new("RGBA", (w, h), P.TRANSPARENT)


def outline(img, color=P.OUTLINE, diagonal=False):
    """Adds a 1px outline around opaque pixels (inside the canvas)."""
    w, h = img.size
    src = img.load()
    out = img.copy()
    dst = out.load()
    offsets = [(1, 0), (-1, 0), (0, 1), (0, -1)]
    if diagonal:
        offsets += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
    for y in range(h):
        for x in range(w):
            if src[x, y][3] != 0:
                continue
            for dx, dy in offsets:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0 and src[nx, ny] != color:
                    dst[x, y] = color
                    break
    return out


def shade_mask(mask_rows, ramp, light=(-1, -1), rim=True):
    """
    Shades a silhouette (list of strings, '#' = filled) with a ramp: pixels facing the light
    (default top-left) get lighter shades, the far edge darker, interior mid-tones.
    """
    h, w = len(mask_rows), max(len(r) for r in mask_rows)
    filled = lambda x, y: 0 <= y < h and 0 <= x < len(mask_rows[y]) and mask_rows[y][x] == "#"
    img = canvas(w, h)
    px = img.load()
    lx, ly = light
    for y in range(h):
        for x in range(w):
            if not filled(x, y):
                continue
            lit = not filled(x + lx, y + ly)
            dark = not filled(x - lx, y - ly)
            edge = any(not filled(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if lit and not dark:
                idx = 4
            elif dark and not lit:
                idx = 1 if rim else 2
            elif edge:
                idx = 3
            else:
                idx = 2 + ((x + y) % 7 == 0)
            px[x, y] = ramp[idx]
    return img


def paste(base, sprite, x, y):
    base.alpha_composite(sprite, (x, y))
    return base


def recolor(img, mapping):
    """Swaps exact colours (e.g. turn an iron sprite into a gold one ramp-by-ramp)."""
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            c = px[x, y]
            if c in mapping:
                px[x, y] = mapping[c]
    return out


def ramp_swap(img, src_ramp, dst_ramp):
    return recolor(img, {src_ramp[i]: dst_ramp[i] for i in range(5)})


def upscale(img, factor):
    return img.resize((img.width * factor, img.height * factor), Image.NEAREST)
