#!/usr/bin/env python3
"""Download the pack's locked mods for one side into a folder (hash-verified).

    python3 tools/pack/fetch_mods.py --side client --dest run/client/mods
"""
import argparse
import hashlib
import pathlib
import re
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]


def field(text, name):
    m = re.search(rf'^{name}\s*=\s*"([^"]*)"', text, re.M)
    return m.group(1) if m else None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--side", choices=["client", "server"], required=True)
    ap.add_argument("--dest", required=True)
    args = ap.parse_args()
    dest = pathlib.Path(args.dest)
    dest.mkdir(parents=True, exist_ok=True)
    count = 0
    for toml in sorted((ROOT / "pack" / "mods").glob("*.pw.toml")):
        text = toml.read_text(encoding="utf-8")
        side = field(text, "side") or "both"
        if side not in ("both", args.side):
            continue
        name, url = field(text, "filename"), field(text, "url")
        algo, want = field(text, "hash-format"), field(text, "hash")
        target = dest / name
        if not target.exists() or hashlib.new(algo, target.read_bytes()).hexdigest() != want:
            req = urllib.request.Request(url, headers={"User-Agent": "KingSaled/Untamed-Realms CI"})
            data = urllib.request.urlopen(req, timeout=120).read()
            if hashlib.new(algo, data).hexdigest() != want:
                sys.exit(f"hash mismatch for {name}")
            target.write_bytes(data)
        count += 1
    print(f"{count} {args.side} mods in {dest}")


if __name__ == "__main__":
    main()
