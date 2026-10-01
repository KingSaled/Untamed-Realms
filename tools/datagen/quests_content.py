#!/usr/bin/env python3
"""Default quest content for ur-quests: main story chapter 1, side quests and radiant bounties."""
import json, os

BASE = os.path.join(os.path.dirname(__file__), "..", "..", "mods", "ur-quests", "src", "main", "resources")
QDIR = os.path.join(BASE, "data", "urquests", "urquests", "quests")

def write(path, obj):
    full = os.path.join(QDIR, path + ".json")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    json.dump(obj, open(full, "w"), indent=2)

def obj(type_, target="", count=1, text=None, consume=False):
    o = {"type": type_}
    if target: o["target"] = target
    if count != 1: o["count"] = count
    if consume: o["consume"] = True
    if text: o["text"] = text
    return o

def stage(desc, *objectives):
    return {"description": desc, "objectives": list(objectives)}

def quest(title, desc, stages, category="side", giver=None, rewards=None, requires=None, **extra):
    q = {"title": title, "description": desc, "category": category, "stages": stages}
    if giver: q["giver"] = giver
    if rewards: q["rewards"] = rewards
    if requires: q["requirements"] = requires
    q.update(extra)
    return q

NPC = lambda n: f"urnpcs:{n}"
VILLAGE = "#minecraft:village"

# ---------------------------------------------------------------- main story, chapter 1: "Embers of the Old Kingdom"
write("main/a_new_beginning", quest(
    "A New Beginning",
    "You wake on the road with a little coin and less memory. Every traveller in these lands says the same thing: find a town, and the Elder will know what to do with you.",
    [stage("Find a settlement.", obj("visit", VILLAGE, text="Find a village or town")),
     stage("Speak with the Village Elder.", obj("talk", NPC("village_elder"), text="Speak with the Village Elder"))],
    category="main", abandonable=False,
    rewards={"coins": 50, "xp": {"speech": 60}}))

write("main/the_elders_request", quest(
    "The Elder's Request",
    "The Elder will vouch for a stranger who proves useful. Winter stores are thin - timber and bread would go a long way.",
    [stage("Gather supplies for the village.",
           obj("collect", "#minecraft:logs", 16, "Gather logs", consume=True),
           obj("collect", "minecraft:bread", 6, "Bring bread", consume=True),
           obj("turn_in", NPC("village_elder"), text="Return to the Elder"))],
    category="main", giver=NPC("village_elder"), abandonable=False,
    requires={"quests": ["urquests:main/a_new_beginning"]},
    rewards={"coins": 75, "xp": {"woodcutting": 150, "speech": 80}}))

write("main/trouble_on_the_roads", quest(
    "Trouble on the Roads",
    "Monsters are prowling the roads, the caves and the woods in numbers no one remembers. The Guard Captain wants them thinned out before the next caravan.",
    [stage("Speak with the Guard Captain.", obj("talk", NPC("guard_captain"), text="Speak with the Guard Captain")),
     stage("Clear the lands around the village of monsters.",
           obj("kill", "@monster", 12, "Slay hostile creatures"),
           obj("turn_in", NPC("guard_captain"), text="Report to the Guard Captain"))],
    category="main", giver=NPC("village_elder"), abandonable=False,
    requires={"quests": ["urquests:main/the_elders_request"]},
    rewards={"coins": 120, "xp": {"one_handed": 300, "archery": 150, "block": 150}}))

write("main/the_raiders_banner", quest(
    "The Raider's Banner",
    "The undead were driven toward the roads by something. Survivors speak of raiders under a grey banner camped in the wilds, digging for something old.",
    [stage("Find the raiders' outpost.", obj("visit", "minecraft:pillager_outpost", text="Find a pillager outpost")),
     stage("Break the raiders.",
           obj("kill", "#minecraft:raiders", 8, "Defeat raiders"),
           obj("turn_in", NPC("village_elder"), text="Return to the Elder"))],
    category="main", giver=NPC("guard_captain"), abandonable=False,
    requires={"quests": ["urquests:main/trouble_on_the_roads"]},
    rewards={"coins": 200, "perk_points": 1, "xp": {"one_handed": 250, "two_handed": 250, "archery": 250}}))

