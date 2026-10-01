#!/usr/bin/env python3
"""Default townsfolk, dialogue trees, shops and settlement rosters for ur-npcs."""
import json, os

BASE = os.path.join(os.path.dirname(__file__), "..", "..", "mods", "ur-npcs", "src", "main", "resources")
DATA = os.path.join(BASE, "data", "urnpcs", "urnpcs")

def write(path, obj):
    full = os.path.join(DATA, path + ".json")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    json.dump(obj, open(full, "w"), indent=2)

def cond(t, target="", value=0, negate=False):
    c = {"type": t}
    if target: c["target"] = target
    if value: c["value"] = value
    if negate: c["negate"] = True
    return c

def act(t, target="", value=0, item=None):
    a = {"type": t}
    if target: a["target"] = target
    if value: a["value"] = value
    if item: a["item"] = item
    return a

def opt(text, next=None, conditions=(), actions=(), check=None):
    o = {"text": text}
    if next: o["next"] = next
    if conditions: o["conditions"] = list(conditions)
    if actions: o["actions"] = list(actions)
    if check: o["check"] = check
    return o

def node(text, *options, actions=()):
    n = {"text": text, "options": list(options)}
    if actions: n["actions"] = list(actions)
    return n

Q = lambda q: f"urquests:{q}"
MALE = ["Aldric", "Bram", "Cedric", "Dagny", "Eirik", "Fenwick", "Gorm", "Halvard", "Ivor", "Joren", "Kell", "Leif", "Maddox", "Njal", "Osric", "Torvald", "Ulf", "Varek"]
FEMALE = ["Astrid", "Brenna", "Calla", "Dalla", "Edda", "Freya", "Gisla", "Helga", "Ingrid", "Jora", "Kaija", "Lyra", "Maren", "Nessa", "Runa", "Sigrid", "Thyra", "Wynne"]
ANY = MALE + FEMALE

def npc(id_, title, skin, names=ANY, dialogue=True, shop=None, trainer=None, essential=True, wander=8, equipment=None, greeting=None):
    d = {"title": title, "names": names, "skin": f"urnpcs:{skin}", "essential": essential, "wander": wander}
    if dialogue: d["dialogue"] = f"urnpcs:{id_}"
    if shop: d["shop"] = f"urnpcs:{shop}"
    if trainer: d["trainer"] = {"skill": trainer[0], "max_level": trainer[1]}
    if equipment: d["equipment"] = equipment
    if greeting: d["greeting"] = greeting
    write(f"npcs/{id_}", d)

# --------------------------------------------------------------------------- townsfolk
npc("village_elder", "Village Elder", "elder", names=["Aldric", "Halvard", "Osric", "Ingrid", "Sigrid", "Thyra", "Eirik", "Edda"], wander=6)
npc("guard_captain", "Guard Captain", "guard_captain", trainer=("block", 40), wander=10,
    equipment={"mainhand": {"id": "minecraft:iron_sword"}, "offhand": {"id": "minecraft:shield"}})
npc("guard", "Town Guard", "guard", dialogue=False, essential=False, wander=16,
    equipment={"mainhand": {"id": "minecraft:iron_sword"}, "head": {"id": "minecraft:iron_helmet"}},
    greeting="Keep your weapons sheathed and we'll get along fine.")
npc("court_wizard", "Court Wizard", "court_wizard", shop="arcane_wares", trainer=("destruction", 50), wander=5)
npc("blacksmith", "Blacksmith", "blacksmith", shop="smithy", trainer=("smithing", 50), wander=4)
npc("innkeeper", "Innkeeper", "innkeeper", shop="tavern", wander=5)
npc("merchant", "Merchant", "merchant", shop="general_goods", trainer=("speech", 40), wander=6)
npc("farmer", "Farmer", "farmer", shop="farm_stall", trainer=("farming", 40), wander=12)
npc("miner", "Miner", "miner", shop="mining_supplies", trainer=("mining", 40), wander=8)
npc("hunter", "Hunter", "hunter", shop="hunting_supplies", trainer=("archery", 40), wander=12,
    equipment={"mainhand": {"id": "minecraft:bow"}})
