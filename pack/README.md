# The Untamed Realms modpack (packwiz)

* **`mods.yml`** — the curated list (source of truth). Sections: performance, qol, immersion, world,
  content, and `exclude` (mods we never ship even if something depends on them).
* **`mods/*.pw.toml`** — lock files written by CI (`tools/pack/resolve.py` → `packwiz modrinth add`).
  Don't hand-edit; change `mods.yml` and push.
* **`resolve-report.md`** — last resolve: what was added, what failed (with Modrinth suggestions), and
  which mods arrived as dependencies.
* **`config/`, `defaultconfigs/`** — config overrides shipped to players/servers (add files here; packwiz
  indexes everything in this folder except the files in `.packwizignore`).

Our own mods (`ur-*`) are added to the exported `.mrpack` by the Release workflow, downloaded from the
GitHub release, so the tagged `.mrpack` is a complete one-click install.

Local use (needs Go): `go install github.com/packwiz/packwiz@latest`, then in this folder
`packwiz refresh` / `packwiz modrinth export`.
