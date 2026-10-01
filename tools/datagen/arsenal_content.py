#!/usr/bin/env python3
"""
Default content for ur-arsenal: item / block models, lang, station recipes (forge, tanning rack),
vanilla recipes (smelting, station blocks), ore generation, loot, tags, skill requirements, XP and
Better Combat weapon profiles. Numbers mirror ArsenalTier / WeaponType / ArsenalArmor.Set in Java.
"""
import json, os, shutil

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "mods", "ur-arsenal", "src", "main", "resources")
NS = "urarsenal"


def write(rel, obj):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def title(s):
    return " ".join(w.capitalize() for w in s.split("_"))


# tier: (name, smithing level, forging material)
TIERS = [
    ("iron", 1, "minecraft:iron_ingot"),
    ("steel", 10, "urarsenal:steel_ingot"),
    ("orcish", 20, "urarsenal:orichalcum_ingot"),
    ("dwarven", 30, "urarsenal:dwarven_ingot"),
    ("elven", 40, "urarsenal:refined_moonstone"),
    ("glass", 55, "urarsenal:refined_malachite"),
    ("ebony", 70, "urarsenal:ebony_ingot"),
    ("daedric", 85, "urarsenal:ebony_ingot"),
]
# weapon: (ingots, strips, hand, bettercombat parent)
WEAPONS = {
    "dagger": (1, 1, "one_handed", "bettercombat:dagger"),
    "sword": (2, 1, "one_handed", "bettercombat:sword"),
    "war_axe": (2, 1, "one_handed", "bettercombat:axe"),
    "mace": (3, 1, "one_handed", "bettercombat:mace"),
    "greatsword": (4, 2, "two_handed", "bettercombat:claymore"),
    "battleaxe": (4, 2, "two_handed", "bettercombat:double_axe"),
    "warhammer": (5, 2, "two_handed", "bettercombat:hammer"),
    "bow": (2, 2, "archery", None),
}
# armor set: (light, level, material)
ARMOR = {
    "hide": (True, 1, "minecraft:leather"), "leather": (True, 10, "minecraft:leather"),
    "elven": (True, 40, "urarsenal:refined_moonstone"), "glass": (True, 55, "urarsenal:refined_malachite"),
    "iron": (False, 1, "minecraft:iron_ingot"), "steel": (False, 10, "urarsenal:steel_ingot"),
    "orcish": (False, 20, "urarsenal:orichalcum_ingot"), "dwarven": (False, 30, "urarsenal:dwarven_ingot"),
    "ebony": (False, 70, "urarsenal:ebony_ingot"), "daedric": (False, 85, "urarsenal:ebony_ingot"),
}
PIECES = {"helmet": (2, 1), "chestplate": (4, 2), "leggings": (3, 2), "boots": (2, 1)}   # (material, strips)
PIECE_NAMES = {"helmet": "Helmet", "chestplate": "Armor", "leggings": "Greaves", "boots": "Boots"}

MATERIALS = ["steel_ingot", "orichalcum_ingot", "dwarven_ingot", "refined_moonstone", "refined_malachite", "ebony_ingot",
             "raw_orichalcum", "raw_moonstone", "raw_malachite", "raw_ebony", "dwarven_scrap", "daedra_heart", "leather_strips"]
MATERIAL_NAMES = {"orichalcum_ingot": "Orichalcum Ingot", "refined_moonstone": "Refined Moonstone", "refined_malachite": "Refined Malachite"}
# ore: (raw drop, ingot, mining level, xp, y range, veins per chunk, vein size, deepslate only)
ORES = {
    "orichalcum": ("raw_orichalcum", "orichalcum_ingot", 20, 30, (-16, 72), 7, 7, False),
    "moonstone": ("raw_moonstone", "refined_moonstone", 35, 45, (24, 160), 6, 6, False),
    "malachite": ("raw_malachite", "refined_malachite", 45, 60, (-32, 40), 4, 5, False),
    "ebony": ("raw_ebony", "ebony_ingot", 60, 90, (-64, -16), 3, 4, True),
}
STATIONS = {"forge": "Forge", "tanning_rack": "Tanning Rack", "workbench": "Workbench"}

