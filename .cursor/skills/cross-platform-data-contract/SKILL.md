---
name: cross-platform-data-contract
description: >-
  Design and maintain a shared data contract across desktop and mobile clients:
  one JSON schema, domain layer without UI, platform-specific shells and secrets.
  Use when adding mobile, sync, or refactoring monolith UI into domain/adapters/shell.
---

# Cross-platform data contract

## Principle

**One data contract, multiple shells.**

| Layer | Responsibility | Typical location |
|-------|----------------|------------------|
| **Domain** | models, merge, queries, business rules | `domain/` — **no I/O, no UI framework** |
| **Application** | orchestration, save/load, sync hooks | `controller` / `use-cases` |
| **Adapters** | filesystem, HTTP/WebDAV, external APIs | `storage`, `sync`, `integrations` |
| **Shell** | windows, tray, notifications | UI module |
| **Platform** | paths, OS hooks, secure storage | `platform_paths`, keystore |

Mobile clients are **native** (Kotlin/Swift), not a port of the desktop runtime. They implement the **same JSON schema** and **same merge rules**, verified by shared test fixtures.

## What goes in sync payload vs local secrets

| In shared file (sync) | Local only (never sync) |
|-----------------------|-------------------------|
| user/domain data (tasks, sessions, UI prefs without secrets) | API keys, webhooks, sync passwords |
| schema version markers | device-specific config |
| non-sensitive app state | signing keys, `.env` |

Document the split in `docs/data-schema.md` + JSON Schema when possible.

## Platform paths

Single module resolves OS-specific directories:

- user data (app state file)
- config / secrets (0600 files, 0700 dirs)
- optional `.env` for dev

Never hardcode `~/.config/...` in business logic.

## Migration

- Bump `schema_version` in persisted state.
- Run migration **once** on load, before daily rollover / sync.
- Keep backward compatibility tests for N-1 schema.

## Anti-patterns

- One desktop binary on mobile (PySide/Electron as «mobile app»)
- PWA as sole mobile client when reliable background timer/sync is required
- Storing integration secrets inside the sync file
- Duplicating merge logic only on one platform without lockstep tests

## Checklist

- [ ] Domain module has zero imports from UI framework
- [ ] JSON Schema or equivalent contract checked in CI
- [ ] Secrets in platform secure store / chmod 0600 files
- [ ] Mobile reimplements merge with **same fixtures** as desktop (see `merge-lockstep-testing`)

## See also

- Rule: `multi-platform-version-sync.mdc`
- Doc: `docs/cross-platform-architecture.md`
