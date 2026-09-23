<!-- cloudcli-platform:bitrix24:start -->
## Managed CloudCLI project architecture

Project type: `ordinary`. Authorization mode: `none`. Application state: `not_configured`. Agent channel state: `not_configured`.
The business task is intentionally not generated here; it will arrive in a separate user message.
Never read, print, log, paste, commit or copy credential files, *_FILE values, OAuth tokens, client secrets or webhooks.
Use external_api_request for configured external APIs. Credentials are injected by the privileged platform boundary.
For live MVP work, the supervisor selects package.json scripts.mvp, then scripts.dev, then scripts.start. The child server must bind 0.0.0.0 and use the PORT supplied by the platform; never bind the public supervisor port yourself.
Plain Node must use scripts.start: node server.js without scripts.mvp or scripts.dev, which take precedence and disable the supervisor's source watcher. Commands in mvp/dev must provide their own working reload; preserve a framework's own HMR without adding another watcher. Never put plain node server.js in mvp/dev.
After changing the live command, run executable HTTP checks for entrypoint and imported-module edits, atomic replacement followed by another edit, and syntax-error recovery without stale successful responses. Do not infer working reload from a script name or the supervisor health endpoint.
After an agent source edit, verify the result at the same public URL without Build or Publish. Build and Publish remain explicit stable-release actions, not part of the live edit loop.
Keep package.json and package-lock.json synchronized. After changing dependencies, run npm install in the worker so node_modules and the lockfile match, then verify the live URL. The live runtime never installs dependencies; use npm ci for a clean build check.
Within the user's requested business scope, you are authorized without additional confirmation to create, edit, rename and remove project-owned source, tests, assets and configuration inside this exact project root; manage npm dependencies; and run tests, linters and live verification. Preserve platform-managed blocks and adapters, neighboring projects and all paths outside this project root.
This ordinary Node.js application is directly public and has no Bitrix24 runtime or credential channel.
## Platform schedules

Scheduled work is an internal application endpoint, never a public API. First implement the handler below /internal/jobs/ and keep it free of platform credentials. Then register the complete schedule with cron_create. Verify it with cron_run_now; report success only after a 2xx run result, and include the returned next_run_at. Use cron_runs to inspect a bounded failure result before changing the handler or schedule.
For example, SKILL_CUP_KEY_FILE identifies only the logical secret name SKILL_CUP_KEY; never open that file.
<!-- cloudcli-platform:bitrix24:end -->

<!-- autopilot:start -->
# Games Puzzle

Нативное Android-приложение для фотопазлов: Kotlin 2.2.21, Jetpack Compose, один модуль `:app`, JVM 17.

## Команды

| Команда | Что делает |
|---------|------------|
| `./gradlew test` | Запустить JVM unit-тесты; нужны JDK 17 и Android SDK |
| `python scripts/check_version_sync.py` | Проверить согласованность `VERSION` и Android-версии |

APK собирает только GitHub Actions: `.github/workflows/android.yml` выполняет `./gradlew test assembleDebug` и публикует artifact `games-puzzle-debug`. Локальную APK-сборку не считать поддержанной командой. Runtime-тесты в текущей среде не завершены, поэтому их зелёный статус не предполагается.

## Структура

- `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts` — плагины, модуль `:app`, Android/JVM-конфигурация и зависимости.
- `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/PlaySession.kt` — правила партии, таймер, `snapshot/restore`.
- `app/src/main/java/ru/alexandrgert/gamespuzzle/data/ActivePlaySessionStore.kt` — durable-снимки в DataStore Preferences.
- `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModel.kt` — координация партии, UI и сохранения.
- `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayScreen.kt` — Compose UI и lifecycle-события.
- `app/src/main/assets/` — каталог, полноразмерные изображения и миниатюры.
- `app/src/test/` — JVM unit-тесты и фикстуры.
- `.github/workflows/android.yml` — проверки, debug artifact и подписанная release-сборка в CI.
- `docs/github-release-draft.md` — накопительный черновик release notes.

## Архитектура активной сессии

- `PlaySession` владеет доской, ходами и таймером; `pauseTimer/resumeTimer` исключают фон приложения из elapsed time.
- `PlayViewModel` сначала использует `SavedStateHandle`, затем загружает durable-снимок по ключу партии через `ActivePlaySessionStore`.
- `PlayScreen` передаёт `ON_STOP/ON_START` в `onBackground/onForeground`; фон приостанавливает таймер и сохраняет снимок.
- DataStore хранит JSON со `schemaVersion = 1`; повреждённые или несовместимые данные безопасно отклоняются и по возможности удаляются.
- `tick()` обновляет UI и `SavedStateHandle`, но сам не пишет durable-состояние; `abandon()` очищает оба слоя.

## Тесты и окружение

- Основные швы: `app/src/test/java/ru/alexandrgert/gamespuzzle/domain/PlaySessionTest.kt`, `app/src/test/java/ru/alexandrgert/gamespuzzle/data/ActivePlaySessionStoreTest.kt`, `app/src/test/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModelTest.kt`, `app/src/test/java/ru/alexandrgert/gamespuzzle/ui/play/PlayLifecycleTest.kt`.
- Gradle требует JDK 17 и Android SDK. В текущей среде runtime-набор не завершён; ориентир сборочного статуса — GitHub Actions.

## Подводные камни

- Не выполнять сериализацию DataStore на main dispatcher и не превращать частый `tick()` в durable-запись.
- Не обходить валидацию `PlaySession.restore`: неподходящий снимок должен дать `null`, а не аварийно восстановиться.
- Не читать и не выводить keystore или значения переменных подписи; signed release APK создаётся только CI при наличии секретов.
- `VERSION` остаётся `0.5.3`; placeholder в release draft не означает опубликованный релиз.

## Как здесь работает Autopilot

Сборка ведётся навыком `/autopilot`. Требования, спецификация и таски — в `.autopilot/`.
Прогресс — `.autopilot/dashboard.html`. Правило: требование из `manifest.md`
может снять только пользователь.

Если работа продолжается — скажи «продолжи автопилот»: состояние поднимется
из `.autopilot/state.js`, переспрашивать ничего не нужно.
<!-- autopilot:end -->
