# ArtForge — the Untamed Realms asset pipeline

Every texture, skin and block model we add to the game is **generated from code** in this
folder, so art stays consistent, reviewable in diffs, and re-colourable in bulk.

```bash
cd tools/artforge
pip install pillow
python3 -m artforge build   # writes into mods/*/src/main/resources/assets + docs/art previews
python3 -m artforge check   # CI: fails if committed assets are stale
```

## What it can make

| Kind | How | Where it's defined |
|---|---|---|
| 16×16 item / UI sprites | ASCII **templates** + 5-shade **material ramps**. One sword template × N materials = a whole tier set. | `assets/items.py` |
| Procedural sprites | Planks, parchment, noise, shaded orbs, spell medallions + glyphs | `artforge/procedural.py`, `assets/spells.py` |
| 64×64 NPC skins | `Outfit(...)` descriptions (skin tone, hair, beard, shirt, apron/robe/armor/cloak/hood...) | `assets/npcs.py`, `artforge/skins.py` |
| 3D block models | Cuboids → Minecraft model JSON + blockstate, **Blockbench `.bbmodel`** (in `out/bbmodel/`) and an isometric preview render | `assets/models.py`, `artforge/model3d.py` |

### Template legend
`.` transparent · `o`/`O` outline · `1-5` primary ramp (dark→light) · `a-e` secondary · `A-E` tertiary · `w` white · `k` black.

### Hand-finishing in Blockbench
Open `out/bbmodel/<model>.bbmodel` in Blockbench, refine, then export the Java model over the
generated one **and** remove the model from `assets/models.py` so ArtForge stops overwriting it.

## Previews
`docs/art/sprites.png`, `docs/art/npc_skins.png`, `docs/art/model_*.png` are regenerated on each build.
