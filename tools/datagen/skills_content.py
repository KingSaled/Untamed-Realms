#!/usr/bin/env python3
"""
Generates the default data content for ur-skills (perk trees, XP tables, requirements, tags,
skill-book loot). Run from the repo root:  python3 tools/datagen/skills_content.py

The generated JSON is the shipped default. Server owners override or extend it with a datapack
using the same paths, so this script is only for us, the authors.
"""
import json, os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "mods", "ur-skills", "src", "main", "resources", "data")
NS = "urskills"

def write(path, obj):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")

def fx(type_, value, key=""):
    e = {"type": type_, "value": value}
    if key: e["key"] = key
    return e

def attr(attribute, value, op="add_value"):
    return {"type": "attribute", "attribute": attribute, "operation": op, "value": value}

def perk(id_, name, desc, level, effects, requires=()):
    p = {"id": id_, "name": name, "description": desc, "level": level, "effects": effects}
    if requires: p["requires"] = list(requires)
    return p

def ranked(prefix, name, desc_fmt, levels, effect_fn):
    """A chain of ranks: Name I, II, III, each requiring the previous."""
    numerals = ["I", "II", "III", "IV", "V"]
    out, prev = [], None
    for i, lvl in enumerate(levels):
        pid = f"{prefix}_{i + 1}"
        out.append(perk(pid, f"{name} {numerals[i]}", desc_fmt.format(rank=i + 1), lvl, effect_fn(i), [prev] if prev else ()))
        prev = pid
    return out

MS = "minecraft:generic."

def school(s, title):
    return ranked(f"{s}_mastery", f"{title} Mastery", "Spells of this school cost 15% less magicka (rank {rank}).",
                  [1, 25, 50, 75], lambda i: [fx("cost_reduction", 0.15, s)])

