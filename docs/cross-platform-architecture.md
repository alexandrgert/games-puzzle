# Кросс-платформенная архитектура приложения

Обобщённая модель для **desktop (Win/macOS/Linux) + mobile (Android/iOS)** с offline-first данными и file-sync.

## Принцип

**Один контракт данных, несколько оболочек.**

| Слой | Содержимое | Типичная реализация |
|------|------------|---------------------|
| **Domain** | модели, merge, бизнес-правила | `domain/` — без I/O и UI |
| **Application** | orchestration, save/load, sync hooks | controller / use-cases |
| **Adapters** | файлы, WebDAV, внешние API | storage, sync, integrations |
| **Shell** | окна, tray, notifications | UI-модуль |
| **Platform** | пути, OS-интеграции, secure storage | `platform_paths`, keystore |

Mobile — **отдельные нативные клиенты** (Kotlin/Swift), не порт desktop runtime (PySide/Electron). Общее: **JSON schema** и **правила merge**, проверенные shared fixtures.

## Пути данных (desktop)

Единый модуль путей (`platform_paths` или аналог):

| Назначение | Linux | macOS | Windows |
|------------|-------|-------|---------|
| Данные приложения | `~/.local/share/<app>/` | `~/Library/Application Support/<app>/` | `%LOCALAPPDATA%\<app>\` |
| Секреты / конфиг | `~/.config/<app>/` | то же | `%APPDATA%\<app>\` |

Файлы секретов **не синхронизируются** (webhooks, пароли sync, API keys).

## Что в sync payload vs local

| Синхронизируется | Только локально |
|------------------|-----------------|
| пользовательские данные (задачи, сессии, UI prefs без секретов) | пароли, токены, webhook URL |
| маркеры версии схемы | device-specific config |
| | signing keys, `.env` |

Контракт: `docs/data-schema.md` + JSON Schema в CI.

## Desktop сборка

| Платформа | Локально (dev) | CI |
|-----------|----------------|-----|
| Linux | primary пакет (часто `.deb`) | полная матрица: deb, rpm, AppImage, Flatpak, Snap, … |
| Windows | — | `.exe` |
| macOS | — | `.app` / zip |

**Правило:** локально — один быстрый артефакт; полная матрица — только GitHub Actions после push.

## Mobile (план)

| | Android | iOS |
|---|---------|-----|
| UI | Kotlin + Compose | Swift + SwiftUI |
| Хранилище | Room/SQLite + export JSON | SwiftData / SQLite |
| Секреты | EncryptedSharedPrefs / Keystore | Keychain |
| Фон | Foreground Service + notification | Live Activity + BG tasks |
| Sync | тот же merge по контракту | то же |

Python/desktop-код **не портируется**; портируются **схема** и **merge** с lockstep-тестами.

## Дорожная карта (типовая)

### Фаза A — desktop

- [ ] `platform_paths` / аналог
- [ ] `domain/` без UI framework
- [ ] JSON Schema для payload
- [ ] primary local build + CI matrix

### Фаза B — mobile MVP

- Задачи, таймер, sync, базовый import/export

### Фаза C — паритет

- Widgets, push, UI разрешения конфликтов sync

## Структура пакетов (пример)

```
src/<app>/
  domain/          # merge, models — pure Python/Rust/Go
  storage.py       # atomic save, backup
  sync/            # WebDAV adapter
  platform_paths.py
  main_window.py   # shell
android/           # Kotlin client
ios/               # Swift client (optional)
tests/fixtures/merge_lockstep/
docs/schemas/data.schema.json
```

## Антипаттерны

- Один desktop binary «как mobile app»
- PWA как единственный mobile client при жёстких требованиях к фону
- Секреты внутри sync-файла
- Merge только на одной платформе без shared fixtures

## Связанные материалы

- Skill: `cross-platform-data-contract`
- Doc: `offline-sync-patterns.md`, `merge-lockstep-across-platforms.md`
- Rule: `multi-platform-version-sync.mdc`
