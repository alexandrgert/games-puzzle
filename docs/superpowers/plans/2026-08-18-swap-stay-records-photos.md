# Persist swaps, always record bests, replace catalog photos

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Non-joining swaps stay on the board and count as moves; every win shows time/moves and writes bests; replace all 22 bundled photos with high-relief Commons images and a new launcher icon.

**Architecture:** Keep join/snap/lock in `BoardEngine`. A valid swap of two unlocked cells is always `MoveResult.Applied`; `joined` is true only when that swap snaps or attaches. Shuffle uses `joined`, not mere `Applied`. `PlaySession` always increments moves. `PlayViewModel` always ticks time and always saves records. Compose HUD stays gated on `stats_enabled`. Catalog files and `LAUNCHER_ICON_PUZZLE_ID` change; schema does not.

**Tech Stack:** Kotlin 2.0, JUnit 4, Jetpack Compose, Pillow/WebP for catalog import, Wikimedia Commons (PD/CC0/CC BY/CC BY-SA).

**Spec:** `docs/superpowers/specs/2026-08-18-swap-stay-records-photos.md`

## Global Constraints

- minSdk 26; compileSdk 36; targetSdk 36.
- applicationId / namespace: `ru.alexandrgert.gamespuzzle`.
- Builds: GitHub Actions only. Do not run `assembleRelease` / `assembleDebug` locally unless the user explicitly overrides. `./gradlew :app:testDebugUnitTest` is allowed.
- UI copy: Russian only in `app/src/main/res/values/strings.xml`. No user-visible string literals in Kotlin.
- Images: Wikimedia Commons PD / CC0 / CC BY / CC BY-SA only. Record Commons *file page* URL, author, license. No fog, bokeh, large empty sky, or calm-horizon lake splits.
- Do not commit unless the user explicitly asked to commit in this session.
- Do not bump `VERSION` or publish a GitHub Release in this plan.

---

## File map

| Path | Responsibility |
|------|----------------|
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Board.kt` | `MoveResult.Applied(board, joined)` |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/BoardEngine.kt` | persist non-join; shuffle via `joined` |
| `app/src/test/java/ru/alexandrgert/gamespuzzle/domain/BoardEngineTest.kt` | persist / join / locked / shuffle |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/PlaySession.kt` | always count moves; drop revert flash |
| `app/src/test/java/ru/alexandrgert/gamespuzzle/domain/PlaySessionTest.kt` | moves with stats off; persist swap |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModel.kt` | always save records; drop revert fields |
| `app/src/test/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModelTest.kt` | save when stats off |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/WinDialog.kt` | always time, moves, bests |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayScreen.kt` | always tick; no flash |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayRevertedFlash.kt` | delete |
| `app/src/test/java/ru/alexandrgert/gamespuzzle/ui/play/PlayRevertedFlashTest.kt` | delete |
| `app/src/main/res/values/strings.xml` | in-play stats label |
| `app/src/main/assets/catalog.json` | 22 new entries |
| `app/src/main/assets/puzzles/`, `thumbs/` | new WebP; delete old |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/credits/CreditsScreen.kt` | new `LAUNCHER_ICON_PUZZLE_ID` |
| `scripts/fetch_commons_puzzles.py` | download, square-crop, WebP |
| `scripts/catalog_sources.json` | Commons sources for the 22 |

---

### Task 1: Persist non-joining swaps in BoardEngine

**Files:**
- Modify: `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Board.kt`
- Modify: `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/BoardEngine.kt`
- Test: `app/src/test/java/ru/alexandrgert/gamespuzzle/domain/BoardEngineTest.kt`

**Interfaces:**
- Consumes: existing `trySwap`, `hasResultativeSwap`, `snapPairToHome`, `lockIfHomeAgainstLocked`
- Produces: `MoveResult.Applied(val board: Board, val joined: Boolean)`  
  `joined == true` only if the resulting board has a new snap/lock. Invalid swaps stay `MoveResult.Reverted`.  
  `hasResultativeSwap` returns true iff some unlocked pair yields `Applied(..., joined = true)`.

- [ ] **Step 1: Write the failing persist test and flip the old revert tests**

In `BoardEngineTest.kt` replace `swapNonJoiningTilesReverts` and `joiningPairWrongOrderDoesNotJoin` with:

