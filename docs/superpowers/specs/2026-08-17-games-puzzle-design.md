# Games Puzzle — design spec

Date: 2026-08-17  
Status: approved  
Amended: 2026-08-18 (`docs/superpowers/specs/2026-08-18-swap-stay-records-photos.md`)  
Repo: https://github.com/alexandrgert/games-puzzle  
Approach: native Kotlin + Jetpack Compose, single Android app module

## Goal

Android picture puzzle: a photo is split into a full grid with **no empty cells**. The player swaps any two unlocked tiles; a correct edge snaps that pair onto the finished picture and locks them, otherwise the swap **stays**. Built-in catalog of Russia-themed free-license photos plus user imports. APK is built only on GitHub Actions. The app can check GitHub Releases: a **manual** check always shows the changelog; an optional launch check (default **off**) stays silent unless a newer version is available.

## Out of scope (v1)

- Desktop, iOS, PWA
- Local APK/AAB builds (GitHub Actions only)
- English UI strings (resource slots exist; `values-en` is not filled)
- Online accounts, cloud sync, ads
- Play Store listing
- Pause, undo, hints
- Camera capture (gallery / document picker only)
- Downloading the built-in catalog from the network (all bundled in APK)

## Constraints

- Platform: Android phone, portrait, layout tuned for Poco X7 (6.67", 1220×2712) and Poco X8 Pro Max (6.83", 1280×2772), usable on other 20:9 phones.
- minSdk: 26. targetSdk / compileSdk: 36.
- applicationId / namespace: `ru.alexandrgert.gamespuzzle`.
- Builds: GitHub Actions in this repo only. Agent does not run Gradle assemble locally unless the user explicitly overrides.
- Signing: one long-lived release keystore in GitHub Secrets (`ANDROID_KEYSTORE_BASE64`, passwords, alias). Never generate a new keystore per CI run. Keystore files are gitignored.
- Version: one canonical semver in `VERSION`. Android `versionName` matches it; `versionCode` is monotonic integer. CI verifies sync before assemble.
- UI copy: Russian in `values/strings.xml`. No hardcoded user-visible Russian/English in Kotlin. Adding a language later is a new `values-xx/strings.xml` only.
- Images: public domain or licenses that allow free redistribution (prefer Wikimedia Commons CC0 / Public Domain / CC BY with attribution). No copyrighted stock. Attribution stored in catalog metadata and shown in Credits. The launcher icon is a jigsaw-piece crop of one bundled catalog photo; Credits lists that derivative. After 2026-08-18 the icon must not use the withdrawn `dzhangyskol-autumn-altai` file.

## Architecture

One data contract, one Android shell.

| Layer | Responsibility | Location |
|-------|----------------|----------|
| Domain | board, unlock/lock, swap, snap-to-home, shuffle, win, record comparison | `domain/` — no Android, no Compose |
| Application | start game, catalog filters, import/delete user image, update check | `app/` use-cases |
| Adapters | assets, gallery, DataStore, GitHub Releases HTTP | `data/` |
| Shell | Compose screens | `ui/` |
| Platform | app files, APK install intent | `platform/` |

Domain has zero UI-framework imports. Catalog JSON is validated in CI against JSON Schema.

## Data contract

### Built-in catalog (`assets/catalog.json`)

Each entry:

- `id` — stable string
- `file` — relative path under `assets/puzzles/` (WebP)
- `thumb` — relative path under `assets/thumbs/` (WebP)
- `category` — one of: `nature`, `animals`, `birds`, `aquatic`, `trees`, `flowers`
- `season` — one of: `spring`, `summer`, `autumn`, `winter` (or `any` only if the photo is not season-specific; filter "all seasons" shows everything)
- `title_ru` — short Russian title
- `license` — SPDX-like or Commons license id (`CC0`, `PD`, `CC-BY-4.0`, …)
- `attribution` — author / source text required for display
- `source_url` — page URL of the original file
- `schema_version` — catalog file has top-level `schema_version` integer

Images: 200–300 items, good quality, themes limited to Russia (mountains, sunset/sunrise, rivers, waterfalls, animals, birds, aquatic fauna, trees, flowers, seasons). Encoded as WebP. Long edge sized for a 6×6 board on ~1280 px width (decoded bitmap for play ≈ 1200 px on the long side; thumbs ≈ 256 px).