lang = {}

# ------------------------------------------------------------------ clean generated folders
for sub in ("data/urarsenal/urarsenal/station_recipes", "data/urarsenal/weapon_attributes", "data/urarsenal/recipe"):
    shutil.rmtree(os.path.join(ROOT, sub), ignore_errors=True)

# ------------------------------------------------------------------ weapons
requirements = []
one, two = [], []
for tier, level, material in TIERS:
    for kind, (ingots, strips, hand, bc) in WEAPONS.items():
        name = f"{tier}_{kind}"
        item = f"{NS}:{name}"
        lang[f"item.{NS}.{name}"] = f"{title(tier)} {title(kind)}"
        if kind == "bow":
            write(f"assets/{NS}/models/item/{name}.json", {
                "parent": "minecraft:item/bow", "textures": {"layer0": f"{NS}:item/{name}"},
                "overrides": [
                    {"predicate": {"pulling": 1}, "model": f"{NS}:item/{name}_pulling_0"},
                    {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"{NS}:item/{name}_pulling_1"},
                    {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"{NS}:item/{name}_pulling_2"},
                ]})
            for p in range(3):
                write(f"assets/{NS}/models/item/{name}_pulling_{p}.json",
                      {"parent": f"{NS}:item/{name}", "textures": {"layer0": f"{NS}:item/{name}_pulling_{p}"}})
        else:
            write(f"assets/{NS}/models/item/{name}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/{name}"}})
            write(f"data/{NS}/weapon_attributes/{name}.json", {"parent": bc})
            (one if hand == "one_handed" else two).append(item)
        if level > 1:
            requirements.append({"match": item, "kind": "weapon", "skill": hand, "level": level})
        inputs = [{"item": material, "count": ingots}, {"item": f"{NS}:leather_strips", "count": strips}]
        if kind == "bow":
            inputs.append({"item": "minecraft:string", "count": 2})
        if tier == "daedric":
            inputs.append({"item": f"{NS}:daedra_heart", "count": 1})
        write(f"data/{NS}/{NS}/station_recipes/{name}.json", {
            "station": "forge", "result": item, "inputs": inputs, "level": level,
            "xp": round(ingots * (6 + level * 0.5), 1), "category": "weapons"})

# ------------------------------------------------------------------ armor
light_items, heavy_items = [], []
for set_name, (light, level, material) in ARMOR.items():
    for piece, (count, strips) in PIECES.items():
        name = f"{set_name}_{piece}"
        item = f"{NS}:{name}"
        lang[f"item.{NS}.{name}"] = f"{title(set_name)} {PIECE_NAMES[piece]}"
        write(f"assets/{NS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        (light_items if light else heavy_items).append(item)
        if level > 1:
            requirements.append({"match": item, "kind": "armor", "skill": "light_armor" if light else "heavy_armor", "level": level})
        inputs = [{"item": material, "count": count}, {"item": f"{NS}:leather_strips", "count": strips}]
        if set_name == "leather":
            inputs.append({"item": "minecraft:iron_ingot", "count": 1})
        if set_name in ("elven", "glass"):
            inputs.append({"item": "minecraft:leather", "count": 1})
        if set_name == "daedric":
            inputs.append({"item": f"{NS}:daedra_heart", "count": 1})
        write(f"data/{NS}/{NS}/station_recipes/{name}.json", {
            "station": "forge", "result": item, "inputs": inputs, "level": level,
            "xp": round(count * (6 + level * 0.5), 1), "category": "armor"})

# ------------------------------------------------------------------ materials, ores, stations
for m in MATERIALS:
    lang[f"item.{NS}.{m}"] = MATERIAL_NAMES.get(m, title(m))
    write(f"assets/{NS}/models/item/{m}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{m}"}})

write(f"data/{NS}/{NS}/station_recipes/steel_ingot.json", {
    "station": "forge", "result": f"{NS}:steel_ingot", "inputs": [{"item": "minecraft:iron_ingot"}, {"item": "#minecraft:coals"}],
    "level": 1, "xp": 4, "category": "materials"})
