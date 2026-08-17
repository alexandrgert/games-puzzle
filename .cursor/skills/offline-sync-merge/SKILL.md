---
name: offline-sync-merge
description: >-
  Implement offline-first file sync (WebDAV or similar): pull-before-push, content
  hash conflicts, session/task merge, periodic remote checks with user prompt.
  Use when building multi-device sync, merge algorithms, or sync UX (Later / pull now).
---

# Offline sync and merge

## Transport pattern

File-based sync over HTTPS (WebDAV, S3-compatible, etc.):

| Remote file | Role |
|-------------|------|
| `data.json` (or app-specific name) | canonical payload |
| `data.sync-meta.json` | content hash, revision, device id |

Local credentials live **outside** the payload (e.g. `webdav.json` with 0600 perms).

## Pull (download and merge)

1. Download payload + meta (if present).
2. If remote `content_hash` ≠ last known hash → record **conflict** (still merge, notify user).
3. Merge local and remote:
   - entities keyed by stable **id**;
   - **union** child records by id where possible;
   - on same id with divergent fields → **richer-wins** rules (document explicitly);
   - pick «richest» UI block from file with more entities / larger payload.
4. **Normalize** global invariants (e.g. only one «running» timer — demote extras to paused).
5. Save locally; update `last_remote_content_hash` on successful full sync.

## Push (upload)

**Always pull-before-push** when remote file exists:

1. Download + merge as above.
2. Normalize.
3. Upload payload + meta (retry meta; on partial failure do not advance local hash).
4. Update `last_remote_content_hash` only after success.

Never blind overwrite remote if another device may have written.

## Periodic check (not auto-merge)

When interval > 0:

- Compare remote hash to local `last_remote_content_hash`.
- If changed → prompt: **Merge now** / **Later** (with remind interval).
- «Later» binds to **that** remote hash; new hash on server resets timer and shows prompt immediately.

Background checks may run in tray / WorkManager with OS minimum interval (e.g. 15 min on Android).

## Reconnect

On offline → online: debounced push (pull-before-push). Cooldown to avoid storms. Log failures; success can be silent.

## User-facing manual actions

- **Download and merge** — works even when auto-sync checkbox is off (credentials sufficient).
- **Upload now** — pull-before-push then upload.
- **Sync log** — local JSONL ring buffer; not synced.

## Conflict footguns

- Same entity edited **simultaneously** on two devices → same child id may lose one side's fields. **Do not** run the same live timer on two devices.
- `shutdown upload only` mode: uploads local copy without merge — can clobber newer remote; warn on next startup.

## Agent tasks

When implementing sync:

1. Read existing merge module; add tests **before** changing pick logic.
2. Update user docs and technical doc together.
3. Do not store sync password in payload.

## See also

- Skill: `merge-lockstep-testing`
- Doc: `docs/offline-sync-patterns.md`