```kotlin
@Test
fun swapNonJoiningTilesPersistsWithoutLocking() {
    val start = Board(
        GridSize.FIVE,
        intArrayOf(
            1, 0, 2, 3, 4,
            5, 6, 7, 8, 9,
            10, 11, 12, 13, 14,
            15, 16, 17, 18, 19,
            20, 21, 22, 23, 24,
        ),
        BooleanArray(25),
    )
    val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(4, 4))
    assertTrue(result is MoveResult.Applied)
    val applied = result as MoveResult.Applied
    assertTrue(!applied.joined)
    assertTrue(applied.board.tiles[0] == 24)
    assertTrue(applied.board.tiles[24] == 1)
    assertTrue(applied.board.locked.all { !it })
}

@Test
fun joiningPairWrongOrderPersistsWithoutJoin() {
    val start = BoardEngine.identityUnlocked(GridSize.FIVE)
    val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(0, 2))
    assertTrue(result is MoveResult.Applied)
    val applied = result as MoveResult.Applied
    assertTrue(!applied.joined)
    assertTrue(applied.board.tiles[0] == 2)
    assertTrue(applied.board.tiles[2] == 0)
}

@Test
fun hasResultativeSwapIgnoresPersistOnlySwaps() {
    val start = BoardEngine.identityUnlocked(GridSize.FIVE)
    val persist = BoardEngine.trySwap(start, Cell(0, 0), Cell(0, 2)) as MoveResult.Applied
    assertTrue(!persist.joined)
    assertTrue(BoardEngine.hasResultativeSwap(start))
}
```

Keep `swapLockedCellReverts` and `cannotSwapOntoLockedTile` as `Reverted`. Update every `MoveResult.Applied` smart-cast that reads `.board` — the constructor now has `joined`. Existing join tests must assert `applied.joined`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.domain.BoardEngineTest`

Expected: FAIL — `swapNonJoiningTilesPersistsWithoutLocking` still gets `Reverted`, and `Applied` has no `joined`.

- [ ] **Step 3: Minimal implementation**

In `Board.kt`:

```kotlin
sealed class MoveResult {
    data class Applied(val board: Board, val joined: Boolean) : MoveResult()
    data class Reverted(val board: Board) : MoveResult()
}
```

In `BoardEngine.trySwap`, keep invalid paths as `Reverted`. On join/snap/attach return `MoveResult.Applied(board, joined = true)`. Replace the final `else Reverted` with:

```kotlin
MoveResult.Applied(swapped, joined = false)
```

In `hasResultativeSwap`, change the check to:

```kotlin
val result = trySwap(board, cells[i], cells[j])
if (result is MoveResult.Applied && result.joined) return true
```

Fix every other `Applied(...)` call site in this file to pass `joined = true`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.domain.BoardEngineTest`

Expected: PASS. Then run full `./gradlew :app:testDebugUnitTest` — other modules will fail until Tasks 2–3 update `Applied` call sites / session tests. Fix only compile errors in those files (`joined` argument). Do not change session move-counting yet.

- [ ] **Step 5: Commit (only if the user asked)**

```bash
git add app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Board.kt \
  app/src/main/java/ru/alexandrgert/gamespuzzle/domain/BoardEngine.kt \
  app/src/test/java/ru/alexandrgert/gamespuzzle/domain/BoardEngineTest.kt
git commit -m "$(cat <<'EOF'
feat: keep non-joining tile swaps on the board

EOF
)"
```

---

### Task 2: Count every Applied swap; drop revert-flash session state

**Files:**
- Modify: `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/PlaySession.kt`
- Test: `app/src/test/java/ru/alexandrgert/gamespuzzle/domain/PlaySessionTest.kt`

**Interfaces:**
- Consumes: `MoveResult.Applied(board, joined)` from Task 1
- Produces: `PlaySession.swap` updates `board` and increments `moves` on every `Applied`, including `joined == false`. `statsEnabled` no longer gates moves. Remove `lastReverted`, `revertedA`, `revertedB`, `clearLastReverted`, and the swapped-tile preview in `tileShownAt`. Invalid `Reverted` still does not increment moves and does not change `board`.

- [ ] **Step 1: Write the failing session tests**

Replace `revertedSwapClearsSelectionWithoutCountingMove`, `revertedSwapShowsAttemptedTilesUntilCleared`, and `appliedSwapDoesNotCountWhenStatsDisabled` with:

