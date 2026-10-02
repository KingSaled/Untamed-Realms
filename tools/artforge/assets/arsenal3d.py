"""
3D arsenal: Skyrim's weapons as Minecraft models (artforge/voxel.py turns each design into cuboids).

Every design is drawn upright at 3 texels per model unit, modelled on Skyrim's own weapons (reference
pictures are fetched by .github/workflows/art-reference.yml and never stored in the repo):

  Iron      plain straight blade with a ridge, short down-turned guard, wrapped grip, bell pommel
  Steel     fullered blade, wide knotwork guard with drooping ends, trefoil pommel
  Orcish    brassy-green falchion that widens to the tip, hook at the base, crescent pommel
  Dwarven   bronze blade with engraved ridge lines, pointed shoulder guard, ribbed grip, disc pommel
  Elven     golden leaf blade, fanned wing guard, forked pommel
  Glass     green feathered blade on a moonstone spine, curled horn guard
  Ebony     slender, curved black blade with silvery engraving, small swept guard
  Daedric   black serrated blade with red veins, hooked tip, horned guard, spiked pommel

Depths (model units): blade edges 0.5, blade core 1.0, grips 1.5, pommels 2, guards 2.5.
"""
import math

from PIL import Image, ImageDraw

from artforge import palette as P
from artforge.voxel import Design

EDGE, CORE, GRIP_D, POMMEL_D, GUARD_D = 0.5, 1.0, 1.5, 2.0, 2.5


# ------------------------------------------------------------------------------- drawing kit

def noise(x, y, seed=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF


def poly_pixels(size, pts):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).polygon([(round(x, 2), round(y, 2)) for x, y in pts], fill=255)
    px = m.load()
    return [(x, y) for y in range(size[1]) for x in range(size[0]) if px[x, y]]


def shape(d, pts, ramp, depth, grit=0.0, seed=1, base=2):
    """A solid part (guard, pommel, collar): lit from the left, highlight on top rows, shadow at the bottom."""
    pix = poly_pixels(d.size, pts)
    if not pix:
        return
    xs = [p[0] for p in pix]
    ys = [p[1] for p in pix]
    cx = (min(xs) + max(xs)) / 2
    top, bot = min(ys), max(ys)
    pset = set(pix)
    for x, y in pix:
        i = base
        if x < cx - 0.5:
            i += 1
        elif x > cx + 0.5:
            i -= 1
        if (x, y - 1) not in pset:
            i += 1          # upper edge catches the light
        if (x, y + 1) not in pset:
            i -= 1          # underside in shadow
        if grit and noise(x, y, seed) < grit:
            i += 1 if noise(y, x, seed) < 0.5 else -1
        d.put(x, y, ramp[max(0, min(4, i))], depth)


def blade(d, cx, y_base, length, half, ramp, center=lambda t: 0.0, grit=0.0, seed=3, pattern=None,
          edge=EDGE, core=CORE):
    """
    A blade from row y_base (t=0) up to its tip (t=1). half(t) -> (left, right) half-widths in texels;
    center(t) shifts the blade sideways (curved blades). Shading: lit bevel on the left, the spine in
    the middle, shadowed bevel on the right; pattern(x, y, t, s) may override a texel's colour.
    """
    rows = {}
    for y in range(int(y_base - length) - 2, int(y_base) + 1):
        t = (y_base - (y + 0.5)) / length
        if not 0 <= t <= 1:
            continue
        c = cx + center(t)
        l, r = half(t)
        for x in range(int(c - l) - 1, int(c + r) + 2):
            xc = x + 0.5
            s = (xc - c) / l if xc < c else (xc - c) / max(r, 1e-6)
            if l <= 0 and xc < c or r <= 0 and xc >= c:
                continue
            if abs(s) <= 1.0:
                rows[(x, y)] = (t, s)
    for (x, y), (t, s) in rows.items():
        if s < -0.6:
            i = 4 if (x - 1, y) not in rows else 3
        elif s < 0:
            i = 3
        elif s < 0.6:
            i = 2
        else:
            i = 1 if (x + 1, y) in rows else 2
        if grit and noise(x, y, seed) < grit:
            i += 1 if noise(y, x, seed + 1) < 0.5 else -1
        col = ramp[max(0, min(4, i))]
        if pattern:
            over = pattern(x, y, t, s)
            if over is not None:
                col = over
        d.put(x, y, col, edge if abs(s) > 0.6 else core)
    return rows


def grip(d, cx, y_top, y_bot, half, ramp, style="wrap", seed=5, accent=None):
    """A grip: lit on the left; 'wrap' = diagonal leather bands, 'rings' = ribbed metal, 'plain'."""
    for y in range(int(y_top), int(y_bot) + 1):
        for x in range(int(cx - half), int(cx + half) + 1):
            xc = x + 0.5
            if abs(xc - cx) > half:
                continue
            s = (xc - cx) / half
            i = 3 if s < -0.3 else (2 if s < 0.4 else 1)
            if style == "wrap" and (x + y) % 3 == 0:
                i -= 1
            elif style == "rings" and y % 2 == 0:
                i += 1
            col = ramp[max(0, min(4, i))]
            if accent and style == "rings" and y % 4 == 0:
                col = accent[max(0, min(4, i + 1))]
            d.put(x, y, col, GRIP_D)


# ------------------------------------------------------------------------------- weapon sizes

# blade length / grip length in texels, blade width factor, guard width factor
KINDS = {
    "dagger": dict(blade=27, grip=9, width=1.0, guard=0.8),
    "sword": dict(blade=52, grip=12, width=1.0, guard=1.0),
    "greatsword": dict(blade=72, grip=21, width=1.25, guard=1.3),
}