PERKS = {
    "one_handed": ranked("armsman", "Armsman", "One-handed weapons deal 20% more damage (rank {rank}).", [10, 30, 60], lambda i: [fx("damage_bonus", 0.2, "one_handed")]) + [
        perk("fighting_stance", "Fighting Stance", "Actions cost 25% less stamina.", 20, [fx("cost_reduction", 0.25, "stamina")]),
        perk("savage_strike", "Savage Strike", "One-handed attacks deal another 10% damage and knock foes harder.", 50,
             [fx("damage_bonus", 0.1, "one_handed"), attr(MS + "attack_knockback", 0.5)], ["armsman_2"]),
        perk("bladesman", "Bladesman", "Attack 10% faster.", 75, [attr(MS + "attack_speed", 0.1, "add_multiplied_base")], ["armsman_3"]),
    ],
    "two_handed": ranked("barbarian", "Barbarian", "Two-handed weapons deal 20% more damage (rank {rank}).", [10, 30, 60], lambda i: [fx("damage_bonus", 0.2, "two_handed")]) + [
        perk("champions_stance", "Champion's Stance", "Actions cost 25% less stamina.", 20, [fx("cost_reduction", 0.25, "stamina")]),
        perk("devastating_blow", "Devastating Blow", "Two-handed attacks deal another 15% damage.", 50, [fx("damage_bonus", 0.15, "two_handed")], ["barbarian_2"]),
        perk("warmaster", "Warmaster", "Attack 10% faster and resist knockback.", 75,
             [attr(MS + "attack_speed", 0.1, "add_multiplied_base"), attr(MS + "knockback_resistance", 0.2)], ["barbarian_3"]),
    ],
    "archery": ranked("overdraw", "Overdraw", "Arrows deal 20% more damage (rank {rank}).", [10, 30, 60], lambda i: [fx("damage_bonus", 0.2, "archery")]) + [
        perk("eagle_eye", "Eagle Eye", "Sneak attacks with arrows deal +0.5x damage.", 20, [fx("sneak_attack", 0.5, "archery")]),
        perk("ranger", "Ranger", "Move 5% faster.", 40, [attr(MS + "movement_speed", 0.05, "add_multiplied_base")], ["overdraw_1"]),
        perk("bullseye", "Bullseye", "Arrows deal another 25% damage.", 80, [fx("damage_bonus", 0.25, "archery")], ["overdraw_3"]),
    ],
    "block": ranked("shield_wall", "Shield Wall", "+10% knockback resistance (rank {rank}).", [10, 30, 60], lambda i: [attr(MS + "knockback_resistance", 0.1)]) + [
        perk("deflect", "Deflect", "Take 5% less damage from all sources.", 25, [fx("damage_reduction", 0.05)]),
        perk("quick_reflexes", "Quick Reflexes", "Take 15% less damage while blocking.", 50, [fx("damage_reduction", 0.15, "block")], ["shield_wall_2"]),
        perk("unbreakable", "Unbreakable", "+2 armor toughness.", 80, [attr(MS + "armor_toughness", 2)], ["shield_wall_3"]),
    ],
    "heavy_armor": ranked("juggernaut", "Juggernaut", "In a full set of heavy armor, take 8% less damage (rank {rank}).", [10, 30, 60], lambda i: [fx("damage_reduction", 0.08, "heavy_armor")]) + [
        perk("well_fitted", "Well Fitted", "+2 armor toughness.", 30, [attr(MS + "armor_toughness", 2)]),
        perk("conditioning", "Conditioning", "Actions cost 15% less stamina.", 50, [fx("cost_reduction", 0.15, "stamina")], ["juggernaut_2"]),
        perk("tower_of_strength", "Tower of Strength", "+25% knockback resistance.", 75, [attr(MS + "knockback_resistance", 0.25)], ["juggernaut_3"]),
    ],
    "smithing": [
        perk("steel_smithing", "Steel Smithing", "Crafted gear is more likely to be of fine quality.", 10, [fx("smithing_quality", 0.05)]),
        perk("ore_sense", "Ore Sense", "Mining XP +10%.", 20, [fx("xp_bonus", 0.1, "mining")]),
        perk("arcane_blacksmith", "Arcane Blacksmith", "Crafted gear quality improves further.", 40, [fx("smithing_quality", 0.1)], ["steel_smithing"]),
        perk("masterwork", "Masterwork", "Crafted gear quality improves greatly.", 70, [fx("smithing_quality", 0.15)], ["arcane_blacksmith"]),
        perk("forgemaster", "Forgemaster", "Your craft is legendary.", 90, [fx("smithing_quality", 0.2)], ["masterwork"]),
    ],
    "destruction": school("destruction", "Destruction") + [
        perk("augmented_flames", "Augmented Elements", "Destruction spells are 25% stronger.", 30, [fx("spell_power", 0.25, "destruction")], ["destruction_mastery_2"]),
        perk("impact", "Impact", "Destruction spells are another 25% stronger.", 60, [fx("spell_power", 0.25, "destruction")], ["augmented_flames"]),
    ],
    "restoration": school("restoration", "Restoration") + [
        perk("regeneration", "Regeneration", "Healing spells are 50% stronger.", 30, [fx("spell_power", 0.5, "restoration")]),
        perk("recovery", "Recovery", "Magicka regenerates 25% faster.", 40, [fx("regen", 0.25, "magicka")], ["regeneration"]),
        perk("avoid_death", "Avoid Death", "Slowly regenerate health at all times.", 80, [fx("regen", 1.0, "health")], ["recovery"]),
    ],
    "alteration": school("alteration", "Alteration") + [
        perk("mage_armor", "Mage Armor", "+4 armor.", 30, [attr(MS + "armor", 4)]),
        perk("magic_resistance", "Magic Resistance", "Take 5% less damage from all sources.", 50, [fx("damage_reduction", 0.05)], ["mage_armor"]),
        perk("stability", "Stability", "Alteration spells are 50% stronger.", 70, [fx("spell_power", 0.5, "alteration")], ["alteration_mastery_3"]),
    ],
    "conjuration": school("conjuration", "Conjuration") + [
        perk("summoner", "Summoner", "Conjured allies are 30% stronger and last longer.", 30, [fx("spell_power", 0.3, "conjuration")]),
        perk("twin_souls", "Twin Souls", "Conjured allies are another 50% stronger.", 70, [fx("spell_power", 0.5, "conjuration")], ["summoner"]),
    ],
    "illusion": school("illusion", "Illusion") + [
        perk("animage", "Animage", "Illusion spells are 25% stronger.", 20, [fx("spell_power", 0.25, "illusion")]),
        perk("quiet_casting", "Quiet Casting", "You are 10% harder to notice while sneaking.", 50, [fx("stealth", 0.1)], ["animage"]),
        perk("master_of_the_mind", "Master of the Mind", "Illusion spells are 50% stronger.", 80, [fx("spell_power", 0.5, "illusion")], ["quiet_casting"]),
    ],
    "enchanting": ranked("enchanter", "Enchanter", "+0.5 luck, improving enchantments and loot (rank {rank}).", [10, 35, 60], lambda i: [attr(MS + "luck", 0.5)]) + [
        perk("soul_siphon", "Soul Siphon", "Magicka regenerates 15% faster.", 40, [fx("regen", 0.15, "magicka")]),
        perk("extra_effect", "Extra Effect", "+2 luck.", 90, [attr(MS + "luck", 2)], ["enchanter_3"]),
    ],
    "light_armor": ranked("agile_defender", "Agile Defender", "In a full set of light armor, take 8% less damage (rank {rank}).", [10, 30, 60], lambda i: [fx("damage_reduction", 0.08, "light_armor")]) + [
        perk("custom_fit", "Custom Fit", "Move 3% faster.", 30, [attr(MS + "movement_speed", 0.03, "add_multiplied_base")]),
        perk("wind_walker", "Wind Walker", "Stamina regenerates 25% faster.", 50, [fx("regen", 0.25, "stamina")], ["agile_defender_2"]),
        perk("deft_movement", "Deft Movement", "Take 10% less damage from all sources.", 80, [fx("damage_reduction", 0.1)], ["agile_defender_3"]),
    ],
    "sneak": ranked("stealth", "Stealth", "You are 10% harder to notice while sneaking (rank {rank}).", [5, 30, 60], lambda i: [fx("stealth", 0.1)]) + [
        perk("backstab", "Backstab", "Melee sneak attacks deal +1x damage.", 30, [fx("sneak_attack", 1.0, "melee")], ["stealth_1"]),
        perk("deadly_aim", "Deadly Aim", "Ranged sneak attacks deal +1x damage.", 40, [fx("sneak_attack", 1.0, "archery")], ["stealth_1"]),
        perk("muffled_movement", "Muffled Movement", "Take 20% less fall damage.", 45, [fx("fall_reduction", 0.2)]),
        perk("assassins_blade", "Assassin's Blade", "Melee sneak attacks deal +2x damage.", 80, [fx("sneak_attack", 2.0, "melee")], ["backstab"]),
    ],
    "lockpicking": [
        perk("treasure_hunter_1", "Treasure Hunter I", "Unlooted chests are 10% more likely to hold double loot.", 10, [fx("loot_bonus", 0.1)]),
        perk("golden_touch", "Golden Touch", "+1 luck.", 40, [attr(MS + "luck", 1)], ["treasure_hunter_1"]),
        perk("treasure_hunter_2", "Treasure Hunter II", "Unlooted chests are another 15% more likely to hold double loot.", 50, [fx("loot_bonus", 0.15)], ["treasure_hunter_1"]),
        perk("locksmith", "Locksmith", "Unlooted chests are another 20% more likely to hold double loot.", 80, [fx("loot_bonus", 0.2)], ["treasure_hunter_2"]),
    ],
    "pickpocket": ranked("light_fingers", "Light Fingers", "Pickpocketing is 10% more likely to succeed (rank {rank}).", [10, 30, 60], lambda i: [fx("pickpocket", 0.1)]) + [
        perk("cutpurse", "Cutpurse", "Pickpocketing is another 15% more likely to succeed.", 40, [fx("pickpocket", 0.15)], ["light_fingers_2"]),
        perk("misdirection", "Misdirection", "Pickpocketing is another 20% more likely to succeed.", 80, [fx("pickpocket", 0.2)], ["light_fingers_3"]),
    ],
    "speech": ranked("haggling", "Haggling", "Prices are 5% better (rank {rank}).", [10, 30, 50], lambda i: [fx("price_bonus", 0.05)]) + [
        perk("persuasion", "Persuasion", "Speech XP +20%.", 25, [fx("xp_bonus", 0.2, "speech")]),
        perk("master_trader", "Master Trader", "Prices are another 10% better.", 80, [fx("price_bonus", 0.1)], ["haggling_3"]),
    ],
    "alchemy": ranked("alchemist", "Alchemist", "Potions you drink last 15% longer (rank {rank}).", [10, 30, 60], lambda i: [fx("potion_duration", 0.15)]) + [
        perk("physician", "Physician", "Slowly regenerate health.", 40, [fx("regen", 0.5, "health")]),
        perk("purity", "Purity", "Potions you drink last another 30% longer.", 80, [fx("potion_duration", 0.3)], ["alchemist_3"]),
    ],
    "mining": ranked("prospector", "Prospector", "+5% chance of double ore (rank {rank}).", [10, 35, 60], lambda i: [fx("extra_drop", 0.05, "mining")]) + [
        perk("rock_breaker", "Rock Breaker", "Mine 15% faster.", 20, [fx("speed_bonus", 0.15, "mining")]),
        perk("deep_miner", "Deep Miner", "Mine another 20% faster.", 50, [fx("speed_bonus", 0.2, "mining")], ["rock_breaker"]),
        perk("gem_finder", "Gem Finder", "+10% chance of double ore.", 80, [fx("extra_drop", 0.1, "mining")], ["prospector_3"]),
    ],
    "woodcutting": ranked("lumberjack", "Lumberjack", "Chop 15% faster (rank {rank}).", [10, 30, 60], lambda i: [fx("speed_bonus", 0.15, "woodcutting")]) + [
        perk("timber", "Timber", "+10% chance of double logs.", 40, [fx("extra_drop", 0.1, "woodcutting")]),
        perk("forester", "Forester", "+15% chance of double logs.", 75, [fx("extra_drop", 0.15, "woodcutting")], ["timber"]),
    ],
    "fishing": ranked("angler", "Angler", "+8% chance of an extra catch (rank {rank}).", [10, 40], lambda i: [fx("extra_drop", 0.08, "fishing")]) + [
        perk("sea_legs", "Sea Legs", "Hold your breath longer.", 30, [attr(MS + "oxygen_bonus", 1)]),
        perk("master_angler", "Master Angler", "+1 luck of the sea.", 70, [attr(MS + "luck", 1)], ["angler_2"]),
    ],
    "farming": ranked("green_thumb", "Green Thumb", "+10% chance of double harvest (rank {rank}).", [10, 35, 60], lambda i: [fx("extra_drop", 0.1, "farming")]) + [
        perk("animal_husbandry", "Animal Husbandry", "Farming XP +15%.", 30, [fx("xp_bonus", 0.15, "farming")]),
        perk("harvest_festival", "Harvest Festival", "Food heals a little extra.", 70, [fx("food_healing", 0.1)], ["green_thumb_3"]),
    ],
    "cooking": ranked("hearty_meals", "Hearty Meals", "Food heals a little extra (rank {rank}).", [10, 30, 60], lambda i: [fx("food_healing", 0.05)]) + [
        perk("chef", "Chef", "Cooking XP +20%.", 40, [fx("xp_bonus", 0.2, "cooking")]),
        perk("feast", "Feast", "Slowly regenerate health.", 75, [fx("regen", 0.5, "health")], ["hearty_meals_3"]),
    ],
    "agility": ranked("fleet_foot", "Fleet Foot", "Move 3% faster (rank {rank}).", [10, 40, 70], lambda i: [attr(MS + "movement_speed", 0.03, "add_multiplied_base")]) + [
        perk("light_landing", "Light Landing", "Take 20% less fall damage.", 25, [fx("fall_reduction", 0.2)]),
        perk("second_wind", "Second Wind", "Stamina regenerates 30% faster.", 50, [fx("regen", 0.3, "stamina")]),
        perk("marathoner", "Marathoner", "Actions cost 20% less stamina.", 75, [fx("cost_reduction", 0.2, "stamina")], ["second_wind"]),
    ],
}

