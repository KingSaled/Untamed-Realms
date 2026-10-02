#!/usr/bin/env python3
"""Default spells, item models, loot and lang for ur-magic."""
import json, os

BASE = os.path.join(os.path.dirname(__file__), "..", "..", "mods", "ur-magic", "src", "main", "resources")

def write(rel, obj):
    full = os.path.join(BASE, rel)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    json.dump(obj, open(full, "w"), indent=2)

def eff(effect, duration=600, amplifier=0):
    return {"effect": effect, "duration": duration, "amplifier": amplifier}

S = {}
def spell(id_, name, desc, school, level, cost, kind, **kw):
    d = {"name": name, "description": desc, "school": school, "level": level, "cost": cost, "kind": kind}
    d.update(kw)
    S[id_] = d

# Destruction
spell("flames", "Flames", "A short stream of fire that sets foes alight.", "destruction", 1, 12, "cone", damage=3, range=6, radius=3, element="fire", cooldown=8, xp=5)
spell("firebolt", "Firebolt", "A bolt of fire that burns its target.", "destruction", 10, 25, "projectile", damage=7, speed=1.8, element="fire", cooldown=16, xp=8)
spell("frostbite", "Frostbite", "A shard of cold that slows and chills.", "destruction", 15, 25, "projectile", damage=6, speed=1.6, element="frost", cooldown=16, xp=8)
spell("sparks", "Sparks", "Crackling lightning arcs at foes in front of you, sapping their strength.", "destruction", 20, 18, "cone", damage=4, range=7, radius=3, element="shock", cooldown=10, xp=6)
spell("ice_spike", "Ice Spike", "A heavy spear of ice.", "destruction", 35, 45, "projectile", damage=12, speed=2.2, element="frost", cooldown=24, xp=12)
spell("thunderbolt", "Thunderbolt", "Call down lightning where you look.", "destruction", 55, 80, "lightning", damage=16, range=32, radius=2.5, element="shock", cooldown=60, xp=20)
# Restoration
spell("healing", "Healing", "Mend your own wounds.", "restoration", 1, 20, "self", heal=6, element="heal", cooldown=20, xp=8)
spell("heal_other", "Heal Other", "A mote of light that heals the ally it touches.", "restoration", 20, 30, "projectile", heal=8, speed=1.2, element="heal", cooldown=20, xp=10)
spell("ward", "Ward", "A shimmering ward absorbs incoming harm.", "restoration", 15, 30, "self", element="holy", cooldown=60, xp=10,
      effects=[eff("minecraft:absorption", 400, 1), eff("minecraft:resistance", 200, 0)])
spell("sun_fire", "Sun Fire", "Searing holy light; devastating to the undead.", "restoration", 30, 35, "projectile", damage=7, speed=1.8, element="holy", cooldown=18, xp=10)
spell("turn_undead", "Turn Undead", "Holy light burns and repels nearby undead.", "restoration", 25, 45, "area", damage=6, radius=8, area="undead", element="holy", cooldown=60, xp=14)
# Alteration
spell("oakflesh", "Oakflesh", "Your skin hardens like bark.", "alteration", 1, 30, "self", element="arcane", cooldown=60, xp=8,
      effects=[eff("minecraft:resistance", 1200, 0)])
spell("candlelight", "Candlelight", "See in the dark.", "alteration", 5, 15, "self", element="holy", cooldown=40, xp=5,
      effects=[eff("minecraft:night_vision", 2400, 0)])
spell("detect_life", "Detect Life", "Reveals living creatures around you, even through walls.", "alteration", 20, 35, "area", radius=32, duration=300, area="glow", element="arcane", cooldown=80, xp=10)
spell("waterbreathing", "Waterbreathing", "Breathe beneath the waves.", "alteration", 15, 30, "self", element="arcane", cooldown=60, xp=8,
      effects=[eff("minecraft:water_breathing", 2400, 0)])
spell("featherfall", "Featherfall", "Drift gently instead of falling.", "alteration", 10, 20, "self", element="arcane", cooldown=40, xp=6,
      effects=[eff("minecraft:slow_falling", 600, 0)])
# Conjuration
spell("conjure_familiar", "Conjure Familiar", "Summon a spectral wolf to fight at your side.", "conjuration", 1, 40, "summon", entity="minecraft:wolf", duration=1200, element="shadow", cooldown=100, xp=12)
spell("bound_sword", "Bound Sword", "Conjure a blade of pure magic for a time.", "conjuration", 15, 45, "bound_weapon", duration=2400, element="arcane", cooldown=200, xp=12)
spell("conjure_guardian", "Conjure Guardian", "Summon a stone guardian to defend you.", "conjuration", 40, 90, "summon", entity="minecraft:iron_golem", duration=1200, element="arcane", cooldown=600, xp=25)
# Illusion
spell("calm", "Calm", "Nearby hostile creatures lose the will to fight.", "illusion", 1, 30, "area", radius=10, duration=300, area="calm", element="arcane", cooldown=60, xp=10)
spell("fear", "Fear", "Nearby hostile creatures flee in terror.", "illusion", 20, 40, "area", radius=10, duration=200, area="fear", element="shadow", cooldown=60, xp=12)
spell("invisibility", "Invisibility", "Fade from sight for a short while.", "illusion", 35, 70, "self", element="shadow", cooldown=300, xp=18,
      effects=[eff("minecraft:invisibility", 600, 0)])
spell("muffle", "Muffle", "Your footsteps make no sound; monsters struggle to notice you.", "illusion", 10, 25, "self", element="shadow", cooldown=60, xp=8,
      effects=[eff("urmagic:muffled", 1200, 0)])

for k, v in S.items():
    write(f"data/urmagic/urmagic/spells/{k}.json", v)

