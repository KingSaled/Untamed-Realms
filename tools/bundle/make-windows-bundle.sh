#!/usr/bin/env bash
# Assembles dist/Untamed-Realms-Windows.zip: the Windows scripts, the locked modpack and our built mod jars.
# Run after ./gradlew build.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/dist/Untamed-Realms"
rm -rf "$ROOT/dist" && mkdir -p "$OUT/ur-mods" "$OUT/pack"
cp -r "$ROOT/windows/." "$OUT/"
cp "$ROOT/pack/pack.toml" "$ROOT/pack/index.toml" "$OUT/pack/"
cp -r "$ROOT/pack/mods" "$OUT/pack/"
for extra in config defaultconfigs; do [ -d "$ROOT/pack/$extra" ] && cp -r "$ROOT/pack/$extra" "$OUT/pack/"; done
for jar in $(find "$ROOT/mods" -path '*/build/libs/ur-*.jar' ! -name '*-sources.jar'); do
  if unzip -l "$jar" | grep -q 'META-INF/neoforge.mods.toml'; then cp "$jar" "$OUT/ur-mods/"; fi
done
# Windows line endings for the batch files, whatever git did on checkout
for f in "$OUT"/*.bat "$OUT"/scripts/*.ps1; do sed -i 's/\r$//; s/$/\r/' "$f"; done
# Line 1 is the build id the updater compares with dist/version.txt (published next to the zip).
BUILD_ID="$(git -C "$ROOT" rev-parse HEAD)-${GITHUB_RUN_ID:-local}"
printf '%s\nBuilt from %s on %s\n' "$BUILD_ID" "$(git -C "$ROOT" rev-parse --short HEAD)" "$(date -u +%Y-%m-%d)" > "$OUT/VERSION.txt"
echo "$BUILD_ID" > "$ROOT/dist/version.txt"
(cd "$ROOT/dist" && zip -qr Untamed-Realms-Windows.zip Untamed-Realms)
echo "dist/Untamed-Realms-Windows.zip: $(ls "$OUT/ur-mods" | wc -l) Untamed Realms mods, $(ls "$OUT/pack/mods" | wc -l) pack mods"
