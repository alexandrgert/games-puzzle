# Merge lockstep across platforms

Desktop и mobile должны выбирать **одинаковый** winner при конфликте одной и той же записи по id. Иначе пользователь теряет доверие к sync.

## Цель

Единое правило richer-record на всех платформах; общие JSON-fixtures; документация совпадает с кодом.

## Fixture layout

```
tests/fixtures/merge_lockstep/
  case_01_input_local.json
  case_01_input_remote.json
  case_01_expected.json
mobile/src/test/resources/merge_lockstep/   # копия тех же файлов
```

Один канонический каталог; mobile тесты **загружают те же JSON**, что desktop.

## Пример алгоритма (timer sessions, same id)

Документировать **до** кодирования:

1. Закрытая сессия (`ended_at`) beats open.
2. Большая длительность (секунды) wins.
3. Meta score: external id (+2), non-empty comment (+1) → больший score wins.
4. Tie → **candidate** (второй аргумент / позже в обходе `left + right`).

Обе реализации (Python + Kotlin/Swift) должны совпадать на шагах 1–4. Расхождение tie-break — failing test.

## Test layers

| Layer | Tool |
|-------|------|
| Pure merge functions | pytest / JUnit / XCTest |
| Golden fixtures | обе платформы |
| Property tests (optional) | same invariants |

## Workflow изменения merge

1. Добавить **failing** fixture на desktop.
2. Скопировать fixture в mobile resources.
3. Исправить обе реализации в одном PR (или desktop first + immediate mobile follow-up).
4. Обновить user-facing «ограничения» (same-id conflict).
5. Bump версии продукта — по политике релиза (merge-only PR может обойтись без bump).

## CI

- Python merge tests — каждый push.
- Mobile unit tests — в `build-apk` job или отдельный test job.

## Типичные расхождения (из практики)

| Симптом | Причина |
|---------|---------|
| Desktop теряет external id с mobile | Python без meta-score на equal duration |
| Mobile оставляет stale при equal meta | tie-break `existing` vs `candidate` |
| Docs говорят «union intervals» | код делает pick-one для same id |

Исправление: lockstep plan → tests → code → docs в одном цикле.

## Global constraints для агента

- Не менять UI merge preview в merge-only задаче без явного scope.
- Секреты sync/integration не трогать.
- Локально mobile release build не гонять (CI после push).

## Связанные материалы

- Skill: `merge-lockstep-testing`, `offline-sync-merge`
- Doc: `offline-sync-patterns.md`