for skill, perks in PERKS.items():
    ids = [p["id"] for p in perks]
    assert len(ids) == len(set(ids)), skill
    write(f"{NS}/{NS}/perks/{skill}.json", {"skill": skill, "perks": perks})

# Perk ids are global within the namespace - make sure no two trees collide.
all_ids = [p["id"] for ps in PERKS.values() for p in ps]
dupes = {i for i in all_ids if all_ids.count(i) > 1}
assert not dupes, f"duplicate perk ids across trees: {dupes}"

# ------------------------------------------------------------------ XP tables
def table(skill, trigger, entries):
    return {"skill": skill, "trigger": trigger,
            "entries": [dict({"match": m, "xp": x}, **({"mature_only": True} if mature else {})) for m, x, mature in entries]}

def e(match, xp, mature=False):
    return (match, xp, mature)

write(f"{NS}/{NS}/xp_sources/mining.json", table("mining", "break_block", [
    e("#minecraft:base_stone_overworld", 1), e("#minecraft:base_stone_nether", 1), e("minecraft:end_stone", 2),
    e("#minecraft:coal_ores", 8), e("#minecraft:copper_ores", 6), e("#minecraft:iron_ores", 15),
    e("#minecraft:redstone_ores", 12), e("#minecraft:lapis_ores", 15), e("#minecraft:gold_ores", 25),
    e("minecraft:nether_gold_ore", 10), e("minecraft:nether_quartz_ore", 10),
    e("#minecraft:diamond_ores", 50), e("#minecraft:emerald_ores", 60), e("minecraft:ancient_debris", 120),
    e("minecraft:amethyst_cluster", 10), e("minecraft:obsidian", 6), e("minecraft:crying_obsidian", 8),
    e("#c:ores", 12),
]))
write(f"{NS}/{NS}/xp_sources/woodcutting.json", table("woodcutting", "break_block", [
    e("#minecraft:logs", 5), e("#minecraft:oak_logs", 5), e("#minecraft:birch_logs", 6), e("#minecraft:spruce_logs", 6),
    e("#minecraft:jungle_logs", 7), e("#minecraft:acacia_logs", 7), e("#minecraft:dark_oak_logs", 8),
    e("#minecraft:mangrove_logs", 8), e("#minecraft:cherry_logs", 9), e("#minecraft:crimson_stems", 10),
    e("#minecraft:warped_stems", 10), e("minecraft:mushroom_stem", 4),
]))
write(f"{NS}/{NS}/xp_sources/farming.json", table("farming", "break_block", [
    e("minecraft:wheat", 4, True), e("minecraft:carrots", 4, True), e("minecraft:potatoes", 4, True),
    e("minecraft:beetroots", 5, True), e("minecraft:nether_wart", 6, True), e("minecraft:cocoa", 5, True),
    e("minecraft:torchflower_crop", 8, True), e("minecraft:pitcher_crop", 8, True),
    e("minecraft:melon", 3), e("minecraft:pumpkin", 5), e("#minecraft:crops", 3, True),
    e("farmersdelight:cabbages", 5, True), e("farmersdelight:tomatoes", 5, True), e("farmersdelight:onions", 5, True),
    e("farmersdelight:rice_panicles", 5, True),
]))
write(f"{NS}/{NS}/xp_sources/fishing.json", table("fishing", "fish", [
    e("*", 10), e("#minecraft:fishes", 15), e("minecraft:salmon", 18), e("minecraft:pufferfish", 20),
    e("minecraft:tropical_fish", 25), e("minecraft:enchanted_book", 40), e("minecraft:name_tag", 40),
    e("minecraft:nautilus_shell", 40), e("minecraft:saddle", 30),
]))
write(f"{NS}/{NS}/xp_sources/smithing_smelt.json", table("smithing", "smelt", [
    e("minecraft:iron_ingot", 6), e("minecraft:gold_ingot", 8), e("minecraft:copper_ingot", 3),
    e("minecraft:netherite_scrap", 30), e("#c:ingots", 5),
]))
write(f"{NS}/{NS}/xp_sources/smithing_craft.json", table("smithing", "craft", [
    e("#minecraft:swords", 15), e("#minecraft:axes", 12), e("#minecraft:pickaxes", 12), e("#minecraft:shovels", 6),
    e("#minecraft:hoes", 6), e("#c:armors", 20), e("#minecraft:head_armor", 18), e("#minecraft:chest_armor", 28),
    e("#minecraft:leg_armor", 24), e("#minecraft:foot_armor", 16), e("minecraft:shield", 12), e("minecraft:bow", 10),
    e("minecraft:crossbow", 14), e("minecraft:anvil", 30), e("minecraft:iron_bars", 1), e("minecraft:chain", 2),
    e("minecraft:lantern", 2), e("minecraft:netherite_ingot", 60), e("minecraft:mace", 60),
]))
write(f"{NS}/{NS}/xp_sources/cooking_smelt.json", table("cooking", "smelt", [
    e("#c:foods/cooked_meat", 6), e("#c:foods/cooked_fish", 6), e("minecraft:baked_potato", 3),
    e("minecraft:dried_kelp", 1), e("minecraft:cooked_beef", 6), e("minecraft:cooked_porkchop", 6),
    e("minecraft:cooked_chicken", 5), e("minecraft:cooked_mutton", 6), e("minecraft:cooked_rabbit", 6),
    e("minecraft:cooked_cod", 5), e("minecraft:cooked_salmon", 6),
]))
write(f"{NS}/{NS}/xp_sources/cooking_craft.json", table("cooking", "craft", [
    e("minecraft:bread", 4), e("minecraft:cake", 12), e("minecraft:pumpkin_pie", 8), e("minecraft:cookie", 1),
    e("minecraft:mushroom_stew", 5), e("minecraft:rabbit_stew", 12), e("minecraft:beetroot_soup", 6),
    e("minecraft:suspicious_stew", 6), e("minecraft:golden_carrot", 6), e("minecraft:golden_apple", 15),
    e("#c:foods/soup", 8), e("#farmersdelight:meals", 12),
]))
write(f"{NS}/{NS}/xp_sources/alchemy_brew.json", table("alchemy", "brew", [
    e("minecraft:potion", 15), e("minecraft:splash_potion", 20), e("minecraft:lingering_potion", 25),
]))

