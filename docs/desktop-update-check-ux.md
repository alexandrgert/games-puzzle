# Desktop UX: проверка обновлений и About

Обобщённые паттерны для desktop-приложений с дистрибуцией через GitHub Releases (или аналог).

## Цель

Пользователь может:

- включить/выключить **автопроверку** с интервалом в днях;
- нажать **«Проверить сейчас»** в **Настройках** и в **О программе**;
- увидеть понятный статус (актуальная версия / ошибка / доступно обновление);
- открыть страницу релиза или отложить («Позже»).

Один и тот же сценарий manual check **не дублируется** в двух диалогах — выносится в shared helper.

## Архитектура

```
update_check.py          # pure: fetch, parse semver, compare, UpdateCheckResult
app_prefs.py             # check_updates, interval, last_check, dismissed_version
ManualUpdateCheckHelper  # UI: button + status + background thread
  ├── SettingsDialog
  └── AboutDialog
MainWindow               # startup auto-check (respect_dismissed=True)
```

| Слой | Ответственность |
|------|-----------------|
| **Service** | HTTP к Releases API, semver compare, без UI |
| **Prefs** | когда запускать auto-check, какую версию пользователь «отложил» |
| **Helper** | async check, disable button, status colors, dialog «Открыть релиз» |
| **Shell** | Settings + About подключают один helper |

## Backend: check_for_update

Типичный контракт:

```python
@dataclass(frozen=True)
class UpdateCheckResult:
    ok: bool
    error: str = ""
    current_version: str = ""
    latest: LatestRelease | None = None
    update_available: bool = False
```

Правила:

- **semver compare** по tuple `(major, minor, patch, …)` — не строковое сравнение;
- `normalize_version("v1.2.3")` → `"1.2.3"`;
- сетевые/parse ошибки → `ok=False`, **не** падать в UI thread;
- `respect_dismissed=True` для **startup** auto-check: если пользователь нажал «Позlater» на эту версию — не считать update available;
- `respect_dismissed=False` для **manual** check: всегда показать, если remote новее.

Источник: GitHub Releases API `/releases/latest` или static manifest URL.

User-Agent: `<app-name>/<current-version>`.

## Prefs

| Поле | Назначение |
|------|------------|
| `check_updates` | вкл/выкл автопроверку |
| `update_check_interval_days` | период между auto-check |
| `last_update_check_at` | ISO timestamp последней проверки |
| `dismissed_update_version` | версия, которую пользователь отложил |

`should_run_auto_update_check(prefs)` — true если enabled и прошло ≥ interval с `last_update_check_at`.

`mark_update_check_done(prefs, dismissed_version=…)` — обновить timestamp и optional dismiss.

## Shared ManualUpdateCheckHelper

Один класс для Settings и About:

**Inputs:** parent widget, button, status label, prefs_loader, optional `on_status_changed`.

**Flow:**

1. `start()` — если thread уже running → return; disable button; status «Проверяю…».
2. Background thread вызывает `check_for_update(respect_dismissed=False)`.
3. `ok=False` → красный статус с текстом ошибки.
4. `update_available` → зелёный статус + `QMessageBox` / native dialog: **«Открыть релиз»** / **«Позже»**; при любом выборе — `mark_update_check_done` с `dismissed_version=latest`.
5. Иначе → «✓ Установлена актуальная версия».
6. `finished` → re-enable button.
7. `wait()` при закрытии диалога — дождаться thread (избежать use-after-close).

**Не дублировать** этот код в Settings и About — только wiring.

## About dialog

Минимальный состав:

- заголовок приложения;
- read-only блок: версия, пути данных, build info (без секретов);
- кнопка **«Проверить сейчас»** + status label;
- OK.

About — естественное место для manual check (пользователь ищет версию). Settings дублирует ту же кнопку для power users.

При `accept()` / `reject()` — `helper.wait()` перед destroy.

## Settings dialog

Секция «Приложение» / «Обновления»:

- checkbox «Проверять обновления»;
- spinbox «Период автопроверки» (дней), enabled только при checkbox;
- label с **текущей установленной версией**;
- те же **«Проверить сейчас»** + status через shared helper.

Сохранение prefs — вместе с остальными настройками приложения.

## Startup auto-check

На старте main window (debounced, один thread):

- `should_run_auto_update_check(prefs)` → skip если рано;
- `check_for_update(respect_dismissed=True)`;
- если update available → **tray notification** / toast: «Доступна версия X. Откройте Настройки → Приложение» (не modal dialog на старте);
- `mark_update_check_done` всегда после попытки (даже при ошибке сети).

Manual check в About/Settings может показать modal с кнопкой «Открыть релиз».

## UX: status label после async update

**Типичный баг:** status label инициализирован с `fixedHeight(0)` когда текст пустой (compact layout). После async check текст появляется, но **высота остаётся 0** — статус «невидим».

**Fix:**

1. При пустом тексте — `setFixedHeight(0)`.
2. При смене текста — сбросить max height, пересчитать wrapped height по ширине контента.
3. В Settings передать `on_status_changed=callback` → refit label после каждого `_set_status`.

About может обойтись без refit, если label не использует zero-height trick — но word wrap всё равно нужен.

Покрыть тестом: click «Проверить сейчас» → mock fetch → assert `status.height() > 0` и текст содержит «актуальн» / «версия».

## Цвета и тексты статуса

| Состояние | Цвет (пример) | Текст |
|-----------|---------------|-------|
| In progress | neutral gray | «Проверяю обновления…» |
| Up to date | green | «✓ Установлена актуальная версия» |
| Update available | green + dialog | «Доступна версия X (сейчас Y)» |
| Error | red | «✗ Не удалось проверить: …» |

Использовать короткие фразы; длинные ошибки сети — в status + optional tooltip.

## Тестирование

| Test | Что проверяет |
|------|----------------|
| Unit `update_check` | semver, dismiss logic, parse errors |
| Unit `should_run_auto_update_check` | interval, disabled flag |
| UI About | кнопка «Проверить сейчас» exists |
| UI Settings/About + mock | status visible after click, button re-enabled |
| Startup | не запускает второй thread если первый running |

Inject `fetch` / `opener` в `check_for_update` для tests без network.

## Mobile (Android/iOS)

- Play Store / App Store — свой update path; GitHub Releases check — **desktop-first**.
- На mobile: in-app «О приложении» с версией; update через store policies.
- Если нужен sideload APK: optional check тот же API, открытие browser на release page.

## Антипаттерны

- Два разных алгоритма compare в About vs Settings
- Modal blocking dialog на каждом startup
- Network call в UI thread
- Секреты или tokens в update check request
- Copy-paste 80 строк check logic в каждый dialog
- Status label с zero height без refit после async

## Checklist для агента

- [ ] `check_for_update` — pure, testable, no UI imports
- [ ] Shared helper wired in Settings **and** About
- [ ] Manual: `respect_dismissed=False`; startup: `respect_dismissed=True`
- [ ] Status refit callback в compact Settings layout
- [ ] Tests: unit + UI visibility after mock check
- [ ] Tray/toast on startup, modal only on manual «Открыть релиз»

## Связанные материалы

- Doc: `multi-platform-build-release.md` (CI → Release → версия для compare)
- Skill: `multi-platform-ci-release`
- Rule: `multi-platform-version-sync.mdc`