write("main/into_the_deep", quest(
    "Into the Deep",
    "Among the raiders' plunder was a copper key stamped with a sigil the Court Wizard recognised at once - the Old Kingdom's trial halls lie beneath the earth.",
    [stage("Show the key to the Court Wizard.", obj("talk", NPC("court_wizard"), text="Speak with the Court Wizard")),
     stage("Find the Trial Chambers.", obj("visit", "minecraft:trial_chambers", text="Find the Trial Chambers")),
     stage("Claim a vault's reward.",
           obj("collect", "minecraft:trial_key|minecraft:ominous_trial_key|minecraft:heavy_core|minecraft:breeze_rod", 1, "Recover a relic from the Trial Chambers"),
           obj("turn_in", NPC("court_wizard"), text="Return to the Court Wizard"))],
    category="main", giver=NPC("court_wizard"), abandonable=False,
    requires={"quests": ["urquests:main/the_raiders_banner"]},
    rewards={"coins": 300, "perk_points": 1, "xp": {"destruction": 300, "alteration": 300},
             "items": [{"id": "urmagic:spell_tome", "components": {"urmagic:spell": "urmagic:thunderbolt"}}]}))

# ---------------------------------------------------------------- side quests
write("side/the_smiths_apprentice", quest(
    "The Smith's Apprentice",
    "The blacksmith's forge is cold for want of iron. Bring ingots and you might learn a thing or two at the anvil.",
    [stage("Bring iron to the blacksmith.",
           obj("collect", "minecraft:iron_ingot", 12, "Bring iron ingots", consume=True),
           obj("turn_in", NPC("blacksmith"), text="Deliver to the Blacksmith"))],
    giver=NPC("blacksmith"), rewards={"coins": 60, "xp": {"smithing": 400}}))

write("side/fresh_catch", quest(
    "Fresh Catch",
    "The innkeeper's stew pot is empty and the travellers are grumbling. Fresh fish, quickly!",
    [stage("Catch fish for the inn.",
           obj("collect", "#minecraft:fishes", 10, "Bring fish", consume=True),
           obj("turn_in", NPC("innkeeper"), text="Deliver to the Innkeeper"))],
    giver=NPC("innkeeper"), rewards={"coins": 45, "xp": {"fishing": 250, "cooking": 120}}))

write("side/the_miners_plight", quest(
    "The Miner's Plight",
    "A cave-in sealed the old miner's best tunnels. They'll pay well for someone with a strong arm to dig out a fresh seam.",
    [stage("Mine coal and iron.",
           obj("mine", "#minecraft:coal_ores", 20, "Mine coal ore"),
           obj("mine", "#minecraft:iron_ores", 10, "Mine iron ore"),
           obj("turn_in", NPC("miner"), text="Report to the Miner"))],
    giver=NPC("miner"), rewards={"coins": 80, "xp": {"mining": 500}}))

write("side/hunters_contract", quest(
    "The Hunter's Contract",
    "Spiders have been taking the hunter's snares - and the occasional dog. Clear them out and bring hides for the tanner.",
    [stage("Hunt spiders and gather hides.",
           obj("kill", "minecraft:spider|minecraft:cave_spider", 8, "Slay spiders"),
           obj("collect", "minecraft:leather", 8, "Gather leather", consume=True),
           obj("turn_in", NPC("hunter"), text="Return to the Hunter"))],
    giver=NPC("hunter"), rewards={"coins": 70, "xp": {"archery": 250, "sneak": 150}}))

