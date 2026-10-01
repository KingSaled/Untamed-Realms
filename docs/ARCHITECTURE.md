# Architecture

## Platform decisions

| Decision | Choice | Why |
|---|---|---|
| Minecraft version | **1.21.1** | The long-lived "big modpack" version: the richest selection of maintained worldgen, structure, boss and QoL mods on NeoForge, with Sodium/Lithium/ModernFix available. Newer versions have far thinner adventure-mod coverage. |
| Loader | **NeoForge 21.1.x** | Modern APIs that fit an RPG suite well: data attachments (per-player/per-chunk persistent data), typed payload networking, data components, game tests, auto-routed event subscribers. Most 1.21.1 content mods target it. |
| Build | Gradle multi-project + **ModDevGradle** | One repo, six mod jars, shared conventions in the root `build.gradle`. |
| Pack | **packwiz** | Mods are referenced by Modrinth hash, never re-hosted; the pack exports to `.mrpack` and servers sync with `packwiz-installer`. |

## Module graph

```
ur-core  <-  ur-skills  <-  ur-classes
               ^    ^------- ur-magic
ur-core  <-  ur-quests  <-  ur-npcs   (ur-npcs also uses ur-skills)
```

Each module is a separate mod jar with its own mod id (`urcore`, `urskills`, `urclasses`,
`urquests`, `urnpcs`, `urmagic`). Dependencies only point "down" the graph. When two modules that
cannot see each other need to cooperate, they meet in **ur-core**:

* `SkillEffect` + `EffectProvider` (in ur-skills) — perks, classes and birthsigns all describe their
  bonuses in one effect format; every module reads the aggregate through `SkillsApi.effect(...)`.
* `ProgressEvent` (in ur-core) — e.g. ur-magic posts `("cast", urmagic:firebolt)` and ur-quests turns
  it into quest progress, without either depending on the other.

## Data-driven content

`DataRegistry<T>` (ur-core) loads `data/<namespace>/<directory>/*.json` with a Codec, reloads on
`/reload`, and (optionally) syncs the whole registry to clients on login / reload. Every content type
is one of these:

| Registry | Path | Synced |
|---|---|---|
| Perk trees | `data/*/urskills/perks/` | yes |
| XP tables | `data/*/urskills/xp_sources/` | no |
| Skill requirements | `data/*/urskills/requirements/` | yes (tooltips, break speed prediction) |
| Classes / birthsigns | `data/*/urclasses/{classes,birthsigns}/` | yes |
| Quests | `data/*/urquests/quests/` | yes |
| NPCs / dialogues / shops / settlements | `data/*/urnpcs/...` | NPCs only |
| Spells | `data/*/urmagic/spells/` | yes |

Item stacks inside content (loadouts, rewards, shop stock) are kept as raw data and decoded lazily,
so content that references an uninstalled mod's item only loses that one item.

Default content lives in `tools/datagen/*.py` (the source of truth for what we ship); server owners
override or extend it with datapacks using the same paths.

## Player state

All per-player state is a NeoForge **data attachment**, serialised with the player and copied on
death where it should survive:

| Attachment | Module | Copied on death |
|---|---|---|
| `urcore:vitals` (current magicka/stamina) | core | no |
| `urcore:wallet` | core | yes (minus the configurable death loss, dropped as coins) |
| `urskills:skills` (xp, levels, perks, attribute picks) | skills | yes |
| `urskills:placed_blocks` (per chunk) | skills | — |
| `urclasses:class` | classes | yes |
| `urquests:quest_log` | quests | yes |
| `urmagic:spellbook` | magic | yes |

The server is authoritative. Each module syncs its own state to the owning client with a small
payload, batched (dirty flag + flush every few ticks). The client never decides outcomes: dialogue
options, shop prices, quest progress and spell casts are all validated server-side.

## Gameplay numbers

* **Skill XP curve**: RuneScape's formula scaled by `xpCurveScale` (default 0.25 → level 99 at ~3.26M XP).
* **Character level**: Skyrim's rule — each skill level gained adds that many character XP; the next
  level needs `25 × (level + 3)`. Each level grants one perk point and one attribute choice.
* **Attributes**: `urcore:max_magicka`, `urcore:max_stamina`, regen and stamina-cost multipliers are
  real attributes, so gear and other mods can modify them.

All of it is configurable in each module's **server config** (`world/serverconfig/*.toml`).

## Quality gates

* `./gradlew build` — compiles all modules.
* `./gradlew :devenv:runGameTestServer` — boots a headless server with every module and runs
  NeoForge game tests (data loads and cross-references, codecs round-trip, mechanics behave).
* `python3 -m artforge check` — committed art matches the generators.
* **Modpack smoke test** — installs NeoForge + every pack mod + our jars on a fresh dedicated server and
  waits for `Done`. Catches missing dependencies and mod conflicts before players do.

## Art pipeline

See [tools/artforge/README.md](../tools/artforge/README.md). Textures, skins and models are code;
previews are regenerated into `docs/art/`.
