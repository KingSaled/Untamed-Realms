"""Untamed Realms UI theme: dark wood frames with gold trim around parchment panels.

Replaces vanilla GUI textures (from ur-core, so every player gets them). Slot positions match vanilla
exactly - only the look changes. Parchment stays light so vanilla's dark labels remain readable.
"""
import random
from PIL import Image

from artforge import palette as P

OUT = P.OUTLINE
WOOD = P.WOOD_DARK                  # 0 darkest .. 4 lightest
PARCH = P.PARCHMENT
GOLD_TRIM = P.hex_rgba("8c7853")
GOLD_BRIGHT = P.hex_rgba("e8c872")
GOLD_DEEP = P.hex_rgba("6b5530")
WELL = P.hex_rgba("1a120c")
CLEAR = P.TRANSPARENT

SLOT_FILL = P.hex_rgba("9a8258")
SLOT_DARK = P.hex_rgba("5a4630")
SLOT_LIGHT = P.hex_rgba("efe2bd")


def rect(img, x0, y0, x1, y1, color):
    """Filled rectangle, inclusive corners."""
    for y in range(max(0, y0), min(img.height, y1 + 1)):
        for x in range(max(0, x0), min(img.width, x1 + 1)):
            img.putpixel((x, y), color)


def frame(img, x0, y0, x1, y1, color):
    rect(img, x0, y0, x1, y0, color)
    rect(img, x0, y1, x1, y1, color)
    rect(img, x0, y0, x0, y1, color)
    rect(img, x1, y0, x1, y1, color)


def wood(img, x0, y0, x1, y1, seed, light=0):
    """Dark wood with horizontal grain."""
    rnd = random.Random(seed)
    for y in range(y0, y1 + 1):
        band = rnd.random()
        for x in range(x0, x1 + 1):
            r = rnd.random()
            shade = 1 + light if band < 0.75 else 2 + light
            if r < 0.06:
                shade -= 1
            elif r > 0.97:
                shade += 1
            img.putpixel((x, y), WOOD[max(0, min(4, shade))])


def parchment(img, x0, y0, x1, y1, seed):
    rnd = random.Random(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            r = rnd.random()
            img.putpixel((x, y), PARCH[3] if r < 0.95 else PARCH[2] if r < 0.99 else PARCH[4])


def slot(img, x, y, size=18):
    """Inset slot whose top-left corner is (x, y) - i.e. slot position minus one."""
    rect(img, x, y, x + size - 1, y + size - 1, SLOT_FILL)
    rect(img, x, y, x + size - 2, y, SLOT_DARK)
    rect(img, x, y, x, y + size - 2, SLOT_DARK)
    rect(img, x + 1, y + size - 1, x + size - 1, y + size - 1, SLOT_LIGHT)
    rect(img, x + size - 1, y + 1, x + size - 1, y + size - 1, SLOT_LIGHT)


def big_slot(img, x, y):
    """The 26x26 result slot frame."""
    rect(img, x, y, x + 25, y + 25, GOLD_DEEP)
    slot(img, x + 1, y + 1, 24)


def panel(img, x0, y0, x1, y1, seed, top=True, bottom=True):
    """Parchment page in a 4px wood frame with a gold inner line. top/bottom=False leaves that edge open."""
    wood(img, x0, y0, x1, y1, seed)
    iy0 = y0 + 4 if top else y0
    iy1 = y1 - 4 if bottom else y1
    parchment(img, x0 + 4, iy0, x1 - 4, iy1, seed + 1)
    # gold line just inside the wood
    rect(img, x0 + 3, iy0, x0 + 3, iy1, GOLD_TRIM)
    rect(img, x1 - 3, iy0, x1 - 3, iy1, GOLD_TRIM)
    if top:
        rect(img, x0 + 3, y0 + 3, x1 - 3, y0 + 3, GOLD_TRIM)
    if bottom:
        rect(img, x0 + 3, y1 - 3, x1 - 3, y1 - 3, GOLD_TRIM)
    # dark outline, with clipped corners like vanilla's rounded panel
    rect(img, x0, y0, x0, y1, OUT)
    rect(img, x1, y0, x1, y1, OUT)
    if top:
        rect(img, x0, y0, x1, y0, OUT)
        img.putpixel((x0, y0), CLEAR)
        img.putpixel((x1, y0), CLEAR)
    if bottom:
        rect(img, x0, y1, x1, y1, OUT)
        img.putpixel((x0, y1), CLEAR)
        img.putpixel((x1, y1), CLEAR)


def arrow(img, x, y, length=22):
    """Crafting arrow pointing right, {length}x15, in dark ink."""
    ink = P.hex_rgba("5a4630")
    shaft = length - 8
    rect(img, x, y + 5, x + shaft - 1, y + 9, ink)
    for i in range(8):
        rect(img, x + shaft + i, y + i, x + shaft + i, y + 14 - i, ink)


def player_inventory(img, y):
    """Main inventory (3 rows) + hotbar with frames starting at row y (slot y - 1)."""
    for r in range(3):
        for c in range(9):
            slot(img, 7 + c * 18, y + r * 18)
    for c in range(9):
        slot(img, 7 + c * 18, y + 58)


# ---------------------------------------------------------------------------------- containers

def inventory():
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 175, 165, 11)
    for i in range(4):
        slot(img, 7, 7 + i * 18)                      # armor
    slot(img, 76, 61)                                 # offhand
    rect(img, 25, 7, 75, 77, OUT)                     # player preview
    rect(img, 26, 8, 74, 76, WELL)
    for r in range(2):
        for c in range(2):
            slot(img, 97 + c * 18, 17 + r * 18)       # 2x2 crafting
    arrow(img, 135, 29, 15)
    rect(img, 152, 26, 171, 45, GOLD_DEEP)            # result (slot at 154, 28): narrow gold rim
    slot(img, 153, 27)
    player_inventory(img, 83)
    return img


