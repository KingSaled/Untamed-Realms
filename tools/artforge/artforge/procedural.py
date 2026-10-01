"""Procedural sprite generators: textures and icons that are better computed than hand-placed."""
import math, random
from PIL import Image
from . import palette as P
from .sprite import canvas, outline


def noise_texture(ramp, size=16, seed=0, weights=(1, 3, 6, 3, 1)):
    """Grainy material texture (stone, cloth) using a weighted pick from the ramp."""
    rnd = random.Random(seed)
    img = canvas(size, size)
    px = img.load()
    pool = [i for i, w in enumerate(weights) for _ in range(w)]
    for y in range(size):
        for x in range(size):
            px[x, y] = ramp[rnd.choice(pool)]
    return img


def planks(ramp, size=16, seed=1, boards=4, vertical=True):
    """Wood planks: board seams, grain streaks and a few knots."""
    rnd = random.Random(seed)
    img = canvas(size, size)
    px = img.load()
    bw = size // boards
    for b in range(boards):
        base = 2 + rnd.choice([0, 0, 1])
        offset = rnd.randrange(size)
        for i in range(size):
            for j in range(bw):
                x, y = (b * bw + j, i) if vertical else (i, b * bw + j)
                idx = base
                if j == 0:
                    idx = 0                       # seam
                elif j == bw - 1:
                    idx = max(1, base - 1)        # shadow side
                elif (i * 7 + j * 3 + offset) % 11 == 0:
                    idx = min(4, base + 1)        # grain highlight
                elif (i + offset) % 9 == 0 and j == bw // 2:
                    idx = base - 1                # grain streak
                px[x, y] = ramp[idx]
        if rnd.random() < 0.5:                    # knot
            kx = b * bw + bw // 2
            ky = rnd.randrange(2, size - 2)
            if vertical:
                px[kx, ky] = ramp[0]
    return img


def parchment(size=16, seed=3, lines=True):
    """Aged paper with ink lines - for notices, scrolls, quest UI."""
    rnd = random.Random(seed)
    img = canvas(size, size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            edge = min(x, y, size - 1 - x, size - 1 - y)
            idx = 3 if edge > 1 else 2
            if rnd.random() < 0.12:
                idx -= 1
            px[x, y] = P.PARCHMENT[max(0, min(4, idx))]
    if lines:
        ink = P.INK
        for y in range(3, size - 2, 2):
            start = 2 + rnd.randrange(0, 2)
            end = size - 2 - rnd.randrange(0, 4)
            for x in range(start, end):
                if rnd.random() < 0.85:
                    px[x, y] = ink
    return img


def orb(ramp, radius=6, size=16, glow=True):
    """Shaded sphere - gems, orbs, spell cores."""
    img = canvas(size, size)
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if d > radius:
                continue
            light = (-(x - c) - (y - c)) / (radius * 1.6) + 0.5 - (d / radius) * 0.35
            idx = max(0, min(4, int(light * 5)))
            px[x, y] = ramp[idx]
    # specular highlight
    hx, hy = int(c - radius * 0.4), int(c - radius * 0.4)
    px[hx, hy] = (255, 255, 255, 255)
    return outline(img, P.OUTLINE)


def glyph(rows, color):
    img = canvas(len(rows[0]), len(rows))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "#":
                px[x, y] = color
    return img


def spell_icon(school_ramp, glyph_rows, size=16):
    """Round school-coloured medallion with a bright glyph - used for spell HUD/book icons."""
    img = canvas(size, size)
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if d <= 7.2:
                ring = d > 6.0
                idx = 2 if ring else (1 if d > 4.5 else 0)
                if ring and (x + y) % 2 == 0:
                    idx = 3
                px[x, y] = school_ramp[idx]
    g = glyph(glyph_rows, school_ramp[4])
    gx = (size - g.width) // 2
    gy = (size - g.height) // 2
    img.alpha_composite(g, (gx, gy))
    # soft inner glow pixel above the glyph
    return outline(img, P.OUTLINE)


def stretch_to(img, size):
    return img.resize(size, Image.NEAREST)


def medallion_icon(ramp, glyph_rows, size=16):
    """
    v2 spell / ability icon (docs/ART_STYLE.md): a medallion symmetric about the canvas centre
    (7.5, 7.5) with a metal rim lit from the top-left, a dark field, and an 8x8 glyph centred on it
    with a one-pixel drop shadow. '#' glyph pixels get the bright tones, '+' the mid tone.
    """
    from .lint import centered_glyph
    centered_glyph(glyph_rows, "glyph")
    img = canvas(size, size)
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            d = math.hypot(dx, dy)
            if d > 7.6:
                continue
            light = -(dx + dy) / (math.sqrt(2) * max(d, 0.01))       # +1 facing top-left, -1 bottom-right
            if d > 6.6:
                px[x, y] = P.OUTLINE
            elif d > 5.4:
                px[x, y] = ramp[4 if light > 0.55 else 3 if light > 0.1 else 2 if light > -0.45 else 1]
            else:
                px[x, y] = ramp[1] if (light > 0.35 and d > 3.6) else ramp[0]
    gh, gw = len(glyph_rows), len(glyph_rows[0])
    ox, oy = (size - gw) // 2, (size - gh) // 2
    filled = lambda gx, gy: 0 <= gy < gh and 0 <= gx < gw and glyph_rows[gy][gx] != "."
    for gy in range(gh):                                  # shadow first, so the glyph draws over it
        for gx in range(gw):
            if filled(gx, gy) and not filled(gx + 1, gy + 1):
                px[ox + gx + 1, oy + gy + 1] = P.OUTLINE
    for gy in range(gh):
        for gx in range(gw):
            ch = glyph_rows[gy][gx]
            if ch == ".":
                continue
            if ch == "+":
                px[ox + gx, oy + gy] = ramp[2]
            else:
                lit = not filled(gx - 1, gy) or not filled(gx, gy - 1)
                px[ox + gx, oy + gy] = ramp[4] if lit else ramp[3]
    return img
