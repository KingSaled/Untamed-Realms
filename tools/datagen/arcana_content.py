#!/usr/bin/env python3
"""
Default content for ur-arcana: alchemy effects and ingredients, the Soul Trap spell, recipes (stations,
soul gems; vanilla enchanting table and brewing stand switched off), loot (salts from mobs, gems and
nirnroot in chests), item/block models and lang.
"""
import json, os, shutil
from collections import Counter

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "mods", "ur-arcana", "src", "main", "resources")
NS = "urarcana"


def write(rel, obj):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


for sub in ("data/urarcana/urarcana/alchemy_effects", "data/urarcana/urarcana/ingredients", "data/urarcana/recipe"):
    shutil.rmtree(os.path.join(ROOT, sub), ignore_errors=True)

lang = {}
MC = "minecraft:"

# ------------------------------------------------------------------ alchemy effects
# id: (name, mob effect, duration ticks (0 = instant), amplifier, harmful, value)
EFFECTS = {
    "restore_health":    ("Restore Health", MC + "instant_health", 0, 0, False, 40),
    "restore_magicka":   ("Restore Magicka", NS + ":restore_magicka", 0, 0, False, 35),
    "restore_stamina":   ("Restore Stamina", NS + ":restore_stamina", 0, 0, False, 30),
    "regenerate_health": ("Regenerate Health", MC + "regeneration", 400, 0, False, 45),
    "fortify_health":    ("Fortify Health", MC + "health_boost", 1200, 0, False, 45),
    "fortify_strength":  ("Fortify Strength", MC + "strength", 1200, 0, False, 50),
    "resist_damage":     ("Resist Damage", MC + "resistance", 600, 0, False, 50),
    "ward":              ("Ward", MC + "absorption", 1200, 0, False, 40),
    "invisibility":      ("Invisibility", MC + "invisibility", 600, 0, False, 55),
    "swiftness":         ("Swiftness", MC + "speed", 1200, 0, False, 30),
    "resist_fire":       ("Resist Fire", MC + "fire_resistance", 1800, 0, False, 35),
    "waterbreathing":    ("Waterbreathing", MC + "water_breathing", 1800, 0, False, 25),
    "fortify_mining":    ("Fortify Mining", MC + "haste", 1200, 0, False, 25),
    "night_eye":         ("Night Eye", MC + "night_vision", 1800, 0, False, 20),
    "leaping":           ("Leaping", MC + "jump_boost", 1200, 0, False, 15),
    "featherfall":       ("Featherfall", MC + "slow_falling", 600, 0, False, 20),
    "fortune":           ("Fortune", MC + "luck", 2400, 0, False, 20),
    "damage_health":     ("Damage Health", MC + "instant_damage", 0, 0, True, 45),
    "lingering_damage":  ("Lingering Damage Health", MC + "poison", 200, 0, True, 40),
    "withering":         ("Withering", MC + "wither", 200, 0, True, 45),
    "paralysis":         ("Paralysis", MC + "slowness", 80, 5, True, 60),
    "slow":              ("Slow", MC + "slowness", 300, 1, True, 30),
    "weakness":          ("Weakness", MC + "weakness", 600, 0, True, 30),
    "ravage_magicka":    ("Ravage Magicka", NS + ":ravage_magicka", 0, 0, True, 30),
    "ravage_stamina":    ("Ravage Stamina", NS + ":ravage_stamina", 0, 0, True, 25),
    "blindness":         ("Blindness", MC + "blindness", 200, 0, True, 30),
    "fatigue":           ("Fatigue", MC + "mining_fatigue", 600, 1, True, 15),
    "levitation":        ("Levitation", MC + "levitation", 100, 0, True, 20),
    "confusion":         ("Confusion", MC + "nausea", 300, 0, True, 10),
    "hunger":            ("Hunger", MC + "hunger", 600, 0, True, 10),
}
for eid, (name, mob, duration, amp, harmful, value) in EFFECTS.items():
    obj = {"name": name, "mob_effect": mob}
    if duration: obj["duration"] = duration
    if amp: obj["amplifier"] = amp
    if harmful: obj["harmful"] = True
    obj["value"] = value
    write(f"data/{NS}/{NS}/alchemy_effects/{eid}.json", obj)

