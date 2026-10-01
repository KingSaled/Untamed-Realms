# Running an Untamed Realms server

## Hardware
* 4+ fast cores (single-thread speed matters most), **10 GB heap** for ~20 players, SSD/NVMe.
* Pre-generating the world removes the biggest source of lag (structure-heavy worldgen).

## Install (bare metal)
```bash
git clone https://github.com/KingSaled/Untamed-Realms && cd Untamed-Realms
./gradlew build                                   # builds mods/*/build/libs/ur-*.jar
bash server/scripts/install.sh --dir /srv/untamed --mods-from mods --memory 10G
cd /srv/untamed && ./start.sh                     # auto-restarts after crashes
```
`install.sh` is safe to re-run for updates: it installs/updates NeoForge, syncs server-side mods and
config overrides via packwiz, replaces the Untamed Realms jars, and never touches your world or edited
`server.properties` / `user_jvm_args.txt`.

## Install (Docker)
See [`server/docker-compose.yml`](../server/docker-compose.yml) (itzg/minecraft-server, NeoForge + packwiz).
Put the `ur-*.jar` files in `server/custom-mods/`.

## First start checklist
1. Op yourself, then pre-generate: `/chunky radius 3000` → `/chunky start` (takes a while; leave it).
2. Check `/spark tps` and `/spark health`; profile lag with `/spark profiler`.
3. Tune gameplay in `world/serverconfig/` (`urcore-server.toml`, `urskills-server.toml`, ...):
   XP curve scale, requirement strictness, stamina costs, death coin loss, bounties per board...
4. Optional: build a capital city at spawn and place NPCs with `/ur npc spawn <id>` (or
   `/ur npc populate urnpcs:village` to drop a full roster + notice board where you stand).

## Performance notes
* JVM flags: Aikar's G1 set (`server/config-templates/user_jvm_args.txt`). Don't give more heap than needed.
* `view-distance=10`, `simulation-distance=8`; ServerCore can lower them dynamically under load.
* `sync-chunk-writes=false` for faster saves; keep backups.
* Lithium, ModernFix, FerriteCore, Noisium, AI Improvements, Clumps and Let Me Despawn run server-side.

## Admin commands
| Command | |
|---|---|
| `/ur wallet give|set <player> <amount>` | Crowns |
| `/ur vitals refill <player>` | full health / magicka / stamina |
| `/ur skills show|set|addxp|respec|reset ...` | skills and perks |
| `/ur class info|reset <player>` / `/ur class choose` | classes |
| `/ur quest start|abandon|forget|reset|talk ...` / `/ur quest list` | quests |
| `/ur npc spawn <npc>` / `/ur npc populate <settlement>` | NPCs |
| `/ur magic learn|learnall|forget ...` | spells |

## Updating the modpack
Edit `pack/mods.yml` and push: CI resolves new mods, commits lock files, exports the `.mrpack` and
boots a test server with everything. Run the *Modpack* workflow manually with **update** ticked to
move every mod to its newest NeoForge 1.21.1 build.
