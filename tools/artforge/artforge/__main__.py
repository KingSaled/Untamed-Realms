"""
python3 -m artforge build   -> writes textures/models into the mod resource folders + previews in docs/art
python3 -m artforge check   -> fails if committed assets differ from what the code generates (CI)
"""
import io, os, sys, json
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.dirname(HERE)
REPO = os.path.abspath(os.path.join(TOOL, "..", ".."))
sys.path.insert(0, TOOL)

from artforge.sprite import upscale                   # noqa: E402
from artforge.model3d import blockstate_horizontal    # noqa: E402
from assets import items, spells, npcs, models        # noqa: E402

MODULES = {"urcore": "ur-core", "urskills": "ur-skills", "urclasses": "ur-classes",
           "urquests": "ur-quests", "urnpcs": "ur-npcs", "urmagic": "ur-magic"}


def assets_dir(modid):
    return os.path.join(REPO, "mods", MODULES[modid], "src", "main", "resources", "assets", modid)


def collect():
    """Returns {relative_path: bytes-or-image} for every generated file."""
    files = {}
    for (modid, tex), img in {**items.all_items(), **spells.all_spell_icons(), **npcs.all_skins()}.items():
        files[os.path.join(assets_dir(modid), "textures", tex + ".png")] = img
    for m in models.all_models():
        base = assets_dir(m.namespace)
        for key, img in m.textures.items():
            files[os.path.join(base, "textures", m.folder, f"{m.name}_{key}.png")] = img
        files[os.path.join(base, "models", "block", m.name + ".json")] = m.to_minecraft_json()
        files[os.path.join(base, "models", "item", m.name + ".json")] = {"parent": f"{m.namespace}:block/{m.name}"}
        files[os.path.join(base, "blockstates", m.name + ".json")] = blockstate_horizontal(f"{m.namespace}:block/{m.name}")
        files[os.path.join(TOOL, "out", "bbmodel", m.name + ".bbmodel")] = m.to_bbmodel()
    return files


def encode(obj):
    if isinstance(obj, Image.Image):
        buf = io.BytesIO()
        obj.save(buf, "PNG", optimize=True)
        return buf.getvalue()
    return (json.dumps(obj, indent=2) + "\n").encode()


def previews():
    out = os.path.join(REPO, "docs", "art")
    os.makedirs(out, exist_ok=True)
    sprites = {**items.all_items(), **spells.all_spell_icons()}
    cols = 10
    rows = (len(sprites) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 80, rows * 80), (32, 28, 36, 255))
    for i, img in enumerate(sprites.values()):
        sheet.alpha_composite(upscale(img, 4), ((i % cols) * 80 + 8, (i // cols) * 80 + 8))
    sheet.save(os.path.join(out, "sprites.png"))

    skins = npcs.all_skins()
    sheet = Image.new("RGBA", (len(skins) * 70, 150), (32, 28, 36, 255))
    for i, img in enumerate(skins.values()):
        sheet.alpha_composite(upscale(paper_doll(img), 4), (i * 70 + 3, 8))
    sheet.save(os.path.join(out, "npc_skins.png"))

    for m in models.all_models():
        m.render_iso(scale=14).save(os.path.join(out, f"model_{m.name}.png"))


def paper_doll(skin):
    """Front view of a skin (16x32): head, body, arms, legs, with overlay layers."""
    doll = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    def part(box, dest):
        doll.alpha_composite(skin.crop(box), dest)
    part((8, 8, 16, 16), (4, 0)); part((40, 8, 48, 16), (4, 0))          # head + hat
    part((20, 20, 28, 32), (4, 8)); part((20, 36, 28, 48), (4, 8))      # body + jacket
    part((44, 20, 48, 32), (0, 8)); part((44, 36, 48, 48), (0, 8))      # right arm + sleeve
    part((36, 52, 40, 64), (12, 8)); part((52, 52, 56, 64), (12, 8))    # left arm + sleeve
    part((4, 20, 8, 32), (4, 20)); part((4, 36, 8, 48), (4, 20))        # right leg + pants
    part((20, 52, 24, 64), (8, 20)); part((4, 52, 8, 64), (8, 20))      # left leg + pants
    return doll


def main():
    cmd = sys.argv[1] if len(sys.argv) > 1 else "build"
    files = collect()
    if cmd == "check":
        stale = []
        for path, obj in files.items():
            if "/out/" in path:
                continue
            if not os.path.exists(path):
                stale.append(path); continue
            if isinstance(obj, Image.Image):
                if list(Image.open(path).convert("RGBA").getdata()) != list(obj.convert("RGBA").getdata()):
                    stale.append(path)
            elif json.load(open(path)) != obj:
                stale.append(path)
        if stale:
            print("Generated assets are out of date - run `python3 -m artforge build`:")
            for p in stale: print("  " + os.path.relpath(p, REPO))
            sys.exit(1)
        print(f"{len(files)} generated assets up to date")
        return
    for path, obj in files.items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(encode(obj))
    previews()
    print(f"wrote {len(files)} files + previews in docs/art/")


if __name__ == "__main__":
    main()
