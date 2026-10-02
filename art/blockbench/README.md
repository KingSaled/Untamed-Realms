# Blockbench models

Hand-made models go here. Each one replaces the generated model for that item; everything you haven't
modelled yet keeps the generated one, so you can do them one at a time.

## Weapons: `art/blockbench/arsenal/`

### Recommended: glTF (`.glb`), any shapes

Model however you like: cubes, cylinders, meshes, any rotation. Export with **File > Export >
Export glTF Model**, binary (`.glb`), and keep the textures embedded (the default). Name the file
after the item: `iron_dagger.glb`. An `ur_` prefix is fine (`ur_iron_dagger.glb`).

A few rules make the weapon sit right in the hand:

- **Model it standing up.** The blade or head goes up (+Y) and the pommel at the bottom.
- **Name the part you hold `handle`** (or `grip`). The middle of that part goes in the player's hand.
  If there's no such part, the importer uses a point 20% up from the bottom.
- **Don't worry about size.** Each weapon is scaled to a fixed length for its type, so a dagger is
  always shorter than a sword and a sword shorter than a greatsword. In pixels (a vanilla sword is
  about 19):

  | Type | Length |
  |---|---|
  | dagger | 14 |
  | sword | 20 |
  | war axe | 19 |
  | mace | 18 |
  | greatsword | 27 |
  | battleaxe | 26 |
  | warhammer | 26 |

  Proportions are kept: a model twice as thick looks twice as thick.

The importer then tilts the weapon 45 degrees and uses vanilla's sword poses for first person, third
person, the ground and item frames. The inventory shows the 3D model, shrunk to fit the slot.
`docs/art/blockbench_weapons.png` previews every imported weapon. Its left view puts the weapon on the
16x16 item grid, which a vanilla sword fills corner to corner.

### Also accepted: Java item models (`.json`), boxes only

Use **File > Export > Export Block/Item Model**. The `.json` keeps your Display-tab poses but has
Minecraft's limits (see below). Every texture it uses must sit next to it as a `.png`.

### Names, icons and bows

For either format, `<name>` is the item id. Each of these tiers:

    iron, steel, orcish, dwarven, elven, glass, ebony, daedric

goes with each of these types, as `<tier>_<type>` (for example `daedric_dagger`, `glass_greatsword`, `ebony_war_axe`):

    dagger, sword, greatsword, war_axe, mace, battleaxe, warhammer, bow

- **Icons:** `<name>_icon.png` is an optional flat picture for inventory slots, used instead of the 3D model.
- **Bows:** they also need their draw stages, `<tier>_bow_pulling_0`, `_pulling_1` and `_pulling_2`
  (start, middle and full draw). Each stage is its own model.

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

## Minecraft's limits for `.json` models (glTF models don't have them)

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