write(f"data/{NS}/{NS}/station_recipes/leather_strips.json", {
    "station": "tanning_rack", "result": f"{NS}:leather_strips", "count": 4, "inputs": [{"item": "minecraft:leather"}],
    "level": 1, "xp": 2, "category": "materials"})
write(f"data/{NS}/{NS}/station_recipes/leather_from_hide.json", {
    "station": "tanning_rack", "result": "minecraft:leather", "inputs": [{"item": "minecraft:rabbit_hide", "count": 4}],
    "level": 1, "xp": 2, "category": "materials"})
write(f"data/{NS}/{NS}/station_recipes/leather_from_flesh.json", {
    "station": "tanning_rack", "result": "minecraft:leather", "inputs": [{"item": "minecraft:rotten_flesh", "count": 4}],
    "level": 5, "xp": 2, "category": "materials"})

block_requirements = []
pickaxe, axe, needs_iron, needs_diamond = ["forge"], ["tanning_rack", "workbench"], [], []
ore_features = []
xp_entries = []
for ore, (raw, ingot, mining, xp, (ymin, ymax), count, size, deep) in ORES.items():
    block = f"{ore}_ore"
    lang[f"block.{NS}.{block}"] = f"{title(ore)} Ore"
    write(f"assets/{NS}/blockstates/{block}.json", {"variants": {"": {"model": f"{NS}:block/{block}"}}})
    write(f"assets/{NS}/models/block/{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{block}"}})
    write(f"assets/{NS}/models/item/{block}.json", {"parent": f"{NS}:block/{block}"})
    pickaxe.append(block)
    (needs_diamond if deep else needs_iron).append(block)
    block_requirements.append({"match": f"{NS}:{block}", "kind": "block", "skill": "mining", "level": mining})
    xp_entries.append({"match": f"{NS}:{block}", "xp": xp})
    write(f"data/{NS}/loot_table/blocks/{block}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{NS}:{block}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": f"{NS}:{raw}", "functions": [
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]})
    for kind, time in (("smelting", 200), ("blasting", 100)):
        write(f"data/{NS}/recipe/{ingot}_from_{kind}.json", {
            "type": f"minecraft:{kind}", "ingredient": {"item": f"{NS}:{raw}"},
            "result": {"id": f"{NS}:{ingot}"}, "experience": 0.8, "cookingtime": time})
    target = "minecraft:deepslate_ore_replaceables" if deep else "minecraft:stone_ore_replaceables"
    write(f"data/{NS}/worldgen/configured_feature/ore_{ore}.json", {
        "type": "minecraft:ore", "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": target}, "state": {"Name": f"{NS}:{block}"}}]}})
    write(f"data/{NS}/worldgen/placed_feature/ore_{ore}.json", {
        "feature": f"{NS}:ore_{ore}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                          "min_inclusive": {"absolute": ymin}, "max_inclusive": {"absolute": ymax}}},
            {"type": "minecraft:biome"}]})
    ore_features.append(f"{NS}:ore_{ore}")

write(f"data/{NS}/neoforge/biome_modifier/ores.json", {
    "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld", "features": ore_features, "step": "underground_ores"})
write(f"data/{NS}/recipe/dwarven_ingot_from_scrap.json", {
    "type": "minecraft:smelting", "ingredient": {"item": f"{NS}:dwarven_scrap"}, "result": {"id": f"{NS}:dwarven_ingot"},
    "experience": 0.5, "cookingtime": 200})

for station, name in STATIONS.items():
    lang[f"block.{NS}.{station}"] = name
    write(f"data/{NS}/loot_table/blocks/{station}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{station}"}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
write(f"data/{NS}/recipe/forge.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "CFC", "CCC"],
                                       "key": {"I": {"item": "minecraft:iron_ingot"}, "C": {"item": "minecraft:cobblestone"},
                                               "F": {"item": "minecraft:furnace"}}, "result": {"id": f"{NS}:forge"}})