npc("priest", "Priest", "priest", shop="temple", trainer=("restoration", 50), wander=5)
npc("bard", "Bard", "bard", trainer=("speech", 50), wander=10)
npc("thief", "Shady Stranger", "thief", shop="fence", trainer=("pickpocket", 40), wander=10)
npc("villager", "Villager", "villager_a", dialogue=False, wander=14,
    greeting="Fine day, traveller. Mind the roads after dark.")
npc("villager_b", "Villager", "villager_b", dialogue=False, wander=14,
    greeting="Have you seen the notice board? There's always someone needing help.")

# --------------------------------------------------------------------------- dialogues
write("dialogues/village_elder", {
    "entry": [
        {"conditions": [cond("quest_active", Q("main/the_elders_request"))], "node": "supplies_waiting"},
        {"conditions": [cond("quest_completed", Q("main/a_new_beginning")), cond("quest_available", Q("main/the_elders_request"))], "node": "request"},
        {"conditions": [cond("quest_completed", Q("main/the_elders_request")), cond("quest_available", Q("main/trouble_on_the_roads"))], "node": "roads"},
        {"node": "greet"}],
    "nodes": {
        "greet": node("Welcome, traveller. Few come this way any more, and fewer stay. What brings you to our village?",
                      opt("Tell me about this place.", "lore"),
                      opt("Is there any work?", "work"),
                      opt("I could use a better price on my way out of town. (Persuade)", "persuaded", check={"skill": "speech", "level": 20, "fail": "unpersuaded"})),
        "lore": node("This land was the heart of the Old Kingdom once. Now it's forest and ruin and the things that live in both. We keep the fires lit and the gates closed at night.",
                     opt("What happened to the Old Kingdom?", "kingdom"), opt("Is there any work?", "work")),
        "kingdom": node("Some say a war of mages. Some say plague. The barrows in the hills say nothing at all - but the dead in them have begun to walk.",
                        opt("I'll keep my eyes open.")),
        "work": node("Check the notice board - there's always a bounty or a supply order pinned to it. And if you want to earn my trust, speak to me again once you've settled in.",
                     opt("I will.")),
        "persuaded": node("Ha! You've a silver tongue. Here - for the road.", opt("Thank you."), actions=[act("give_coins", value=15), act("set_flag", "elder_persuaded")]),
        "unpersuaded": node("Nice try. Coin is earned here, not talked out of old men.", opt("Fair enough.")),
        "request": node("You found us, and you're still breathing - that's more than most. If you mean to stay, help us through the winter. Bring timber and bread.",
                        opt("I'll gather your supplies.", actions=[act("start_quest", Q("main/the_elders_request"))]),
                        opt("Not now.")),
        "supplies_waiting": node("Sixteen logs and six loaves. The cold won't wait for us, traveller.", opt("I'm working on it.")),
        "roads": node("You've done right by us. Now a harder task: the dead walk the roads at night. Our Guard Captain is gathering anyone who can hold a blade.",
                      opt("I'll speak with the Captain.", actions=[act("start_quest", Q("main/trouble_on_the_roads"))]),
                      opt("Let me prepare first.")),
    }})

write("dialogues/guard_captain", {
    "entry": [{"conditions": [cond("quest_completed", Q("main/trouble_on_the_roads")), cond("quest_available", Q("main/the_raiders_banner"))], "node": "raiders"},
              {"node": "greet"}],
    "nodes": {
        "greet": node("Captain of the watch. If you're here to cause trouble, don't. If you're here to stop it - I could use you.",
                      opt("Any news from the roads?", "news"), opt("Can you teach me to use a shield?", "teach")),
        "news": node("Zombies by the dozen, skeletons picking off travellers. Something is stirring them up.", opt("I'll deal with them.")),
        "teach": node("Shield up, feet planted, let the blow slide off. Practice costs coin, mind you - steel isn't free.", opt("Maybe later.")),
        "raiders": node("Scouts found tracks leading to a raider outpost under a grey banner. They're behind this - I'd stake my badge on it. Break them.",
                        opt("Consider it done.", actions=[act("start_quest", Q("main/the_raiders_banner"))]), opt("Not yet.")),
    }})

