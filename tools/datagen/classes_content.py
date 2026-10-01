#!/usr/bin/env python3
"""Default classes and birthsigns for ur-classes. Run from the repo root."""
import json, os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "mods", "ur-classes", "src", "main", "resources", "data", "urclasses", "urclasses")
MS = "minecraft:generic."

def write(path, obj):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    json.dump(obj, open(full, "w"), indent=2)

def fx(t, v, key=""):
    e = {"type": t, "value": v}
    if key: e["key"] = key
    return e

def attr(a, v, op="add_value"):
    return {"type": "attribute", "attribute": a, "operation": op, "value": v}

def item(i, n=1, **components):
    e = {"id": i, "count": n}
    if components: e["components"] = components
    return e

def tome(spell):
    return item("urmagic:spell_tome", 1, **{"urmagic:spell": f"urmagic:{spell}"})

DARK = {"minecraft:dyed_color": {"rgb": 2697513}}
FOREST = {"minecraft:dyed_color": {"rgb": 3830302}}

def leather(*pieces, dye=None):
    return [item(f"minecraft:leather_{p}", 1, **(dye or {})) for p in pieces]

CLASSES = {
    "warrior": dict(order=1, name="Warrior", tagline="Steel, shield and stubbornness.",
        description="A seasoned soldier who wins fights by standing in them. Strong with one-handed weapons and shields, comfortable in heavy armor.",
        icon="minecraft:iron_sword",
        skills={"one_handed": 15, "block": 12, "heavy_armor": 12, "smithing": 8, "archery": 5},
        effects=[attr(MS + "max_health", 4), attr("urcore:max_stamina", 20)],
        loadout=[item("minecraft:iron_sword"), item("minecraft:shield"), item("minecraft:iron_helmet"), item("minecraft:iron_chestplate")]
            + leather("leggings", "boots") + [item("minecraft:bread", 8), item("minecraft:torch", 16)],
        coins=40),
    "knight": dict(order=2, name="Knight", tagline="Oath-bound and iron-clad.",
        description="A sworn defender in full plate. Unmatched with a shield, hard to knock down, and well-spoken at court.",
        icon="minecraft:shield",
        skills={"heavy_armor": 15, "block": 15, "one_handed": 10, "speech": 8, "restoration": 5},
        effects=[attr(MS + "armor", 2), attr(MS + "knockback_resistance", 0.1)],
        loadout=[item("minecraft:iron_sword"), item("minecraft:shield")] + [item(f"minecraft:iron_{p}") for p in ("helmet", "chestplate", "leggings", "boots")]
            + [item("minecraft:cooked_beef", 6)],
        coins=80),
    "barbarian": dict(order=3, name="Barbarian", tagline="Born in the wilds, raised by the axe.",
        description="A northern raider who hits hard and keeps moving. Excels with heavy weapons, light armor and long marches; distrusts magic.",
        icon="minecraft:iron_axe",
        skills={"two_handed": 15, "one_handed": 10, "light_armor": 10, "agility": 10, "woodcutting": 8},
        effects=[attr(MS + "max_health", 6), attr("urcore:max_magicka", -30), fx("damage_bonus", 0.1, "two_handed"), fx("damage_bonus", 0.05, "one_handed")],
        loadout=[item("minecraft:iron_axe")] + leather("helmet", "chestplate", "leggings", "boots") + [item("minecraft:cooked_porkchop", 8)],
        coins=25),
    "ranger": dict(order=4, name="Ranger", tagline="Never seen first.",
        description="A wilderness scout and hunter. Deadly with a bow, quiet on their feet and at home far from any road.",
        icon="minecraft:bow",
        skills={"archery": 15, "sneak": 10, "light_armor": 10, "agility": 8, "woodcutting": 5, "fishing": 5},
        effects=[attr(MS + "movement_speed", 0.03, "add_multiplied_base")],
        loadout=[item("minecraft:bow"), item("minecraft:arrow", 48), item("minecraft:stone_sword")]
            + leather("helmet", "chestplate", "leggings", "boots", dye=FOREST) + [item("minecraft:cooked_chicken", 8)],
        coins=35),
    "thief": dict(order=5, name="Thief", tagline="What's yours is negotiable.",
        description="A city-bred rogue. Picks locks, lifts purses and strikes from the shadows - and talks fast when caught.",
        icon="minecraft:tripwire_hook",
        skills={"sneak": 15, "lockpicking": 12, "pickpocket": 12, "light_armor": 8, "one_handed": 5, "speech": 5},
        effects=[fx("stealth", 0.1), fx("sneak_attack", 0.5, "melee")],
        loadout=[item("minecraft:stone_sword"), item("minecraft:ender_pearl", 2)] + leather("helmet", "chestplate", "leggings", "boots", dye=DARK),
        coins=60),
    "mage": dict(order=6, name="Mage", tagline="Knowledge is the sharpest weapon.",
        description="A scholar of the arcane. Throws fire and frost, shapes flesh into armor and calls on summoned servants - but is frail up close.",
        icon="minecraft:enchanted_book",
        skills={"destruction": 15, "alteration": 12, "conjuration": 10, "enchanting": 8, "restoration": 8},
        effects=[attr("urcore:max_magicka", 60), attr(MS + "max_health", -2), fx("regen", 0.25, "magicka")],
        loadout=[tome("firebolt"), tome("oakflesh"), tome("conjure_familiar"), item("minecraft:book", 2), item("minecraft:bread", 6)]
            + leather("chestplate", dye={"minecraft:dyed_color": {"rgb": 3755418}}),
        coins=50),
    "cleric": dict(order=7, name="Cleric", tagline="Mend the faithful, smite the dead.",
        description="A temple healer who carries a mace as readily as a prayer. Heals allies, wards blows and burns the undead.",
        icon="minecraft:golden_apple",
        skills={"restoration": 15, "heavy_armor": 10, "block": 8, "one_handed": 8, "alchemy": 8, "speech": 5},
        effects=[attr("urcore:max_magicka", 25), fx("spell_power", 0.15, "restoration")],
        loadout=[item("minecraft:stone_sword"), item("minecraft:shield"), item("minecraft:iron_helmet"), item("minecraft:iron_chestplate"),
                 tome("healing"), tome("turn_undead"), item("minecraft:golden_apple")],
        coins=45),
    "spellsword": dict(order=8, name="Spellsword", tagline="Blade in one hand, fire in the other.",
        description="A battle-mage who mixes steel and sorcery. Fights in armor with a sword and opens with a firebolt.",
        icon="minecraft:blaze_powder",
        skills={"one_handed": 12, "destruction": 10, "heavy_armor": 10, "restoration": 8, "block": 5},
        effects=[attr("urcore:max_magicka", 20), attr("urcore:max_stamina", 10)],
        loadout=[item("minecraft:iron_sword"), item("minecraft:iron_chestplate"), tome("flames"), tome("healing")] + leather("leggings", "boots"),
        coins=40),
    "nightblade": dict(order=9, name="Nightblade", tagline="A whisper, then nothing.",
        description="An assassin who blends illusion with the knife. Calms or frightens foes, vanishes, and strikes where armor is thinnest.",
        icon="minecraft:ender_eye",
        skills={"sneak": 12, "illusion": 12, "light_armor": 10, "one_handed": 8, "destruction": 8, "alchemy": 5},
        effects=[fx("sneak_attack", 1.0, "melee"), attr("urcore:max_magicka", 15)],
        loadout=[item("minecraft:stone_sword"), tome("calm"), tome("invisibility")] + leather("helmet", "chestplate", "leggings", "boots", dye=DARK),
        coins=45),
    "bard": dict(order=10, name="Bard", tagline="Every tavern knows your name.",
        description="A wandering performer and smooth talker. Gets the best prices, the best rumours and - occasionally - the best purses.",
        icon="minecraft:goat_horn",
        skills={"speech": 15, "illusion": 8, "one_handed": 8, "light_armor": 8, "pickpocket": 8},
        effects=[fx("price_bonus", 0.1), fx("xp_bonus", 0.1, "speech")],
        loadout=[item("minecraft:stone_sword"), item("minecraft:goat_horn"), item("minecraft:writable_book")] + leather("chestplate", "boots"),
        coins=150),
    "artisan": dict(order=11, name="Artisan", tagline="The realm runs on what you make.",
        description="A miner, woodsman, cook and smith. Starts with good tools and learns every gathering and crafting skill faster.",
        icon="minecraft:iron_pickaxe",
        skills={"mining": 12, "woodcutting": 12, "smithing": 12, "cooking": 10, "farming": 10, "fishing": 10},
        effects=[fx("xp_bonus", 0.15, "category:gathering"), fx("xp_bonus", 0.1, "smithing")],
        loadout=[item("minecraft:iron_pickaxe"), item("minecraft:iron_axe"), item("minecraft:fishing_rod"), item("minecraft:wheat_seeds", 16),
                 item("minecraft:bread", 8), item("minecraft:crafting_table"), item("minecraft:furnace")] + leather("chestplate", "leggings"),
        coins=100),
}