```kotlin
@Test
fun persistSwapCountsMoveAndKeepsTiles() {
    val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(3L))
    val (first, second) = findPair(session.board, joined = false)
    val firstTile = session.board.tileAt(first)
    val secondTile = session.board.tileAt(second)

    val result = session.swap(first, second) as MoveResult.Applied

    assertTrue(!result.joined)
    assertEquals(secondTile, session.board.tileAt(first))
    assertEquals(firstTile, session.board.tileAt(second))
    assertEquals(1, session.moves)
    assertNull(session.selected)
}

@Test
fun persistSwapCountsMoveWhenStatsDisabled() {
    val session = PlaySession(GridSize.FIVE, statsEnabled = false, Random(4L))
    val (first, second) = findPair(session.board, joined = false)
    assertTrue(session.swap(first, second) is MoveResult.Applied)
    assertEquals(1, session.moves)
}

@Test
fun lockedSwapDoesNotCount() {
    val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(6L))
    val (first, second) = findPair(session.board, joined = true)
    session.swap(first, second)
    val locked = (0 until session.board.n * session.board.n)
        .map { Cell.fromIndex(it, session.board.n) }
        .first(session.board::isLockedCell)
    val unlocked = (0 until session.board.n * session.board.n)
        .map { Cell.fromIndex(it, session.board.n) }
        .first { !session.board.isLockedCell(it) }
    val movesBefore = session.moves
    val boardBefore = session.board
    assertTrue(session.swap(unlocked, locked) is MoveResult.Reverted)
    assertEquals(movesBefore, session.moves)
    assertSame(boardBefore, session.board)
}
```

Change `findPair` to:

```kotlin
private fun findPair(board: Board, joined: Boolean): Pair<Cell, Cell> {
    val cells = (0 until board.n * board.n).map { Cell.fromIndex(it, board.n) }
    for (first in cells) {
        for (second in cells) {
            if (first == second) continue
            val result = BoardEngine.trySwap(board, first, second)
            if (result is MoveResult.Applied && result.joined == joined) {
                return first to second
            }
        }
    }
    error("No matching pair")
}
```

Update `twoTapsApplySwapAndCountMoveWhenStatsEnabled` and `dragSwapAppliesJoinWithoutPriorSelection` to use `findPair(..., joined = true)` and drop `lastReverted` assertions.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.domain.PlaySessionTest`

Expected: FAIL — `persistSwapCountsMoveWhenStatsDisabled` still sees `moves == 0` and/or persist swap does not update `board`.

- [ ] **Step 3: Minimal implementation**

In `PlaySession.swap`:

```kotlin
when (result) {
    is MoveResult.Applied -> {
        board = result.board
        moves++
        if (isWin() && wonAtMs == null) wonAtMs = currentTimeMillis()
    }
    is MoveResult.Reverted -> Unit
}
```

Delete `lastReverted`, `revertedA`, `revertedB`, `clearLastReverted`. Make `tileShownAt` always `board.tileAt(cell)`. Clear the same fields in `tap`. Keep `statsEnabled` on the constructor (PlayViewModel still passes it) but do not use it for moves.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.domain.PlaySessionTest`

Expected: PASS.

- [ ] **Step 5: Commit (only if the user asked)**

```bash
git add app/src/main/java/ru/alexandrgert/gamespuzzle/domain/PlaySession.kt \
  app/src/test/java/ru/alexandrgert/gamespuzzle/domain/PlaySessionTest.kt
git commit -m "$(cat <<'EOF'
feat: count every accepted tile swap as a move

EOF
)"
```

---

### Task 3: Always save records and tick time

**Files:**
- Modify: `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModel.kt`
- Modify: `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayScreen.kt` (tick loop only)
- Test: `app/src/test/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModelTest.kt`

**Interfaces:**
- Consumes: `PlaySession` from Task 2 (no revert fields)
- Produces: On first `won`, always call `RecordSaver.save`. Timer `tick` runs even when `statsEnabled` is false. `PlayState` drops `lastReverted`, `revertedA`, `revertedB`.

- [ ] **Step 1: Write the failing ViewModel test**

Replace `winningWithStatsDisabledDoesNotWaitForRecordSave` with:

```kotlin
@Test
fun winningWithStatsDisabledSavesRecord() {
    val releaseSave = CompletableDeferred<Unit>()
    val update = RecordUpdate(
        record = BestRecord(bestTimeMs = 400L, bestMoves = 3),
        improvedTime = true,
        improvedMoves = true,
    )
    val saver = FakeRecordSaver(releaseSave, update)
    val viewModel = PlayViewModel(
        statsEnabled = false,
        puzzleId = "puzzle-2",
        recordSaver = saver,
    )
    viewModel.start(GridSize.FIVE, Random(5L))
    solve(viewModel)
    assertTrue(viewModel.state!!.won)
    assertTrue(viewModel.state!!.recordSavePending)
    assertEquals(1, saver.calls)
    releaseSave.complete(Unit)
    assertFalse(viewModel.state!!.recordSavePending)
    assertSame(update, viewModel.state!!.recordUpdate)
}
```