write("dialogues/court_wizard", {
    "entry": [{"conditions": [cond("quest_completed", Q("main/the_raiders_banner")), cond("quest_available", Q("main/into_the_deep"))], "node": "key"},
              {"conditions": [cond("quest_available", Q("side/tome_of_secrets"))], "node": "greet_tome"},
              {"node": "greet"}],
    "nodes": {
        "greet": node("Mm? Oh. A visitor. Mind the candles - and do not touch the book on the lectern. It bites.",
                      opt("Tell me about magic.", "magic"), opt("The lost explorer - any news?", "explorer", conditions=[cond("quest_available", Q("side/the_lost_explorer"))])),
        "greet_tome": node("You have the look of someone who finds things. I collect enchanted books - for safekeeping, naturally. Bring me one and I'll teach you a spell worth knowing.",
                           opt("I'll find you a book.", actions=[act("start_quest", Q("side/tome_of_secrets"))]), opt("Tell me about magic.", "magic")),
        "magic": node("Five schools: Destruction, Restoration, Alteration, Conjuration, Illusion. Read a spell tome to learn its spell; ready it from your spellbook; cast with focus and magicka. Practice makes the rest.",
                      opt("Thank you.")),
        "explorer": node("A cartographer of mine went south seeking a sand-buried temple. Find it - and find out what became of them.",
                         opt("I'll go.", actions=[act("start_quest", Q("side/the_lost_explorer"))]), opt("Not now.")),
        "key": node("Show me that key... yes. Yes! This is a seal of the Old Kingdom's trial halls. They are real, and they are below us. We must know what the raiders were looking for.",
                    opt("Then I'll go down.", actions=[act("start_quest", Q("main/into_the_deep"))]), opt("I need time to prepare.")),
    }})

def simple_quest_npc(id_, intro, quest, offer, accept, busy, extra=None):
    nodes = {
        "greet": node(intro, *([opt(offer, "offer", conditions=[cond("quest_available", Q(quest))])] + (extra or []))),
        "offer": node(busy[0], opt(accept, actions=[act("start_quest", Q(quest))]), opt("Not right now.")),
        "waiting": node(busy[1], opt("I'm on it.")),
    }
    write(f"dialogues/{id_}", {"entry": [{"conditions": [cond("quest_active", Q(quest))], "node": "waiting"}, {"node": "greet"}], "nodes": nodes})

simple_quest_npc("blacksmith", "Hot work and honest steel. Need something forged, or are you here to sweat?", "side/the_smiths_apprentice",
                 "Need a hand at the forge?", "I'll bring you iron.", ("Iron's scarce since the mine flooded. Bring me twelve ingots and I'll show you how a real smith works.", "Twelve iron ingots. The forge is hungry."))
simple_quest_npc("innkeeper", "Welcome to the inn! Sit, warm yourself. Rooms are cheap and the ale's cheaper.", "side/fresh_catch",
                 "You look worried.", "I'll catch some fish.", ("The stew pot's empty and the roads are full of hungry travellers. Ten fresh fish would save my reputation.", "Ten fish, friend. My cook is threatening to quit."))
simple_quest_npc("miner", "Mind your head. These tunnels are older than the village.", "side/the_miners_plight",
                 "How's the digging?", "I'll dig you a new seam.", ("Cave-in took my best tunnel. If you've a strong arm, dig out twenty coal and ten iron for me.", "Coal and iron, from the deep seams."))
simple_quest_npc("hunter", "Quiet. You'll scare the deer. ...What do you want?", "side/hunters_contract",
                 "Any hunting work?", "I'll clear the spiders.", ("Spiders keep raiding my snares. Kill eight, bring me eight hides, and I'll pay.", "Eight spiders, eight hides."))
simple_quest_npc("priest", "Peace be with you. The temple's doors are open to all who are weary.", "side/alchemists_errand",
                 "Can I help the temple?", "I'll find the reagents.", ("A fever spreads in the lower town. I need spider eyes, sugar and something golden from the fields to brew a remedy.", "The sick are waiting."))