SIGNS = {
    "warrior": ("The Warrior", "Combat skills improve 20% faster.", "minecraft:iron_sword", [fx("xp_bonus", 0.2, "category:combat")]),
    "mage": ("The Mage", "Magic skills improve 20% faster.", "minecraft:amethyst_shard", [fx("xp_bonus", 0.2, "category:magic")]),
    "thief": ("The Thief", "Stealth skills improve 20% faster.", "minecraft:leather_boots", [fx("xp_bonus", 0.2, "category:stealth")]),
    "artisan": ("The Artisan", "Gathering and crafting skills improve 20% faster.", "minecraft:iron_pickaxe", [fx("xp_bonus", 0.2, "category:gathering")]),
    "lord": ("The Lord", "+4 armor and 5% less damage taken.", "minecraft:golden_helmet", [attr(MS + "armor", 4), fx("damage_reduction", 0.05)]),
    "lady": ("The Lady", "Health and stamina regenerate faster.", "minecraft:poppy", [fx("regen", 0.5, "health"), fx("regen", 0.25, "stamina")]),
    "lover": ("The Lover", "All skills improve 10% faster.", "minecraft:pink_tulip", [fx("xp_bonus", 0.1)]),
    "apprentice": ("The Apprentice", "Magicka regenerates 50% faster.", "minecraft:book", [fx("regen", 0.5, "magicka")]),
    "atronach": ("The Atronach", "+50 Magicka, but it regenerates 40% slower.", "minecraft:lapis_lazuli", [attr("urcore:max_magicka", 50), fx("regen", -0.4, "magicka")]),
    "ritual": ("The Ritual", "All spells are 10% stronger; healing 25% stronger.", "minecraft:totem_of_undying", [fx("spell_power", 0.1), fx("spell_power", 0.25, "restoration")]),
    "shadow": ("The Shadow", "Harder to notice while sneaking; sneak attacks hit harder.", "minecraft:black_dye", [fx("stealth", 0.15), fx("sneak_attack", 0.5)]),
    "steed": ("The Steed", "Move 5% faster; actions cost 10% less stamina.", "minecraft:saddle", [attr(MS + "movement_speed", 0.05, "add_multiplied_base"), fx("cost_reduction", 0.1, "stamina")]),
    "tower": ("The Tower", "+1 luck and a better chance of double loot in untouched chests.", "minecraft:spyglass", [attr(MS + "luck", 1), fx("loot_bonus", 0.15)]),
    "serpent": ("The Serpent", "Deal 5% more damage of every kind.", "minecraft:spider_eye", [fx("damage_bonus", 0.05)]),
}