write(f"data/{NS}/recipe/tanning_rack.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["S S", "SLS", "S S"],
                                              "key": {"S": {"item": "minecraft:stick"}, "L": {"item": "minecraft:leather"}},
                                              "result": {"id": f"{NS}:tanning_rack"}})
write(f"data/{NS}/recipe/workbench.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "PPP", "P P"],
                                           "key": {"I": {"item": "minecraft:iron_ingot"}, "P": {"tag": "minecraft:planks"}},
                                           "result": {"id": f"{NS}:workbench"}})

# ------------------------------------------------------------------ tags
def tag(rel, values):
    write(rel, {"replace": False, "values": values})

tag("data/minecraft/tags/block/mineable/pickaxe.json", [f"{NS}:{b}" for b in pickaxe])
tag("data/minecraft/tags/block/mineable/axe.json", [f"{NS}:{b}" for b in axe])
tag("data/minecraft/tags/block/needs_iron_tool.json", [f"{NS}:{b}" for b in needs_iron])
tag("data/minecraft/tags/block/needs_diamond_tool.json", [f"{NS}:{b}" for b in needs_diamond])
tag("data/urskills/tags/item/one_handed.json", one)
tag("data/urskills/tags/item/two_handed.json", two)
tag("data/urskills/tags/item/armor/light.json", light_items)
tag("data/urskills/tags/item/armor/heavy.json", heavy_items)
tag("data/minecraft/tags/item/swords.json", [f"{NS}:{t}_{k}" for t, _, _ in TIERS for k in ("dagger", "sword", "greatsword", "mace", "warhammer")])
tag("data/minecraft/tags/item/axes.json", [f"{NS}:{t}_{k}" for t, _, _ in TIERS for k in ("war_axe", "battleaxe")])
tag("data/minecraft/tags/item/bow_enchantable.json", [f"{NS}:{t}_bow" for t, _, _ in TIERS])
tag("data/minecraft/tags/item/durability_enchantable.json", [f"{NS}:{t}_bow" for t, _, _ in TIERS])
for piece, tagname in (("helmet", "head_armor"), ("chestplate", "chest_armor"), ("leggings", "leg_armor"), ("boots", "foot_armor")):
    tag(f"data/minecraft/tags/item/{tagname}.json", [f"{NS}:{s}_{piece}" for s in ARMOR])

# ------------------------------------------------------------------ skills: requirements + XP
write(f"data/{NS}/urskills/requirements/arsenal.json", {"requirements": requirements + block_requirements})
write(f"data/{NS}/urskills/xp_sources/arsenal_ores.json", {"skill": "mining", "trigger": "break_block", "entries": xp_entries})