# ------------------------------------------------------------------ ingredients
# item: four effects, in the order they are discovered (tasting reveals the first). Most are vanilla items;
# the pairings follow Skyrim's where an ingredient has a Skyrim twin (mountain flowers, salts, nirnroot...).
INGREDIENTS = {
    # our own
    NS + ":fire_salts":           ["resist_fire", "fortify_strength", "ravage_magicka", "regenerate_health"],
    NS + ":frost_salts":          ["slow", "restore_magicka", "ward", "fatigue"],
    NS + ":void_salts":           ["damage_health", "ward", "invisibility", "restore_magicka"],
    NS + ":nirnroot":             ["damage_health", "ravage_stamina", "invisibility", "resist_damage"],
    # flowers and plants
    MC + "poppy":                 ["restore_magicka", "ravage_stamina", "fortune", "damage_health"],
    MC + "cornflower":            ["restore_health", "fortify_health", "resist_damage", "ravage_magicka"],
    MC + "allium":                ["restore_stamina", "invisibility", "ward", "lingering_damage"],
    MC + "dandelion":             ["featherfall", "fortune", "swiftness", "hunger"],
    MC + "wither_rose":           ["withering", "paralysis", "ravage_stamina", "weakness"],
    MC + "red_mushroom":          ["lingering_damage", "fortify_strength", "confusion", "ravage_magicka"],
    MC + "brown_mushroom":        ["fatigue", "restore_stamina", "night_eye", "lingering_damage"],
    MC + "glow_berries":          ["restore_magicka", "night_eye", "fortify_mining", "regenerate_health"],
    MC + "sweet_berries":         ["restore_stamina", "swiftness", "damage_health", "fortune"],
    MC + "kelp":                  ["waterbreathing", "restore_health", "hunger", "swiftness"],
    MC + "chorus_fruit":          ["levitation", "invisibility", "confusion", "featherfall"],
    MC + "nether_wart":           ["ravage_magicka", "restore_magicka", "weakness", "withering"],
    MC + "beetroot":              ["restore_health", "fortify_strength", "fortune", "fatigue"],
    MC + "poisonous_potato":      ["lingering_damage", "hunger", "weakness", "restore_stamina"],
    # creatures
    MC + "spider_eye":            ["lingering_damage", "ravage_stamina", "invisibility", "weakness"],
    MC + "fermented_spider_eye":  ["weakness", "invisibility", "paralysis", "blindness"],
    MC + "rotten_flesh":          ["hunger", "restore_health", "lingering_damage", "fortify_strength"],
    MC + "bone_meal":             ["ravage_stamina", "fortify_strength", "regenerate_health", "blindness"],
    MC + "feather":               ["featherfall", "leaping", "swiftness", "levitation"],
    MC + "rabbit_foot":           ["leaping", "swiftness", "fortune", "confusion"],
    MC + "ink_sac":               ["blindness", "invisibility", "waterbreathing", "night_eye"],
    MC + "glow_ink_sac":          ["night_eye", "restore_magicka", "waterbreathing", "blindness"],
    MC + "pufferfish":            ["waterbreathing", "lingering_damage", "confusion", "hunger"],
    MC + "cod":                   ["waterbreathing", "restore_stamina", "fortify_health", "hunger"],
    MC + "turtle_scute":          ["resist_damage", "waterbreathing", "slow", "fortify_health"],
    MC + "slime_ball":            ["leaping", "slow", "featherfall", "resist_damage"],
    MC + "honeycomb":             ["restore_stamina", "regenerate_health", "resist_damage", "slow"],
    MC + "phantom_membrane":      ["featherfall", "night_eye", "fatigue", "levitation"],
    MC + "ghast_tear":            ["regenerate_health", "featherfall", "withering", "resist_damage"],
    MC + "magma_cream":           ["resist_fire", "leaping", "slow", "damage_health"],
    MC + "blaze_powder":          ["fortify_strength", "resist_fire", "ravage_magicka", "withering"],
    # gold, gems and dusts
    MC + "glistering_melon_slice": ["restore_health", "regenerate_health", "fortify_health", "swiftness"],
    MC + "golden_carrot":         ["night_eye", "restore_health", "fortify_health", "leaping"],
    MC + "sugar":                 ["swiftness", "restore_stamina", "hunger", "confusion"],
    MC + "amethyst_shard":        ["ward", "restore_magicka", "fortify_mining", "paralysis"],
    MC + "redstone":              ["swiftness", "fortify_mining", "confusion", "damage_health"],
    MC + "glowstone_dust":        ["night_eye", "fortify_mining", "ward", "resist_fire"],
}
uses = Counter(e for effects in INGREDIENTS.values() for e in effects)
for item, effects in INGREDIENTS.items():
    assert len(effects) == 4 and len(set(effects)) == 4, item
    for e in effects:
        assert e in EFFECTS, f"{item}: unknown effect {e}"
    write(f"data/{NS}/{NS}/ingredients/{item.split(':')[1]}.json", {"item": item, "effects": [f"{NS}:{e}" for e in effects]})