class Layout:
    """Rows from the top: margin, blade (tip first), guard, grip, pommel."""
    def __init__(self, kind, guard_rows, pommel_rows, width=36):
        k = KINDS[kind]
        self.kind = kind
        self.L = k["blade"]
        self.G = k["grip"]
        self.wf = k["width"]
        self.gf = k["guard"]
        self.W = width
        self.cx = width / 2
        self.blade_base = 1 + self.L                  # first row below the blade
        self.guard_top = self.blade_base
        self.guard_bot = self.guard_top + guard_rows - 1
        self.grip_top = self.guard_bot + 1
        self.grip_bot = self.grip_top + self.G - 1
        self.pommel_top = self.grip_bot + 1
        self.H = self.pommel_top + pommel_rows + 1
        # one hand holds the middle of the grip; two hands hold it nearer the guard
        hold = self.grip_top + (self.G // 3 if kind == "greatsword" else self.G // 2)
        self.design = Design(self.W, self.H, (int(self.cx), hold))


def taper(base, mid, t_mid, point_from, point_w=0.4):
    """Half-width: base -> mid at t_mid, held, then down to a point from point_from."""
    def f(t):
        if t < t_mid:
            w = base + (mid - base) * t / t_mid
        else:
            w = mid
        if t > point_from:
            w = w * (1 - (t - point_from) / (1 - point_from)) + point_w * (t - point_from) / (1 - point_from)
        return w
    return f


# ------------------------------------------------------------------------------- the eight tiers

def iron(kind):
    lay = Layout(kind, guard_rows=4, pommel_rows=5)
    d, cx, wf, gf = lay.design, lay.cx, lay.wf, lay.gf
    w = taper(3.2 * wf, 2.7 * wf, 0.8, 0.86)
    blade(d, cx, lay.blade_base, lay.L, lambda t: (w(t), w(t)), P.IRON_AGED, grit=0.28,
          pattern=lambda x, y, t, s: P.IRON_AGED[1] if abs(s) < 0.15 and t < 0.88 else None)
    g = 8.5 * gf
    y0 = lay.guard_top
    # a plain bar whose ends curl down
    shape(d, [(cx - g, y0), (cx + g, y0), (cx + g + 0.5, y0 + 4), (cx + g - 1.5, y0 + 4), (cx + g - 2, y0 + 2), (cx - g + 2, y0 + 2),
              (cx - g + 1.5, y0 + 4), (cx - g - 0.5, y0 + 4)], P.IRON_AGED, GUARD_D, grit=0.25)
    grip(d, cx, lay.grip_top, lay.grip_bot, 1.7, P.LEATHER)
    p = lay.pommel_top
    shape(d, [(cx - 1.5, p), (cx + 1.5, p), (cx + 3, p + 4), (cx - 3, p + 4)], P.IRON_AGED, POMMEL_D, grit=0.25)
    return d


def steel(kind):
    lay = Layout(kind, guard_rows=4, pommel_rows=4)
    d, cx, wf, gf = lay.design, lay.cx, lay.wf, lay.gf
    w = taper(3.1 * wf, 2.9 * wf, 0.8, 0.82)

    def fuller(x, y, t, s):
        if abs(s) < 0.32 and t < 0.8:
            return P.STEEL[1] if s < 0 else P.STEEL[0]
        return None
    blade(d, cx, lay.blade_base, lay.L, lambda t: (w(t), w(t)), P.STEEL, grit=0.06, pattern=fuller)
    g = 8.5 * gf
    y0 = lay.guard_top
    # wide bar with ends drooping down, a square knotwork boss in the middle
    shape(d, [(cx - g, y0 + 1), (cx + g, y0 + 1), (cx + g, y0 + 4), (cx + g - 1, y0 + 4), (cx + g - 2, y0 + 3),
              (cx - g + 2, y0 + 3), (cx - g + 1, y0 + 4), (cx - g, y0 + 4)], P.STEEL, GUARD_D, grit=0.05)
    shape(d, [(cx - 3, y0), (cx + 3, y0), (cx + 3, y0 + 4), (cx - 3, y0 + 4)], P.STEEL, GUARD_D + 0.5, base=2)
    for (dx, dy) in ((-1, 1), (0, 2), (1, 1), (0, 0), (-2, 2), (2, 2)):
        d.put(int(cx) + dx, y0 + dy, P.STEEL[0], GUARD_D + 0.5)
    grip(d, cx, lay.grip_top, lay.grip_bot, 1.5, P.CLOTH_BROWN)
    p = lay.pommel_top
    shape(d, [(cx - 1, p), (cx + 1, p), (cx + 3, p + 2), (cx + 1, p + 4), (cx - 1, p + 4), (cx - 3, p + 2)], P.STEEL, POMMEL_D)
    return d


def orcish(kind):
    lay = Layout(kind, guard_rows=2, pommel_rows=5)
    d, cx, wf = lay.design, lay.cx, lay.wf

    def half(t):
        # the back (left) stays straight; the edge (right) swells towards the tip, which hooks back left
        left = 2.2 * wf
        right = (2.0 + 3.6 * min(1.0, t / 0.72)) * wf
        if t > 0.72:
            k = (t - 0.72) / 0.28
            right = right * (1 - k) + 0.3 * k
            left = left * (1 - k * 0.6)
        return (left, right)
    center = lambda t: -2.5 * max(0.0, t - 0.6) ** 1.5 * 4

    def hook_and_wear(x, y, t, s):
        if s > 0.6 and noise(x, y, 9) < 0.07:
            return P.ORCISH_BRASS[0]                    # notched, battered edge
        return None
    blade(d, cx, lay.blade_base, lay.L, half, P.ORCISH_BRASS, center=center, grit=0.12, pattern=hook_and_wear)
    y0 = lay.blade_base
    # the hook: a spike curving down from the back of the blade, just above the hilt
    shape(d, [(cx - 2, y0 - 9), (cx - 6, y0 - 4), (cx - 8, y0 + 1), (cx - 5.5, y0 - 1), (cx - 2, y0 - 3)], P.ORCISH_BRASS, GUARD_D)
    shape(d, [(cx - 2.5, y0), (cx + 2.5, y0), (cx + 2.5, y0 + 2), (cx - 2.5, y0 + 2)], P.ORCISH_BRASS, GUARD_D)
    grip(d, cx, lay.grip_top, lay.grip_bot, 1.5, P.CLOTH_BLACK)
    p = lay.pommel_top
    # crescent pommel opening downwards
    shape(d, [(cx - 1.5, p), (cx + 1.5, p), (cx + 4, p + 3), (cx + 3, p + 5), (cx + 1, p + 2), (cx - 1, p + 2),
              (cx - 3, p + 5), (cx - 4, p + 3)], P.ORCISH_BRASS, POMMEL_D, grit=0.2)
    return d


def dwarven(kind):
    lay = Layout(kind, guard_rows=4, pommel_rows=4)
    d, cx, wf, gf = lay.design, lay.cx, lay.wf, lay.gf
    w = taper(3.3 * wf, 3.0 * wf, 0.85, 0.86)

    def engraving(x, y, t, s):
        if t < 0.86 and (abs(abs(s) - 0.45) < 0.12):
            return P.DWARVEN[1]                         # two engraved lines running up the blade
        if abs(s) < 0.15 and t < 0.9:
            return P.DWARVEN[3]                         # bright central ridge
        return None
    blade(d, cx, lay.blade_base, lay.L, lambda t: (w(t), w(t)), P.DWARVEN, grit=0.12, pattern=engraving)
    g = 5.5 * gf
    y0 = lay.guard_top
    # angular shoulders: a flat bar whose ends rise into points against the blade
    shape(d, [(cx - g, y0 - 2), (cx - g + 2, y0), (cx + g - 2, y0), (cx + g, y0 - 2), (cx + g, y0 + 2), (cx + 2, y0 + 4),
              (cx - 2, y0 + 4), (cx - g, y0 + 2)], P.DWARVEN, GUARD_D)
    d.put(int(cx) - 1, y0 + 1, P.GOLD[4], GUARD_D + 0.5)
    d.put(int(cx), y0 + 1, P.GOLD[3], GUARD_D + 0.5)
    grip(d, cx, lay.grip_top, lay.grip_bot, 1.6, P.DWARVEN, style="rings")
    p = lay.pommel_top
    shape(d, [(cx - 1.5, p), (cx + 1.5, p), (cx + 3, p + 2), (cx + 3, p + 4), (cx - 3, p + 4), (cx - 3, p + 2)], P.DWARVEN, POMMEL_D)
    return d


def elven(kind):
    lay = Layout(kind, guard_rows=7, pommel_rows=6)
    d, cx, wf, gf = lay.design, lay.cx, lay.wf, lay.gf
    w = taper(2.6 * wf, 3.6 * wf, 0.5, 0.6, 0.3)

    def lines(x, y, t, s):
        if abs(s) < 0.1:
            return P.ELVEN[3]                           # bright raised centre line
        if abs(s) < 0.55 and t < 0.82:
            return P.ELVEN[1] if s > 0 else P.ELVEN[2]  # darker etched panel down the middle
        return None
    blade(d, cx, lay.blade_base, lay.L, lambda t: (w(t), w(t)), P.ELVEN, grit=0.04, pattern=lines)
    g = 10.0 * gf
    y0 = lay.guard_top
    # fanned wings sweeping down and out, feathered in alternating shades
    for side in (-1, 1):
        pts = [(cx, y0 - 1), (cx + side * g, y0 + 3), (cx + side * (g - 1), y0 + 6), (cx + side * 2, y0 + 4), (cx, y0 + 7)]
        shape(d, pts, P.ELVEN, GUARD_D)
        for k in range(3, int(g), 2):
            yy = y0 + 1 + int(k * 0.35)
            d.put(int(cx + side * k - (1 if side < 0 else 0)), yy + 1, P.ELVEN[1], GUARD_D)
    grip(d, cx, lay.grip_top, lay.grip_bot, 1.3, P.ELVEN, style="plain")
    p = lay.pommel_top
    # forked pommel: two prongs splaying downwards
    shape(d, [(cx - 1.5, p), (cx + 1.5, p), (cx + 4.5, p + 5), (cx + 2.5, p + 5), (cx, p + 2.5), (cx - 2.5, p + 5), (cx - 4.5, p + 5)],
          P.ELVEN, POMMEL_D)
    return d


def glass(kind):
    lay = Layout(kind, guard_rows=5, pommel_rows=4)
    d, cx, wf, gf = lay.design, lay.cx, lay.wf, lay.gf
    w = taper(2.5 * wf, 3.8 * wf, 0.72, 0.74, 0.3)

    def feathers(x, y, t, s):
        if abs(s) < 0.14:
            return P.MOONSTONE[3] if s < 0 else P.MOONSTONE[2]     # moonstone spine
        # chevron feathers: bands that sweep down and out from the spine
        band = int((y + abs(s) * 6) // 3) % 2
        if band:
            return P.GLASS[3] if s < 0 else P.GLASS[1]
        return None
    blade(d, cx, lay.blade_base, lay.L, lambda t: (w(t), w(t)), P.GLASS, pattern=feathers)
    g = 6.5 * gf
    y0 = lay.guard_top
    # curled horns: from the hilt out, then curling back up beside the blade
    for side in (-1, 1):
        pts = [(cx, y0 + 1), (cx + side * (g - 1), y0 + 2), (cx + side * g, y0 - 1), (cx + side * (g + 1), y0 + 1),
               (cx + side * (g - 0.5), y0 + 4), (cx, y0 + 4)]
        shape(d, pts, P.MOONSTONE, GUARD_D)
    shape(d, [(cx - 1.5, y0), (cx + 1.5, y0), (cx + 1.5, y0 + 4), (cx - 1.5, y0 + 4)], P.GLASS, GUARD_D + 0.5, base=3)
    grip(d, cx, lay.grip_top, lay.grip_bot, 1.4, P.GLASS, style="rings", accent=P.MOONSTONE)
    p = lay.pommel_top
    shape(d, [(cx, p + 4), (cx - 2, p + 1), (cx - 1, p), (cx + 1, p), (cx + 2, p + 1)], P.GLASS, POMMEL_D, base=3)
    return d


def ebony(kind):
    lay = Layout(kind, guard_rows=3, pommel_rows=5)
    d, cx, wf, gf = lay.design, lay.cx, lay.wf, lay.gf
    w = taper(2.2 * wf, 2.6 * wf, 0.55, 0.72, 0.3)
    center = lambda t: -5.0 * t ** 2 + 2.0 * math.sin(t * math.pi)       # an S-curve, tip swept back

    def engraving(x, y, t, s):
        if -0.5 < s < 0.4 and noise(x, y, 21) < 0.2:
            return P.SILVER[2]                          # faint silvery tracery
        return None
    blade(d, cx, lay.blade_base, lay.L, lambda t: (w(t), w(t)), P.EBONY, center=center, pattern=engraving)
    g = 5.5 * gf
    y0 = lay.guard_top
    # swept, angular guard: one wing rising, one falling
    shape(d, [(cx - g, y0 - 3), (cx - 1, y0), (cx + 1, y0), (cx + g, y0 + 2), (cx + g - 1, y0 + 3), (cx + 1, y0 + 3),
              (cx - 1, y0 + 3), (cx - g + 1, y0)], P.EBONY, GUARD_D)
    grip(d, cx, lay.grip_top, lay.grip_bot, 1.3, P.EBONY, style="wrap")
    p = lay.pommel_top
    shape(d, [(cx - 1.5, p), (cx + 1.5, p), (cx + 1, p + 3), (cx - 2, p + 5), (cx - 1, p + 2)], P.EBONY, POMMEL_D)
    return d


def daedric(kind):
    lay = Layout(kind, guard_rows=6, pommel_rows=8)
    d, cx, wf, gf = lay.design, lay.cx, lay.wf, lay.gf

    def half(t):
        # wide, then narrowing into a hooked point; the back (left) is serrated with forward teeth
        right = (3.0 + 2.0 * math.sin(min(1.0, t / 0.75) * math.pi / 2)) * wf
        left = 3.0 * wf
        tooth = (t * 6) % 1.0
        if 0.12 < t < 0.78:
            left += 3.5 * wf * max(0.0, 1 - tooth * 1.8)     # forward-raked teeth along the back
        if t > 0.78:
            k = (t - 0.78) / 0.22
            right *= (1 - k)
            left *= (1 - k * 0.6)
        return (left, right)
    center = lambda t: -9.0 * max(0.0, t - 0.7) ** 1.3       # the tip hooks back

    def veins(x, y, t, s):
        v = 0.15 * math.sin(y * 0.4) - 0.1
        if abs(s - v) < 0.07 and t < 0.82:
            return P.EMBER_RED[3] if noise(x, y, 31) < 0.6 else P.EMBER_RED[2]
        return None
    blade(d, cx, lay.blade_base, lay.L, half, P.DAEDRIC_BLACK, center=center, pattern=veins)
    g = 8.5 * gf
    y0 = lay.guard_top
    # horns sweeping up and out from the hilt
    for side in (-1, 1):
        pts = [(cx, y0 + 2), (cx + side * g, y0 - 3), (cx + side * (g + 1), y0 - 7), (cx + side * (g + 2.5), y0 - 2),
               (cx + side * (g - 1), y0 + 4), (cx, y0 + 6)]
        shape(d, pts, P.DAEDRIC_BLACK, GUARD_D)
    d.put(int(cx) - 1, y0 + 2, P.EMBER_RED[3], GUARD_D + 0.5)
    d.put(int(cx), y0 + 2, P.EMBER_RED[4], GUARD_D + 0.5)
    grip(d, cx, lay.grip_top, lay.grip_bot, 1.4, P.DAEDRIC_BLACK, style="rings", accent=P.EMBER_RED)
    p = lay.pommel_top
    # spiked pommel hooking back
    shape(d, [(cx - 2.5, p), (cx + 2.5, p), (cx + 1.5, p + 4), (cx - 2, p + 8), (cx - 4, p + 5), (cx - 1.5, p + 3)],
          P.DAEDRIC_BLACK, POMMEL_D)
    return d


TIERS = {"iron": iron, "steel": steel, "orcish": orcish, "dwarven": dwarven, "elven": elven, "glass": glass,
         "ebony": ebony, "daedric": daedric}


def designs(kinds=("dagger", "sword", "greatsword")):
    """{'<tier>_<kind>': Design}"""
    return {f"{t}_{k}": fn(k) for t, fn in TIERS.items() for k in kinds}


# =============================================================================== hafted weapons
# War axes, battleaxes, maces and warhammers: a shaft with a head on top. Axe blades face left in the
# upright design, which becomes forward (top-left) once the model is laid diagonally, like vanilla's axe.

HAFTED = {
    # shaft rows, head rows (height), canvas width, two-handed
    "war_axe": dict(shaft=52, head=22, width=36, two=False),
    "mace": dict(shaft=46, head=20, width=30, two=False),
    "battleaxe": dict(shaft=84, head=36, width=48, two=True),
    "warhammer": dict(shaft=84, head=26, width=48, two=True),
}

# per tier: head metal, shaft material + style, accent
STYLE = {
    "iron": dict(metal=P.IRON_AGED, shaft=P.WOOD_DARK, wrap=P.LEATHER, accent=P.IRON_AGED, grit=0.25),
    "steel": dict(metal=P.STEEL, shaft=P.WOOD_DARK, wrap=P.CLOTH_BROWN, accent=P.STEEL, grit=0.06),
    "orcish": dict(metal=P.ORCISH_BRASS, shaft=P.ORCISH_BRASS, wrap=P.CLOTH_BLACK, accent=P.ORCISH_BRASS, grit=0.12),
    "dwarven": dict(metal=P.DWARVEN, shaft=P.DWARVEN, wrap=P.DWARVEN, accent=P.GOLD, grit=0.1),
    "elven": dict(metal=P.ELVEN, shaft=P.ELVEN, wrap=P.ELVEN, accent=P.MOONSTONE, grit=0.04),
    "glass": dict(metal=P.GLASS, shaft=P.MOONSTONE, wrap=P.GLASS, accent=P.MOONSTONE, grit=0.0),
    "ebony": dict(metal=P.EBONY, shaft=P.EBONY, wrap=P.EBONY, accent=P.SILVER, grit=0.0),
    "daedric": dict(metal=P.DAEDRIC_BLACK, shaft=P.DAEDRIC_BLACK, wrap=P.DAEDRIC_BLACK, accent=P.EMBER_RED, grit=0.0),
}


class Haft:
    def __init__(self, kind):
        k = HAFTED[kind]
        self.kind = kind
        self.W = k["width"]
        self.cx = self.W / 2
        self.head_top = 1
        self.head_bot = 1 + k["head"]
        self.shaft_top = self.head_top + 2
        self.shaft_bot = self.head_bot + k["shaft"]
        self.H = self.shaft_bot + 4
        hold = self.shaft_bot - (k["shaft"] * 0.28 if k["two"] else k["shaft"] * 0.12)
        self.design = Design(self.W, self.H, (int(self.cx), int(hold)))


def edge_highlight(d, side, color, rows):
    """Brighten the outermost texel of each row on one side (the cutting edge of an axe)."""
    w, _ = d.size
    for y in rows:
        xs = [x for x in range(w) if d.get(x, y)[3] > 0]
        if xs:
            x = min(xs) if side < 0 else max(xs)
            d.put(x, y, color, d.depth[y][x])


def shaft(h, st, half=1.4):
    d, cx = h.design, h.cx
    # plain wooden/metal shaft, a wrapped grip near the bottom, an end cap
    grip(d, cx, h.shaft_top, h.shaft_bot, half, st["shaft"], style="plain")
    g0 = int(h.shaft_bot - (h.shaft_bot - h.shaft_top) * (0.45 if HAFTED[h.kind]["two"] else 0.3))
    grip(d, cx, g0, h.shaft_bot, half + 0.3, st["wrap"], style="wrap" if st["wrap"] in (P.LEATHER, P.CLOTH_BROWN, P.CLOTH_BLACK) else "rings")
    shape(d, [(cx - half - 1, h.shaft_bot + 1), (cx + half + 1, h.shaft_bot + 1), (cx + half, h.shaft_bot + 3), (cx - half, h.shaft_bot + 3)],
          st["metal"], POMMEL_D)


def bezier(p0, p1, p2, n=10):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]) for t in (i / n for i in range(n + 1))]


