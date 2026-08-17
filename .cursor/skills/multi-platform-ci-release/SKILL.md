---
name: multi-platform-ci-release
description: >-
  Multi-platform build matrix in GitHub Actions, version sync before push,
  download CI artifacts and publish GitHub Release. Use when shipping desktop +
  mobile + Linux packaging or preparing agent-driven releases.
---

# Multi-platform CI and GitHub Release

## Build topology

Typical CI after `test` job:

| Job | Host | Output |
|-----|------|--------|
| `build-primary` | Linux | primary desktop package (e.g. `.deb`) + optional Linux extras matrix |
| `build-mobile` | Linux/macOS | signed `.apk` / `.aab` |
| `build-desktop-other` | Windows / macOS | `.exe`, `.zip` |

**Local dev:** build **one** fast artifact for the developer's machine (e.g. `.deb` on Linux). **Do not** run full matrix locally unless user explicitly overrides.

Flow for «build all platforms»:

1. Finish code + tests.
2. Align version fields across platforms (see rule `multi-platform-version-sync`).
3. Commit + push → CI produces artifacts.
4. Download artifacts → `gh release create` with notes file.

## Version source of truth

One file holds semver for **naming** all artifacts (e.g. `pyproject.toml`, `package.json`, `VERSION`).

Platform-specific files (Android `versionName`/`versionCode`, Windows resource version) must match before push. CI check script exits non-zero on mismatch.

## CI version bump

CI builds often set `NO_BUMP=1` / `VERSION=from file` so pipeline does not mutate repo during build.

Local release builds **may** auto-bump patch when code changed — product policy in rule `local-primary-build-ci-matrix`.

## GitHub Release steps (agent)

1. Wait for green workflow on target commit.
2. `gh run download <run-id> --dir staging/`
3. Verify file list (`*-VERSION-*` naming).
4. `gh release create vX.Y.Z --notes-file docs/github-release-vX.Y.Z.md --target main <files...>`
5. Confirm assets on release page.

Draft release notes in repo (`docs/github-release-v*.md`) before publishing; link to full `release-notes-v*.md`.

## Android signing in CI

- **Never** generate a new release keystore per CI run.
- Restore keystore from GitHub Secrets (`KEYSTORE_BASE64`, passwords, alias).
- Fail build if secrets missing in CI (see doc `android-release-signing-ci.md`).
- Document one-time uninstall for users stuck on old ephemeral signing.

## Linux packaging matrix (optional)

From one PyInstaller/desktop bundle, stage and produce: deb, rpm, tar.xz, AppImage, Flatpak, Snap, niche formats — **CI only**.

Pin third-party packager downloads (SHA-256) in workflow.

## Agent guardrails

- Ask before local primary build after code changes.
- Do not commit secrets or keystore files.
- Do not force-push `main` for release fixes — new commit instead.

## See also

- Rules: `multi-platform-version-sync.mdc`, `local-primary-build-ci-matrix.mdc`, `github-release-from-artifacts.mdc`
- Doc: `docs/multi-platform-build-release.md`
