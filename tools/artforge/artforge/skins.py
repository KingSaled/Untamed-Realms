"""
64x64 player-format skins for NPCs, generated from an outfit description.

    Outfit(skin=1, hair="brown", hair_style="short", beard=True,
           shirt=P.CLOTH_GREEN, pants=P.CLOTH_BROWN, boots=P.LEATHER,
           overlay="apron", overlay_ramp=P.LEATHER)

Faces are painted per box using the standard skin UV layout, with light from above: top faces
lighter, bottoms darker, a subtle per-pixel dither so cloth reads as fabric.
"""
import random
from dataclasses import dataclass, field
from PIL import Image
from . import palette as P


@dataclass
class Outfit:
    skin: int = 0
    hair: str = "brown"
    hair_style: str = "short"        # short, long, bald, hood, tied
    beard: bool = False
    eyes: tuple = (60, 90, 140, 255)
    shirt: list = field(default_factory=lambda: P.CLOTH_GREEN)
    pants: list = field(default_factory=lambda: P.CLOTH_BROWN)
    boots: list = field(default_factory=lambda: P.LEATHER)
    belt: list = field(default_factory=lambda: P.LEATHER)
    overlay: str = ""                # apron, robe, armor, cloak, vest, hood
    overlay_ramp: list = field(default_factory=lambda: P.LEATHER)
    trim: list = field(default_factory=lambda: P.GOLD)
    seed: int = 0


# (u, v, w, h, d) for each box in the 64x64 layout: base layer, then overlay layer.
BOXES = {
    "head": (0, 0, 8, 8, 8),       "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4),    "jacket": (16, 32, 8, 12, 4),
    "r_arm": (40, 16, 4, 12, 4),   "r_sleeve": (40, 32, 4, 12, 4),
    "l_arm": (32, 48, 4, 12, 4),   "l_sleeve": (48, 48, 4, 12, 4),
    "r_leg": (0, 16, 4, 12, 4),    "r_pants": (0, 32, 4, 12, 4),
    "l_leg": (16, 48, 4, 12, 4),   "l_pants": (0, 48, 4, 12, 4),
}


def faces(box):
    """Rectangles (x, y, w, h) of each face of a box: top, bottom, right, front, left, back."""
    u, v, w, h, d = BOXES[box]
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


class SkinPainter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def fill(self, rect, ramp, base=2, dither=True, light=0):
        x0, y0, w, h = rect
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                idx = base + light
                if dither:
                    r = self.rnd.random()
                    # sparse, mostly-darker speckle reads as woven cloth rather than noise
                    if r < 0.07:
                        idx -= 1
                    elif r < 0.09:
                        idx += 1
                    if y == y0 + h - 1 and h > 3:
                        idx -= 1  # shadow at the bottom edge of each face
                self.px[x, y] = ramp[max(0, min(4, idx))]

    def paint_box(self, box, ramp, base=2, dither=True, rows=None):
        """Paints all faces; `rows` limits side faces to a vertical slice (start, end) of the height."""
        for name, (x, y, w, h) in faces(box).items():
            light = 1 if name == "top" else -1 if name in ("bottom", "back") else 0
            if rows and name not in ("top", "bottom"):
                start, end = rows
                y, h = y + start, max(0, min(h, end) - start)
                if h == 0:
                    continue
            elif rows and name == "top" and rows[0] > 0:
                continue
            elif rows and name == "bottom" and rows[1] < BOXES[box][3]:
                continue
            self.fill((x, y, w, h), ramp, base, dither, light)

    def set(self, x, y, color):
        self.px[x, y] = color