User-imported photos are **not** in `catalog.json`. They live only in app-private storage plus a local `user_puzzles.json` that is never uploaded.

### Settings (DataStore)

- `stats_enabled` — show timer and move count **during play** (default: **off**). Does not affect win dialog or writing records.
- `auto_check_updates` — check GitHub Releases on launch (default: **off**)
- `last_update_check_at` — ISO timestamp of the last successful check
- `dismissed_update_version` — latest version the user postponed from a startup prompt

### Best results (DataStore or local JSON)

Key: `(puzzle_id, grid)` where `grid` is `5`, `6`, `8`, `10`, or `12`.  
Value: best time (ms) and best move count, stored independently (a run can beat time without beating moves).  
Written on every win. User-imported ids use prefix `user:`.

Secrets never appear in any puzzle file.

## Screens and navigation

Single-activity Compose app.

1. **Catalog** — category chips (природа, животные, птицы, водная фауна, деревья, цветы) + season filter + thumbnail grid. Entry to Settings, My photos, Credits.
2. **Preview** — full picture, grid toggle **5×5 / 6×6 / 8×8 / 10×10 / 12×12** (all always available), button **Запуск**. No shuffle yet.
3. **Play** — board filling the width; **all cells occupied** (no gap). Locked tiles sit in their final-plan cells and do not move. Timer and move counter on the board only if stats enabled (values are always tracked). Button **Картинка** shows the original as an overlay; peek is not a move. Back asks to abandon the run.
4. **Win** — success message, this run’s time and moves, and whether a best was beaten. Buttons: again (same picture/grid), catalog.
5. **My photos** — list of imports, add from gallery, delete (removes file + records for that id).
6. **Settings** — toggle for in-play timer/moves (default off) and check-on-launch (default off); **Проверить обновления**; app version.
7. **Credits** — bundled image attributions from catalog metadata.

## Game rules

- Grids: **5×5**, **6×6**, **8×8**, **10×10**, **12×12**. User picks per picture; nothing is locked.
- Mechanic: **not** пятнашки. The picture is cut into N×N tiles. Every slot has a tile. There is **never** an empty cell. Tiles join along horizontal and vertical edges to restore the photo.
- Groups: a **correct join** is two tiles that are neighbours in the original photo and currently share that same edge. After a join they **lock** and **do not travel** around the board. The group is **snapped onto the final-picture plan**: those tiles occupy their unique home cells and stay there until the run is reset. Locked tiles cannot be selected, dragged, or swapped.
- Input: only **unlocked** tiles move. The player swaps any two unlocked tiles (tap-tap or drag one onto another). Locked cells are not valid drop targets: a swap may not displace a locked tile. The board stays full (no empty cells).
- Resultative move:
  1. Two unlocked tiles share a correct relative edge after the swap → both **snap to their home cells** (the unique place of that pair on the finished picture). Unlocked tiles that sat in those home cells are displaced into the cells the pair left. Then the pair locks.
  2. Or an unlocked tile lands in the home cell that correctly neighbours an already locked group → it locks onto that group in place (no snap of the group; the group is already home).
  If home cells needed for a snap still hold **locked** tiles, the move is invalid.
- Non-joining swap: none of the cases above. Both tiles **stay** in the new cells. Counted as a move. No rollback flash. Locked groups never split and never move.
- Preview: show the complete picture. **Запуск** then shuffles.
- Shuffle: random permutation of all N×N tiles, never the identity, all unlocked. Retry until unsolved and there is **at least one** joining swap of unlocked tiles (snap or attach-to-locked — not merely a permutation).
- Win: every tile is locked in its home cell (one group of N×N).
- Example: `B` and `C` belong side by side. A swap puts them on the matching edge anywhere on the field → they jump to the home slots of `B` and `C` on the plan and freeze. Next, swap the tile that belongs at `A` into the cell left of locked `B` (that cell is `A`’s home). It locks. You cannot drag `BC` to another place.

## User images

- Pick via Android photo picker / `GetContent` (`image/*`).
- Decode, center-crop to square, write WebP into app storage, add to `user_puzzles.json`.
- If decode fails, image too small (shortest side &lt; 512 px), or user cancels — show an error, leave no orphan files.
- Delete is always available for user images (and their best-result rows). Built-in catalog entries cannot be deleted.

## Updates

Source: GitHub Releases API for `alexandrgert/games-puzzle`.

