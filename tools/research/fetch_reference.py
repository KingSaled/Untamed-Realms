"""
Downloads Skyrim reference pictures of every weapon/armor we model (from the Elder Scrolls fandom
wiki's page images) into reference/. Used only as visual reference for our own models; the images
are never committed or published (CI puts them in a draft release visible only to collaborators).
"""
import json, os, sys, time, urllib.parse, urllib.request

API = "https://elderscrolls.fandom.com/api.php"
UA = {"User-Agent": "untamed-realms-art-reference/1.0 (modpack art research)"}

TIERS = ["Iron", "Steel", "Orcish", "Dwarven", "Elven", "Glass", "Ebony", "Daedric"]
TYPES = ["Dagger", "Sword", "War Axe", "Mace", "Greatsword", "Battleaxe", "Warhammer"]
BOWS = {"Iron": "Long Bow", "Steel": "Hunting Bow"}
ARMOR = ["Hide", "Leather", "Elven", "Glass", "Iron", "Steel", "Orcish", "Dwarven", "Ebony", "Daedric"]
PIECES = {"Armor": "chest", "Helmet": "helmet", "Boots": "boots", "Gauntlets": "gauntlets"}


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30) as r:
        return r.read()


def page_image(title):
    q = {"action": "query", "titles": title, "prop": "pageimages", "pithumbsize": "800", "format": "json", "redirects": "1"}
    data = json.loads(get(API + "?" + urllib.parse.urlencode(q)))
    for page in data.get("query", {}).get("pages", {}).values():
        thumb = page.get("thumbnail", {}).get("source")
        if thumb:
            return thumb
    return None


def fetch(name, titles, out):
    for title in titles:
        try:
            url = page_image(title)
        except Exception as e:
            print(f"  {title}: {e}")
            continue
        if url:
            path = os.path.join(out, name + os.path.splitext(urllib.parse.urlparse(url).path)[1].split("/")[0][:5])
            with open(path, "wb") as f:
                f.write(get(url))
            print(f"{name}: {title}")
            return True
        time.sleep(0.2)
    print(f"{name}: NOT FOUND ({titles})")
    return False


def main(out):
    os.makedirs(out, exist_ok=True)
    for tier in TIERS:
        for typ in TYPES:
            item = f"{tier} {typ}"
            fetch(f"{tier.lower()}_{typ.lower().replace(' ', '_')}", [f"{item} (Skyrim)", item], out)
        bow = BOWS.get(tier, f"{tier} Bow")
        fetch(f"{tier.lower()}_bow", [f"{bow} (Skyrim)", bow], out)
    for s in ARMOR:
        for piece, short in PIECES.items():
            fetch(f"{s.lower()}_{short}", [f"{s} {piece} (Skyrim)", f"{s} {piece}"], out)
        fetch(f"{s.lower()}_set", [f"{s} Armor Set (Skyrim)", f"{s} Armor (set)"], out)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "reference")
