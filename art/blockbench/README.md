# Blockbench models

Hand-made models go here. Each one replaces the generated model for that item; everything you haven't
modelled yet keeps the generated one, so you can do them one at a time.

## Weapons: `art/blockbench/arsenal/`

For each weapon, put these files in the folder:

| File | What it is |
|---|---|
| `<name>.json` | Required. The model, from **File > Export > Export Block/Item Model** (Java). |
| `<texture>.png` | Required. Every texture the model uses, with the same name as in Blockbench's texture list. |
| `<name>_icon.png` | Optional. A flat picture for inventory slots. Without it, the slot shows your 3D model using the **gui** pose from the Display tab. |

`<name>` is the item id. Each of these tiers:

    iron, steel, orcish, dwarven, elven, glass, ebony, daedric

goes with each of these types, as `<tier>_<type>` (for example `daedric_dagger`, `glass_greatsword`, `ebony_war_axe`):

    dagger, sword, greatsword, war_axe, mace, battleaxe, warhammer, bow

Bows also need their draw stages: `<tier>_bow_pulling_0`, `_pulling_1` and `_pulling_2`. These are
the bow at the start, middle and full draw. Each one is its own model, and they can share one texture.

Textures can be any size (16, 32, 64 px...), and several weapons can share one texture.

### Upload to GitHub

Upload the files to `art/blockbench/arsenal/` on the branch the test build comes from (currently
`claude/epic-shannon-n64q1c`). You can use GitHub's **Add file > Upload files** or git. The
*Import Blockbench models* workflow then:

1. checks each model,
2. turns it into the game's files,
3. commits them, and
4. publishes a new test build.

Your launcher picks that build up on its next start.

If a model breaks one of Minecraft's rules, the workflow fails and its log says which cube to fix.

## Minecraft's limits, and how to make big weapons

- **Cube size:** every cube has to stay inside **-16 to 32** on every axis. That is 3 blocks, centred on the
  middle block.
- **Long weapons:** for a greatsword or battleaxe, model at a smaller size, then make it bigger in the
  **Display** tab with a scale of up to **4**. The display poses decide how big and where the weapon
  appears in first person, third person, on the ground and in item frames.
- **Cube rotation:** you can only rotate a cube to -45, -22.5, 0, 22.5 or 45 degrees, around one axis
  at a time.
- **Starting point:** you can open the current generated model in Blockbench (File > Open Model) from
  `mods/ur-arsenal/src/main/resources/assets/urarsenal/models/item/3d/<name>.json`. It comes with poses
  where the hand holds the grip, so you can see them, copy them, or rebuild over them.
- **Checking the poses:** the Display tab's previews (first-person right hand, third-person right hand,
  gui) show exactly what you'll see in game. Use them to fix any weapon that sits too far forward or
  points the wrong way.