write("side/alchemists_errand", quest(
    "An Alchemist's Errand",
    "The temple priest is brewing remedies for a fever in the lower town and needs reagents.",
    [stage("Gather reagents.",
           obj("collect", "minecraft:spider_eye", 4, "Gather spider eyes", consume=True),
           obj("collect", "minecraft:sugar", 4, "Gather sugar", consume=True),
           obj("collect", "minecraft:glistering_melon_slice|minecraft:golden_carrot", 1, "A glistering melon or golden carrot", consume=True),
           obj("turn_in", NPC("priest"), text="Bring them to the Priest"))],
    giver=NPC("priest"), rewards={"coins": 90, "xp": {"alchemy": 400, "restoration": 150},
                                  "items": [{"id": "minecraft:potion", "count": 2, "components": {"minecraft:potion_contents": {"potion": "minecraft:healing"}}}]}))

write("side/tome_of_secrets", quest(
    "A Tome of Secrets",
    "The Court Wizard collects enchanted books 'for safekeeping'. Find one and you'll be rewarded with real knowledge.",
    [stage("Find an enchanted book.",
           obj("collect", "minecraft:enchanted_book", 1, "Find an enchanted book", consume=True),
           obj("turn_in", NPC("court_wizard"), text="Bring it to the Court Wizard"))],
    giver=NPC("court_wizard"),
    rewards={"coins": 50, "xp": {"enchanting": 300},
             "items": [{"id": "urmagic:spell_tome", "components": {"urmagic:spell": "urmagic:detect_life"}}]}))

write("side/farmers_woes", quest(
    "A Farmer's Woes",
    "Crows, blight and now zombies trampling the wheat. The farmer needs a harvest brought in and the fields made safe.",
    [stage("Bring in the harvest and protect the fields.",
           obj("collect", "minecraft:wheat", 32, "Harvest wheat", consume=True),
           obj("kill", "minecraft:zombie|minecraft:husk", 5, "Drive off the zombies"),
           obj("turn_in", NPC("farmer"), text="Return to the Farmer"))],
    giver=NPC("farmer"), rewards={"coins": 60, "xp": {"farming": 400}}))

write("side/a_merchants_favour", quest(
    "A Merchant's Favour",
    "The merchant's caravan lost a crate of trade goods to the river. Emeralds and gold will make it right.",
    [stage("Recover the merchant's goods.",
           obj("collect", "minecraft:emerald", 3, "Bring emeralds", consume=True),
           obj("collect", "minecraft:gold_ingot", 4, "Bring gold ingots", consume=True),
           obj("turn_in", NPC("merchant"), text="Return to the Merchant"))],
    giver=NPC("merchant"), rewards={"coins": 160, "xp": {"speech": 300}}))

write("side/the_lost_explorer", quest(
    "The Lost Explorer",
    "A cartographer went looking for the sunken temples of the south and never came back. Their last letter mentions sand and a pyramid.",
    [stage("Search the desert for an ancient temple.",
           obj("visit", "minecraft:desert_pyramid|#minecraft:desert_pyramid|betterdeserttemples:desert_temple", text="Find a desert temple")),
     stage("Report what you found.", obj("talk", NPC("court_wizard"), text="Report to the Court Wizard"))],
    giver=NPC("court_wizard"), rewards={"coins": 120, "xp": {"agility": 300, "lockpicking": 200}}))

# ---------------------------------------------------------------- radiant bounties (notice boards)
def bounty(id_, title, desc, objectives, coins, xp):
    write(f"bounty/{id_}", quest(title, desc, [stage(desc, *objectives)], category="bounty",
                                  repeatable=True, cooldown_minutes=20, pool="bounty",
                                  rewards={"coins": coins, "xp": xp}))