simple_quest_npc("farmer", "Mind the crops! Took me all spring to get that wheat in.", "side/farmers_woes",
                 "Trouble on the farm?", "I'll help with the harvest.", ("Thirty-two sheaves to bring in and zombies trampling the rows at night. I can't do both.", "Harvest's waiting, and so are the zombies."))
simple_quest_npc("merchant", "Finest goods this side of the mountains! Well - the only goods this side of the mountains.", "side/a_merchants_favour",
                 "You seem short of stock.", "I'll recover your goods.", ("Lost a crate to the river. Three emeralds and four gold ingots would make my creditors smile.", "Emeralds and gold, my friend."))

write("dialogues/bard", {"entry": [{"node": "greet"}], "nodes": {
    "greet": node("Ah, an audience! Shall I sing of the Old Kingdom's fall, or of the time a goat ate the Jarl's crown?",
                  opt("The Old Kingdom.", "song"), opt("The goat. Obviously.", "goat")),
    "song": node("'When the towers burned and the mages fell, the deep halls closed like a closing shell...' It goes on. For forty verses.", opt("Lovely.")),
    "goat": node("A classic! The goat, I'll have you know, was later knighted.", opt("Of course it was.", actions=[act("give_xp", "speech", 10)])),
}})
write("dialogues/thief", {"entry": [{"node": "greet"}], "nodes": {
    "greet": node("Keep your voice down. You buying, selling, or just staring?",
                  opt("What do you sell?", "wares"), opt("Teach me your tricks. (Persuade)", "trick", check={"skill": "speech", "level": 15, "fail": "no"})),
    "wares": node("Things that fell off carts. Ask no questions, hear no lies.", opt("I'll take a look.", actions=[act("open_shop")])),
    "trick": node("Fine, fine. Approach from behind, sneak, keep your hands light. Most folk never look back.", opt("Thanks.", actions=[act("give_xp", "pickpocket", 40)])),
    "no": node("Ha. Nice try, mark.", opt("...")),
}})

# --------------------------------------------------------------------------- shops
def sale(item, price, count=1, components=None):
    i = {"id": item}
    if count != 1: i["count"] = count
    if components: i["components"] = components
    return {"item": i, "price": price}

def buys(match, price, display=""):
    b = {"match": match, "price": price}
    if display: b["display"] = display
    return b

TOME = lambda s: {"urmagic:spell": f"urmagic:{s}"}

write("shops/general_goods", {"name": "General Goods", "sells": [
    sale("minecraft:torch", 8, 16), sale("minecraft:bread", 6, 4), sale("minecraft:lead", 10),
    sale("minecraft:bucket", 15), sale("minecraft:compass", 40), sale("minecraft:map", 20), sale("minecraft:white_bed", 25),
    sale("minecraft:lantern", 12), sale("minecraft:paper", 8, 8), sale("urquests:notice_board", 30)],
    "buys": [buys("minecraft:emerald", 12), buys("minecraft:diamond", 60), buys("#c:gems", 15, "minecraft:amethyst_shard"),
             buys("#minecraft:wool", 2, "minecraft:white_wool"), buys("minecraft:leather", 3), buys("minecraft:feather", 1)]})
write("shops/smithy", {"name": "The Smithy", "sells": [
    sale("minecraft:iron_sword", 60), sale("minecraft:iron_axe", 50), sale("minecraft:iron_pickaxe", 55), sale("minecraft:shield", 35),
    sale("minecraft:iron_helmet", 70), sale("minecraft:iron_chestplate", 110), sale("minecraft:iron_leggings", 95), sale("minecraft:iron_boots", 60),
    sale("minecraft:chainmail_chestplate", 90), sale("minecraft:iron_ingot", 9), sale("minecraft:coal", 2, 4)],
    "buys": [buys("minecraft:iron_ingot", 5), buys("minecraft:gold_ingot", 7), buys("minecraft:copper_ingot", 1), buys("minecraft:raw_iron", 3),
             buys("minecraft:coal", 1), buys("minecraft:diamond", 55), buys("#minecraft:swords", 6, "minecraft:iron_sword")]})
