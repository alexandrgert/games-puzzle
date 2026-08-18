# Games Puzzle — persist swaps, always record bests, replace catalog photos

Date: 2026-08-18  
Status: approved (awaiting implementation)  
Repo: https://github.com/alexandrgert/games-puzzle  
Amends: `docs/superpowers/specs/2026-08-17-games-puzzle-design.md`

## Goal

Three product changes on top of v0.3.0:

1. An unsuccessful join **does not roll back**. The player may drop an unlocked tile onto any other unlocked cell; the occupant moves to the origin cell. A correct edge still snaps the pair onto the finished-picture plan and locks them.
2. After a win, always show success, this run’s time and moves, and whether a best was beaten, plus **Ещё раз** / **В каталог**. Best time and best moves are stored for every solved run.
3. Replace **all 22** bundled photos with sharper, high-relief Russia pictures (less fog, bokeh, empty sky, or smooth water). Count stays 22.

## Out of scope

- Growing the catalog toward 200–300 (still a later content pass)
- Changing grid sizes (keep 5×5, 6×6, 8×8, 10×10, 12×12)
- Allowing drops onto locked cells
- Pause, undo, hints
- Local APK builds
- English UI

## Move rules

The board stays full (no empty cell). Only unlocked tiles move (tap–tap or drag onto another unlocked cell). Locked cells are never drop targets; that gesture does nothing.

After a swap of two unlocked cells:

1. If the two tiles now share a correct original edge → snap that pair to their home cells, lock them, displace any unlocked occupants of those homes into the cells the pair left. If a needed home still holds a **locked** tile, the swap is rejected (board unchanged).
2. Then every unlocked tile already in its home cell that orthogonally joins a locked neighbour locks in place (group does not move). Repeat until none remain, including tiles that were not part of the swap.
3. If neither a snap nor any new lock happened, the swapped positions **stay**. No red flash. No animation back.

Every accepted swap of two distinct unlocked cells is a **move** (join or not). Rejected gestures (same cell, out of bounds, locked target) are not moves.

Shuffle: not identity, all unlocked, and there exists at least one swap of unlocked cells that produces a **join** (snap or attach-to-locked). “Any swap succeeds” must not be treated as a join; solvability still means a locking join is possible.

Win: every tile is locked in its home cell.

## Time, HUD, records, win dialog

Time and moves are tracked on **every** run, from Запуск until the board is won.

| Surface | Behaviour |
|---------|-----------|
| Play HUD (ходы / время) | Visible only if `stats_enabled` is on |
| Win dialog | Always: title, message, this run’s time, this run’s moves. If this run beat stored best time and/or best moves for `(puzzle_id, grid n)`, show those congratulations. Buttons: Ещё раз, В каталог |
| Best results store | Always written on win. Key `(puzzle_id, grid n)` for n in {5,6,8,10,12}. Best time and best moves independent. User imports use `user:` prefix |

`stats_enabled` (default **off**) only hides the in-play counters. It does not hide the win stats and does not skip writing records.

Timer ticks while the play screen is open and the run is not yet won, even when the HUD is hidden (otherwise win-dialog time would freeze at 0 with stats off).

## Catalog photos

Replace every bundled puzzle and thumb. New stable `id`s. Delete old WebP files so Credits cannot list withdrawn licenses.

Keep **22** entries. Cover all six categories and all four seasons at least once. Themes: Russia only. Licenses: Wikimedia Commons Public Domain, CC0, CC BY, or CC BY-SA. Record `source_url` (Commons file page), author, license. Square 1200 px play image + 256 px thumb, WebP.

Selection bar (all must pass):

- sharp subject and texture across the crop (fur, bark, ice, rock, petals, feathers);
- no fog/mist wash, no photographic bokeh, no large empty sky, no “horizon splits a calm lake” frames.

Launcher icon: jigsaw-piece crop of **one new** catalog photo (not `dzhangyskol-autumn-altai`). Credits lists that derivative.

`catalog.json` schema unchanged. App code that reads the catalog does not need a format change.

## Domain / UI impact

- `MoveResult.Reverted` remains only for **invalid** swaps (same cell, OOB, locked). A non-joining valid swap is `Applied` with the swapped board and no new locks.
- `hasResultativeSwap` / shuffle must detect a swap that **locks or snaps**, not merely `Applied`.
- Remove reverted-flash UI (`PlayRevertedFlash`, red overlay, `lastReverted` attempt tiles).
- `PlaySession` increments `moves` on every `Applied` swap, regardless of `statsEnabled`.
- `PlayViewModel` always saves records on first transition to won; `RecordSaver` is required for play, not only when stats are on.
- Settings copy: the stats switch describes in-play counters only, not “disable records”.

## Testing

- Valid non-join swap persists; board differs from the pre-swap board; no new locks.
- Join still snaps to home and locks.
- Swap involving a locked cell is rejected; board unchanged; moves unchanged.
- `hasResultativeSwap` is false when every unlocked swap would only permute without a join.
- Shuffle still never identity and still has ≥1 joining swap.
- Moves increase on persist-swap and on join-swap.
- Win dialog path: time and moves present with `statsEnabled == false`; record merge still runs.
- Record merge still independent for time vs moves.
- Catalog: 22 entries, schema valid, every category and season present, old ids gone.

## Success criteria

- On device: drop two unlocked tiles that do not match; they stay swapped; a later correct edge still locks.
- Finish a puzzle with stats **off**: dialog shows time, moves, and catalog/again; a second faster or fewer-move run reports a new best.
- Catalog thumbs are the new 22; Credits and launcher icon match the new set; no leftover old files.

## Error handling

| Case | Behaviour |
|------|-----------|
| Drop on locked / invalid cell | Ignore; no move |
| Snap home occupied by locked tile | Reject swap; board unchanged |
| Catalog asset missing | Skip entry, log; catalog still opens |
| Wikimedia license unverifiable | Do not ship that file; pick another |
