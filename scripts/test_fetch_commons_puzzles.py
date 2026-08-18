import unittest

import fetch_commons_puzzles


class FetchCommonsPuzzlesTest(unittest.TestCase):
    def test_accepts_cc_by_licenses(self) -> None:
        self.assertTrue(fetch_commons_puzzles.is_allowed_license("CC BY 4.0"))

    def test_accepts_cc_by_sa_licenses(self) -> None:
        self.assertTrue(fetch_commons_puzzles.is_allowed_license("CC BY-SA 4.0"))

    def test_rejects_noncommercial_licenses(self) -> None:
        self.assertFalse(fetch_commons_puzzles.is_allowed_license("CC BY-NC 4.0"))


if __name__ == "__main__":
    unittest.main()