for e in EFFECTS:
    assert uses[e] >= 2, f"effect {e} is on {uses[e]} ingredient(s); it needs two to be brewable"

# ------------------------------------------------------------------ Soul Trap (a ur-magic spell; read only when ur-magic is installed)
write(f"data/{NS}/urmagic/spells/soul_trap.json", {
    "name": "Soul Trap", "description": "If the target dies within 30 seconds, its soul fills an empty soul gem you carry.",
    "school": "conjuration", "level": 10, "cost": 35, "kind": "projectile", "speed": 1.8, "element": "arcane",
    "cooldown": 20, "xp": 10, "effects": [{"effect": f"{NS}:soul_trapped", "duration": 600}]})

# ------------------------------------------------------------------ items, blocks, models
GEMS = ["petty", "lesser", "common", "greater", "grand"]
SALTS = {"fire_salts": "Fire Salts", "frost_salts": "Frost Salts", "void_salts": "Void Salts", "nirnroot": "Nirnroot"}
for name, label in SALTS.items():
    lang[f"item.{NS}.{name}"] = label
    write(f"assets/{NS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
for g in GEMS:
    lang[f"item.{NS}.{g}_soul_gem"] = f"{g.capitalize()} Soul Gem"
    lang[f"soul.{NS}.{g}"] = g.capitalize()
    write(f"assets/{NS}/models/item/{g}_soul_gem.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{g}_soul_gem"}})
lang[f"soul.{NS}."] = "Empty"

STATIONS = {"alchemy_lab": "Alchemy Lab", "arcane_enchanter": "Arcane Enchanter"}
for station, label in STATIONS.items():
    lang[f"block.{NS}.{station}"] = label
    write(f"data/{NS}/loot_table/blocks/{station}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{station}"}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
write(f"data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": [f"{NS}:alchemy_lab"]})
write(f"data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"{NS}:arcane_enchanter"]})
write(f"data/{NS}/tags/item/soul_gems.json", {"replace": False, "values": [f"{NS}:{g}_soul_gem" for g in GEMS]})

# ------------------------------------------------------------------ recipes
def shaped(name, pattern, key, result, count=1):
    write(f"data/{NS}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                            "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()},
                                            "result": {"id": result, "count": count}})