def crescent(cx, y, side, reach, top, bot, belly, neck=(0.25, 0.6), hook=0.0, beard=0.0):
    """
    An axe blade on one side of the shaft (side -1 = left/front): its neck meets the shaft between
    neck[0] and neck[1] (fractions of the head), the cutting edge runs from the top tip (height `top`)
    to the bottom tip (`bot`) bulging out by `belly`; `hook` curls the top tip back towards the shaft,
    `beard` drops the lower edge into a beard.
    """
    sx = lambda k: cx + side * k
    t_tip = (sx(reach - hook), y(top))
    b_tip = (sx(reach * 0.85), y(bot))
    pts = [(sx(1), y(neck[0]))]
    pts += bezier((sx(1), y(neck[0])), (sx(reach * 0.5), y(neck[0] - 0.05)), t_tip, 6)[1:]
    pts += bezier(t_tip, (sx(reach + belly), y((top + bot) / 2)), b_tip, 12)[1:]
    pts += bezier(b_tip, (sx(reach * 0.45), y(neck[1] + beard)), (sx(1), y(neck[1])), 6)[1:]
    return pts


AXES = {
    # front blade: reach, top, bot, belly, neck, hook, beard; back: None, 'spike', 'block' or a second blade
    "iron": (dict(reach=9, top=0.0, bot=1.0, belly=2.5, neck=(0.2, 0.55), beard=0.25), "block"),
    "steel": (dict(reach=8.5, top=0.0, bot=0.95, belly=2.5, neck=(0.2, 0.7)), dict(reach=6, top=0.1, bot=0.85, belly=1.5, neck=(0.25, 0.65))),
    "orcish": (dict(reach=10, top=-0.08, bot=0.95, belly=3, neck=(0.3, 0.7), hook=3), "spike"),
    "dwarven": (dict(reach=10, top=0.05, bot=0.92, belly=0.5, neck=(0.15, 0.6)), "block"),
    "elven": (dict(reach=10, top=0.0, bot=1.05, belly=3, neck=(0.15, 0.45), hook=1.5, beard=0.35), None),
    "glass": (dict(reach=10, top=0.0, bot=0.95, belly=3, neck=(0.2, 0.7)), dict(reach=10, top=0.0, bot=0.95, belly=3, neck=(0.2, 0.7))),
    "ebony": (dict(reach=9, top=0.25, bot=1.08, belly=2, neck=(0.2, 0.5), beard=0.3), "spike"),
    "daedric": (dict(reach=11, top=-0.08, bot=1.05, belly=4, neck=(0.15, 0.65), hook=3), "horn"),
}