# ------------------------------------------------------------------ dwarven scrap in chests
write(f"data/{NS}/loot_table/inject/dwarven_scrap.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": f"{NS}:dwarven_scrap", "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 3}}]}]}]})
write(f"data/{NS}/loot_modifiers/dwarven_scrap.json", {"type": "urcore:inject_chest_loot", "conditions": [],
                                                      "table": f"{NS}:inject/dwarven_scrap", "chance": 0.25, "prefixes": ["chests/"]})
write("data/neoforge/loot_modifiers/global_loot_modifiers.json", {"replace": False, "entries": [f"{NS}:dwarven_scrap"]})

# ------------------------------------------------------------------ jewelry (Curios rings & amulets)
# name: (description, metal, gem, gem count, smithing level)
JEWELRY = {
    "ring_of_strength": ("+10% one- and two-handed damage", "minecraft:gold_ingot", "minecraft:redstone", 8, 25),
    "ring_of_the_archer": ("+15% archery damage", "minecraft:iron_ingot", "minecraft:emerald", 1, 25),
    "ring_of_magicka": ("+30 Magicka", "minecraft:iron_ingot", "minecraft:lapis_lazuli", 6, 30),
    "ring_of_stamina": ("+30 Stamina", "minecraft:gold_ingot", "minecraft:emerald", 1, 30),
    "ring_of_vitality": ("+2 hearts", "minecraft:iron_ingot", "minecraft:redstone", 8, 35),
    "ring_of_the_thief": ("+15% pickpocket chance, harder to notice while sneaking", "minecraft:iron_ingot", "minecraft:amethyst_shard", 4, 35),
    "amulet_of_destruction": ("Destruction spells 15% stronger and 10% cheaper", "minecraft:gold_ingot", "minecraft:redstone", 8, 40),
    "amulet_of_restoration": ("Restoration spells 15% stronger and 10% cheaper", "minecraft:gold_ingot", "urarsenal:refined_moonstone", 2, 40),
    "amulet_of_the_merchant": ("10% better prices with merchants", "minecraft:gold_ingot", "minecraft:emerald", 2, 30),
    "amulet_of_warding": ("Take 8% less damage", "minecraft:iron_ingot", "minecraft:amethyst_shard", 4, 50),
    "amulet_of_learning": ("+10% skill experience", "minecraft:iron_ingot", "minecraft:lapis_lazuli", 6, 50),
    "amulet_of_vigor": ("Magicka and Stamina regenerate 25% faster", "minecraft:gold_ingot", "minecraft:lapis_lazuli", 6, 45),
}
rings, necklaces = [], []
for name, (desc, metal, gem, gems, level) in JEWELRY.items():
    lang[f"item.{NS}.{name}"] = title(name).replace(" Of ", " of ").replace(" The ", " the ")
    lang[f"item.{NS}.{name}.desc"] = desc
    write(f"assets/{NS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
    (rings if name.startswith("ring") else necklaces).append(f"{NS}:{name}")
    write(f"data/{NS}/{NS}/station_recipes/{name}.json", {
        "station": "forge", "result": f"{NS}:{name}", "level": level, "xp": 20 + level, "category": "jewelry",
        "inputs": [{"item": metal, "count": 2}, {"item": gem, "count": gems}]})
tag("data/curios/tags/item/ring.json", rings)
tag("data/curios/tags/item/necklace.json", necklaces)
write(f"data/{NS}/curios/entities/player.json", {"entities": ["player"], "slots": ["ring", "necklace"]})

# ------------------------------------------------------------------ UI strings
lang.update({
    "screen.urarsenal.all": "All",
    "screen.urarsenal.category.weapons": "Weapons",
    "screen.urarsenal.category.armor": "Armor",
    "screen.urarsenal.category.materials": "Materials",
    "screen.urarsenal.category.jewelry": "Jewelry",
    "tooltip.urarsenal.wear_ring": "Wear in a ring slot (accessories: G)",
    "tooltip.urarsenal.wear_amulet": "Wear in the necklace slot (accessories: G)",
    "screen.urarsenal.craft": "Craft",
    "screen.urarsenal.craft_five": "Craft x5",
    "screen.urarsenal.temper": "Temper",
    "screen.urarsenal.requires": "Requires %s %s",
    "screen.urarsenal.materials": "Materials:",
    "screen.urarsenal.any": "Any %s",
    "screen.urarsenal.no_recipes": "Nothing to make here yet.",
    "screen.urarsenal.no_gear": "Carry or wear weapons or armor to temper them.",
    "screen.urarsenal.quality": "Quality: %s",
    "screen.urarsenal.quality_none": "Standard",
    "screen.urarsenal.quality_max": "Your Smithing allows up to: %s",
    "screen.urarsenal.temper_cost": "Uses one %s",
    "screen.urarsenal.temper_need": "You need one of its repair material (the ingot or leather it is made of).",
    "message.urarsenal.needs_level": "You need %s %s to make that.",
    "message.urarsenal.missing": "You don't have the materials.",
    "message.urarsenal.temper_max": "Your Smithing isn't good enough to improve this further.",
    "message.urarsenal.temper_material": "You need its repair material to temper it.",
})
write(f"assets/{NS}/lang/en_us.json", dict(sorted(lang.items())))
print(f"arsenal: {len(lang)} lang keys, {len(requirements)} requirements")