def generic_54():
    """Chest texture: top part (6 rows) at y 0..125 and the player part at 126..221, drawn separately."""
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 175, 125, 21, bottom=False)
    for r in range(6):
        for c in range(9):
            slot(img, 7 + c * 18, 17 + r * 18)
    panel(img, 0, 126, 175, 221, 22, top=False)
    player_inventory(img, 126 + 13)
    return img


def crafting_table():
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 175, 165, 31)
    for r in range(3):
        for c in range(3):
            slot(img, 29 + c * 18, 16 + r * 18)
    arrow(img, 89, 34)
    big_slot(img, 119, 30)                            # result (slot at 124, 35)
    player_inventory(img, 83)
    return img


# ---------------------------------------------------------------------------------- HUD sprites

def hotbar_well(img, x, y):
    """18x18 dark slot well for the hotbar."""
    rect(img, x, y, x + 17, y + 17, WELL)
    rect(img, x, y, x + 17, y, WOOD[0])
    rect(img, x, y, x, y + 17, WOOD[0])
    rect(img, x + 1, y + 17, x + 17, y + 17, WOOD[3])
    rect(img, x + 17, y + 1, x + 17, y + 17, WOOD[3])


def hotbar():
    img = Image.new("RGBA", (182, 22), CLEAR)
    wood(img, 0, 0, 181, 21, 41)
    frame(img, 0, 0, 181, 21, OUT)
    frame(img, 1, 1, 180, 20, GOLD_TRIM)
    for i in range(9):
        hotbar_well(img, 2 + i * 20, 2)
    return img


def hotbar_selection():
    img = Image.new("RGBA", (24, 23), CLEAR)
    frame(img, 0, 0, 23, 23, OUT)
    frame(img, 1, 1, 22, 22, GOLD_BRIGHT)
    frame(img, 2, 2, 21, 21, GOLD_DEEP)
    return img.crop((0, 0, 24, 23))


def hotbar_offhand(left):
    img = Image.new("RGBA", (29, 24), CLEAR)
    x0 = 0 if left else 7
    wood(img, x0, 1, x0 + 21, 22, 51)
    frame(img, x0, 1, x0 + 21, 22, OUT)
    frame(img, x0 + 1, 2, x0 + 20, 21, GOLD_TRIM)
    hotbar_well(img, x0 + 2, 3)
    return img


# ---------------------------------------------------------------------------------- buttons

def button(state):
    """200x20 nine-slice button: wood face, gold trim (bright when hovered), grey when disabled."""
    img = Image.new("RGBA", (200, 20), CLEAR)
    if state == "disabled":
        rect(img, 0, 0, 199, 19, P.hex_rgba("2c2a2a"))
        frame(img, 0, 0, 199, 19, OUT)
        frame(img, 1, 1, 198, 18, P.hex_rgba("4a4646"))
        return img
    wood(img, 0, 0, 199, 19, 61, light=1 if state == "highlighted" else 0)
    frame(img, 0, 0, 199, 19, OUT)
    frame(img, 1, 1, 198, 18, GOLD_BRIGHT if state == "highlighted" else GOLD_TRIM)
    rect(img, 2, 17, 197, 17, WOOD[0])                # bottom shadow inside the trim
    return img


BUTTON_META = {"gui": {"scaling": {"type": "nine_slice", "width": 200, "height": 20,
                                   "border": {"left": 3, "top": 3, "right": 3, "bottom": 3}}}}


def all_ui():
    """{path under assets/minecraft/textures/gui/: image or mcmeta dict}."""
    files = {
        "container/inventory.png": inventory(),
        "container/generic_54.png": generic_54(),
        "container/crafting_table.png": crafting_table(),
        "sprites/hud/hotbar.png": hotbar(),
        "sprites/hud/hotbar_selection.png": hotbar_selection(),
        "sprites/hud/hotbar_offhand_left.png": hotbar_offhand(True),
        "sprites/hud/hotbar_offhand_right.png": hotbar_offhand(False),
    }
    for state, name in (("normal", "button"), ("highlighted", "button_highlighted"), ("disabled", "button_disabled")):
        files[f"sprites/widget/{name}.png"] = button(state)
        files[f"sprites/widget/{name}.png.mcmeta"] = BUTTON_META
    return files
