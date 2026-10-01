"""
ArtForge - the Untamed Realms asset pipeline.

Everything visual we add to the game is generated from code in tools/artforge/assets/:
  * 16x16 item / UI sprites from ASCII templates + material palettes (one template, many tiers)
  * procedural sprites (spell icons, coins, gems, parchment, wood)
  * 64x64 NPC skins from outfit descriptions
  * 3D block/item models from cuboids -> Minecraft model JSON + Blockbench .bbmodel + iso previews

Run `python3 -m artforge build` from tools/artforge (see README.md there).
"""
