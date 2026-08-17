# Multi-platform build and release

Обобщённый процесс сборки и публикации desktop + mobile + Linux packaging matrix.

## Topology CI

После job `test`:

| Job | Host | Output |
|-----|------|--------|
| `build-primary` | Linux | primary desktop (`.deb`, …) + optional Linux extras |
| `build-mobile` | Linux | signed `.apk` / `.aab` |
| `build-desktop-other` | Windows / macOS | `.exe`, `.zip` |

Локально — **один** primary артефакт для smoke на машине разработчика.

## Version source of truth

Один файл semver для **именования** всех артефактов:

- Python: `pyproject.toml` → `[project].version`
- Node: `package.json` → `version`
- Generic: `VERSION`

Platform-specific:

- Android: `versionName`, `versionCode` (монотонно)
- iOS: CFBundleShortVersionString / build number
- Windows: resource version (если есть)

Скрипт `check_version_sync.py` (или аналог) — exit 1 при расхождении. CI step **перед** mobile build.

## Local build policy

| Ситуация | Bump |
|----------|------|
| Есть продуктовые изменения | patch (или minor/major по типу) |
| Повторная сборка того же кода | спросить: bump или `NO_BUMP=1` |
| CI pipeline | `NO_BUMP=1`, версия из файла |

## Flow «все платформы»

1. Код + тесты готовы.
2. Выровнять версии (`check_version_sync.py` → OK).
3. Commit + push.
4. Дождаться green CI.
5. `gh run download` → staging.
6. `gh release create vX.Y.Z --notes-file docs/github-release-vX.Y.Z.md --target main <files>`.

## Release notes

| Файл | Назначение |
|------|------------|
| `docs/github-release-vX.Y.Z.md` | краткий текст на странице Release |
| `docs/release-notes-vX.Y.Z.md` | подробности, migration, signing |

## Linux packaging matrix (optional)

Из одного desktop bundle (PyInstaller, Tauri, …) в CI:

deb, rpm, tar.xz, tgz, AppImage, Flatpak, Snap, niche formats (ebuild, …).

Pin SHA-256 сторонних packager downloads в workflow.

## Update check in app

Подробнее: [`desktop-update-check-ux.md`](desktop-update-check-ux.md).

Desktop/mobile могут проверять GitHub Releases API или static manifest:

- Сравнение semver с установленной версией.
- Кнопка «Проверить сейчас» в Settings и About — общий helper.
- После async check — refit layout status label (избегать `fixedHeight(0)` после смены текста).

## Agent guardrails

- Спросить перед local primary build.
- Не коммитить keystore / `.env`.
- Не force-push `main`.
- Commit/push — только по запросу пользователя.

## Связанные материалы

- Skill: `multi-platform-ci-release`
- Rules: `multi-platform-version-sync.mdc`, `local-primary-build-ci-matrix.mdc`, `github-release-from-artifacts.mdc`
- Doc: `android-release-signing-ci.md`, `desktop-update-check-ux.md`
