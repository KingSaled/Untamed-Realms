#!/usr/bin/env python3
"""Writes the shared 5x4x5 stone-floored game-test arena into every module."""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from nbt import structure, write_gzip

MODS = {"ur-core": "urcore", "ur-skills": "urskills", "ur-classes": "urclasses",
        "ur-quests": "urquests", "ur-npcs": "urnpcs", "ur-magic": "urmagic"}
root = os.path.join(os.path.dirname(__file__), "..", "..", "mods")
floor = [(x, 0, z, 0) for x in range(5) for z in range(5)]
nbt = structure((5, 4, 5), ["minecraft:stone"], floor)
for folder, modid in MODS.items():
    d = os.path.join(root, folder, "src", "main", "resources", "data", modid, "structure")
    os.makedirs(d, exist_ok=True)
    write_gzip(os.path.join(d, "arena.nbt"), nbt)
print("ok")
