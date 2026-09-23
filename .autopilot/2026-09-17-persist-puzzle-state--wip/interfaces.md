# Интерфейсы

## Правила проекта

- Kotlin 2.2.21, Android Gradle Plugin 8.9.1, Jetpack Compose, JVM 17.
- Полный unit-набор: `./gradlew test`.
- Не изменять `.cloudcli` и управляемый блок `AGENTS.md`.
- Не устанавливать отсутствующие зависимости; сообщать о блокировке среды.

## Границы, решённые в спецификации

| Модуль | Владеет | Выставляет | Прячет |
|---|---|---|---|
| `PlaySession` | правила и текущее состояние партии | создание новой партии, создание из снимка, `snapshot()` | расчёт времени и внутренние мутации |
| `PlayViewModel` | Android-сохранение конкретной открытой партии | старт/действия для Compose | ключи и запись `SavedStateHandle` |

Швы тестирования: чистые `PlaySession.snapshot()/restore` и публичное состояние `PlayViewModel` после создания с заранее заполненным `SavedStateHandle`.

## Из таска 01 — развёрнутый Android-проект

- Корневой проект `games-puzzle`, модуль `:app`, Gradle wrapper 8.11.1.
- Kotlin 2.2.21, Android Gradle Plugin 8.9.1, JVM 17.
- В среде нет Java/JAVA_HOME, поэтому Gradle-тесты здесь пока недоступны.

## Из таска 02 — восстановление партии

- `PlaySessionSnapshot : Serializable` — полный сохраняемый снимок партии.
- `PlaySession.snapshot(): PlaySessionSnapshot` — получить снимок.
- `PlaySession.restore(snapshot, currentTimeMillis): PlaySession?` — безопасно восстановить либо вернуть `null`.
- `PlayViewModel(SavedStateHandle, statsEnabled, puzzleId, recordSaver, currentTimeMillis)` — сохраняет снимок после изменений и восстанавливает подходящую партию.
