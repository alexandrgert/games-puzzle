# Интерфейсы и правила проекта

## Границы, решённые в спецификации

| Модуль | Владеет | Выставляет | Прячет |
|---|---|---|---|
| `PlaySession` | доска, ходы, таймер, снимок | `snapshot`, `restore`, `pauseTimer`, `resumeTimer` | расчёт накопленного времени и проверку снимка |
| `ActivePlaySessionStore` | постоянные снимки активных партий | `load`, `save`, `clear` по ключу партии | JSON, schema version и DataStore Preferences |
| `PlayViewModel` | координация сессии, UI-состояния и сохранения | `start`, игровые команды, `onBackground`, `onForeground`, `abandon` | выбор источника восстановления и порядок записей |
| `PlayScreen` | Android lifecycle экрана | события жизненного цикла в ViewModel | Compose-наблюдатель lifecycle |

Основные тестовые швы: публичный контракт `PlaySession` и интерфейс `ActivePlaySessionStore`, внедряемый в `PlayViewModel`.

## Правила проекта

- Kotlin 2.2.21, Android/Compose, JVM 17, один модуль `:app`.
- Проверка: `./gradlew test assembleDebug`.
- `VERSION` остаётся `0.5.3`; этот прогон не публикует релиз и не собирает release APK.
- Существующие пользовательские изменения и несвязанные файлы не трогать.
- Новую зависимость не добавлять без необходимости; отсутствующая обязательная зависимость возвращается как `BLOCKED`.
- Секреты, keystore и значения переменных подписи не читать и не выводить.

## Реализованные интерфейсы

### Из таска 02 — проверки и CI

- CI artifact `games-puzzle-debug` публикует `app/build/outputs/apk/debug/app-debug.apk` после `./gradlew test assembleDebug`.
- Production API не изменён; проверки каталога работают по фактическим `app/src/main/assets`.

### Из таска 01 — активная игровая сессия

- `PlaySession.pauseTimer()` и `resumeTimer()` исключают фоновый интервал из результата.
- `ActivePlaySessionStore.load(key)`, `save(key, snapshot)`, `clear(key)` владеют версионированным JSON активной партии.
- Формат: `{schemaVersion:1,snapshot:{sizeN,statsEnabled,tiles,locked,selectedIndex,moves,peek,elapsedMs,completed}}`.
- `PlayViewModel.onBackground()`, `onForeground()`, `abandon()` связывают lifecycle и постоянное состояние.

### Из таска 03 — release notes

- `docs/github-release-draft.md` — накопительный черновик; версия и APK остаются placeholder до зелёного CI.

### Из таска 04 — доводка сохранения

- `PlayViewModel.tick()` обновляет UI и `SavedStateHandle`, но не создаёт durable-запись.
- `PlayViewModel.onBackground()` приостанавливает таймер и выполняет критическое постоянное сохранение.
- `ActivePlaySessionStore.load/save/clear` сохраняют прежний публичный контракт; codec выполняется вне main dispatcher.
