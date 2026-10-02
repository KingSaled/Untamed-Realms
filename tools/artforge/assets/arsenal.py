"""
ur-arsenal art (docs/ART_STYLE.md): Skyrim material tiers x weapon types, armor icons and armor
layer textures. Every weapon is drawn by the same construction code on the same diagonal (grip
bottom-left, tip top-right, lit from the top-left); a tier only swaps the ramps:

    blade / plate = primary, grip / straps = secondary, guard / pommel / trim = accent

Outlines are added automatically, so every sprite passes the style lint by construction.
"""
from PIL import Image

from artforge import palette as P
from artforge.sprite import canvas, outline

# tier: (primary metal, grip / strap, accent)
TIERS = {
    "iron":    (P.IRON, P.LEATHER, P.IRON),
    "steel":   (P.STEEL, P.LEATHER, P.COPPER),
    "orcish":  (P.ORCISH, P.CLOTH_BLACK, P.ORCISH),
    "dwarven": (P.DWARVEN, P.LEATHER, P.GOLD),
    "elven":   (P.ELVEN, P.CLOTH_GREEN, P.GOLD),
    "glass":   (P.GLASS, P.CLOTH_WHITE, P.SILVER),
    "ebony":   (P.EBONY, P.CLOTH_BLACK, P.SILVER),
    "daedric": (P.DAEDRIC, P.EBONY, P.EBONY),
}
LIGHT_ARMOR = {"hide": (P.CLOTH_BROWN, P.LEATHER, P.IRON), "leather": (P.LEATHER, P.CLOTH_BROWN, P.IRON),
               "elven": TIERS["elven"], "glass": TIERS["glass"]}
HEAVY_ARMOR = {k: TIERS[k] for k in ("iron", "steel", "orcish", "dwarven", "ebony", "daedric")}
WEAPONS = ["dagger", "sword", "war_axe", "mace", "greatsword", "battleaxe", "warhammer"]


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def blade(img, ramp, x, y, length, width):
    """A 45-degree blade from (x, y) up-right; the up-left-facing edge is lit."""
    shades = {1: [4], 2: [4, 2], 3: [4, 3, 1]}[width]
    for t in range(length):
        for w, idx in enumerate(shades):
            if t == length - 1 and w > 0:        # pointed tip
                continue
            put(img, x + t + w, y - t, ramp[idx])


def grip(img, ramp, x, y, length):
    """Wrapped grip going up-right from the pommel end."""
    for t in range(length):
        put(img, x + t, y - t, ramp[2 if t % 2 else 1])
        put(img, x + t + 1, y - t, ramp[3 if t % 2 else 2])


def pommel(img, ramp, x, y):
    put(img, x, y, ramp[3])
    put(img, x - 1, y + 1, ramp[2])
    put(img, x, y + 1, ramp[1])


def guard(img, ramp, x, y, half):
    """Crossguard across the blade (along the down-right diagonal) centred on (x, y)."""
    for d in range(-half, half + 1):
        put(img, x + d, y + d, ramp[4 if d < 0 else 3 if d == 0 else 2])


def shaft(img, ramp, x, y, length):
    """A wooden or metal haft, one pixel wide with a lit edge."""
    for t in range(length):
        put(img, x + t, y - t, ramp[2])
        put(img, x + t, y - t - 1, ramp[3]) if t < length - 1 else None


def head(img, ramp, mask, x, y):
    """A shaded head from an ASCII mask ('#' = metal, '+' = edge glint), top-left corner at (x, y)."""
    h, w = len(mask), len(mask[0])
    full = lambda a, b: 0 <= b < h and 0 <= a < w and mask[b][a] != "."
    for b in range(h):
        for a in range(w):
            ch = mask[b][a]
            if ch == ".":
                continue
            if ch == "+":
                idx = 4
            else:
                lit = not full(a - 1, b) or not full(a, b - 1)
                dark = not full(a + 1, b) or not full(a, b + 1)
                idx = 3 if lit and not dark else 1 if dark and not lit else 2
            put(img, x + a, y + b, ramp[idx])


AXE_HEAD = ["+##....", "+###...", "+####..", ".+####.", "...###.", "....##."]
BATTLEAXE_HEAD = [".+#...#+.", "+##...##+", "+###.###+", ".+#####+.", "..+###+..", "...###...", "....#...."]
MACE_HEAD = [".#.#.", "#####", ".###.", "#####", ".#.#."]
HAMMER_HEAD = ["..##...", ".####..", "######.", ".######", "..####.", "...##.."]