bounty("undead_menace", "Bounty: Undead Menace", "The restless dead plague the outskirts.", [obj("kill", "#minecraft:undead", 12, "Slay the undead")], 70, {"one_handed": 200})
bounty("spider_nest", "Bounty: Spider Nest", "Spiders are nesting near the roads.", [obj("kill", "minecraft:spider|minecraft:cave_spider", 8, "Slay spiders")], 55, {"archery": 150, "one_handed": 100})
bounty("creeper_cull", "Bounty: Creeper Cull", "Something keeps blowing holes in the town wall.", [obj("kill", "minecraft:creeper", 6, "Slay creepers")], 80, {"archery": 200})
bounty("bone_collector", "Bounty: Bone Collector", "Skeleton archers harry the farms.", [obj("kill", "minecraft:skeleton|minecraft:stray|minecraft:bogged", 10, "Slay skeletons")], 65, {"block": 150, "one_handed": 100})
bounty("raider_scouts", "Bounty: Raider Scouts", "Raider scouts have been seen watching the town.", [obj("kill", "#minecraft:raiders", 5, "Defeat raiders")], 120, {"one_handed": 200, "archery": 100})
bounty("drowned_shore", "Bounty: Drowned Shore", "Drowned crawl from the water at night.", [obj("kill", "minecraft:drowned", 6, "Slay drowned")], 70, {"one_handed": 150, "agility": 80})
bounty("witch_hunt", "Bounty: Witch Hunt", "A witch is cursing the livestock.", [obj("kill", "minecraft:witch", 1, "Slay a witch")], 90, {"destruction": 150, "archery": 150})
bounty("endless_eyes", "Bounty: Endless Eyes", "The Court Wizard wants pearls from the tall ones.", [obj("deliver", "minecraft:ender_pearl", 3, "Hand in ender pearls at a notice board")], 130, {"sneak": 200})
bounty("timber_order", "Supply Order: Timber", "The carpenter needs timber for repairs.", [obj("deliver", "#minecraft:logs", 32, "Deliver logs at a notice board")], 45, {"woodcutting": 250})
bounty("ore_order", "Supply Order: Raw Iron", "The smithy is short of ore.", [obj("deliver", "minecraft:raw_iron", 16, "Deliver raw iron at a notice board")], 70, {"mining": 300})
bounty("provisions", "Supply Order: Provisions", "The garrison needs cooked meat.", [obj("deliver", "minecraft:cooked_beef|minecraft:cooked_porkchop|minecraft:cooked_mutton|minecraft:cooked_chicken", 16, "Deliver cooked meat at a notice board")], 60, {"cooking": 250})
bounty("wool_order", "Supply Order: Wool", "Winter is coming and the weavers need wool.", [obj("deliver", "#minecraft:wool", 16, "Deliver wool at a notice board")], 40, {"farming": 200})
bounty("fishmonger", "Supply Order: Fish", "The fishmonger's stall is bare.", [obj("deliver", "#minecraft:fishes", 12, "Deliver fish at a notice board")], 50, {"fishing": 250})