def axe_head(h, tier, st, big):
    """Axe heads, after Skyrim's: the main blade on the left (front)."""
    d, cx = h.design, h.cx
    m = st["metal"]
    top, bot = h.head_top, h.head_bot
    hh = bot - top
    s = 1.55 if big else 1.0
    y = lambda f: top + hh * f
    front, back = AXES[tier]
    f = dict(front)
    f["reach"] *= s
    f["belly"] *= s
    f["hook"] = f.get("hook", 0) * s
    shape(d, crescent(cx, y, -1, **f), m, GUARD_D, grit=st["grit"])
    edge_highlight(d, -1, m[4], range(int(top), int(bot) + 2))
    if isinstance(back, dict):
        b = dict(back)
        b["reach"] *= s
        b["belly"] *= s
        shape(d, crescent(cx, y, 1, **b), m, GUARD_D - 0.5, grit=st["grit"])
    elif back == "spike":
        shape(d, [(cx + 1, y(0.25)), (cx + 8 * s, y(0.42)), (cx + 1, y(0.55))], m, GUARD_D - 0.5)
    elif back == "block":
        shape(d, [(cx + 1, y(0.2)), (cx + 3.5 * s, y(0.2)), (cx + 3.5 * s, y(0.55)), (cx + 1, y(0.55))], m, GUARD_D - 0.5)
    elif back == "horn":
        shape(d, bezier((cx + 1, y(0.3)), (cx + 7 * s, y(0.3)), (cx + 6 * s, y(-0.1)), 6) + [(cx + 1, y(0.5))], m, GUARD_D - 0.5)
    if tier == "glass":
        for yy in range(int(top), int(bot) + 1):
            for xx in range(h.W):
                c = d.get(xx, yy)
                if c[3] and c in (m[2], m[3]) and (yy + abs(xx - cx)) % 4 == 0:
                    d.put(xx, yy, m[1], d.depth[yy][xx])     # feathering
    if tier == "daedric":
        for k in range(int(hh * 0.6)):
            d.put(int(cx - 5 * s - k * 0.12), int(y(0.2)) + k, P.EMBER_RED[3], GUARD_D)
    if tier == "dwarven":
        shape(d, [(cx - 6 * s, y(0.3)), (cx - 3, y(0.3)), (cx - 3, y(0.5)), (cx - 6 * s, y(0.5))], P.DWARVEN, GUARD_D - 0.25, base=1)
    # the eye: a slim collar where the head grips the shaft, with a cap on top
    shape(d, [(cx - 1.8, y(0.08)), (cx + 1.8, y(0.08)), (cx + 1.8, y(0.72)), (cx - 1.8, y(0.72))], m, GUARD_D + 0.5, base=2)
    shape(d, [(cx - 1.2, y(0.08)), (cx, y(-0.08)), (cx + 1.2, y(0.08))], m, GUARD_D + 0.5)
    if tier in ("steel", "dwarven"):
        for k in range(2, int(hh * 0.55), 3):
            d.put(int(cx) - 1, int(y(0.1)) + k, m[0] if tier == "steel" else P.GOLD[4], GUARD_D + 0.5)
    if tier == "daedric":
        d.put(int(cx), int(y(0.4)), P.EMBER_RED[4], GUARD_D + 0.75)