def weapon(kind, tier):
    metal, wrap, accent = TIERS[tier]
    img = canvas()
    if kind == "dagger":
        pommel(img, accent, 2, 13); grip(img, wrap, 3, 12, 2); guard(img, accent, 5, 10, 1)
        blade(img, metal, 6, 9, 6, 2)
    elif kind == "sword":
        pommel(img, accent, 2, 13); grip(img, wrap, 3, 12, 3); guard(img, accent, 6, 9, 2)
        blade(img, metal, 7, 8, 8, 2)
    elif kind == "greatsword":
        pommel(img, accent, 2, 13); grip(img, wrap, 3, 12, 2); guard(img, accent, 5, 10, 3)
        blade(img, metal, 6, 9, 8, 3)
    elif kind == "war_axe":
        pommel(img, accent, 2, 13); shaft(img, P.WOOD, 3, 12, 8)
        head(img, metal, AXE_HEAD, 6, 1)
    elif kind == "battleaxe":
        pommel(img, accent, 2, 13); shaft(img, P.WOOD, 3, 12, 8)
        head(img, metal, BATTLEAXE_HEAD, 6, 1)
    elif kind == "mace":
        pommel(img, accent, 2, 13); grip(img, wrap, 3, 12, 2); shaft(img, accent, 5, 10, 5)
        head(img, metal, MACE_HEAD, 9, 1)
    elif kind == "warhammer":
        pommel(img, accent, 2, 13); shaft(img, P.WOOD, 3, 12, 7)
        head(img, metal, HAMMER_HEAD, 8, 2)
    return outline(img, P.OUTLINE)


# -------------------------------------------------------------------------------------- bows

def bow(tier, pull=-1):
    """
    Bow, oriented like vanilla's (measured by tools/research/inspect.py): the limb arcs towards the
    top-left, the string runs between the tips at top-right and bottom-left; pull 0..2 draws the
    string's middle back towards the bottom-right with an arrow nocked, its head pointing top-left.
    """
    import math
    metal, wrap, accent = TIERS[tier]
    img = canvas()
    t1, t2 = (13, 2), (2, 13)
    bulge = 5.5
    nx, ny = -1 / math.sqrt(2), -1 / math.sqrt(2)          # towards the top-left
    pts = []
    for k in range(0, 61):
        u = k / 60
        off = bulge * 4 * u * (1 - u)
        pts.append((round(t1[0] + (t2[0] - t1[0]) * u + nx * off), round(t1[1] + (t2[1] - t1[1]) * u + ny * off), u))
    for x, y, u in pts:
        mid = abs(u - 0.5) < 0.09
        put(img, x, y, wrap[2] if mid else metal[3])
        put(img, x + 1, y + 1, wrap[1] if mid else metal[1])     # inner (shadow) side of the limb
    for tx, ty in (t1, t2):
        put(img, tx, ty, accent[3])
    back = 0 if pull < 0 else 1 + pull
    for k in range(1, 11):
        u = k / 11
        x = t1[0] + (t2[0] - t1[0]) * u
        y = t1[1] + (t2[1] - t1[1]) * u
        pullback = back * (1 - abs(u - 0.5) * 2)
        put(img, round(x + pullback * 0.7), round(y + pullback * 0.7), P.CLOTH_WHITE[3])
    if pull >= 0:
        # the arrow: nock at the drawn string, shaft through the grip, head past the limb
        nock = (round(7.5 + back * 0.7), round(7.5 + back * 0.7))
        length = 7 + back
        for i in range(length):
            x, y = nock[0] - i, nock[1] - i
            put(img, x, y, P.IRON[4] if i >= length - 2 else P.WOOD[3])
        put(img, nock[0] + 1, nock[1], P.CLOTH_WHITE[4])
        put(img, nock[0], nock[1] + 1, P.CLOTH_WHITE[4])
    return outline(img, P.OUTLINE)


# -------------------------------------------------------------------------------------- armor icons

HELMET = ["..######..", ".########.", "##########", "####..####", "###....###", "####..####", ".########."]
HELMET_LIGHT = ["...####...", "..######..", ".########.", ".###..###.", ".##....##.", ".###..###.", "..######.."]
CHEST = ["##..##..##", "##########", "##########", ".########.", ".########.", ".########.", ".########.", "..######.."]
CHEST_LIGHT = ["##......##", "###....###", "##########", ".########.", ".########.", ".########.", "..######.."]
LEGS = ["##########", "##########", "####..####", "###....###", "###....###", "###....###", "###....###"]
BOOTS = ["###...###.", "###...###.", "###...###.", "####..####", "#####.#####"]


