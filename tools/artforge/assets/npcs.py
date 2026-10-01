"""NPC skins. Keys are referenced from urnpcs NPC definitions as urnpcs:textures/entity/npc/<name>.png."""
from artforge import palette as P
from artforge.skins import Outfit, generate

OUTFITS = {
    "blacksmith": Outfit(skin=1, hair="black", beard=True, shirt=P.CLOTH_GREY, pants=P.CLOTH_BROWN, overlay="apron", overlay_ramp=P.LEATHER, seed=1),
    "innkeeper": Outfit(skin=3, hair="red", hair_style="tied", shirt=P.CLOTH_WHITE, pants=P.CLOTH_BROWN, overlay="vest", overlay_ramp=P.CLOTH_GREEN, seed=2),
    "merchant": Outfit(skin=0, hair="brown", beard=True, shirt=P.CLOTH_PURPLE, pants=P.CLOTH_BLACK, overlay="vest", overlay_ramp=P.CLOTH_RED, trim=P.GOLD, seed=3),
    "guard": Outfit(skin=0, hair="brown", shirt=P.CLOTH_RED, pants=P.CLOTH_GREY, boots=P.IRON, overlay="armor", overlay_ramp=P.STEEL, trim=P.GOLD, seed=4),
    "guard_captain": Outfit(skin=2, hair="grey", beard=True, shirt=P.CLOTH_BLUE, pants=P.CLOTH_GREY, boots=P.IRON, overlay="armor", overlay_ramp=P.SILVER, trim=P.GOLD, seed=5),
    "mage": Outfit(skin=3, hair="white", hair_style="long", beard=True, shirt=P.CLOTH_BLUE, pants=P.CLOTH_BLUE, overlay="robe", overlay_ramp=P.CLOTH_BLUE, trim=P.GOLD, seed=6),
    "court_wizard": Outfit(skin=1, hair="black", hair_style="hood", shirt=P.CLOTH_PURPLE, pants=P.CLOTH_PURPLE, overlay="robe", overlay_ramp=P.CLOTH_PURPLE, trim=P.SILVER, seed=7),
    "farmer": Outfit(skin=0, hair="blond", shirt=P.CLOTH_GREEN, pants=P.CLOTH_BROWN, overlay="", seed=8),
    "hunter": Outfit(skin=2, hair="brown", hair_style="short", beard=True, shirt=P.CLOTH_GREEN, pants=P.LEATHER, overlay="cloak", overlay_ramp=P.CLOTH_BROWN, seed=9),
    "thief": Outfit(skin=1, hair="black", hair_style="hood", shirt=P.CLOTH_BLACK, pants=P.CLOTH_BLACK, overlay="hood", overlay_ramp=P.CLOTH_GREY, seed=10),
    "priest": Outfit(skin=3, hair="grey", hair_style="bald", shirt=P.CLOTH_WHITE, pants=P.CLOTH_WHITE, overlay="robe", overlay_ramp=P.CLOTH_WHITE, trim=P.GOLD, seed=11),
    "elder": Outfit(skin=0, hair="white", beard=True, shirt=P.CLOTH_BROWN, pants=P.CLOTH_BROWN, overlay="robe", overlay_ramp=P.CLOTH_BROWN, trim=P.CLOTH_GREEN, seed=12),
    "jarl": Outfit(skin=0, hair="blond", hair_style="long", beard=True, shirt=P.CLOTH_RED, pants=P.CLOTH_BLACK, overlay="cloak", overlay_ramp=P.CLOTH_BLUE, trim=P.GOLD, seed=13),
    "bard": Outfit(skin=3, hair="red", hair_style="long", shirt=P.CLOTH_RED, pants=P.CLOTH_GREEN, overlay="vest", overlay_ramp=P.GOLD, seed=14),
    "miner": Outfit(skin=2, hair="black", beard=True, shirt=P.CLOTH_BROWN, pants=P.CLOTH_GREY, overlay="apron", overlay_ramp=P.CLOTH_GREY, seed=15),
    "villager_a": Outfit(skin=1, hair="brown", hair_style="tied", shirt=P.CLOTH_BLUE, pants=P.CLOTH_BROWN, seed=16),
    "villager_b": Outfit(skin=3, hair="blond", hair_style="long", shirt=P.CLOTH_RED, pants=P.CLOTH_GREY, seed=17),
    "villager_c": Outfit(skin=2, hair="grey", beard=True, shirt=P.CLOTH_GREEN, pants=P.CLOTH_BROWN, overlay="vest", overlay_ramp=P.LEATHER, seed=18),
}


def all_skins():
    return {("urnpcs", f"entity/npc/{name}"): generate(o) for name, o in OUTFITS.items()}
