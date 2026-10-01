#!/usr/bin/env python3
"""
Resolves pack/mods.yml into packwiz lock files (pack/mods/*.pw.toml) using the packwiz CLI.

For each slug not yet locked, runs `packwiz modrinth add <slug> -y`, which picks the newest build
for the pack's Minecraft/NeoForge versions and pulls in required dependencies. Mods that have no
NeoForge 1.21.1 build fail loudly in the report instead of silently vanishing.

Usage (CI):  python3 tools/pack/resolve.py [--update]
  --update   also run `packwiz update --all` to move locked mods to their newest builds
"""
import json, os, re, subprocess, sys, urllib.parse, urllib.request

PACK = os.path.join(os.path.dirname(__file__), "..", "..", "pack")
MANIFEST = os.path.join(PACK, "mods.yml")


def read_manifest():
    """Tiny parser for our manifest format: `section:` headers and `  - slug  # comment` items."""
    sections, current = {}, None
    for raw in open(MANIFEST):
        line = raw.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        m = re.match(r"^([a-z_]+):\s*$", line)
        if m:
            current = m.group(1)
            sections[current] = []
            continue
        m = re.match(r"^\s+-\s+([A-Za-z0-9_.\-]+)\s*$", line)
        if m and current:
            sections[current].append(m.group(1))
    return sections


def locked(slug):
    return os.path.exists(os.path.join(PACK, "mods", f"{slug}.pw.toml"))


def run(args):
    return subprocess.run(args, cwd=PACK, capture_output=True, text=True, timeout=300)


def suggest(slug):
    """Modrinth search for NeoForge 1.21.1 projects resembling a slug that failed to resolve."""
    facets = json.dumps([["categories:neoforge"], ["versions:1.21.1"], ["project_type:mod"]])
    url = "https://api.modrinth.com/v2/search?" + urllib.parse.urlencode(
        {"query": slug.replace("-", " "), "facets": facets, "limit": 4})
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "untamed-realms-pack-resolver"})
        hits = json.load(urllib.request.urlopen(req, timeout=20))["hits"]
        return ", ".join(f"`{h['slug']}` ({h['title']}, {h['downloads']:,} dl)" for h in hits) or "no matches"
    except Exception as e:  # noqa: BLE001 - suggestions are best effort
        return f"search failed: {e}"


def main():
    update = "--update" in sys.argv
    sections = read_manifest()
    results = []
    for section, slugs in sections.items():
        for slug in slugs:
            if locked(slug):
                results.append((section, slug, "locked", ""))
                continue
            # --project-id does an exact lookup (Modrinth accepts slugs as ids); a bare argument is a fuzzy search.
            proc = run(["packwiz", "modrinth", "add", "--project-id", slug, "-y"])
            out = (proc.stdout + proc.stderr).strip().splitlines()
            tail = out[-1] if out else ""
            if proc.returncode == 0 and locked(slug):
                results.append((section, slug, "added", tail))
            elif proc.returncode == 0:
                # packwiz may name the file after the mod's display slug; treat as added.
                results.append((section, slug, "added (renamed file)", tail))
            else:
                results.append((section, slug, "FAILED", tail + " -- candidates: " + suggest(slug)))
            print(f"[{section}] {slug}: {results[-1][2]} {tail}", flush=True)

    if update:
        proc = run(["packwiz", "update", "--all", "-y"])
        print(proc.stdout, proc.stderr)
    proc = run(["packwiz", "refresh"])
    if proc.returncode != 0:
        print(proc.stdout, proc.stderr)
        sys.exit(1)

    failed = [r for r in results if r[2] == "FAILED"]
    wanted = {slug for _, slug, _, _ in results}
    mods_dir = os.path.join(PACK, "mods")
    extra = sorted(f[:-len(".pw.toml")] for f in os.listdir(mods_dir) if f.endswith(".pw.toml") and f[:-len(".pw.toml")] not in wanted) if os.path.isdir(mods_dir) else []
    lines = ["# Modpack resolve report", "",
             f"{len(results)} mods in manifest, {len(failed)} failed.", "",
             "| Section | Mod | Status | Detail |", "|---|---|---|---|"]
    lines += [f"| {s} | `{slug}` | {status} | {detail.replace('|', '/')} |" for s, slug, status, detail in results]
    lines += ["", "## Pulled in as dependencies (review these)", ""] + [f"- `{e}`" for e in extra]
    report = "\n".join(lines) + "\n"
    with open(os.path.join(PACK, "resolve-report.md"), "w") as f:
        f.write(report)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a") as f:
            f.write(report)
    print(report)


if __name__ == "__main__":
    main()