def armor_icon(piece, light, ramps):
    metal, strap, accent = ramps
    masks = {"helmet": HELMET_LIGHT if light else HELMET, "chestplate": CHEST_LIGHT if light else CHEST,
             "leggings": LEGS, "boots": BOOTS}
    mask = masks[piece]
    img = canvas()
    w, h = max(len(r) for r in mask), len(mask)
    mask = [r.ljust(w, ".") for r in mask]
    x0, y0 = (16 - w) // 2, (16 - h) // 2
    head(img, metal, mask, x0, y0)
    # trim: a strap / belt line in the accent or strap ramp
    if piece == "chestplate":
        for x in range(w):
            if mask[h - 3][x] != ".":
                put(img, x0 + x, y0 + h - 3, (strap if light else accent)[2])
    elif piece == "helmet":
        for x in range(w):
            if mask[h - 1][x] != ".":
                put(img, x0 + x, y0 + h - 1, accent[2])
    elif piece == "leggings":
        for x in range(w):
            put(img, x0 + x, y0, (strap if light else accent)[2])
    elif piece == "boots":
        for x in range(w):
            if mask[0][x] != ".":
                put(img, x0 + x, y0, (strap if light else accent)[3])
    return outline(img, P.OUTLINE)


# -------------------------------------------------------------------------------------- armor layers

def _face(img, ramp, box, light_edge=True, seed=0):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            idx = 2
            if light_edge and (y == y0 or x == x0):
                idx = 3
            elif y == y1 - 1 or x == x1 - 1:
                idx = 1
            elif (x * 7 + y * 3 + seed) % 11 == 0:
                idx = 3
            put(img, x, y, ramp[idx])


def _box_faces(ox, oy, w, h, d):
    """Minecraft cube UV layout for a box of size w x h x d with its texture origin at (ox, oy)."""
    return {
        "top": (ox + d, oy, ox + d + w, oy + d),
        "bottom": (ox + d + w, oy, ox + d + 2 * w, oy + d),
        "right": (ox, oy + d, ox + d, oy + d + h),
        "front": (ox + d, oy + d, ox + d + w, oy + d + h),
        "left": (ox + d + w, oy + d, ox + 2 * d + w, oy + d + h),
        "back": (ox + 2 * d + w, oy + d, ox + 2 * d + 2 * w, oy + d + h),
    }


def armor_layers(ramps, light):
    """Returns (layer_1, layer_2), 64x32 each, for the humanoid armor model."""
    metal, strap, accent = ramps
    layer1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    layer2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    parts1 = [(_box_faces(0, 0, 8, 8, 8), metal), (_box_faces(16, 16, 8, 12, 4), metal),
              (_box_faces(40, 16, 4, 12, 4), metal), (_box_faces(0, 16, 4, 12, 4), metal)]
    for i, (faces, ramp) in enumerate(parts1):
        for name, box in faces.items():
            _face(layer1, ramp, box, seed=i)
    # helmet: visor slit / leather cap opening on the front face
    fx0, fy0, fx1, fy1 = _box_faces(0, 0, 8, 8, 8)["front"]
    for x in range(fx0 + 1, fx1 - 1):
        put(layer1, x, fy0 + 3, P.OUTLINE if not light else (0, 0, 0, 0))
        put(layer1, x, fy1 - 1, accent[3])
    if light:
        for y in range(fy0 + 3, fy1 - 1):
            for x in range(fx0 + 2, fx1 - 2):
                put(layer1, x, y, (0, 0, 0, 0))
    # chest: belt + trim
    bx0, by0, bx1, by1 = _box_faces(16, 16, 8, 12, 4)["front"]
    for x in range(bx0, bx1):
        put(layer1, x, by0 + 8, (strap if light else accent)[2])
        put(layer1, x, by0, accent[3])
    # arms: shoulder pauldrons in the accent
    ax0, ay0, ax1, ay1 = _box_faces(40, 16, 4, 12, 4)["front"]
    for y in range(ay0, ay0 + 3):
        for x in range(40, 56):
            put(layer1, x, y, accent[2 if y > ay0 else 3])
    # boots (layer 1, legs): darken the upper leg so only the boot shows strongly
    for name, (x0, y0, x1, y1) in _box_faces(0, 16, 4, 12, 4).items():
        if name in ("top", "bottom"):
            continue
        for y in range(y0, y0 + 6):
            for x in range(x0, x1):
                put(layer1, x, y, (0, 0, 0, 0))
    # leggings (layer 2): body (waist) + legs
    for faces in (_box_faces(16, 16, 8, 12, 4), _box_faces(0, 16, 4, 12, 4)):
        for name, box in faces.items():
            _face(layer2, strap if light else metal, box)
    lx0, ly0, lx1, ly1 = _box_faces(16, 16, 8, 12, 4)["front"]
    for x in range(lx0, lx1):
        put(layer2, x, ly1 - 4, accent[3])
    return layer1, layer2


