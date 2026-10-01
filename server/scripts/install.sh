#!/usr/bin/env bash
# Installs (or updates) an Untamed Realms dedicated server.
#
#   bash server/scripts/install.sh --dir /srv/untamed-realms [--pack-url URL | --pack-dir pack] [--mods-from mods|DIR]
#
#  --dir        target server folder (created if missing; existing worlds are never touched)
#  --pack-url   URL of pack/pack.toml (e.g. raw.githubusercontent.com/.../main/pack/pack.toml)
#  --pack-dir   local pack folder instead of a URL (served over a throwaway localhost HTTP server)
#  --mods-from  where to take the Untamed Realms mod jars from: "mods" = this repo's build output
#               (run ./gradlew build first), or any folder containing ur-*.jar
#  --memory     heap size for user_jvm_args.txt (default 10G)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DIR=""; PACK_URL=""; PACK_DIR=""; MODS_FROM=""; MEMORY="10G"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dir) DIR="$2"; shift 2;;
    --pack-url) PACK_URL="$2"; shift 2;;
    --pack-dir) PACK_DIR="$2"; shift 2;;
    --mods-from) MODS_FROM="$2"; shift 2;;
    --memory) MEMORY="$2"; shift 2;;
    *) echo "Unknown option $1"; exit 2;;
  esac
done
[[ -n "$DIR" ]] || { echo "--dir is required"; exit 2; }
[[ -n "$PACK_URL" || -n "$PACK_DIR" ]] || PACK_DIR="$REPO_ROOT/pack"

NEOFORGE_VERSION="$(grep -E '^neoforge *=' "${PACK_DIR:-$REPO_ROOT/pack}/pack.toml" | sed -E 's/.*"(.*)".*/\1/')"
echo ">> NeoForge $NEOFORGE_VERSION -> $DIR"
mkdir -p "$DIR"
DIR="$(cd "$DIR" && pwd)"
cd "$DIR"

# 1. NeoForge server (skipped if this exact version is already installed)
if [[ ! -d "libraries/net/neoforged/neoforge/$NEOFORGE_VERSION" ]]; then
  curl -fsSL -o neoforge-installer.jar \
    "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEOFORGE_VERSION/neoforge-$NEOFORGE_VERSION-installer.jar"
  java -jar neoforge-installer.jar --installServer > installer.log 2>&1 || { tail -50 installer.log; exit 1; }
  rm -f neoforge-installer.jar neoforge-installer.jar.log
fi

# 2. Third-party mods + config overrides via packwiz (server-side files only)
curl -fsSL -o packwiz-installer-bootstrap.jar \
  https://github.com/packwiz/packwiz-installer-bootstrap/releases/latest/download/packwiz-installer-bootstrap.jar
HTTP_PID=""
if [[ -n "$PACK_DIR" ]]; then
  PACK_DIR="$(cd "$REPO_ROOT" && cd "$PACK_DIR" && pwd)"
  python3 -m http.server 8765 --bind 127.0.0.1 --directory "$PACK_DIR" > /dev/null 2>&1 &
  HTTP_PID=$!
  sleep 1
  PACK_URL="http://127.0.0.1:8765/pack.toml"
fi
java -jar packwiz-installer-bootstrap.jar -g -s server "$PACK_URL"
[[ -n "$HTTP_PID" ]] && kill "$HTTP_PID" || true

# 3. Untamed Realms mods
if [[ -n "$MODS_FROM" ]]; then
  rm -f mods/ur-*.jar
  if [[ "$MODS_FROM" == "mods" ]]; then
    SRC=$(find "$REPO_ROOT/mods" -path '*/build/libs/ur-*.jar' ! -name '*-sources.jar')
  else
    SRC=$(ls "$MODS_FROM"/ur-*.jar)
  fi
  for jar in $SRC; do
    # modules still under construction have no mod descriptor yet - don't install them
    if unzip -l "$jar" | grep -q 'META-INF/neoforge.mods.toml'; then cp "$jar" mods/; else echo "skip (not a mod yet): $jar"; fi
  done
  echo ">> Installed Untamed Realms mods:"; ls mods/ur-*.jar
fi

# 4. First-install defaults (never overwrite an operator's edits)
echo "eula=true" > eula.txt
[[ -f server.properties ]] || cp "$REPO_ROOT/server/config-templates/server.properties" server.properties
if ! grep -q "Untamed Realms" user_jvm_args.txt 2>/dev/null; then
  sed "s/@MEMORY@/$MEMORY/g" "$REPO_ROOT/server/config-templates/user_jvm_args.txt" > user_jvm_args.txt
fi
cp -n "$REPO_ROOT/server/scripts/start.sh" start.sh 2>/dev/null || true
chmod +x run.sh start.sh 2>/dev/null || true
echo ">> Done. Start with: cd $DIR && ./start.sh"
