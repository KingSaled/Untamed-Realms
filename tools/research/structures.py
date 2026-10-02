"""Lists a worldgen mod's structures and which structure tags they are in (ids only)."""
import json, sys, zipfile

for path in sys.argv[1:]:
    z = zipfile.ZipFile(path)
    names = z.namelist()
    structs = sorted(n for n in names if "/worldgen/structure/" in n and n.endswith(".json") and "/tags/" not in n)
    print(f"== {path}: {len(structs)} structures")
    for n in structs[:12]:
        print("  ", n.split("/data/")[-1] if "/data/" in n else n)
    tags = sorted(n for n in names if "/tags/worldgen/structure/" in n)
    for n in tags:
        try:
            vals = json.loads(z.read(n)).get("values", [])
        except Exception:
            vals = "?"
        print("  TAG", n, vals if len(str(vals)) < 600 else str(vals)[:600])
    sets = sorted(n for n in names if "/worldgen/structure_set/" in n)
    print("   structure sets:", [s.rsplit("/", 1)[1] for s in sets][:40])