def mace_head(h, tier, st):
    d, cx = h.design, h.cx
    m = st["metal"]
    top, bot = h.head_top, h.head_bot
    hh = bot - top
    y = lambda f: top + hh * f
    if tier in ("iron", "steel", "ebony"):     # flanged heads: a core with blades standing out
        flare = {"iron": 6, "steel": 4.5, "ebony": 5}[tier]
        shape(d, [(cx - 2.5, y(0.05)), (cx + 2.5, y(0.05)), (cx + 2.5, y(0.95)), (cx - 2.5, y(0.95))], m, GUARD_D + 0.5)
        shape(d, [(cx - flare, y(0.25)), (cx + flare, y(0.25)), (cx + flare, y(0.7)), (cx - flare, y(0.7))], m, GUARD_D, grit=st["grit"])
        shape(d, [(cx - 1.5, y(0.05)), (cx, y(-0.2)), (cx + 1.5, y(0.05))], m, GUARD_D)          # top spike
        if tier == "ebony":
            for k in range(int(hh)):
                d.put(int(cx - flare) + 1, int(y(0.25)) + k % int(hh * 0.45), P.SILVER[2], GUARD_D)
    elif tier == "orcish":                     # tall spiked head
        shape(d, [(cx - 2, y(0.0)), (cx + 2, y(0.0)), (cx + 3, y(1.0)), (cx - 3, y(1.0))], m, GUARD_D + 0.5)
        for k, f in enumerate((0.1, 0.4, 0.7)):
            shape(d, [(cx - 2, y(f)), (cx - 7, y(f - 0.1)), (cx - 3, y(f + 0.15))], m, GUARD_D)
            shape(d, [(cx + 2, y(f)), (cx + 7, y(f - 0.1)), (cx + 3, y(f + 0.15))], m, GUARD_D)
    elif tier == "dwarven":                    # a cylinder with ribs
        shape(d, [(cx - 4, y(0.05)), (cx + 4, y(0.05)), (cx + 4, y(0.95)), (cx - 4, y(0.95))], m, GUARD_D)
        for f in (0.2, 0.5, 0.8):
            shape(d, [(cx - 5, y(f)), (cx + 5, y(f)), (cx + 5, y(f) + 1.5), (cx - 5, y(f) + 1.5)], P.GOLD, GUARD_D + 0.5)
    elif tier == "elven":                      # anchor: an arch of two down-curving blades
        shape(d, [(cx - 1.5, y(0.0)), (cx + 1.5, y(0.0)), (cx + 1.5, y(1.0)), (cx - 1.5, y(1.0))], m, GUARD_D + 0.5)
        for side in (-1, 1):
            shape(d, [(cx, y(0.15)), (cx + side * 7, y(0.35)), (cx + side * 8, y(0.8)), (cx + side * 5, y(0.55)), (cx, y(0.45))], m, GUARD_D)
    elif tier == "glass":                      # green wings around a spike
        shape(d, [(cx - 1.5, y(-0.1)), (cx + 1.5, y(-0.1)), (cx + 1.5, y(1.0)), (cx - 1.5, y(1.0))], P.MOONSTONE, GUARD_D + 0.5)
        for side in (-1, 1):
            shape(d, [(cx, y(0.3)), (cx + side * 4, y(0.0)), (cx + side * 8, y(0.15)), (cx + side * 6, y(0.7)), (cx, y(0.75))], m, GUARD_D)
    else:                                      # daedric: horned crown
        shape(d, [(cx - 3, y(0.2)), (cx + 3, y(0.2)), (cx + 2, y(1.0)), (cx - 2, y(1.0))], m, GUARD_D + 0.5)
        for side in (-1, 1):
            shape(d, [(cx + side * 2, y(0.5)), (cx + side * 6, y(0.1)), (cx + side * 5, y(-0.25)), (cx + side * 8, y(0.15)),
                      (cx + side * 4, y(0.8))], m, GUARD_D)
        d.put(int(cx), int(y(0.55)), P.EMBER_RED[4], GUARD_D + 1)
        d.put(int(cx) - 1, int(y(0.55)), P.EMBER_RED[3], GUARD_D + 1)


