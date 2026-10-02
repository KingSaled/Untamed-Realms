"""
Ores, raw ores and ingots are the player's own vanilla iron textures, recoloured by Minecraft when it
loads its textures (a `paletted_permutations` atlas source, the mechanism vanilla uses for armor
trims). We ship only palettes, never Mojang's art; a resource pack that changes iron ore changes ours too.

Each palette maps the iron colours (keys measured from 1.21.1 by tools/research/inspect.py, sorted
dark -> light) onto one of our material ramps, so a new ore reads exactly like vanilla iron ore,
just made of another metal. Generated sprites are named `minecraft:<texture>_urarsenal_<material>`.
"""
from PIL import Image

from artforge import palette as P


def hx(s):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


# vanilla 1.21.1 colours, darkest first
ORE_SPOTS = [hx(c) for c in ["77674f", "887455", "af8e77", "d8af93", "e2c0aa"]]          # iron_ore and deepslate_iron_ore
RAW_IRON = [hx(c) for c in ["3b3429", "574d39", "887455", "af8e77", "d8af93", "e9c8b1", "fedec8", "fef4ed"]]
IRON_INGOT = [hx(c) for c in ["353535", "585858", "5e5e5e", "727272", "828282", "a8a8a8", "d8d8d8", "ffffff"]]

# material -> 5-shade ramp (dark -> light)
EBONY_SHEEN = [P.EBONY[0], P.EBONY[2], P.EBONY[3], P.EBONY[4], P.AMETHYST[4]]
MATERIALS = {
    "orichalcum": P.ORCISH,
    "moonstone": P.MOONSTONE,
    "malachite": P.GLASS,
    "ebony": EBONY_SHEEN,
    "steel": P.STEEL,
    "dwarven": P.DWARVEN,
}


def luma(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def sample(ramp, u):
    """The ramp at position u (0 = darkest, 1 = lightest), blending between shades."""
    x = max(0.0, min(1.0, u)) * (len(ramp) - 1)
    i = min(int(x), len(ramp) - 2)
    f = x - i
    a, b = ramp[i], ramp[i + 1]
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)


def remap(keys, ramp):
    """Each key colour goes to the ramp shade at its relative brightness within the key set."""
    if len(keys) == len(ramp):
        return list(ramp)
    lo, hi = luma(keys[0]), luma(keys[-1])
    return [sample(ramp, (luma(k) - lo) / max(1.0, hi - lo)) for k in keys]


def row(colors):
    img = Image.new("RGBA", (len(colors), 1))
    for i, c in enumerate(colors):
        img.putpixel((i, 0), c)
    return img


# which vanilla texture each of our items/blocks is made from
ORES = ["orichalcum", "moonstone", "malachite", "ebony"]
RAWS = {"raw_orichalcum": "orichalcum", "raw_moonstone": "moonstone", "raw_malachite": "malachite", "raw_ebony": "ebony"}
INGOTS = {"steel_ingot": "steel", "orichalcum_ingot": "orichalcum", "dwarven_ingot": "dwarven", "ebony_ingot": "ebony",
          "refined_moonstone": "moonstone", "refined_malachite": "malachite"}


def sprite(vanilla, material):
    """The generated sprite's id, e.g. minecraft:block/iron_ore_urarsenal_ebony."""
    return f"minecraft:{vanilla}_urarsenal_{material}"


def palettes():
    """{texture path under urarsenal/textures: image}"""
    out = {"palettes/iron_ore_key": row(ORE_SPOTS), "palettes/raw_iron_key": row(RAW_IRON), "palettes/iron_ingot_key": row(IRON_INGOT)}
    for m, ramp in MATERIALS.items():
        out[f"palettes/ore_{m}"] = row(remap(ORE_SPOTS, ramp))
        out[f"palettes/raw_{m}"] = row(remap(RAW_IRON, ramp))
        out[f"palettes/ingot_{m}"] = row(remap(IRON_INGOT, ramp))
    return out


def atlas():
    """assets/minecraft/atlases/blocks.json: added to vanilla's block/item atlas."""
    def perms(prefix, materials):
        return {f"urarsenal_{m}": f"urarsenal:palettes/{prefix}_{m}" for m in materials}
    return {"sources": [
        {"type": "paletted_permutations", "textures": ["minecraft:block/iron_ore", "minecraft:block/deepslate_iron_ore"],
         "palette_key": "urarsenal:palettes/iron_ore_key", "permutations": perms("ore", ORES)},
        {"type": "paletted_permutations", "textures": ["minecraft:item/raw_iron"],
         "palette_key": "urarsenal:palettes/raw_iron_key", "permutations": perms("raw", sorted(set(RAWS.values())))},
        {"type": "paletted_permutations", "textures": ["minecraft:item/iron_ingot"],
         "palette_key": "urarsenal:palettes/iron_ingot_key", "permutations": perms("ingot", sorted(set(INGOTS.values())))},
    ]}


def preview():
    """A swatch sheet (palettes only - the real textures exist only in game)."""
    pals = palettes()
    keys = sorted(k for k in pals if not k.endswith("_key"))
    sheet = Image.new("RGBA", (8 * 16 + 8, len(keys) * 18 + 4), (32, 28, 36, 255))
    for r, k in enumerate(keys):
        img = pals[k]
        for i in range(img.width):
            for dx in range(14):
                for dy in range(14):
                    sheet.putpixel((4 + i * 16 + dx, 4 + r * 18 + dy), img.getpixel((i, 0)))
    return sheet
