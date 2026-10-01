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
from assets import items, spells, npcs, models, ui, arsenal    # noqa: E402

MODULES = {"urcore": "ur-core", "urskills": "ur-skills", "urclasses": "ur-classes",
           "urquests": "ur-quests", "urnpcs": "ur-npcs", "urmagic": "ur-magic", "urarsenal": "ur-arsenal"}


def assets_dir(modid):
    return os.path.join(REPO, "mods", MODULES[modid], "src", "main", "resources", "assets", modid)


def collect():
    """Returns {relative_path: bytes-or-image} for every generated file."""
    files = {}
    for (modid, tex), img in {**items.all_items(), **spells.all_spell_icons(), **npcs.all_skins()}.items():
        files[os.path.join(assets_dir(modid), "textures", tex + ".png")] = img
    for m in models.all_models() + arsenal.stations():
        base = assets_dir(m.namespace)
        for key, img in m.textures.items():
            files[os.path.join(base, "textures", m.folder, f"{m.name}_{key}.png")] = img
        files[os.path.join(base, "models", "block", m.name + ".json")] = m.to_minecraft_json()
        files[os.path.join(base, "models", "item", m.name + ".json")] = {"parent": f"{m.namespace}:block/{m.name}"}
        files[os.path.join(base, "blockstates", m.name + ".json")] = blockstate_horizontal(f"{m.namespace}:block/{m.name}")
        files[os.path.join(TOOL, "out", "bbmodel", m.name + ".bbmodel")] = m.to_bbmodel()
    # ur-arsenal: weapons, armor icons + layers, materials, ores
    ars = assets_dir("urarsenal")
    for name, img in {**arsenal.all_weapon_sprites(), **arsenal.all_armor_sprites(), **arsenal.all_material_sprites()}.items():
        files[os.path.join(ars, "textures", "item", name + ".png")] = img
    for name in arsenal.ORES:
        files[os.path.join(ars, "textures", "block", name + ".png")] = arsenal.ore(name)
    for name, (ramps, light) in arsenal.armor_sets().items():
        l1, l2 = arsenal.armor_layers(ramps, light)
        files[os.path.join(ars, "textures", "models", "armor", name + "_layer_1.png")] = l1
        files[os.path.join(ars, "textures", "models", "armor", name + "_layer_2.png")] = l2
    # UI theme: overrides vanilla GUI textures, shipped in ur-core
    gui = os.path.join(REPO, "mods", "ur-core", "src", "main", "resources", "assets", "minecraft", "textures", "gui")
    for rel, obj in ui.all_ui().items():
        files[os.path.join(gui, rel)] = obj
    for rel, obj in ui.all_mod_ui().items():
        files[os.path.join(assets_dir("urcore"), "textures", "gui", rel)] = obj
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

    for m in models.all_models() + arsenal.stations():
        m.render_iso(scale=14).save(os.path.join(out, f"model_{m.name}.png"))

    # ur-arsenal sheet: one row per weapon type across the tiers, then armor sets, then materials
    weapons = arsenal.all_weapon_sprites()
    armor = arsenal.all_armor_sprites()
    mats = arsenal.all_material_sprites()
    tiers = list(arsenal.TIERS)
    rows = [[weapons[f"{t}_{k}"] for t in tiers] for k in arsenal.WEAPONS + ["bow"]]
    names = list(arsenal.armor_sets())
    for piece in arsenal.ARMOR_PIECES:
        rows.append([armor[f"{n}_{piece}"] for n in names])
    rows.append(list(mats.values()))
    cols = max(len(r) for r in rows)
    sheet = Image.new("RGBA", (cols * 72, len(rows) * 72), (32, 28, 36, 255))
    for y, row in enumerate(rows):
        for x, img in enumerate(row):
            sheet.alpha_composite(upscale(img, 4), (x * 72 + 4, y * 72 + 4))
    sheet.save(os.path.join(out, "arsenal.png"))

    # UI theme preview: hotbar, buttons and the three container screens
    textures = ui.all_ui()
    sheet = Image.new("RGBA", (3 * 188 + 8, 300), (32, 28, 36, 255))
    for i, name in enumerate(("inventory", "generic_54", "crafting_table")):
        tex = textures[f"container/{name}.png"]
        crop = tex.crop((0, 0, 176, 222 if name == "generic_54" else 166))
        sheet.alpha_composite(crop, (8 + i * 188, 8))
    sheet.alpha_composite(textures["sprites/hud/hotbar.png"], (8, 240))
    sheet.alpha_composite(textures["sprites/hud/hotbar_selection.png"], (8 + 2 * 20 - 1, 239))
    for i, name in enumerate(("button", "button_highlighted", "button_disabled")):
        sheet.alpha_composite(textures[f"sprites/widget/{name}.png"].crop((0, 0, 120, 20)), (200 + i * 124, 241))
    upscale(sheet, 2).save(os.path.join(out, "ui.png"))


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


def style_problems():
    """Runs the ART_STYLE.md rules (artforge.lint) over every sprite family."""
    from artforge.lint import check_sprite
    problems = []
    for (modid, tex), img in items.all_items().items():
        problems += check_sprite(f"{modid}:{tex}", img, "item")
    for (modid, tex), img in spells.all_spell_icons().items():
        problems += check_sprite(f"{modid}:{tex}", img, "icon")
    for family, sprites in extra_sprite_families().items():
        for name, img in sprites.items():
            problems += check_sprite(name, img, family)
    return problems


def extra_sprite_families():
    """Further linted sprite sets, {family: {name: image}}."""
    from assets import arsenal
    return {"weapon": arsenal.all_weapon_sprites(), "item": {**arsenal.all_armor_sprites(), **arsenal.all_material_sprites()}}


def main():
    cmd = sys.argv[1] if len(sys.argv) > 1 else "build"
    if cmd == "lint" or cmd == "check":
        problems = style_problems()
        if problems:
            print("Art style problems (see docs/ART_STYLE.md):")
            for p in problems: print("  " + p)
            sys.exit(1)
        if cmd == "lint":
            print("art style: all sprites pass")
            return
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