def generate(o: Outfit) -> Image.Image:
    s = SkinPainter(o.seed)
    skin = P.SKIN_TONES[o.skin % len(P.SKIN_TONES)]
    hair = P.HAIR.get(o.hair, P.HAIR["brown"])

    # --- head
    s.paint_box("head", skin, base=3, dither=False)
    hx, hy = 8, 8                                    # head front origin
    # hair: top, back and upper sides
    if o.hair_style != "bald":
        for name in ("top", "back"):
            s.fill(faces("head")[name], hair, 2)
        for name in ("right", "left"):
            x, y, w, h = faces("head")[name]
            depth = 6 if o.hair_style in ("long", "tied") else 3
            s.fill((x, y, w, depth), hair, 2)
        # fringe on the forehead
        fringe = 2 if o.hair_style != "long" else 2
        s.fill((hx, hy, 8, fringe), hair, 2)
        if o.hair_style == "long":
            s.fill((hx, hy + 2, 1, 5), hair, 2)
            s.fill((hx + 7, hy + 2, 1, 5), hair, 2)
            s.fill(faces("head")["back"][:2] + (8, 8), hair, 2)
    # eyes (row 4 of the face): white + iris, brows above
    white = (235, 235, 230, 255)
    for ex in (hx + 1, hx + 5):
        s.set(ex, hy + 4, white)
        s.set(ex + 1, hy + 4, o.eyes)
        brow = hair[1] if o.hair_style != "bald" else skin[1]
        s.set(ex, hy + 3, brow)
        s.set(ex + 1, hy + 3, brow)
    # nose shadow and mouth
    s.set(hx + 3, hy + 5, skin[2]); s.set(hx + 4, hy + 5, skin[2])
    mouth = (120, 60, 50, 255)
    s.set(hx + 3, hy + 6, mouth); s.set(hx + 4, hy + 6, mouth)
    if o.beard:
        for x in range(hx + 1, hx + 7):
            s.set(x, hy + 7, hair[2])
        for x in (hx + 1, hx + 2, hx + 5, hx + 6):
            s.set(x, hy + 6, hair[2])
        s.fill((faces("head")["bottom"][0], faces("head")["bottom"][1], 8, 8), hair, 1)

    # --- body / arms / legs
    s.paint_box("body", o.shirt)
    s.paint_box("r_arm", o.shirt)
    s.paint_box("l_arm", o.shirt)
    # hands: bottom 3 rows of the arms are skin
    s.paint_box("r_arm", skin, base=3, dither=False, rows=(9, 12))
    s.paint_box("l_arm", skin, base=3, dither=False, rows=(9, 12))
    s.paint_box("r_leg", o.pants)
    s.paint_box("l_leg", o.pants)
    s.paint_box("r_leg", o.boots, base=1, rows=(8, 12))
    s.paint_box("l_leg", o.boots, base=1, rows=(8, 12))
    # belt on body row 8 (front/back/sides)
    for name in ("front", "back", "left", "right"):
        x, y, w, h = faces("body")[name]
        s.fill((x, y + 8, w, 1), o.belt, 1, dither=False)
    bx, by = faces("body")["front"][:2]
    s.set(bx + 3, by + 8, o.trim[3]); s.set(bx + 4, by + 8, o.trim[3])   # buckle
    # collar / neckline
    s.fill((bx + 2, by, 4, 1), skin, 3, dither=False)

    # --- outfit overlays (second layer)
    ov = o.overlay_ramp
    if o.overlay == "apron":
        s.fill((bx + 1, by + 1, 6, 11), ov, 2)
        s.fill((bx + 1, by + 1, 6, 1), ov, 3, dither=False)
        jx, jy = faces("jacket")["front"][:2]
        s.fill((jx + 1, jy + 1, 6, 11), ov, 2)
        for leg in ("r_pants", "l_pants"):
            x, y, w, h = faces(leg)["front"]
            s.fill((x, y, w, 6), ov, 2)
    elif o.overlay == "robe":
        for box in ("jacket", "r_pants", "l_pants"):
            s.paint_box(box, ov, 2)
        for box in ("r_sleeve", "l_sleeve"):
            s.paint_box(box, ov, 2, rows=(0, 9))
        # trim down the front and at the hem
        jx, jy = faces("jacket")["front"][:2]
        for y in range(jy, jy + 12):
            s.set(jx + 3, y, o.trim[2]); s.set(jx + 4, y, o.trim[3])
        for leg in ("r_pants", "l_pants"):
            x, y, w, h = faces(leg)["front"]
            s.fill((x, y + h - 1, w, 1), o.trim, 2, dither=False)
    elif o.overlay == "armor":
        s.paint_box("jacket", ov, 2)
        for box in ("r_sleeve", "l_sleeve"):
            s.paint_box(box, ov, 2, rows=(0, 4))
        jx, jy = faces("jacket")["front"][:2]
        for x in range(jx, jx + 8):                 # plate lines
            s.set(x, jy + 4, ov[1]); s.set(x, jy + 9, ov[1])
        s.fill((jx + 3, jy + 1, 2, 2), o.trim, 3, dither=False)  # crest
        for leg in ("r_pants", "l_pants"):
            s.paint_box(leg, ov, 2, rows=(0, 7))
        # helmet as the hat layer: band around the head
        for name in ("front", "left", "right", "back"):
            x, y, w, h = faces("hat")[name]
            s.fill((x, y, w, 3), ov, 3)
        s.fill(faces("hat")["top"], ov, 3)
    elif o.overlay == "vest":
        jx, jy = faces("jacket")["front"][:2]
        s.fill((jx, jy, 2, 12), ov, 2); s.fill((jx + 6, jy, 2, 12), ov, 2)
        for name in ("left", "right", "back"):
            s.fill(faces("jacket")[name], ov, 2)
    elif o.overlay == "cloak":
        s.fill(faces("jacket")["back"], ov, 1)
        for name in ("left", "right"):
            s.fill(faces("jacket")[name], ov, 1)
        jx, jy = faces("jacket")["front"][:2]
        s.fill((jx, jy, 8, 1), ov, 2, dither=False)
        s.set(jx + 1, jy + 1, o.trim[3])            # clasp

    if o.hair_style == "hood" or o.overlay == "hood":
        hood = ov if o.overlay == "hood" else o.shirt
        for name in ("top", "back", "left", "right"):
            s.fill(faces("hat")[name], hood, 1)
        x, y, w, h = faces("hat")["front"]
        s.fill((x, y, w, 2), hood, 1)
        s.fill((x, y, 1, h), hood, 1); s.fill((x + w - 1, y, 1, h), hood, 1)

    return s.img