Delete `clearsRevertedSwapSignalAfterUiHandlesIt` and `findRevertedPair`. Remove revert fields from any `PlayState` assertions. Keep `elapsedTimeUpdatesOnTickWhileIdle` but also add:

```kotlin
@Test
fun elapsedTimeTicksWhenStatsDisabled() {
    var now = 1_000L
    val viewModel = PlayViewModel(statsEnabled = false, currentTimeMillis = { now })
    viewModel.start(GridSize.FIVE, Random(1L))
    now = 1_500L
    viewModel.tick()
    assertEquals(500L, viewModel.state!!.elapsedMs)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.ui.play.PlayViewModelTest`

Expected: FAIL — `saver.calls == 0` when stats are off.

- [ ] **Step 3: Minimal implementation**

In `PlayViewModel.publish`:

```kotlin
if (won && previous?.won != true) saveRecord(current)
```

`saveRecord` stays as-is (`recordSaver` required). Drop revert fields from `PlayState` and `publish`. Delete `clearLastReverted`.

In `PlayScreen.kt` change the tick effect to always run:

```kotlin
LaunchedEffect(playViewModel) {
    while (true) {
        delay(250)
        val current = playViewModel.state ?: continue
        if (current.won) break
        playViewModel.tick()
    }
}
```

Do not remove the flash UI yet (Task 4). If `PlayState` no longer has revert fields, comment out flash reads so the module compiles, or jump to Task 4 in the same change set if compile forces it.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.ui.play.PlayViewModelTest`

Expected: PASS.

- [ ] **Step 5: Commit (only if the user asked)**

```bash
git add app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModel.kt \
  app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayScreen.kt \
  app/src/test/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModelTest.kt
git commit -m "$(cat <<'EOF'
feat: save bests and tick time on every run

EOF
)"
```

---

### Task 4: Win dialog always shows stats; remove rollback flash

**Files:**
- Modify: `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/WinDialog.kt`
- Modify: `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Delete: `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayRevertedFlash.kt`
- Delete: `app/src/test/java/ru/alexandrgert/gamespuzzle/ui/play/PlayRevertedFlashTest.kt`

**Interfaces:**
- Consumes: `PlayState.won`, `elapsedMs`, `moves`, `recordUpdate` (always populated after Task 3)
- Produces: Win dialog always shows `win_time` and `win_moves`. New-best lines follow `recordUpdate`. No red flash. Settings label says the toggle is in-play only.

- [ ] **Step 1: Change WinDialog (no Android UI test harness — cover via existing ViewModel + strings)**

`WinDialog` signature:

```kotlin
fun WinDialog(
    elapsedMs: Long,
    moves: Int,
    recordUpdate: RecordUpdate?,
    navigationEnabled: Boolean,
    onAgain: () -> Unit,
    onCatalog: () -> Unit,
)
```

Always:

```kotlin
Text(stringResource(R.string.win_message))
Text(stringResource(R.string.win_time, elapsedMs / 1_000))
Text(stringResource(R.string.win_moves, moves))
if (recordUpdate?.improvedTime == true) {
    Text(stringResource(R.string.win_new_best_time))
}
if (recordUpdate?.improvedMoves == true) {
    Text(stringResource(R.string.win_new_best_moves))
}
```

Call site in `PlayScreen` drops `statsEnabled = ...`.

- [ ] **Step 2: Remove flash UI**

Delete `PlayRevertedFlash.kt` and `PlayRevertedFlashTest.kt`. In `PlayScreen.kt` remove `revertedFlash`, the `LaunchedEffect(state?.lastReverted)` block, `isRevertedFlashCell` usage, and the `tileShownAt` revert branch. `tileShownAt` becomes `state.board.tileAt(cell)`.

- [ ] **Step 3: Settings copy**

In `strings.xml` set:

```xml
<string name="settings_stats">Показывать время и ходы во время игры</string>
```

The switch still binds to `statsEnabled` and still hides only the play HUD (`if (statsEnabled) { Text(play_stats) }`).

- [ ] **Step 4: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: PASS, including no missing `PlayRevertedFlashTest`.

- [ ] **Step 5: Commit (only if the user asked)**

