"""Spell icons: school-coloured medallion + glyph. Keys match urmagic spell ids."""
from artforge import palette as P
from artforge import procedural as G

GLYPHS = {
    "flame": ["..#..", ".##..", ".###.", "#####", "##.##", ".###."],
    "bolt": ["...##", "..##.", ".####", "..##.", ".##..", "##..."],
    "snow": ["#.#.#", ".###.", "#####", ".###.", "#.#.#"],
    "spike": ["..#..", "..#..", ".###.", ".###.", "#####"],
    "spark": ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"],
    "storm": [".###.", "#####", "..#..", ".#...", "#...."],
    "heart": [".#.#.", "#####", "#####", ".###.", "..#.."],
    "cross": ["..#..", "..#..", "#####", "..#..", "..#.."],
    "shield": ["#####", "#####", "#####", ".###.", "..#.."],
    "sun": ["#.#.#", ".###.", "##.##", ".###.", "#.#.#"],
    "skull": [".###.", "#.#.#", "#####", ".###.", ".#.#."],
    "bark": ["#.#.#", "#####", "#.#.#", "#####", "#.#.#"],
    "candle": ["..#..", ".###.", "..#..", ".###.", ".###."],
    "eye": [".###.", "#...#", "#.#.#", "#...#", ".###."],
    "drop": ["..#..", ".###.", "#####", "#####", ".###."],
    "feather": ["...##", "..###", ".###.", "###..", "#...."],
    "paw": ["#.#.#", ".....", ".###.", "#####", ".###."],
    "sword": ["....#", "...#.", "#.#..", ".#...", "#.#.."],
    "golem": [".###.", ".#.#.", "#####", ".###.", ".#.#."],
    "calm": [".....", "#...#", ".###.", ".....", "#####"],
    "fear": ["#...#", ".#.#.", ".....", ".###.", "#...#"],
    "ghost": [".###.", "#.#.#", "#####", "#####", "#.#.#"],
    "hush": ["#####", ".....", ".###.", ".....", "..#.."],
}

SPELLS = {
    # destruction
    "flames": ("destruction", "flame"), "firebolt": ("destruction", "bolt"), "frostbite": ("destruction", "snow"),
    "ice_spike": ("destruction", "spike"), "sparks": ("destruction", "spark"), "thunderbolt": ("destruction", "storm"),
    # restoration
    "healing": ("restoration", "heart"), "heal_other": ("restoration", "cross"), "ward": ("restoration", "shield"),
    "sun_fire": ("restoration", "sun"), "turn_undead": ("restoration", "skull"),
    # alteration
    "oakflesh": ("alteration", "bark"), "candlelight": ("alteration", "candle"), "detect_life": ("alteration", "eye"),
    "waterbreathing": ("alteration", "drop"), "featherfall": ("alteration", "feather"),
    # conjuration
    "conjure_familiar": ("conjuration", "paw"), "bound_sword": ("conjuration", "sword"), "conjure_guardian": ("conjuration", "golem"),
    # illusion
    "calm": ("illusion", "calm"), "fear": ("illusion", "fear"), "invisibility": ("illusion", "ghost"), "muffle": ("illusion", "hush"),
}


def all_spell_icons():
    out = {}
    for spell, (school, glyph) in SPELLS.items():
        out[("urmagic", f"gui/spell/{spell}")] = G.spell_icon(P.SCHOOLS[school], GLYPHS[glyph])
    return out