- Compare semver (tuple, not string). Strip leading `v`.
- **Always show changelog** (release body) on a **manual** check: when a newer release exists, and also when the user is already on latest (then: «установлена актуальная версия» + current notes if present).
- After showing changelog for an available update, wait for explicit **Скачать**.
- Downloaded APK is installed via a package-installer intent (permission to install unknown apps if needed). Failures (network, HTTP, storage, permission) set an error status; they do not crash.
- Setting `auto_check_updates` (default **off**): on launch, if enabled, check GitHub Releases once per process. Startup UI is silent unless a newer version is available and not `dismissed_update_version`. Offline / already latest: no snackbar and no «установлена актуальная версия». The startup dialog appears only on the catalog screen (not over play). **Отмена** on that dialog stores `dismissed_update_version`; OK / back do not. Manual **Проверить обновления** always reports (changelog or offline snackbar) and ignores dismissed.
- CI publishes release notes used as that changelog (`docs/github-release-vX.Y.Z.md` body).

## CI and release

Workflow on `main` (and tags):

1. Validate `catalog.json` against schema.
2. JVM unit tests (domain).
3. Verify `VERSION` ↔ `versionName` / `versionCode`.
4. `assembleRelease` with restored keystore from secrets. Job fails if secrets missing (no debug-key fallback).
5. Upload APK artifact named `games-puzzle-X.Y.Z.apk`.

GitHub Release is created from CI artifacts (download run artifacts, `gh release create`), not from a local binary. Agent asks before publishing a release.

Local Gradle: used for unit tests if needed; **not** for shipping APK.

## Error handling

| Case | Behaviour |
|------|-----------|
| Catalog schema invalid | CI fails; app does not ship |
| Asset missing at runtime | skip that entry, log; catalog still opens |
| Shuffle / move bugs | domain tests must catch; invalid swaps never persist; play screen never writes a board the domain rejected |
| Gallery import fail | snackbar, no partial file |
| Update check offline | status «нет сети», changelog not claimed |
| APK download fail | status with retry; leftover incomplete file deleted |
| Install blocked | explain that unknown-sources install must be allowed for this app |
| Stats off | hide in-play timer/moves; win dialog and records still run |

## Testing

- Domain (JUnit, no Android): no empty cells; successful join snaps to home and locks; locked tiles never move or split; swap cannot displace locked tiles; non-joining swap persists and counts as a move; shuffle never identity, all unlocked, always has ≥1 joining swap; win = all home and locked; record comparison.
- Catalog fixture: at least one JSON example per category and season in tests; schema check in CI.
- Update parser: semver compare, changelog mapping from a fixture JSON of the Releases API.
- No merge-lockstep suite (single platform). Domain stays fixture-friendly if a second client appears later.

## Package layout

```
android/                       # Gradle project root (repo root may be this)
  app/src/main/java/.../
    domain/
    data/
    ui/
    platform/
  app/src/main/assets/catalog.json
  app/src/main/assets/puzzles/
  app/src/main/assets/thumbs/
  app/src/test/                # domain + parser tests
docs/schemas/catalog.schema.json
VERSION
.github/workflows/android.yml
```

If the Gradle root is the git root, omit the extra `android/` wrapper and keep `app/` at repo root. **Decision: Gradle at repo root** (`app/`, `gradle/`, `settings.gradle.kts`) so CI paths stay short.

## Image acquisition (content, not code)

Separate content pass after the app shell works:

1. Search Wikimedia Commons (and equivalent PD sources) for Russia-tagged photos in the six categories and four seasons.
2. Keep only licenses that allow redistribution; record `source_url`, author, license.
3. Convert to WebP, generate thumbs, append `catalog.json`.
4. Target 200–300 files. First playable milestone may ship a smaller subset (minimum 12: one per category×season cell is not required; minimum **12 photos** covering all six categories and all four seasons at least once) then grow to 200–300 without changing code.

## Success criteria

- Install APK from GitHub Release on Poco X7 / X8 Pro Max: catalog, filter, preview, chosen grid play, win.
- Import a photo, play, delete it; it is gone.
- Stats toggle hides or shows in-play timer and moves; win dialog and bests always run.
- Settings update check shows changelog; download starts only after explicit confirmation; APK installs. Startup auto-check runs only when the launch toggle is on and stays silent if already latest or offline.
- UI remains Russian; a second language can be added via resource files only.
- CI on GitHub produces the signed APK; no local release build in the normal workflow.