def shapeless(name, items, result):
    write(f"data/{NS}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                            "ingredients": [{"item": i} for i in items], "result": {"id": result, "count": 1}})

shaped("alchemy_lab", ["BBB", "PCP", "P P"], {"B": MC + "glass_bottle", "C": MC + "cauldron", "P": "#minecraft:planks"}, f"{NS}:alchemy_lab")
shaped("arcane_enchanter", ["ABA", "ODO", "OOO"], {"A": MC + "amethyst_shard", "B": MC + "book", "O": MC + "obsidian", "D": MC + "diamond"},
       f"{NS}:arcane_enchanter")
shapeless("petty_soul_gem", [MC + "amethyst_shard", MC + "amethyst_shard", MC + "gold_nugget"], f"{NS}:petty_soul_gem")
shapeless("lesser_soul_gem", [f"{NS}:petty_soul_gem", MC + "amethyst_shard", MC + "amethyst_shard", MC + "gold_ingot"], f"{NS}:lesser_soul_gem")
shapeless("common_soul_gem", [f"{NS}:lesser_soul_gem", MC + "amethyst_block", MC + "gold_ingot"], f"{NS}:common_soul_gem")
shapeless("greater_soul_gem", [f"{NS}:common_soul_gem", MC + "amethyst_block", MC + "diamond"], f"{NS}:greater_soul_gem")
shapeless("grand_soul_gem", [f"{NS}:greater_soul_gem", MC + "echo_shard", MC + "diamond"], f"{NS}:grand_soul_gem")
# Skyrim's stations replace vanilla's: no crafting the enchanting table or the brewing stand
for vanilla in ("enchanting_table", "brewing_stand"):
    write(f"data/minecraft/recipe/{vanilla}.json", {"neoforge:conditions": [{"type": "neoforge:false"}], "type": "minecraft:crafting_shaped",
                                                    "pattern": ["#"], "key": {"#": {"item": "minecraft:stone"}}, "result": {"id": f"minecraft:{vanilla}"}})

# ------------------------------------------------------------------ loot: salts from mobs, gems + rarities in chests
def item_entry(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e

modifiers = []
for mob, salt, chance in (("blaze", "fire_salts", 0.5), ("stray", "frost_salts", 0.4), ("enderman", "void_salts", 0.15)):
    write(f"data/{NS}/loot_table/inject/{mob}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [item_entry(f"{NS}:{salt}", 1, 1, 2)]}]})
    write(f"data/{NS}/loot_modifiers/{salt}_from_{mob}.json", {"type": "urcore:inject_chest_loot", "conditions": [],
                                                                "table": f"{NS}:inject/{mob}", "chance": chance, "prefixes": [f"entities/{mob}"]})
    modifiers.append(f"{NS}:{salt}_from_{mob}")
write(f"data/{NS}/loot_table/inject/chests.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": [
    item_entry(f"{NS}:petty_soul_gem", 20), item_entry(f"{NS}:lesser_soul_gem", 14), item_entry(f"{NS}:common_soul_gem", 8),
    item_entry(f"{NS}:greater_soul_gem", 3), item_entry(f"{NS}:grand_soul_gem", 1),
    item_entry(f"{NS}:nirnroot", 10, 1, 2), item_entry(f"{NS}:fire_salts", 6, 1, 3), item_entry(f"{NS}:frost_salts", 6, 1, 3),
    item_entry(f"{NS}:void_salts", 3, 1, 2)]}]})
write(f"data/{NS}/loot_modifiers/arcana_in_chests.json", {"type": "urcore:inject_chest_loot", "conditions": [],
                                                           "table": f"{NS}:inject/chests", "chance": 0.25, "prefixes": ["chests/"]})
modifiers.append(f"{NS}:arcana_in_chests")
# the Soul Trap tome, only when ur-magic is there to read it
write(f"data/{NS}/loot_table/inject/soul_trap_tome.json", {"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "urmagic"}],
    "type": "minecraft:chest", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "urmagic:spell_tome",
    "functions": [{"function": "minecraft:set_components", "components": {"urmagic:spell": f"{NS}:soul_trap"}}]}]}]})
write(f"data/{NS}/loot_modifiers/soul_trap_tome.json", {"type": "urcore:inject_chest_loot", "conditions": [],
                                                         "table": f"{NS}:inject/soul_trap_tome", "chance": 0.05, "prefixes": ["chests/"]})
modifiers.append(f"{NS}:soul_trap_tome")
write("data/neoforge/loot_modifiers/global_loot_modifiers.json", {"replace": False, "entries": modifiers})