# Item models
schools = ["destruction", "restoration", "alteration", "conjuration", "illusion"]
write("assets/urmagic/models/item/spell_tome.json", {
    "parent": "minecraft:item/generated", "textures": {"layer0": "urmagic:item/spell_tome_destruction"},
    "overrides": [{"predicate": {"urmagic:school": 0.1 * (i + 1) - 0.01}, "model": f"urmagic:item/spell_tome_{s}"} for i, s in enumerate(schools)]})
for s in schools:
    write(f"assets/urmagic/models/item/spell_tome_{s}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"urmagic:item/spell_tome_{s}"}})
write("assets/urmagic/models/item/bound_sword.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "urmagic:item/bound_sword"}})
for p in ("magicka_potion", "stamina_potion"):
    write(f"assets/urmagic/models/item/{p}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"urmagic:item/{p}"}})

# Tomes in chests (weighted toward low-level spells) + potions
entries = []
for k, v in S.items():
    entries.append({"type": "minecraft:item", "name": "urmagic:spell_tome", "weight": max(1, 12 - v["level"] // 6),
                    "functions": [{"function": "minecraft:set_components", "components": {"urmagic:spell": f"urmagic:{k}"}}]})
entries.append({"type": "minecraft:item", "name": "urmagic:magicka_potion", "weight": 15,
                 "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]})
entries.append({"type": "minecraft:item", "name": "urmagic:stamina_potion", "weight": 15,
                 "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]})
write("data/urmagic/loot_table/inject/arcana.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": entries}]})
write("data/urmagic/loot_modifiers/arcana_in_chests.json", {"type": "urcore:inject_chest_loot", "conditions": [],
                                                            "table": "urmagic:inject/arcana", "chance": 0.2, "prefixes": ["chests/"]})
write("data/neoforge/loot_modifiers/global_loot_modifiers.json", {"replace": False, "entries": ["urmagic:arcana_in_chests"]})

# Recipes for potions (brewing-free, crafted at a table: Alchemy-flavoured)
write("data/urmagic/recipe/magicka_potion.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
      "ingredients": [{"item": "minecraft:glass_bottle"}, {"item": "minecraft:lapis_lazuli"}, {"item": "minecraft:glow_berries"}],
      "result": {"id": "urmagic:magicka_potion", "count": 1}})
write("data/urmagic/recipe/stamina_potion.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
      "ingredients": [{"item": "minecraft:glass_bottle"}, {"item": "minecraft:sugar"}, {"item": "minecraft:sweet_berries"}],
      "result": {"id": "urmagic:stamina_potion", "count": 1}})

lang = {
    "item.urmagic.spell_tome": "Spell Tome",
    "item.urmagic.spell_tome.named": "Spell Tome: %s",
    "item.urmagic.bound_sword": "Bound Sword",
    "item.urmagic.magicka_potion": "Potion of Restore Magicka",
    "item.urmagic.stamina_potion": "Potion of Restore Stamina",
    "entity.urmagic.spell_projectile": "Spell",
    "entity.urmagic.summoned": "%s (summoned by %s)",
    "effect.urmagic.muffled": "Muffled",
    "tooltip.urmagic.school": "%s - level %s",
    "tooltip.urmagic.cost": "%s Magicka",
    "tooltip.urmagic.read": "Use to learn this spell",
    "tooltip.urmagic.restore_magicka": "Restores %s Magicka",
    "tooltip.urmagic.restore_stamina": "Restores %s Stamina",
    "banner.urmagic.learned": "Spell Learned: %s",
    "banner.urmagic.learned.sub": "Pick it on the spell wheel (hold %s) and cast with %s",
    "message.urmagic.already_known": "You already know %s.",
    "message.urmagic.too_complex": "This tome is beyond you. Requires %s %s.",
    "message.urmagic.no_spell": "No spell readied - hold %s for the spell wheel, or open your spellbook (%s).",
    "message.urmagic.no_magicka": "Not enough Magicka.",
    "screen.urmagic.spellbook": "Spellbook",
    "screen.urmagic.hint": "Pick a slot, then a spell. Right-click a slot to clear it.",
    "screen.urmagic.none": "You know no spells. Find or buy spell tomes.",
    "key.urmagic.cast": "Cast Readied Spell",
    "key.urmagic.next": "Next Spell",
    "key.urmagic.spellbook": "Spellbook",
    "key.urmagic.wheel": "Spell Wheel (hold)",
    "screen.urmagic.wheel": "Spell Wheel",
    "screen.urmagic.wheel.cost": "%s Magicka",
    "screen.urmagic.wheel.empty": "Empty slot",
    "screen.urmagic.wheel.hint": "Release to ready the spell - assign slots in the Spellbook (%s)",
    "subtitles.urmagic.fire_cast": "Fire roars", "subtitles.urmagic.fire_impact": "Fire bursts",
    "subtitles.urmagic.shock_cast": "Lightning crackles", "subtitles.urmagic.shock_impact": "Lightning strikes",
    "subtitles.urmagic.frost_cast": "Frost chimes", "subtitles.urmagic.frost_impact": "Ice shatters",
    "subtitles.urmagic.holy_cast": "Holy light rings", "subtitles.urmagic.holy_impact": "Holy light strikes",
    "subtitles.urmagic.heal_cast": "Healing magic", "subtitles.urmagic.shadow_cast": "Shadows whisper",
    "subtitles.urmagic.arcane_cast": "Magic hums", "subtitles.urmagic.arcane_impact": "Magic pops",
    "subtitles.urmagic.conjure": "A portal opens",
}
write("assets/urmagic/lang/en_us.json", lang)
print(f"{len(S)} spells")
