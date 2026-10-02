"""
Spell particles: 8x8 frames per element, played over each particle's life (first frame at birth).
Hard pixels, brightest at the core and darker toward the rim (rendered full-bright in game, so
they read as glowing). Elements use their school's ramp (docs/ART_STYLE.md, "Magic schools").
"""
import math

from PIL import Image

from artforge import palette as P

S = 8
WHITE = (255, 255, 255, 255)


def blank():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def glow(radius, ramp, core_white=True, stretch=(1.0, 1.0)):
    """A round glow: rim in the ramp's mid shades, core in its light end (white at the very centre)."""
    img = blank()
    c = (S - 1) / 2
    for y in range(S):
        for x in range(S):
            d = math.hypot((x - c) / stretch[0], (y - c) / stretch[1])
            if d > radius:
                continue
            u = d / max(radius, 0.01)
            if core_white and u < 0.3:
                col = WHITE
            elif u < 0.55:
                col = ramp[4]
            elif u < 0.8:
                col = ramp[3]
            else:
                col = ramp[2]
            img.putpixel((x, y), col)
    return img


def from_rows(rows, colors):
    img = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), colors[ch])
    return img


def spark():
    """Electric: jagged little zig-zags and stars, white core with sky-blue edges."""
    blue = P.SAPPHIRE
    c = {"w": WHITE, "b": blue[4], "m": blue[3]}
    frames = [
        ["........", "...b....", "...wb...", "..bww...", "...wwb..", "....bw..", "....b...", "........"],
        ["........", "..m...m.", "...b.b..", "....w...", "...bwb..", "..b...b.", ".m......", "........"],
        ["........", "........", "...m....", "..bwwb..", "....wb..", "...m....", "........", "........"],
        ["........", "........", "....m...", "...bw...", "....b...", "........", "........", "........"],
    ]
    return [from_rows(f, c) for f in frames]


def ember():
    """Fire: a hot core cooling and shrinking."""
    r = P.FIRE
    return [glow(3.4, r), glow(2.8, r), glow(2.1, r, core_white=False), glow(1.4, [r[0], r[1], r[1], r[2], r[3]], core_white=False)]


def frost():
    """Frost: six-armed crystals, white tips on pale blue."""
    c = {"w": WHITE, "b": P.MOONSTONE[4], "m": P.SAPPHIRE[4], "d": P.SAPPHIRE[3]}
    frames = [
        ["...w....", ".w.b.w..", "..mbm...", "wbbwbbw.", "..mbm...", ".w.b.w..", "...w....", "........"],
        ["........", "..w.w...", "...b....", ".wbwbw..", "...b....", "..w.w...", "........", "........"],
        ["........", "........", "...m....", "..mwm...", "...m....", "........", "........", "........"],
    ]
    return [from_rows(f, c) for f in frames]


def holy():
    """Holy light: a four-pointed twinkle, gold rays around a white heart."""
    g = P.RESTORATION
    c = {"w": WHITE, "g": g[4], "y": g[3]}
    frames = [
        ["...y....", "...g....", "...g....", "yggwggy.", "...g....", "...g....", "...y....", "........"],
        ["........", "...y....", "...g....", ".ygwgy..", "...g....", "...y....", "........", "........"],
        ["........", "........", "...y....", "..ywy...", "...y....", "........", "........", "........"],
        ["........", "........", "........", "...w....", "........", "........", "........", "........"],
    ]
    return [from_rows(f, c) for f in frames]


def heal():
    """Healing: soft golden-green motes drifting up."""
    ramp = [P.EMERALD[1], P.EMERALD[2], P.EMERALD[3], P.EMERALD[4], P.RESTORATION[4]]
    return [glow(3.0, ramp), glow(2.3, ramp), glow(1.6, ramp, core_white=False)]


def shadow():
    """Shadow: smoky violet-black wisps that swell as they fade."""
    ramp = [P.CONJURATION[0], P.CONJURATION[0], P.CONJURATION[1], P.CONJURATION[2], P.CONJURATION[3]]
    return [glow(1.8, ramp, False), glow(2.5, ramp, False, (1.2, 0.9)), glow(3.2, ramp, False, (1.3, 0.8)),
            glow(3.6, [ramp[0]] * 3 + [ramp[1], ramp[2]], False, (1.4, 0.8))]


def arcane():
    """Arcane: a violet ring that tightens into a point."""
    a = P.ALTERATION
    frames = []
    for radius in (3.4, 2.6, 1.8):
        img = blank()
        c = (S - 1) / 2
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - c, y - c)
                if abs(d - radius) < 0.6:
                    img.putpixel((x, y), a[4] if (x + y) % 2 else a[3])
                elif d < 0.8:
                    img.putpixel((x, y), WHITE)
        frames.append(img)
    frames.append(glow(1.2, a))
    return frames


PARTICLES = {"spark": spark, "ember": ember, "frost": frost, "holy": holy, "heal": heal, "shadow": shadow, "arcane": arcane}


def all_particles():
    """{name: [frames]}"""
    return {name: fn() for name, fn in PARTICLES.items()}