# ------------------------------------------------------------------ lang
lang.update({
    "effect.urarcana.restore_magicka": "Restore Magicka",
    "effect.urarcana.restore_stamina": "Restore Stamina",
    "effect.urarcana.ravage_magicka": "Ravage Magicka",
    "effect.urarcana.ravage_stamina": "Ravage Stamina",
    "effect.urarcana.soul_trapped": "Soul Trapped",
    "item.urarcana.potion": "Potion of %s",
    "item.urarcana.poison": "Poison of %s",
    "tooltip.urarcana.soul_empty": "Empty",
    "tooltip.urarcana.soul_filled": "Holds a %s soul",
    "tooltip.urarcana.soul_capacity": "Holds souls up to: %s",
    "tooltip.urarcana.ingredient": "Alchemy ingredient",
    "screen.urarcana.taste": "Taste",
    "screen.urarcana.taste_hint": "Eat one to learn its first effect (more with higher Alchemy).",
    "screen.urarcana.brew": "Brew",
    "screen.urarcana.brew_five": "Brew x5",
    "screen.urarcana.clear": "Clear",
    "screen.urarcana.no_ingredients": "You carry no alchemy ingredients. Flowers, mushrooms, berries, spider eyes, blaze powder, salts and more all work.",
    "screen.urarcana.alchemy_help": "Pick two or three ingredients. Effects they share go into the brew; brewing reveals them. "
                                    "Taste an ingredient to learn its first effect. Unknown effects show as ?.",
    "screen.urarcana.choose_more": "Pick at least one more ingredient to brew.",
    "screen.urarcana.result": "Brew:",
    "screen.urarcana.result_unknown": "You don't know of any effect these share. Brew anyway to find out - if they share nothing, they're wasted.",
    "screen.urarcana.makes_potion": "A potion of:",
    "screen.urarcana.makes_poison": "A poison (thrown) of:",
    "screen.urarcana.maybe_more": "  ...and perhaps more",
    "screen.urarcana.tab_enchant": "Enchant",
    "screen.urarcana.tab_disenchant": "Disenchant",
    "screen.urarcana.skill": "%s %s",
    "screen.urarcana.col_gear": "Item",
    "screen.urarcana.col_enchanted": "Enchanted items",
    "screen.urarcana.col_known": "Enchantment",
    "screen.urarcana.col_gems": "Soul gem",
    "screen.urarcana.none_gear": "Carry weapons, armor or tools to enchant them.",
    "screen.urarcana.none_enchanted": "You carry nothing enchanted. Enchanted books and gear can be learned from.",
    "screen.urarcana.none_known": "You know no enchantments yet. Disenchant something first.",
    "screen.urarcana.none_fit": "None of your enchantments fit this item.",
    "screen.urarcana.none_gems": "You have no filled soul gems. Cast Soul Trap on a creature, then kill it.",
    "screen.urarcana.enchant": "Enchant",
    "screen.urarcana.disenchant": "Disenchant",
    "screen.urarcana.enchant_help": "Pick an item, an enchantment and a filled soul gem.",
    "screen.urarcana.preview": "%s gains %s",
    "screen.urarcana.disenchant_help": "Destroying an enchanted item teaches you its enchantments, so you can put them on your own gear.",
    "screen.urarcana.teaches": "Destroying it teaches:",
    "screen.urarcana.known": " (known)",
    "screen.urarcana.disenchant_warning": "The item will be destroyed.",
    "screen.urarcana.nothing_new": "You already know everything this could teach.",
    "message.urarcana.tasted": "You learned %s effect(s) of this ingredient.",
    "message.urarcana.tasted_nothing": "Nothing new about this taste.",
    "message.urarcana.brew_failed": "The ingredients share no effect. The brew is ruined.",
    "message.urarcana.missing": "You don't have those ingredients.",
    "message.urarcana.discovered": "You discovered new ingredient effects.",
    "message.urarcana.already_known": "You already know these enchantments.",
    "message.urarcana.learned_enchantments": "You learned %s enchantment(s).",
    "message.urarcana.unknown_enchantment": "You don't know that enchantment.",
    "message.urarcana.cannot_enchant": "That enchantment can't go on this item.",
    "message.urarcana.enchanted": "%s gains %s.",
    "message.urarcana.soul_captured": "Soul captured: %s",
})
write(f"assets/{NS}/lang/en_us.json", dict(sorted(lang.items())))
print(f"arcana: {len(EFFECTS)} effects, {len(INGREDIENTS)} ingredients, {len(lang)} lang keys; "
      f"effect uses min {min(uses.values())} max {max(uses.values())}")
