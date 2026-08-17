# Offline sync: паттерны и merge

Техническое описание file-based sync (WebDAV и аналоги) для multi-device приложений.

## Файлы на сервере

| Файл | Назначение |
|------|------------|
| `<app>/data.json` | База данных приложения (без секретов) |
| `<app>/data.sync-meta.json` | SHA-256, revision, device_id |

Локально (не синхронизируются): credentials sync, integration secrets.

## Pull (скачать и объединить)

1. Скачать payload и meta (если есть).
2. **Conflict detection:** если `content_hash` на сервере ≠ `last_remote_content_hash` локально — зафиксировать конфликт (merge всё равно выполняется, пользователь уведомляется).
3. **Merge** локального и удалённого:
   - сущности по стабильному **id**;
   - дочерние записи — **union по id** где возможно;
   - при одном id и расхождении полей — **richer-wins** (явно документировать порядок);
   - UI/block prefs — из «самого полного» файла (больше сущностей / размер).
4. **Normalize:** глобальные инварианты (например одна «running» сущность — остальные в paused).
5. Сохранить локально; обновить `last_remote_content_hash` после успешного полного цикла.

## Push (загрузить)

**Pull-before-push** — перед upload всегда merge с сервером (если файл существует).

1. Скачать, проверить hash, merge.
2. Normalize.
3. `PUT` payload + meta (retry meta; при partial failure — повтор полного цикла).
4. Обновить `last_remote_content_hash` только после успеха.

Слепой overwrite remote без merge — антипаттерн.

## Ограничения merge

- Разные `session.id` одной задачи **объединяются**. При **одном** `session.id` и разных полях побеждает одна richer-копия — часть полей другой стороны может быть потеряна.
- **Не запускайте** один и тот же live-таймер на двух устройствах одновременно.
- Пароли и webhook **никогда** не попадают в payload.

## Периодическая проверка

При `sync_interval_minutes > 0`:

- Сравнить remote hash с локальным `last_remote_content_hash`.
- При изменении — prompt **«Скачать и объединить?»** / **«Позже»** (с remind interval).
- «Позже» привязан к **конкретному** remote hash; новый hash на сервере сбрасывает таймер и показывает диалог сразу.

На Android фоновые проверки ограничены OS (WorkManager ≥ 15 мин).

## Reconnect (offline → online)

Debounced push (pull-before-push), cooldown против штормов. Ошибки — tray/notification + локальный журнал; успех может быть тихим.

## Ручная синхронизация в UI

Кнопки «Скачать и объединить» / «Загрузить сейчас» работают **без** включённого auto-sync (достаточно credentials).

## Журнал sync

Локальный JSONL ring buffer (не в облаке): дата, операция, ↑/↓ counts, OK или ошибка.

## Безопасность

### Права на секреты

- файл credentials: **0600**
- каталог: **0700** (best effort)

На Windows mount / некоторых FS `chmod` может не сработать — логировать и рекомендовать ручную проверку.

### TLS

Системная проверка CA через HTTPS stack. Self-signed CA — добавить в системное хранилище. Certificate pinning — отдельное решение, если нужно.

### Shutdown / signals

Корректный выход: save → backup → sync push. Режим «upload only без merge» — опциональная настройка с предупреждением о риске затирания remote.

## Code review: типичные ловушки

| Проблема | Решение |
|----------|---------|
| Shutdown upload-only затирает remote | `shutdown_upload_only` + `pending_notice` при конфликте |
| Preview merge не показывает обогащение | счётчики + detailed diff в UI |
| `discover_data_files` подхватывает лишние пути | whitelist legacy roots |
| `quit()` из signal handler | defer через event loop (`QTimer`, `post`, …) |
| Partial upload meta | retry data+meta; не обновлять hash при сбое |

## Связанные материалы

- Skill: `offline-sync-merge`, `merge-lockstep-testing`
- Doc: `merge-lockstep-across-platforms.md`
- Rule: `local-primary-build-ci-matrix.mdc`
