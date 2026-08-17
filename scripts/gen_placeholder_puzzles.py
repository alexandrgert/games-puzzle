#!/usr/bin/env python3

from pathlib import Path

from PIL import Image


COLORS = [
    "#2E7D32",
    "#F9A825",
    "#1565C0",
    "#00838F",
    "#6D4C41",
    "#AD1457",
    "#558B2F",
    "#EF6C00",
    "#5E35B1",
    "#0277BD",
    "#8D6E63",
    "#C2185B",
]
ASSETS_DIR = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets"


def generate_placeholders() -> None:
    puzzle_dir = ASSETS_DIR / "puzzles"
    thumb_dir = ASSETS_DIR / "thumbs"
    puzzle_dir.mkdir(parents=True, exist_ok=True)
    thumb_dir.mkdir(parents=True, exist_ok=True)

    for index, color in enumerate(COLORS, start=1):
        puzzle_id = f"ph-{index:02d}"
        Image.new("RGB", (1200, 1200), color).save(
            puzzle_dir / f"{puzzle_id}.webp",
            "WEBP",
            quality=90,
        )
        Image.new("RGB", (256, 256), color).save(
            thumb_dir / f"{puzzle_id}.webp",
            "WEBP",
            quality=85,
        )


if __name__ == "__main__":
    generate_placeholders()
