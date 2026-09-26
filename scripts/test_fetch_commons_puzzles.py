import io
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from PIL import Image

import fetch_commons_puzzles


class FetchCommonsPuzzlesTest(unittest.TestCase):
    def test_full_frame_applies_exif_orientation_without_upscaling(self) -> None:
        source = Image.new("RGB", (120, 80), "red")
        source.paste("blue", (60, 0, 120, 80))
        source.getexif()[274] = 6
        result = fetch_commons_puzzles.full_frame(source, 2000)
        self.assertEqual(result.size, (80, 120))
        self.assertEqual(result.getpixel((40, 10)), (255, 0, 0))
        self.assertEqual(result.getpixel((40, 110)), (0, 0, 255))

    def test_portrait_fetch_keeps_full_frame(self) -> None:
        self.assert_fetched_frame(preserve_aspect=True)

    def test_existing_fetch_keeps_square_crop(self) -> None:
        self.assert_fetched_frame(preserve_aspect=False)

    def assert_fetched_frame(self, preserve_aspect: bool) -> None:
        source = Image.new("RGB", (1200, 2400), "green")
        source.paste("red", (0, 0, 1200, 240))
        source.paste("blue", (0, 2160, 1200, 2400))
        raw = io.BytesIO()
        source.save(raw, "PNG")
        row = dict(id="portrait-test", commons_file="File:Portrait.jpg",
                   category="nature", season="summer", title_ru="Тест")
        if preserve_aspect:
            row["preserve_aspect"] = True
        metadata = {"query": {"pages": {"1": {"imageinfo": [{
            "url": "https://example.test/image.png",
            "extmetadata": {"LicenseShortName": {"value": "CC BY 4.0"}},
        }]}}}}
        with tempfile.TemporaryDirectory() as folder:
            puzzles, thumbs = Path(folder) / "puzzles", Path(folder) / "thumbs"
            puzzles.mkdir()
            thumbs.mkdir()
            with patch.object(fetch_commons_puzzles, "api", return_value=metadata), \
                    patch.object(fetch_commons_puzzles, "download", return_value=raw.getvalue()):
                fetch_commons_puzzles.fetch_row(row, puzzles, thumbs)
            play_size = (1000, 2000) if preserve_aspect else (1200, 1200)
            thumb_size = (128, 256) if preserve_aspect else (256, 256)
            for path, expected_size in ((puzzles / "portrait-test.webp", play_size),
                                        (thumbs / "portrait-test.webp", thumb_size)):
                with Image.open(path) as image:
                    self.assertEqual(image.size, expected_size)
                    top = image.getpixel((image.width // 2, 5))
                    bottom = image.getpixel((image.width // 2, image.height - 6))
                    if preserve_aspect:
                        self.assertGreater(top[0], 240)
                        self.assertGreater(bottom[2], 240)
                    else:
                        for pixel in (top, bottom):
                            self.assertLess(pixel[0], 10)
                            self.assertGreater(pixel[1], 120)
                            self.assertLess(pixel[2], 10)

    def test_accepts_cc_by_licenses(self) -> None:
        self.assertTrue(fetch_commons_puzzles.is_allowed_license("CC BY 4.0"))

    def test_accepts_cc_by_sa_licenses(self) -> None:
        self.assertTrue(fetch_commons_puzzles.is_allowed_license("CC BY-SA 4.0"))

    def test_rejects_noncommercial_licenses(self) -> None:
        self.assertFalse(fetch_commons_puzzles.is_allowed_license("CC BY-NC 4.0"))


if __name__ == "__main__":
    unittest.main()