def all_weapon_sprites():
    """{name: image} for linting and export (textures/item/<tier>_<kind>.png)."""
    out = {}
    for tier in TIERS:
        for kind in WEAPONS:
            out[f"{tier}_{kind}"] = weapon(kind, tier)
        out[f"{tier}_bow"] = bow(tier)
        for pull in range(3):
            out[f"{tier}_bow_pulling_{pull}"] = bow(tier, pull)
    return out


ARMOR_PIECES = ("helmet", "chestplate", "leggings", "boots")


def armor_sets():
    """{set name: (ramps, light)} - light and heavy set names never clash."""
    sets = {name: (ramps, True) for name, ramps in LIGHT_ARMOR.items()}
    sets.update({name: (ramps, False) for name, ramps in HEAVY_ARMOR.items()})
    return sets


def all_armor_sprites():
    """{<set>_<piece>: icon} for every armor set."""
    return {f"{name}_{piece}": armor_icon(piece, light, ramps)
            for name, (ramps, light) in armor_sets().items() for piece in ARMOR_PIECES}


# -------------------------------------------------------------------------------------- materials

INGOT = ["..........", "....####..", "..#+####..", ".#+######.", "#########.", ".#######..", "..####...."]
RAW = ["...##...", "..####..", ".##+###.", "######..", ".#####..", "..###..."]
HEART = [".##.##..", "#+#####.", "#######.", ".#####..", "..###...", "...#...."]
STRIPS = ["##......", ".##.....", "..##..##", "...##.##", ".....##.", "......##"]
SCRAP = ["#..##...", "##.###..", ".####.#.", "..######", ".###.##.", "#..#...."]

# material item: (mask, ramp)
MATERIALS = {
    "steel_ingot": (INGOT, P.STEEL),
    "orichalcum_ingot": (INGOT, P.ORCISH),
    "dwarven_ingot": (INGOT, P.DWARVEN),
    "refined_moonstone": (INGOT, P.MOONSTONE),
    "refined_malachite": (INGOT, P.GLASS),
    "ebony_ingot": (INGOT, P.EBONY),
    "raw_orichalcum": (RAW, P.ORCISH),
    "raw_moonstone": (RAW, P.MOONSTONE),
    "raw_malachite": (RAW, P.GLASS),
    "raw_ebony": (RAW, P.EBONY),
    "dwarven_scrap": (SCRAP, P.DWARVEN),
    "daedra_heart": (HEART, P.DAEDRIC),
    "leather_strips": (STRIPS, P.LEATHER),
}