```bash
git add app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play \
  app/src/main/res/values/strings.xml \
  app/src/test/java/ru/alexandrgert/gamespuzzle/ui/play
git commit -m "$(cat <<'EOF'
feat: always show win stats and drop swap rollback flash

EOF
)"
```

---

### Task 5: Replace all 22 catalog photos

**Files:**
- Create: `scripts/catalog_sources.json`
- Create: `scripts/fetch_commons_puzzles.py`
- Modify: `app/src/main/assets/catalog.json`
- Modify: `app/src/main/assets/puzzles/*.webp`, `app/src/main/assets/thumbs/*.webp`
- Test: add `app/src/test/java/ru/alexandrgert/gamespuzzle/data/ShippedCatalogTest.kt`

**Interfaces:**
- Consumes: unchanged `CatalogFile` / `catalog.schema.json`
- Produces: exactly 22 puzzles; new ids; no old ids; every `Category` and every non-`ANY` `Season` present; matching WebP files; Credits still render from catalog metadata.

Old ids that must disappear:

`altai-spring-nature`, `kamchatka-geysers-summer`, `moscow-autumn-forest`, `baikal-winter-ice`, `upornyj-amur-tiger`, `novaya-zemlya-arctic-fox`, `abakan-stellers-eagle`, `kamchatka-harlequin-duck`, `baikal-summer-shore`, `olkhon-autumn-forest`, `volga-lotus-summer`, `moscow-lilac-spring`, `olkhon-ice-cave-winter`, `dzhangyskol-autumn-altai`, `altai-birch-spring`, `ladoga-pine-skerry`, `autumn-asters`, `kamchatka-brown-bear`, `baikal-nerpa`, `chistopol-bullfinch-winter`, `krimozero-autumn`, `lena-pillars-summer`

- [ ] **Step 1: Write ShippedCatalogTest (fails on old ids)**

```kotlin
package ru.alexandrgert.gamespuzzle.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Category
import ru.alexandrgert.gamespuzzle.domain.Season

class ShippedCatalogTest {
    private val assets = File("src/main/assets")

    @Test
    fun shippedCatalogHasTwentyTwoHighReliefEntries() {
        val catalog = CatalogJson.parse(File(assets, "catalog.json").readText())
        assertEquals(22, catalog.puzzles.size)
        assertEquals(catalog.puzzles.map { it.id }.toSet().size, 22)
        assertEquals(Category.entries.toSet(), catalog.puzzles.map { it.category }.toSet())
        assertTrue(catalog.puzzles.map { it.season }.containsAll(Season.entries - Season.ANY))
        val oldIds = setOf(
            "altai-spring-nature", "kamchatka-geysers-summer", "moscow-autumn-forest",
            "baikal-winter-ice", "upornyj-amur-tiger", "novaya-zemlya-arctic-fox",
            "abakan-stellers-eagle", "kamchatka-harlequin-duck", "baikal-summer-shore",
            "olkhon-autumn-forest", "volga-lotus-summer", "moscow-lilac-spring",
            "olkhon-ice-cave-winter", "dzhangyskol-autumn-altai", "altai-birch-spring",
            "ladoga-pine-skerry", "autumn-asters", "kamchatka-brown-bear",
            "baikal-nerpa", "chistopol-bullfinch-winter", "krimozero-autumn",
            "lena-pillars-summer",
        )
        assertTrue(catalog.puzzles.none { it.id in oldIds })
        catalog.puzzles.forEach { puzzle ->
            assertTrue(File(assets, puzzle.file).isFile)
            assertTrue(File(assets, puzzle.thumb).isFile)
            assertTrue(puzzle.sourceUrl.startsWith("https://commons.wikimedia.org/wiki/File:"))
            assertTrue(!puzzle.sourceUrl.contains("File%3A"))
        }
    }
}
```

Gradle unit tests run with cwd `app/`, so `src/main/assets` is correct.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.data.ShippedCatalogTest`

Expected: FAIL — old ids still present.

- [ ] **Step 3: Write `scripts/catalog_sources.json` and the fetch script**

`catalog_sources.json` is an array of 22 objects:

```json
{
  "id": "ruskeala-marble-summer",
  "title_ru": "Мраморный карьер Рускеала",
  "category": "nature",
  "season": "summer",
  "commons_file": "File:Ruskeala_Marble_Canyon.jpg"
}
```

Fill 22 rows covering all six categories and all four seasons. Prefer close, textured subjects in Russia (rock, ice, fur, bark, petals, feathers). Reject Commons pages that are PD-ineligible, foggy, bokeh, or mostly sky/water.

`scripts/fetch_commons_puzzles.py` (complete):

```python
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
```

Run: `python3 scripts/fetch_commons_puzzles.py`  
If a file fails license/size/quality, replace that one row in `catalog_sources.json` and re-run. Do not keep any old WebP.

Visually reject (open thumbs): fog wash, bokeh, empty sky, split calm lake. Substitute before finishing the task.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.data.ShippedCatalogTest`

