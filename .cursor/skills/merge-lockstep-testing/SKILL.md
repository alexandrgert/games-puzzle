---
name: merge-lockstep-testing
description: >-
  Keep merge/conflict resolution identical across platforms using shared JSON
  fixtures and parallel unit tests (Python + Kotlin/Swift). Use when changing
  merge.py/DataMerge or documenting sync limitations.
---

# Merge lockstep testing

## Goal

Desktop and mobile must pick the **same** winner when two copies of the same record id diverge. Users trust sync; silent divergence erodes that trust.

## Fixture layout

```
tests/fixtures/merge_lockstep/
  case_01_equal_duration_bitrix_wins.json   # input local + remote
  case_01_expected.json                     # golden output
android/app/src/test/resources/merge_lockstep/   # same files copied
```

One canonical directory in repo; mobile tests **load the same JSON** as desktop.

## Algorithm documentation

Write the pick order in plan + tech doc **before** coding, e.g. for timer sessions with same id:

1. Closed session beats open.
2. Longer duration wins.
3. Meta score: external id (+2), non-empty comment (+1).
4. Tie → **candidate** (consistent traversal order on both platforms).

Both implementations must match; if tie-break differs, tests fail.

## Test layers

| Layer | Tool |
|-------|------|
| Pure merge functions | pytest / JUnit |
| Fixture golden files | both platforms |
| Optional property tests | same invariants |

## When changing merge

1. Add failing fixture test on **both** platforms.
2. Fix desktop + mobile in same PR when possible.
3. Update user-facing «limitations» (same session id conflict).
4. Do not bump release version in merge-only doc/test PR unless product policy says otherwise.

## CI

Run Python merge tests on every push. Android unit tests in `build-apk` job or dedicated test job.

## See also

- Doc: `docs/merge-lockstep-across-platforms.md`
- Skill: `offline-sync-merge`