write("shops/tavern", {"name": "The Tavern", "sells": [
    sale("minecraft:bread", 5, 3), sale("minecraft:cooked_beef", 8, 2), sale("minecraft:mushroom_stew", 10), sale("minecraft:rabbit_stew", 16),
    sale("minecraft:pumpkin_pie", 9), sale("minecraft:cake", 30), sale("minecraft:honey_bottle", 12), sale("minecraft:cookie", 4, 4)],
    "buys": [buys("#minecraft:fishes", 3, "minecraft:cod"), buys("minecraft:wheat", 1), buys("minecraft:carrot", 1), buys("minecraft:potato", 1),
             buys("minecraft:beef|minecraft:porkchop|minecraft:mutton|minecraft:chicken", 2, "minecraft:beef"), buys("minecraft:honey_bottle", 5)]})
write("shops/arcane_wares", {"name": "Arcane Wares", "sells": [
    sale("urmagic:spell_tome", 80, components=TOME("flames")), sale("urmagic:spell_tome", 90, components=TOME("healing")),
    sale("urmagic:spell_tome", 90, components=TOME("oakflesh")), sale("urmagic:spell_tome", 100, components=TOME("candlelight")),
    sale("urmagic:spell_tome", 120, components=TOME("conjure_familiar")), sale("urmagic:spell_tome", 120, components=TOME("calm")),
    sale("urmagic:spell_tome", 180, components=TOME("frostbite")), sale("urmagic:spell_tome", 220, components=TOME("sparks")),
    sale("urmagic:magicka_potion", 25), sale("minecraft:lapis_lazuli", 4, 4), sale("minecraft:book", 10), sale("minecraft:experience_bottle", 30)],
    "buys": [buys("minecraft:enchanted_book", 40), buys("minecraft:lapis_lazuli", 1), buys("minecraft:amethyst_shard", 3), buys("minecraft:ender_pearl", 12),
             buys("minecraft:blaze_rod", 15), buys("minecraft:ghast_tear", 25), buys("urmagic:spell_tome", 30)]})
write("shops/temple", {"name": "Temple Remedies", "sells": [
    sale("minecraft:potion", 30, components={"minecraft:potion_contents": {"potion": "minecraft:healing"}}),
    sale("minecraft:potion", 40, components={"minecraft:potion_contents": {"potion": "minecraft:regeneration"}}),
    sale("minecraft:golden_apple", 90), sale("minecraft:milk_bucket", 15), sale("urmagic:spell_tome", 100, components=TOME("ward")),
    sale("urmagic:spell_tome", 140, components=TOME("turn_undead"))],
    "buys": [buys("minecraft:spider_eye", 2), buys("minecraft:rotten_flesh", 1), buys("minecraft:bone", 1), buys("minecraft:glistering_melon_slice", 8),
             buys("minecraft:nether_wart", 3)]})
write("shops/farm_stall", {"name": "Farm Stall", "sells": [
    sale("minecraft:wheat_seeds", 2, 8), sale("minecraft:carrot", 3, 4), sale("minecraft:potato", 3, 4), sale("minecraft:beetroot_seeds", 2, 8),
    sale("minecraft:bone_meal", 4, 8), sale("minecraft:egg", 4, 4), sale("minecraft:milk_bucket", 12), sale("minecraft:iron_hoe", 30)],
    "buys": [buys("minecraft:wheat", 1), buys("minecraft:pumpkin", 2), buys("minecraft:melon_slice", 1), buys("#minecraft:wool", 2, "minecraft:white_wool"),
             buys("minecraft:sugar_cane", 1), buys("minecraft:egg", 1)]})
write("shops/mining_supplies", {"name": "Mining Supplies", "sells": [
    sale("minecraft:torch", 7, 16), sale("minecraft:iron_pickaxe", 55), sale("minecraft:iron_shovel", 25), sale("minecraft:ladder", 6, 8),
    sale("minecraft:rail", 10, 8), sale("minecraft:minecart", 20), sale("minecraft:tnt", 25)],
    "buys": [buys("minecraft:coal", 1), buys("minecraft:raw_iron", 3), buys("minecraft:raw_gold", 5), buys("minecraft:raw_copper", 1),
             buys("minecraft:redstone", 1), buys("minecraft:lapis_lazuli", 1), buys("minecraft:diamond", 50), buys("minecraft:emerald", 10),
             buys("minecraft:quartz", 1), buys("minecraft:amethyst_shard", 2)]})