# ------------------------------------------------------------------ requirements
def req(match, kind, skill, level):
    return {"match": match, "kind": kind, "skill": skill, "level": level}

reqs = []
for tier, lvl in (("iron", 10), ("diamond", 30), ("netherite", 50)):
    reqs += [req(f"minecraft:{tier}_pickaxe", "tool", "mining", lvl),
             req(f"minecraft:{tier}_shovel", "tool", "mining", max(1, lvl - 5)),
             req(f"minecraft:{tier}_axe", "tool", "woodcutting", lvl),
             req(f"minecraft:{tier}_sword", "weapon", "one_handed", lvl),
             req(f"minecraft:{tier}_axe", "weapon", "one_handed", lvl)]
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        reqs.append(req(f"minecraft:{tier}_{piece}", "armor", "heavy_armor", lvl))
for piece in ("helmet", "chestplate", "leggings", "boots"):
    reqs.append(req(f"minecraft:chainmail_{piece}", "armor", "light_armor", 10))
    reqs.append(req(f"minecraft:golden_{piece}", "armor", "light_armor", 5))
reqs += [
    req("minecraft:mace", "weapon", "two_handed", 30),
    req("minecraft:trident", "weapon", "one_handed", 25),
    req("minecraft:crossbow", "weapon", "archery", 10),
    req("#minecraft:diamond_ores", "block", "mining", 25),
    req("#minecraft:emerald_ores", "block", "mining", 30),
    req("minecraft:ancient_debris", "block", "mining", 45),
    req("minecraft:obsidian", "block", "mining", 15),
]
write(f"{NS}/{NS}/requirements/vanilla.json", {"requirements": reqs})