def hammer_head(h, tier, st):
    """Warhammers: a striking face on the left (front), a curved pick or spike on the right."""
    d, cx = h.design, h.cx
    m = st["metal"]
    top, bot = h.head_top, h.head_bot
    hh = bot - top
    y = lambda f: top + hh * f
    face = {"iron": 11, "steel": 10, "orcish": 9, "dwarven": 12, "elven": 9, "glass": 10, "ebony": 10, "daedric": 10}[tier]
    # the striking block, flaring towards its face
    shape(d, [(cx + 2, y(0.2)), (cx - face + 2, y(0.12)), (cx - face, y(0.02)), (cx - face, y(0.98)), (cx - face + 2, y(0.88)),
              (cx + 2, y(0.8))], m, GUARD_D + 0.5, grit=st["grit"])
    edge_highlight(d, -1, m[4], range(int(y(0.05)), int(y(0.95))))
    if tier == "dwarven":
        shape(d, [(cx + 2, y(0.12)), (cx + 10, y(0.02)), (cx + 10, y(0.98)), (cx + 2, y(0.88))], m, GUARD_D)
        for f in (0.3, 0.7):
            shape(d, [(cx - face, y(f)), (cx + 10, y(f)), (cx + 10, y(f) + 1.5), (cx - face, y(f) + 1.5)], P.GOLD, GUARD_D + 0.75)
    else:
        reach = {"iron": 9, "steel": 13, "orcish": 12, "elven": 14, "glass": 12, "ebony": 14, "daedric": 13}[tier]
        droop = {"iron": 0.55, "steel": 1.0, "orcish": 0.8, "elven": 1.1, "glass": 0.9, "ebony": 1.15, "daedric": 1.05}[tier]
        upper = bezier((cx + 2, y(0.25)), (cx + reach * 0.7, y(0.25)), (cx + reach, y(droop)), 8)
        lower = bezier((cx + reach, y(droop)), (cx + reach * 0.5, y(0.6)), (cx + 2, y(0.7)), 8)
        shape(d, upper + lower[1:], m, GUARD_D, grit=st["grit"])
    shape(d, [(cx - 1.5, y(0.12)), (cx, y(-0.12)), (cx + 1.5, y(0.12))], m, GUARD_D)
    if tier == "iron":
        for f in (0.12, 0.88):
            shape(d, [(cx - face, y(f) - 1), (cx - face - 3, y(f)), (cx - face, y(f) + 1)], m, GUARD_D)
    if tier == "glass":
        shape(d, [(cx - face, y(0.0)), (cx - face - 3, y(0.5)), (cx - face, y(1.0))], P.MOONSTONE, GUARD_D)
    if tier == "elven":
        shape(d, [(cx - face, y(0.15)), (cx - face - 2, y(0.5)), (cx - face, y(0.85))], P.MOONSTONE, GUARD_D)
    if tier == "daedric":
        for k in range(face - 2):
            d.put(int(cx - face) + 1 + k, int(y(0.5)), P.EMBER_RED[3], GUARD_D + 0.5)
        shape(d, [(cx - face + 1, y(0.02)), (cx - face - 2, y(-0.2)), (cx - face + 4, y(0.08))], m, GUARD_D)
    if tier == "orcish":
        for f in (0.0, 1.0):
            shape(d, [(cx - face, y(f) - 1), (cx - face - 3, y(f)), (cx - face + 2, y(f) + 1)], m, GUARD_D)