def material(name):
    mask, ramp = MATERIALS[name]
    img = canvas()
    w, h = len(mask[0]), len(mask)
    head(img, ramp, mask, (16 - w) // 2, (16 - h) // 2)
    return outline(img, P.OUTLINE)


# ore: (vein ramp, base ramp)
ORES = {"orichalcum_ore": (P.ORCISH, P.STONE), "moonstone_ore": (P.MOONSTONE, P.STONE),
        "malachite_ore": (P.GLASS, P.STONE), "ebony_ore": (P.EBONY, P.DEEPSLATE)}


def ore(name, seed=7):
    """Stone with chunky lit specks of the ore (block texture, no outline)."""
    import random
    from artforge.procedural import noise_texture
    vein, base = ORES[name]
    img = noise_texture(base, seed=seed + len(name))
    rnd = random.Random(seed * 31 + len(name))
    for _ in range(6):
        cx, cy = rnd.randrange(1, 14), rnd.randrange(1, 14)
        for dx, dy, idx in ((0, 0, 3), (1, 0, 2), (0, 1, 2), (1, 1, 1), (-1, 0, 4)):
            put(img, cx + dx, cy + dy, vein[idx])
    return img


# Ores, raw ores and ingots are recoloured vanilla iron at runtime (assets/recolors.py); these are ours.
OWN_MATERIALS = ("dwarven_scrap", "daedra_heart", "leather_strips")


def all_material_sprites():
    return {name: material(name) for name in MATERIALS if name in OWN_MATERIALS}


# -------------------------------------------------------------------------------------- stations

def stations():
    """Forge, tanning rack and workbench models (docs/ART_STYLE.md section 5)."""
    from artforge.model3d import Model
    from artforge import procedural as G
    wood = G.planks(P.WOOD_DARK, seed=11, boards=4)
    iron = G.noise_texture(P.IRON, seed=12, weights=(1, 2, 5, 3, 1))
    stone = G.noise_texture(P.STONE, seed=13)
    fire = G.noise_texture(P.FIRE, seed=14, weights=(1, 2, 3, 4, 3))
    hide = G.noise_texture(P.LEATHER, seed=15, weights=(0, 2, 5, 3, 1))
    top = G.planks(P.WOOD, seed=16, boards=4, vertical=False)

    forge = Model("forge", {"stone": stone, "fire": fire, "iron": iron}, namespace="urarsenal")
    forge.cube((0, 0, 0), (16, 9, 16), "stone", name="hearth")
    forge.cube((2, 9, 2), (14, 10, 14), "fire", name="coals")
    forge.cube((0, 9, 0), (16, 12, 2), "stone", name="back_wall")
    forge.cube((0, 9, 0), (2, 12, 16), "stone", name="left_wall")
    forge.cube((14, 9, 0), (16, 12, 16), "stone", name="right_wall")
    forge.cube((4, 10, 5), (12, 11, 11), "iron", name="grate")

    rack = Model("tanning_rack", {"wood": wood, "hide": hide}, namespace="urarsenal")
    rack.cube((0, 0, 7), (2, 16, 9), "wood", name="post_left")
    rack.cube((14, 0, 7), (16, 16, 9), "wood", name="post_right")
    rack.cube((0, 14, 7), (16, 16, 9), "wood", name="top_bar")
    rack.cube((0, 2, 7), (16, 4, 9), "wood", name="bottom_bar")
    rack.cube((3, 4, 7.5), (13, 14, 8.5), "hide", name="hide")

    bench = Model("workbench", {"wood": wood, "top": top, "iron": iron}, namespace="urarsenal")
    bench.cube((0, 12, 0), (16, 15, 16), "top", name="table_top")
    for i, (x, z) in enumerate(((1, 1), (13, 1), (1, 13), (13, 13))):
        bench.cube((x, 0, z), (x + 2, 12, z + 2), "wood", name=f"leg_{i}")
    bench.cube((3, 15, 4), (9, 17, 7), "iron", name="whetstone")
    bench.cube((10, 15, 9), (14, 16, 13), "iron", name="tongs")
    return [forge, rack, bench]


# -------------------------------------------------------------------------------------- jewelry

RING_BAND = ["..####..", ".#....#.", "#......#", "#......#", "#......#", ".#....#.", "..####.."]
AMULET_CHAIN = ["#........#", ".#......#.", "..#....#..", "...#..#...", "....##...."]
GEM = [".##.", "####", "####", ".##."]

# jewel: (kind, metal ramp, gem ramp)
JEWELRY = {
    "ring_of_strength": ("ring", P.GOLD, P.RUBY),
    "ring_of_the_archer": ("ring", P.SILVER, P.EMERALD),
    "ring_of_magicka": ("ring", P.SILVER, P.SAPPHIRE),
    "ring_of_stamina": ("ring", P.GOLD, P.EMERALD),
    "ring_of_vitality": ("ring", P.SILVER, P.RUBY),
    "ring_of_the_thief": ("ring", P.SILVER, P.AMETHYST),
    "amulet_of_destruction": ("amulet", P.GOLD, P.RUBY),
    "amulet_of_restoration": ("amulet", P.GOLD, P.MOONSTONE),
    "amulet_of_the_merchant": ("amulet", P.GOLD, P.EMERALD),
    "amulet_of_warding": ("amulet", P.SILVER, P.AMETHYST),
    "amulet_of_learning": ("amulet", P.SILVER, P.SAPPHIRE),
    "amulet_of_vigor": ("amulet", P.GOLD, P.SAPPHIRE),
}


def jewel(name):
    """Ring: band with the stone set at the top. Amulet: chain with a pendant stone below."""
    kind, metal, gem = JEWELRY[name]
    img = canvas()
    if kind == "ring":
        head(img, metal, RING_BAND, 4, 6)
        head(img, gem, GEM, 6, 3)
    else:
        head(img, metal, AMULET_CHAIN, 3, 2)
        head(img, metal, ["####", "#..#"], 6, 7)
        head(img, gem, GEM, 6, 9)
    return outline(img, P.OUTLINE)


def all_jewelry_sprites():
    return {name: jewel(name) for name in JEWELRY}