Expected: PASS. Also `./gradlew :app:testDebugUnitTest` green.

- [ ] **Step 5: Commit (only if the user asked)**

```bash
git add scripts/catalog_sources.json scripts/fetch_commons_puzzles.py \
  app/src/main/assets app/src/test/java/ru/alexandrgert/gamespuzzle/data/ShippedCatalogTest.kt
git commit -m "$(cat <<'EOF'
content: replace bundled catalog with high-relief Commons photos

EOF
)"
```

---

### Task 6: Launcher icon from a new catalog photo

**Files:**
- Modify: `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/credits/CreditsScreen.kt`
- Modify: `app/src/test/java/ru/alexandrgert/gamespuzzle/ui/credits/CreditsFormattingTest.kt`
- Modify: `app/src/main/res/mipmap-*/ic_launcher_foreground.webp` and `ic_launcher.webp` / `ic_launcher_round.webp`
- Modify: spec line that forbids `dzhangyskol-autumn-altai` (already in 2026-08-18 spec)

**Interfaces:**
- Consumes: one new catalog id from Task 5 (pick a colourful textured nature/animal frame, e.g. the marble quarry or a close animal)
- Produces: `LAUNCHER_ICON_PUZZLE_ID` equals that id; Credits test uses it; mipmap webp is a jigsaw-piece crop with transparent corners (not an opaque square)

- [ ] **Step 1: Failing credits test**

```kotlin
private const val ICON_ID = "ruskeala-marble-summer" // use the actual chosen id

@Test
fun launcherIconPuzzle_usesNewCatalogEntry() {
    val icon = puzzle(ICON_ID)
    val other = puzzle("other-id")
    assertEquals(icon, launcherIconPuzzle(listOf(other, icon)))
    assertEquals(null, launcherIconPuzzle(listOf(other)))
}
```

Remove `launcherIconPuzzle_usesDzhangyskolCatalogEntry`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests ru.alexandrgert.gamespuzzle.ui.credits.CreditsFormattingTest`

Expected: FAIL — still `dzhangyskol-autumn-altai`.

- [ ] **Step 3: Point Credits at the new id and rebuild mipmaps**

Set `const val LAUNCHER_ICON_PUZZLE_ID` to the chosen id.

Generate foreground: center-crop the 1200 WebP, mask a jigsaw-piece silhouette (or inset circle-ish puzzle tab using Pillow), export:

| density | foreground px |
|---------|----------------|
| mdpi | 108 |
| hdpi | 162 |
| xhdpi | 216 |
| xxhdpi | 324 |
| xxxhdpi | 432 |

Keep adaptive background `#0E3A45` unless the new crop needs a different solid that still shows through transparent corners. Copy foreground into `ic_launcher.webp` and `ic_launcher_round.webp` for legacy mipmaps.

- [ ] **Step 4: Run tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: PASS. Confirm `docs/superpowers/specs/2026-08-17-games-puzzle-design.md` no longer names `dzhangyskol-autumn-altai` as the live icon (already amended). Update README only if a title list is hardcoded (it is not).

- [ ] **Step 5: Commit (only if the user asked)**

```bash
git add app/src/main/java/ru/alexandrgert/gamespuzzle/ui/credits/CreditsScreen.kt \
  app/src/test/java/ru/alexandrgert/gamespuzzle/ui/credits/CreditsFormattingTest.kt \
  app/src/main/res
git commit -m "$(cat <<'EOF'
feat: derive launcher icon from the new catalog photo

EOF
)"
```

---

## Self-review

1. **Spec coverage:** persist swap → Task 1; move counting → Task 2; records + tick → Task 3; win dialog + no flash + settings copy → Task 4; 22 photos → Task 5; new icon → Task 6. Locked drop ignored → Tasks 1–2. Shuffle uses join not Applied → Task 1 `joined`.
2. **Placeholders:** none. Catalog file names are chosen at Task 5 against the stated bar; old ids are listed verbatim.
3. **Types:** `MoveResult.Applied(board, joined)` is used the same way in Tasks 1–2.