# ------------------------------------------------------------------ item tags
def tag(path, values):
    write(f"{NS}/tags/item/{path}.json", {"replace": False, "values": [{"id": v, "required": False} for v in values]})

tag("two_handed", ["minecraft:mace"])
tag("one_handed", ["#minecraft:swords", "minecraft:trident"])
tag("armor/heavy", [f"minecraft:{t}_{p}" for t in ("iron", "diamond", "netherite") for p in ("helmet", "chestplate", "leggings", "boots")])
tag("armor/light", [f"minecraft:{t}_{p}" for t in ("leather", "chainmail", "golden") for p in ("helmet", "chestplate", "leggings", "boots")]
    + ["minecraft:turtle_helmet"])

# ------------------------------------------------------------------ skill books in chests
SKILLS = list(PERKS.keys())
write(f"{NS}/loot_table/inject/skill_books.json", {
    "type": "minecraft:chest",
    "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "urskills:skill_book",
         "functions": [{"function": "minecraft:set_components", "components": {"urskills:skill": s}}]}
        for s in SKILLS]}]})
write(f"{NS}/loot_modifiers/skill_books_in_chests.json", {
    "type": "urcore:inject_chest_loot", "conditions": [], "table": "urskills:inject/skill_books",
    "chance": 0.12, "prefixes": ["chests/"]})
write("neoforge/loot_modifiers/global_loot_modifiers.json", {"replace": False, "entries": ["urskills:skill_books_in_chests"]})

print(f"wrote {sum(len(p) for p in PERKS.values())} perks in {len(PERKS)} trees, {len(reqs)} requirements")
