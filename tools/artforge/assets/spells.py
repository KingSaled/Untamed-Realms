"""Spell icons (v2, see docs/ART_STYLE.md): school medallion + centred 8x8 glyph. Keys match urmagic spell ids.

Glyph grids are 8x8; '#' = glyph (lit top-left), '+' = inner detail. A glyph's drawn pixels must span
a box centred in the grid (lint.centered_glyph) - that is what keeps every icon optically centred.
"""
from artforge import palette as P
from artforge import procedural as G

GLYPHS = {
    "flame":   ["...#....", "...##...", "..###+..", ".####++.", ".##++##.", "##+..+##", "##+..+##", ".######."],
    "comet":   ["......##", ".....###", "....####", "...#+##.", "..#+#...", ".#+.....", "#+......", "#......."],
    "snow":    ["...##...", ".#.##.#.", "..####..", "########", "########", "..####..", ".#.##.#.", "...##..."],
    "spike":   ["...##...", "...##...", "..#++#..", "..#++#..", ".#+##+#.", ".#+##+#.", "#+####+#", "########"],
    "spark":   ["#......#", ".#.##.#.", "..#++#..", ".#+##+#.", ".#+##+#.", "..#++#..", ".#.##.#.", "#......#"],
    "storm":   ["....####", "...###..", "..###...", ".######.", "...###..", "..###...", ".##.....", "##......"],
    "heart":   ["........", ".##..##.", "#++#####", "#+######", ".######.", "..####..", "...##...", "........"],
    "cross":   ["...##...", "...##...", "...##...", "########", "########", "...##...", "...##...", "...##..."],
    "shield":  ["########", "#++++++#", "#+####+#", "#+####+#", "#+####+#", ".#+##+#.", "..#++#..", "...##..."],
    "sun":     ["#..##..#", ".#....#.", "...##...", "#.####.#", "#.####.#", "...##...", ".#....#.", "#..##..#"],
    "skull":   [".######.", "########", "#..##..#", "#..##..#", "########", ".##..##.", ".######.", ".#.##.#."],
    "bark":    ["########", "#+#+#+##", "########", "##+#+#+#", "########", "#+#+#+##", "########", "##+#+#+#"],
    "candle":  ["...##...", "..#++#..", "...##...", "..####..", "..#++#..", "..#++#..", ".######.", "########"],
    "eye":     ["........", "..####..", ".#....#.", "#..##..#", "#..##..#", ".#....#.", "..####..", "........"],
    "drop":    ["...##...", "...##...", "..####..", ".##+###.", "##+#####", "##+#####", ".######.", "..####.."],
    "feather": ["......##", ".....###", "....##+#", "...##+#.", "..##+#..", ".##+#...", ".#+#....", "#......."],
    "paw":     ["##....##", "##.##.##", "...##...", "........", "..####..", ".######.", ".######.", "..#..#.."],
    "sword":   ["......##", ".....###", "....###.", "#..###..", ".####...", "..##....", ".#.##...", "#......."],
    "golem":   ["..####..", "..#++#..", "########", "#.####.#", "#.####.#", "..####..", "..#..#..", ".##..##."],
    "calm":    ["........", "##....##", "........", "........", "#......#", ".#....#.", "..####..", "........"],
    "fear":    ["#......#", ".#....#.", "........", "..####..", ".#....#.", ".#....#.", ".#....#.", "..####.."],
    "ghost":   ["..####..", ".######.", "##.##.##", "##.##.##", "########", "########", "########", "#.#..#.#"],
    "steps":   ["..##....", ".####...", ".####...", "..##....", "....##..", "...####.", "...####.", "....##.."],
}

SPELLS = {
    # destruction
    "flames": ("destruction", "flame"), "firebolt": ("destruction", "comet"), "frostbite": ("destruction", "snow"),
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
    "calm": ("illusion", "calm"), "fear": ("illusion", "fear"), "invisibility": ("illusion", "ghost"), "muffle": ("illusion", "steps"),
}


def all_spell_icons():
    out = {}
    for spell, (school, glyph) in SPELLS.items():
        out[("urmagic", f"gui/spell/{spell}")] = G.medallion_icon(P.SCHOOLS[school], GLYPHS[glyph])
    return out
