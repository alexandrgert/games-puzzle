#!/usr/bin/env python3
import io, json, re, sys, urllib.parse, urllib.request
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
UA = "GamesPuzzle/0.3 (https://github.com/alexandrgert/games-puzzle)"


def api(params: dict) -> dict:
    q = urllib.parse.urlencode(params)
    req = urllib.request.Request(
        f"https://commons.wikimedia.org/w/api.php?{q}",
        headers={"User-Agent": UA},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode())


def download(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.read()


def square(im: Image.Image, size: int) -> Image.Image:
    im = im.convert("RGB")
    side = min(im.size)
    left = (im.width - side) // 2
    top = (im.height - side) // 2
    return im.crop((left, top, left + side, top + side)).resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    sources = json.loads((ROOT / "scripts" / "catalog_sources.json").read_text())
    puzzles_dir, thumbs_dir = ASSETS / "puzzles", ASSETS / "thumbs"
    puzzles_dir.mkdir(parents=True, exist_ok=True)
    thumbs_dir.mkdir(parents=True, exist_ok=True)
    for old in list(puzzles_dir.glob("*.webp")) + list(thumbs_dir.glob("*.webp")):
        old.unlink()
    out = []
    for row in sources:
        title = row["commons_file"]
        data = api({
            "action": "query", "format": "json", "prop": "imageinfo",
            "titles": title, "iiprop": "url|extmetadata|mime|size",
        })
        page = next(iter(data["query"]["pages"].values()))
        info = page["imageinfo"][0]
        meta = info.get("extmetadata", {})
        license_short = meta.get("LicenseShortName", {}).get("value", "")
        artist = re.sub("<[^>]+>", "", meta.get("Artist", {}).get("value", "")).strip()
        allowed = ("CC0", "Public domain", "PD", "CC BY")
        if not any(license_short.startswith(p) for p in allowed):
            raise SystemExit(f"license not allowed for {title}: {license_short}")
        raw = download(info["url"])
        im = Image.open(io.BytesIO(raw))
        if min(im.size) < 1200:
            raise SystemExit(f"too small: {title} {im.size}")
        pid = row["id"]
        square(im, 1200).save(puzzles_dir / f"{pid}.webp", "WEBP", quality=90)
        square(im, 256).save(thumbs_dir / f"{pid}.webp", "WEBP", quality=85)
        file_page = "https://commons.wikimedia.org/wiki/" + urllib.parse.quote(title.replace(" ", "_"))
        file_page = file_page.replace("File%3A", "File:")
        out.append({
            "id": pid,
            "file": f"puzzles/{pid}.webp",
            "thumb": f"thumbs/{pid}.webp",
            "category": row["category"],
            "season": row["season"],
            "title_ru": row["title_ru"],
            "license": license_short,
            "attribution": artist or "Unknown",
            "source_url": f"https://commons.wikimedia.org/wiki/{title.replace(' ', '_')}",
        })
    (ASSETS / "catalog.json").write_text(
        json.dumps({"schema_version": 1, "puzzles": out}, ensure_ascii=False, indent=2) + "\n"
    )
    print(f"wrote {len(out)} puzzles")


if __name__ == "__main__":
    sys.exit(main())