for cid, c in CLASSES.items():
    write(f"classes/{cid}.json", c)
for i, (sid, (name, desc, icon, effects)) in enumerate(SIGNS.items()):
    write(f"birthsigns/{sid}.json", {"name": name, "description": desc, "icon": icon, "order": i, "effects": effects})

lang = {
    "screen.urclasses.title": "Create your character",
    "screen.urclasses.step_class": "Choose your Class",
    "screen.urclasses.step_birthsign": "Choose your Birthsign",
    "screen.urclasses.next": "Choose Birthsign >",
    "screen.urclasses.back": "< Back",
    "screen.urclasses.confirm": "Begin your Journey",
    "screen.urclasses.starting_skills": "Starting skills",
    "screen.urclasses.starting_gear": "Starting gear",
    "screen.urclasses.purse": "Purse: %s Crowns",
    "screen.urclasses.summary": "You will begin as a %s, born under %s.",
    "screen.urclasses.class_line": "%s · born under %s",
    "banner.urclasses.chosen": "You are a %s",
    "banner.urclasses.born_under": "Born under %s",
    "message.urclasses.already_chosen": "You have already chosen your path.",
    "message.urclasses.unknown_class": "Unknown class.",
    "message.urclasses.unknown_birthsign": "Unknown birthsign.",
    "message.urclasses.choose_hint": "Welcome to the Untamed Realms! Choose your class to begin (or later with /ur class choose).",
    "command.urclasses.reset": "Reset %s's class; they will be asked to choose again.",
}
lang_path = os.path.join(os.path.dirname(__file__), "..", "..", "mods", "ur-classes", "src", "main", "resources", "assets", "urclasses", "lang", "en_us.json")
json.dump(lang, open(lang_path, "w"), indent=2)
print(f"{len(CLASSES)} classes, {len(SIGNS)} birthsigns")