def hafted(tier, kind):
    h = Haft(kind)
    st = STYLE[tier]
    shaft(h, st, half=1.5 if HAFTED[kind]["two"] else 1.3)
    if kind in ("war_axe", "battleaxe"):
        axe_head(h, tier, st, big=kind == "battleaxe")
    elif kind == "mace":
        mace_head(h, tier, st)
    else:
        hammer_head(h, tier, st)
    return h.design


# =============================================================================== bows
# Upright: limbs along the vertical, bulging left (top-left once laid diagonally, like vanilla's bow),
# string on the right. pull 0..2 draws the string back (right) with an arrow nocked, head pointing left.

BOW = {
    # (limb ramp, grip ramp, recurve, accent, ornate)
    "iron": (P.WOOD, P.LEATHER, 0.0, None, False),
    "steel": (P.WOOD_DARK, P.LEATHER, 0.35, None, False),
    "orcish": (P.ORCISH_BRASS, P.CLOTH_BLACK, 0.6, None, True),
    "dwarven": (P.DWARVEN, P.DWARVEN, 0.55, P.GOLD, True),
    "elven": (P.ELVEN, P.ELVEN, 0.7, P.MOONSTONE, True),
    "glass": (P.GLASS, P.MOONSTONE, 0.6, P.MOONSTONE, True),
    "ebony": (P.EBONY, P.EBONY, 0.7, P.SILVER, True),
    "daedric": (P.DAEDRIC_BLACK, P.DAEDRIC_BLACK, 0.8, P.EMBER_RED, True),
}


