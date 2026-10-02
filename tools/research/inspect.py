"""
Prints measurements (not images) from the vanilla client jar and from L_Ender's Cataclysm, used to
match our art to them: which way the bow texture faces, the colours of iron ore spots / raw iron /
iron ingot (for runtime recolouring via paletted_permutations), and how Cataclysm builds its items.
"""
import io, json, sys, zipfile
from collections import Counter
from PIL import Image

client, cata = sys.argv[1], sys.argv[2]
zc = zipfile.ZipFile(client)

def img(path):
    return Image.open(io.BytesIO(zc.read(path))).convert("RGBA")

def colors(im):
    return Counter(p for p in im.getdata() if p[3] > 0)

def hexes(cs):
    return [f"{r:02x}{g:02x}{b:02x}" for (r, g, b, a) in cs]

def luma(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]

base = "assets/minecraft/textures/"
print("== bow orientation")
for name in ["item/bow", "item/bow_pulling_2", "item/crossbow_standby"]:
    im = img(base + name + ".png")
    px = im.load()
    wood, light = [], []
    for y in range(im.height):
        for x in range(im.width):
            c = px[x, y]
            if c[3] == 0:
                continue
            (light if luma(c) > 170 else wood).append((x, y))
    cen = lambda pts: (round(sum(p[0] for p in pts) / len(pts), 2), round(sum(p[1] for p in pts) / len(pts), 2)) if pts else None
    print(f"{name}: size={im.size} dark/wood centroid={cen(wood)} light/string centroid={cen(light)} n={len(wood)}/{len(light)}")

print("== recolour keys (colours in the ore not in its host stone, sorted by brightness)")
for ore, host in [("block/iron_ore", "block/stone"), ("block/deepslate_iron_ore", "block/deepslate")]:
    spots = set(colors(img(base + ore + ".png"))) - set(colors(img(base + host + ".png")))
    print(ore, len(spots), hexes(sorted(spots, key=luma)))
for item in ["item/raw_iron", "item/iron_ingot", "item/diamond", "item/emerald", "item/gold_ingot", "block/raw_iron_block"]:
    cs = colors(img(base + item + ".png"))
    print(item, len(cs), hexes(sorted(cs, key=luma)))

print("== vanilla handheld transforms")
for m in ["item/bow", "item/handheld", "item/trident_in_hand", "item/spyglass_in_hand"]:
    try:
        print(m, json.loads(zc.read("assets/minecraft/models/" + m + ".json")).get("display"))
    except KeyError:
        print(m, "missing")

print("== Cataclysm items")
zk = zipfile.ZipFile(cata)
names = zk.namelist()
models = [n for n in names if n.startswith("assets/cataclysm/models/item/") and n.endswith(".json")]
geo = [n for n in names if n.endswith(".geo.json")]
kinds = Counter()
loaders = Counter()
elem_counts = []
for n in models:
    d = json.loads(zk.read(n))
    if "loader" in d:
        loaders[d["loader"]] += 1
    if "elements" in d:
        kinds["3d (elements)"] += 1
        elem_counts.append(len(d["elements"]))
    elif d.get("parent", "").endswith(("generated", "handheld")):
        kinds["flat " + d["parent"]] += 1
    else:
        kinds["parent " + d.get("parent", "?")] += 1
print("item models:", len(models), dict(kinds), "loaders:", dict(loaders))
if elem_counts:
    print("cuboids per 3d model: min", min(elem_counts), "median", sorted(elem_counts)[len(elem_counts) // 2], "max", max(elem_counts))
print("geckolib geo files:", len(geo))
tex = [n for n in names if n.startswith("assets/cataclysm/textures/item/") and n.endswith(".png")]
sizes = Counter()
for n in tex:
    sizes[Image.open(io.BytesIO(zk.read(n))).size] += 1
print("item textures:", len(tex), "sizes:", dict(sizes.most_common(8)))
for n in sorted(models)[:400]:
    d = json.loads(zk.read(n))
    if "elements" in d or "loader" in d:
        tx = d.get("texture_size")
        print("  3d:", n.rsplit("/", 1)[1], "elements", len(d.get("elements", [])), "texture_size", tx, "loader", d.get("loader"),
              "gui-scale", (d.get("display", {}).get("gui") or {}).get("scale"), "hand-scale", (d.get("display", {}).get("firstperson_righthand") or {}).get("scale"))
snd = json.loads(zk.read("assets/cataclysm/sounds.json"))
print("custom sound events:", len(snd))
parts = [n for n in names if n.startswith("assets/cataclysm/particles/")]
print("custom particle types:", len(parts))