# ---------------------------------------------------------------- lang, block loot, recipe
lang = {
    "block.urquests.notice_board": "Notice Board",
    "item.urquests.quest_scroll": "Quest Scroll",
    "item.urquests.bounty_notice": "Bounty Notice",
    "key.urquests.journal": "Quest Journal",
    "key.urquests.toggle_tracker": "Toggle Quest Tracker",
    "screen.urquests.journal": "Journal",
    "screen.urquests.tab.active": "Active",
    "screen.urquests.tab.completed": "Completed",
    "screen.urquests.track": "Track",
    "screen.urquests.untrack": "Untrack",
    "screen.urquests.abandon": "Abandon",
    "screen.urquests.none_active": "No active quests",
    "screen.urquests.none_completed": "Nothing completed yet",
    "screen.urquests.rewards": "Rewards",
    "screen.urquests.reward_coins": "%s Crowns",
    "screen.urquests.reward_xp": "%s %s XP",
    "screen.urquests.reward_items": "%s item(s)",
    "screen.urquests.board": "Notice Board",
    "screen.urquests.board_empty": "No work posted today. Check back tomorrow.",
    "screen.urquests.accept": "Accept",
    "quest_category.urquests.main": "Main Quest",
    "quest_category.urquests.side": "Side Quest",
    "quest_category.urquests.bounty": "Bounty",
    "quest_category.urquests.misc": "Miscellaneous",
    "quest_category.urquests.guild": "Guild",
    "banner.urquests.started": "Quest Started: %s",
    "banner.urquests.updated": "Quest Updated: %s",
    "banner.urquests.completed": "Quest Completed: %s",
    "banner.urquests.reward_coins": "+%s Crowns",
    "message.urquests.unknown": "Unknown quest.",
    "message.urquests.already_active": "You are already on that quest.",
    "message.urquests.already_done": "You have already completed that quest.",
    "message.urquests.cooldown": "That job isn't available again yet.",
    "message.urquests.requires_quest": "You must first complete \"%s\".",
    "message.urquests.requires_skill": "Requires %s %s.",
    "message.urquests.requires_level": "Requires character level %s.",
    "message.urquests.abandoned": "Quest abandoned: %s",
    "message.urquests.objective_done": "Objective complete: %s",
    "objective.urquests.kill": "Slay %s",
    "objective.urquests.collect": "Collect %s",
    "objective.urquests.talk": "Speak with %s",
    "objective.urquests.turn_in": "Return to %s",
    "objective.urquests.visit": "Find %s",
    "objective.urquests.biome": "Travel to %s",
    "objective.urquests.mine": "Mine %s",
    "objective.urquests.craft": "Craft %s",
    "objective.urquests.smelt": "Smelt %s",
    "objective.urquests.fish": "Catch %s",
    "objective.urquests.skill": "Reach %s level %2$s",
    "objective.urquests.level": "Reach character level %2$s",
    "objective.urquests.cast": "Cast %s",
    "objective.urquests.anything": "anything",
    "command.urquests.started": "Started %s for %s.",
    "command.urquests.reset": "Reset all quests for %s.",
    "command.urquests.skipped": "Skipped the current step of %s.",
    "command.urquests.nothing_to_skip": "No active quest to skip (track one in the journal, or name it).",
    "objective.urquests.deliver": "Deliver %s",
    "screen.urquests.deliver": "Hand In",
    "screen.urquests.hand_in": "Hand in...",
    "screen.urquests.deliver_none": "Nothing to hand in here.",
    "screen.urquests.deliver_pick": "Your matching items:",
    "screen.urquests.deliver_nothing": "You have none of these with you.",
    "screen.urquests.deliver_hint": "Left-click: whole stack  Right-click: one",
    "message.urquests.arrived": "You arrive at a settlement. Find the Village Elder.",
    "objective.urquests.named": "%s (%s)",
}
os.makedirs(os.path.join(BASE, "assets", "urquests", "lang"), exist_ok=True)
json.dump(lang, open(os.path.join(BASE, "assets", "urquests", "lang", "en_us.json"), "w"), indent=2)

for name in ("quest_scroll", "bounty_notice"):
    p = os.path.join(BASE, "assets", "urquests", "models", "item", name + ".json")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"urquests:item/{name}"}}, open(p, "w"), indent=2)

loot = os.path.join(BASE, "data", "urquests", "loot_table", "blocks", "notice_board.json")
os.makedirs(os.path.dirname(loot), exist_ok=True)
json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "urquests:notice_board"}],
                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]}, open(loot, "w"), indent=2)
recipe = os.path.join(BASE, "data", "urquests", "recipe", "notice_board.json")
os.makedirs(os.path.dirname(recipe), exist_ok=True)
json.dump({"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SSS", "PPP", "S S"],
           "key": {"S": {"item": "minecraft:stick"}, "P": {"item": "minecraft:paper"}},
           "result": {"id": "urquests:notice_board", "count": 1}}, open(recipe, "w"), indent=2)
tag = os.path.join(BASE, "data", "minecraft", "tags", "block", "mineable", "axe.json")
os.makedirs(os.path.dirname(tag), exist_ok=True)
json.dump({"replace": False, "values": ["urquests:notice_board"]}, open(tag, "w"), indent=2)
print("quests written")