def bow(tier, pull=-1):
    limb_ramp, grip_ramp, recurve, accent, ornate = BOW[tier]
    W, H = 28, 66
    cx, cy = 16.0, H / 2
    d = Design(W, H, (int(cx - 11), H // 2))       # held at the grip, the deepest point of the arc
    half_len = H / 2 - 3
    draw = 0 if pull < 0 else 3 + pull * 3
    tips = []
    for side in (-1, 1):                      # -1 upper limb, 1 lower limb
        prev = None
        for k in range(0, 101):
            t = k / 100                         # 0 at the grip, 1 at the tip
            yy = cy + side * t * half_len
            # deepest at the grip, sweeping back towards the string; recurve tips flick away again
            bulge = 9 * math.cos(t * math.pi / 2) ** 1.3 + 2
            bulge += recurve * 7 * (max(0.0, t - 0.7) / 0.3) ** 1.5
            if pull >= 0:
                bulge -= draw * 0.35 * t          # drawn limbs bend in towards the string
            xx = cx - bulge
            thick = 1.8 - 0.9 * t + (0.9 if ornate and 0.25 < t < 0.6 else 0)
            for dx in range(-2, 3):
                for dy in range(-1, 2):
                    px, py = int(xx + dx * 0.5), int(yy + dy * 0.5)
                    if abs(dx * 0.5) <= thick:
                        i = 3 if dx < 0 else (2 if dx == 0 else 1)
                        col = limb_ramp[i]
                        if accent and ornate and 0.45 < t < 0.5:
                            col = accent[3]
                        d.put(px, py, col, 1.5 if t < 0.15 else 1.0)
            if k == 100:
                tips.append((xx, yy))
    # grip, wrapped around the limb at its deepest point
    gx = cx - 11
    for yy in range(int(cy - 5), int(cy + 5)):
        for xx in range(int(gx - 1.5), int(gx + 2.5)):
            d.put(xx, yy, grip_ramp[(3 if xx < gx else 2) - (1 if yy % 3 == 0 else 0)], 2.0)
    # string: tip to tip through the nocking point
    (x1, y1), (x2, y2) = tips
    nock = (max(x1, x2) + 1 + draw, cy)
    for a, b in (((x1 + 1, y1), nock), (nock, (x2 + 1, y2))):
        n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
        for k in range(n + 1):
            px = a[0] + (b[0] - a[0]) * k / n
            py = a[1] + (b[1] - a[1]) * k / n
            d.put(int(px), int(py), P.CLOTH_WHITE[3], 0.5)
    if pull >= 0:
        # the arrow: from the nock to past the grip, head on the left
        for xx in range(int(cx - 15), int(nock[0]) + 1):
            d.put(xx, int(cy), P.WOOD[3], 0.75)
        for k in range(3):
            for dy in range(-k, k + 1):
                d.put(int(cx - 18) + k, int(cy) + dy, P.IRON[4 if dy == 0 else 3], 0.75)
        d.put(int(nock[0]) - 1, int(cy) - 1, P.CLOTH_WHITE[4], 0.75)
        d.put(int(nock[0]) - 1, int(cy) + 1, P.CLOTH_WHITE[4], 0.75)
    return d


def all_designs():
    """{'<tier>_<kind>': Design} for every weapon, plus '<tier>_bow_pulling_<n>'."""
    out = designs()
    for t in TIERS:
        for k in HAFTED:
            out[f"{t}_{k}"] = hafted(t, k)
        out[f"{t}_bow"] = bow(t)
        for n in range(3):
            out[f"{t}_bow_pulling_{n}"] = bow(t, n)
    return out


# =============================================================================== export

def exports():
    """
    {relative path under assets/urarsenal: image or json} for every weapon: the 3D texture
    (textures/item/3d), the inventory icon (textures/item), the 3D model (models/item/3d) and the item
    model that picks between them (models/item).
    """
    from artforge.voxel import model_json, item_json, icon, HANDHELD, BOW as BOW_DISPLAY, BOW_GRIP, GRIP
    out = {}
    for name, d in all_designs().items():
        is_bow = "_bow" in name
        tex = f"urarsenal:item/3d/{name}"
        out[f"textures/item/3d/{name}.png"] = d.img
        out[f"textures/item/{name}.png"] = icon(d, outline=P.OUTLINE)
        out[f"models/item/3d/{name}.json"] = model_json(d, tex, display=BOW_DISPLAY if is_bow else HANDHELD,
                                                         grip=BOW_GRIP if is_bow else GRIP)
        overrides = None
        if name.endswith("_bow"):
            overrides = [
                {"predicate": {"pulling": 1}, "model": f"urarsenal:item/{name}_pulling_0"},
                {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"urarsenal:item/{name}_pulling_1"},
                {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"urarsenal:item/{name}_pulling_2"},
            ]
        out[f"models/item/{name}.json"] = item_json(f"urarsenal:item/3d/{name}", f"urarsenal:item/{name}", overrides)
    return out


def preview_sheet():
    """docs/art/arsenal3d.png: every weapon rendered in 3D, one row per weapon type."""
    from PIL import Image
    from artforge.voxel import model_json, render
    kinds = ["dagger", "sword", "greatsword", "war_axe", "mace", "battleaxe", "warhammer", "bow"]
    ds = all_designs()
    rows = []
    for k in kinds:
        cells = []
        for t in TIERS:
            d = ds[f"{t}_{k}"]
            cells.append(render(model_json(d, "x"), d.img, yaw=30, pitch=15, scale=4, size=(170, 170)))
        rows.append(cells)
    sheet = Image.new("RGBA", (170 * len(TIERS), 170 * len(rows)), (32, 28, 36, 255))
    for r, cells in enumerate(rows):
        for c, img in enumerate(cells):
            sheet.paste(img, (c * 170, r * 170))
    return sheet
