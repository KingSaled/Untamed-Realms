"""Colour ramps. Every ramp runs dark -> light and has exactly 5 entries (indices 1..5 in templates)."""

def hex_rgba(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def ramp(*hexes):
    assert len(hexes) == 5, "ramps have 5 shades"
    return [hex_rgba(h) for h in hexes]

OUTLINE = hex_rgba("1b1420")
OUTLINE_SOFT = hex_rgba("2c2230")
INK = hex_rgba("3a2a1e")          # writing on parchment, maps, notices
TRANSPARENT = (0, 0, 0, 0)

# Metals
IRON      = ramp("3b3f46", "5d636c", "8a9099", "b8bdc4", "e4e7ea")
STEEL     = ramp("2f3a44", "4d5d6b", "74889a", "a7b8c6", "dbe6ee")
GOLD      = ramp("6b3d0c", "a8650f", "d99a1e", "f4cf4a", "fff2a8")
SILVER    = ramp("4a4f5c", "777e8e", "a8afbd", "d3d8e2", "f6f8fc")
COPPER    = ramp("5a2a16", "8e4423", "c06a35", "e3965a", "f8c99a")
ELVEN     = ramp("4d3a12", "86692a", "bf9c48", "e2cc7c", "fbf0c0")
DWARVEN   = ramp("4a2a12", "7b4a20", "b0753a", "d8a668", "f3d7a6")
GLASS     = ramp("174332", "2a7356", "48a77e", "86d8ae", "d4f7e4")
EBONY     = ramp("0f0c14", "221c2b", "3a3046", "5b4d6b", "8a7a9c")
ORCISH    = ramp("1d2420", "34413a", "4f6153", "728a73", "a4b8a0")
# Skyrim-accurate weapon metals for the 3D arsenal (docs/ART_STYLE.md, 3D weapons)
ORCISH_BRASS = ramp("2e2e12", "4f4f20", "7a7634", "a8a256", "d4cf8c")
DAEDRIC_BLACK = ramp("0c0809", "1c1416", "2e2226", "4a3a3e", "6e5a5e")
EMBER_RED = ramp("4a0606", "8c0c0c", "d01818", "ff4a2a", "ffb08a")
IRON_AGED = ramp("2b2925", "4a4740", "6d685e", "958f82", "c4beb0")
MOONSTONE = ramp("3a4452", "5e6a7c", "8c99ab", "bcc7d4", "eef3f8")
DAEDRIC   = ramp("1a0608", "3d0c10", "6e1418", "a8282a", "e05a4a")

# Stone (blocks)
STONE     = ramp("4a4a4f", "626268", "7a7a80", "8f8f95", "a8a8ad")
DEEPSLATE = ramp("232327", "2e2e33", "3c3c42", "4a4a50", "5c5c63")
FIRE      = ramp("5a1606", "a8360c", "e0661a", "f8a838", "ffe48a")

# Organics
LEATHER   = ramp("3a2214", "5a3620", "7d4f2f", "a46e45", "c99a6c")
WOOD_DARK = ramp("2a1a10", "43291a", "5f3c25", "7c5233", "9c6c45")
WOOD      = ramp("3e2716", "5e3d22", "835832", "a87946", "cb9d62")
PARCHMENT = ramp("8a6f45", "b39668", "d4bb8a", "eadbb2", "f8f0d8")
CLOTH_RED = ramp("3d0d10", "651619", "922427", "bf3a36", "e06a5a")
CLOTH_BLUE= ramp("0e1838", "1c2d5e", "2d4a8c", "4c72b8", "86a8de")
CLOTH_GREEN=ramp("0f2412", "1d3d20", "2f5c31", "4a8248", "7caf72")
CLOTH_PURPLE=ramp("1e0e2c", "351a4c", "52306f", "7550a0", "a689cc")
CLOTH_BROWN=ramp("2b1d12", "45301e", "634630", "866247", "a98563")
CLOTH_GREY= ramp("1f2124", "35383d", "50545b", "72777f", "9da2a9")
CLOTH_WHITE=ramp("8d8a82", "b2aea4", "d0ccc0", "e8e4d8", "fbf9f2")
CLOTH_BLACK=ramp("0b0b0e", "17171c", "25252c", "38383f", "4f4f58")

# Magic schools (spell tomes & icons)
DESTRUCTION = ramp("4a0a06", "8c1d0c", "d24a12", "f78a2a", "ffd56a")
RESTORATION = ramp("6b4a06", "a8800f", "e0c02a", "f8e874", "fffbd0")
ALTERATION  = ramp("1c0e3c", "35217a", "5a44b8", "8c7ae2", "c8bdfc")
CONJURATION = ramp("1a1030", "3b1f5c", "6a3594", "a35ec8", "d8a6f2")
ILLUSION    = ramp("061f2c", "0d4058", "17708e", "36a8c4", "8ae2ef")
SCHOOLS = {"destruction": DESTRUCTION, "restoration": RESTORATION, "alteration": ALTERATION,
           "conjuration": CONJURATION, "illusion": ILLUSION}

# Gems
RUBY      = ramp("3d0410", "7a0a1e", "bc1a34", "ef4a5c", "ffb0b8")
SAPPHIRE  = ramp("06143d", "0f2c7a", "1f50bc", "4a86ef", "b0d0ff")
EMERALD   = ramp("04301a", "0a5e30", "16944c", "3ccc7c", "aaf5c8")
AMETHYST  = ramp("26093d", "4d1678", "7a2cb4", "a964e0", "dcb6fb")

# People
SKIN_TONES = [
    ramp("6b4630", "8e5f43", "b07a58", "cf9b78", "e8bf9f"),
    ramp("4a2c1c", "6b412a", "8c573a", "a87252", "c49272"),
    ramp("2a1810", "3e2418", "573424", "714634", "8c5c48"),
    ramp("7a5440", "a07258", "c49276", "e0b496", "f6d6be"),
]
HAIR = {
    "black": ramp("0c0a0c", "1a1618", "2a2426", "3c3436", "524a4a"),
    "brown": ramp("1f120a", "352010", "4e301a", "6b4526", "8a5e38"),
    "blond": ramp("5a4214", "8a6a24", "b8943c", "dcbe62", "f4e09a"),
    "red":   ramp("3a1006", "62200c", "8c3414", "b45024", "d8783e"),
    "grey":  ramp("3a3a3e", "5a5a60", "7e7e84", "a4a4aa", "cacacf"),
    "white": ramp("8a8a8e", "acacb0", "cacace", "e2e2e6", "f8f8fa"),
}

def shade(color, factor):
    r, g, b, a = color
    return (max(0, min(255, int(r * factor))), max(0, min(255, int(g * factor))), max(0, min(255, int(b * factor))), a)

def mix(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))
