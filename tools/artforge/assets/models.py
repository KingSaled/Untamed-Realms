"""3D block models."""
from artforge import palette as P
from artforge import procedural as G
from artforge.model3d import Model
from artforge.sprite import canvas


def notice_board():
    wood = G.planks(P.WOOD_DARK, seed=5, boards=4)
    planks = G.planks(P.WOOD, seed=9, boards=4, vertical=False)
    paper = G.parchment(16, seed=21)
    m = Model("notice_board", {"post": wood, "board": planks, "paper": paper}, namespace="urquests")
    # two posts, a backboard, a little roof and pinned notices (model faces north)
    m.cube((0, 0, 7), (2, 16, 9), "post", name="post_left")
    m.cube((14, 0, 7), (16, 16, 9), "post", name="post_right")
    m.cube((2, 5, 7.5), (14, 15, 8.5), "board", name="board")
    m.cube((-1, 15, 5), (17, 16.5, 11), "post", name="roof")
    m.cube((3, 9, 7.25), (7, 14, 7.5), "paper", faces=("north", "south", "east", "west", "up", "down"), name="notice_1")
    m.cube((8, 10, 7.25), (13, 14, 7.5), "paper", faces=("north", "south", "east", "west", "up", "down"), name="notice_2")
    m.cube((4, 6, 7.25), (9, 8.5, 7.5), "paper", faces=("north", "south", "east", "west", "up", "down"), name="notice_3")
    m.cube((10, 6, 7.25), (13, 9, 7.5), "paper", faces=("north", "south", "east", "west", "up", "down"), name="notice_4")
    return m


def all_models():
    return [notice_board()]
