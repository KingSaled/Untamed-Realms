"""
ur-arcana art (docs/ART_STYLE.md): soul gems, alchemy salts, nirnroot, the Soul Trap spell icon and
the two stations (alchemy lab, arcane enchanter). Sprites are ASCII masks shaded with assets.arsenal.head
(lit from the top-left) and outlined, like the arsenal materials.
"""
from artforge import palette as P
from artforge.sprite import canvas, outline
from assets.arsenal import head, put

# soul gems grow with their size; Skyrim's are violet crystals, the grand one set in gold
GEM_MASKS = {
    "petty":   [".##.", "####", "####", "####", ".##."],
    "lesser":  ["..##..", ".####.", "######", "##+###", "######", ".####.", "..##.."],
    "common":  ["...##...", "..####..", ".######.", "##+#####", "#+######", "########", ".######.", "..####..", "...##..."],
    "greater": ["....##....", "...####...", "..######..", ".########.", "##+#######", "#+########", "##########",
                ".########.", "..######..", "...####...", "....##...."],
    "grand":   [".....##.....", "....####....", "...######...", "..########..", ".##+#######.", "##+#########",
                "#+##########", "############", ".##########.", "..########..", "...######...", "....####...."],
}


def soul_gem(size):
    mask = GEM_MASKS[size]
    img = canvas()
    w, h = len(mask[0]), len(mask)
    x, y = (16 - w) // 2, (16 - h) // 2
    head(img, P.AMETHYST, mask, x, y)
    # a darker core line down the middle: the facet edge
    for b in range(1, h - 1):
        put(img, x + w // 2, y + b, P.AMETHYST[1])
    if size == "grand":
        for a in range(w):
            put(img, x + a, y + h - 1, P.GOLD[2 if a % 2 else 3])
    return outline(img, P.OUTLINE)


SALT_PILE = ["...#.......", "..###..#...", ".#####.##..", ".##+##.###.", "###########", "##+########", ".#########."]
SALTS = {"fire_salts": P.FIRE, "frost_salts": P.MOONSTONE, "void_salts": P.CONJURATION}


def salts(name):
    img = canvas()
    head(img, SALTS[name], SALT_PILE, 2, 5)
    return outline(img, P.OUTLINE)


def nirnroot():
    """A pale stalk with drooping glowing leaves (Skyrim's nirnroot rings and glows)."""
    img = canvas()
    leaves = [".#.......#.", "##.#...#.##", "#..##.##..#", "....###....", "...#.#.#...", "....###...."]
    head(img, P.GLASS, leaves, 2, 2)
    for y in range(8, 14):
        put(img, 7, y, P.CLOTH_GREEN[3] if y % 2 else P.CLOTH_GREEN[2])
    for x, y in ((6, 13), (8, 13), (5, 14), (9, 14), (7, 14)):
        put(img, x, y, P.CLOTH_BROWN[2])
    put(img, 3, 2, P.GLASS[4])
    put(img, 11, 2, P.GLASS[4])
    return outline(img, P.OUTLINE)


def all_item_sprites():
    """{texture path under urarcana textures: image}."""
    out = {f"item/{g}_soul_gem": soul_gem(g) for g in GEM_MASKS}
    out.update({f"item/{name}": salts(name) for name in SALTS})
    out["item/nirnroot"] = nirnroot()
    return out


SOUL_GLYPH = ["...##...", "..####..", ".##++##.", "##+##+##", "##+##+##", ".##++##.", "..####..", "...##..."]


def spell_icons():
    from artforge import procedural as G
    return {"gui/spell/soul_trap": G.medallion_icon(P.CONJURATION, SOUL_GLYPH)}


def stations():
    """Alchemy lab (a work table with a mortar, bottles and a little cauldron) and the arcane enchanter
    (a stone altar with a violet crystal and an open book). Footprints match ArcanaStationBlock's shapes."""
    from artforge.model3d import Model
    from artforge import procedural as G
    wood = G.planks(P.WOOD_DARK, seed=21, boards=4)
    top = G.planks(P.WOOD, seed=22, boards=4, vertical=False)
    iron = G.noise_texture(P.IRON, seed=23, weights=(1, 2, 5, 3, 1))
    glass = G.noise_texture(P.GLASS, seed=24, weights=(0, 1, 3, 4, 2))
    potion = G.noise_texture(P.CLOTH_RED, seed=25, weights=(0, 1, 3, 4, 2))
    stone = G.noise_texture(P.DEEPSLATE, seed=26)
    trim = G.noise_texture(P.ELVEN, seed=27, weights=(1, 3, 4, 2, 0))
    crystal = G.noise_texture(P.AMETHYST, seed=28, weights=(0, 1, 3, 4, 3))
    page = G.noise_texture(P.PARCHMENT, seed=29, weights=(0, 1, 2, 4, 3))

    lab = Model("alchemy_lab", {"wood": wood, "top": top, "iron": iron, "glass": glass, "potion": potion}, namespace="urarcana")
    lab.cube((0, 12, 0), (16, 15, 16), "top", name="table_top")
    for i, (x, z) in enumerate(((1, 1), (13, 1), (1, 13), (13, 13))):
        lab.cube((x, 0, z), (x + 2, 12, z + 2), "wood", name=f"leg_{i}")
    lab.cube((2, 15, 3), (7, 19, 8), "iron", name="cauldron")
    lab.cube((9, 15, 3), (11, 19, 5), "glass", name="bottle_tall")
    lab.cube((12, 15, 4), (14, 18, 6), "potion", name="bottle_red")
    lab.cube((9, 15, 9), (13, 17, 13), "wood", name="mortar")

    altar = Model("arcane_enchanter", {"stone": stone, "trim": trim, "crystal": crystal, "page": page}, namespace="urarcana")
    altar.cube((1, 0, 1), (15, 4, 15), "stone", name="base")
    altar.cube((3, 4, 3), (13, 12, 13), "stone", name="pillar")
    altar.cube((0, 12, 0), (16, 14, 16), "trim", name="top")
    altar.cube((2, 14, 4), (8, 15, 12), "page", name="book_left")
    altar.cube((8, 14, 4), (14, 15, 12), "page", name="book_right")
    altar.cube((6, 15, 6), (10, 21, 10), "crystal", name="crystal")
    return [lab, altar]