write("shops/hunting_supplies", {"name": "Hunter's Camp", "sells": [
    sale("minecraft:bow", 30), sale("minecraft:arrow", 6, 16), sale("minecraft:crossbow", 45), sale("minecraft:leather_chestplate", 25),
    sale("minecraft:leather_boots", 12), sale("minecraft:cooked_mutton", 7, 2)],
    "buys": [buys("minecraft:leather", 3), buys("minecraft:rabbit_hide", 1), buys("minecraft:feather", 1), buys("minecraft:string", 1),
             buys("minecraft:spider_eye", 2), buys("minecraft:bone", 1), buys("minecraft:rabbit_foot", 8)]})
write("shops/fence", {"name": "The Fence", "sells": [
    sale("minecraft:ender_pearl", 40), sale("minecraft:potion", 60, components={"minecraft:potion_contents": {"potion": "minecraft:invisibility"}}),
    sale("minecraft:leather_boots", 20, components={"minecraft:dyed_color": {"rgb": 2697513}}), sale("minecraft:name_tag", 50),
    sale("urmagic:spell_tome", 150, components=TOME("muffle"))],
    "buys": [buys("minecraft:gold_ingot", 9), buys("minecraft:diamond", 65), buys("minecraft:emerald", 14), buys("#c:gems", 18, "minecraft:emerald"),
             buys("minecraft:golden_apple", 40), buys("minecraft:enchanted_book", 45), buys("minecraft:name_tag", 20)]})

# --------------------------------------------------------------------------- settlements
write("settlements/village", {
    "structures": "#minecraft:village",
    "always": ["urnpcs:village_elder", "urnpcs:guard_captain", "urnpcs:court_wizard", "urnpcs:merchant", "urnpcs:guard", "urnpcs:guard"],
    "random": ["urnpcs:blacksmith", "urnpcs:innkeeper", "urnpcs:farmer", "urnpcs:miner", "urnpcs:hunter", "urnpcs:priest",
               "urnpcs:bard", "urnpcs:thief", "urnpcs:villager", "urnpcs:villager_b"],
    "random_count": 5, "notice_board": True, "radius": 16})

# --------------------------------------------------------------------------- lang
lang = {
    "entity.urnpcs.npc": "Townsperson",
    "dialogue.urnpcs.goodbye": "Goodbye.",
    "dialogue.urnpcs.shop": "Let me see your wares.",
    "dialogue.urnpcs.train": "Train me in %s. (%s Crowns)",
    "dialogue.urnpcs.default_greeting": "Hello there.",
    "dialogue.urnpcs.check": "[%s %s]",
    "dialogue.urnpcs.check_passed": "Success",
    "dialogue.urnpcs.check_failed": "Failed",
    "dialogue.urnpcs.train_maxed": "%s has nothing more to teach you.",
    "dialogue.urnpcs.train_limit": "You've trained five times this level. Level up before training again.",
    "dialogue.urnpcs.train_cost": "Training costs %s Crowns.",
    "message.urnpcs.angry": "%s won't speak to you right now.",
    "message.urnpcs.pickpocket_success": "You lifted %s Crowns.",
    "message.urnpcs.pickpocket_caught": "%s caught you! You pay a fine of %s Crowns.",
    "message.urnpcs.pickpocket_wary": "%s is keeping a close eye on their purse.",
    "message.urnpcs.cannot_afford": "You need %s Crowns.",
    "message.urnpcs.nothing_to_sell": "You have nothing they want.",
    "screen.urnpcs.purse": "Purse: %s",
    "screen.urnpcs.buy": "Buy",
    "screen.urnpcs.sell": "Sell",
    "screen.urnpcs.hint": "Shift-click: 8 / all",
    "command.urnpcs.unknown": "Unknown NPC %s.",
    "command.urnpcs.spawned": "Spawned %s (%s).",
}
os.makedirs(os.path.join(BASE, "assets", "urnpcs", "lang"), exist_ok=True)
json.dump(lang, open(os.path.join(BASE, "assets", "urnpcs", "lang", "en_us.json"), "w"), indent=2)
print("npcs written")
