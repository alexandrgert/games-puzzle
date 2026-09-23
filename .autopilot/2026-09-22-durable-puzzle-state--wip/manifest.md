# Манифест требований

Источник: `2026-09-22-brief.md`. Строку из этого списка может снять **только пользователь**.

| ID | Из брифа (дословно) | Статус | Основание | Где |
|----|---------------------|--------|-----------|-----|
| R01 | «делай реализацию» — устойчивое сохранение партии | done | hardening review clean | spec: истории 1–3 → T01, T04 |
| R02 | «делай реализацию» — lifecycle/process-death проверки восстановления | done | hardening review clean | spec: история 4 → T01, T04 |
| R03 | «делай реализацию» — не учитывать время в фоне | done | hardening review clean | spec: история 5 → T01, T04 |
| R04 | «делай реализацию» — регрессии каскадной фиксации | done | static review pass; runtime GitHub CI pending | spec: история 6 → T02 |
| R05 | «делай реализацию» — `test assembleDebug` в CI | done | workflow review pass | spec: история 7 → T02 |
| R06 | «делай реализацию» — проверка целостности поставляемого каталога | done | static review pass; runtime GitHub CI pending | spec: история 8 → T02 |
| R07 | «как закончишь, подготовь черновик release notes» | done | craft review clean | spec: история 9 → T03 |
| R08i | *(подразумевается)* прогнать `./gradlew test assembleDebug` и получить APK локально | dropped | пользователь: «собирать APK только на гитхаб» | — |
| G01 | «собирать APK только на гитхаб» | done | workflow review pass | spec: история 10 → T02 |
| D01 | Среда оборвала прямой Gradle JVM на `:app:compileDebugKotlin` без результата после обхода daemon socket и read-only Android home | done | локальная сборка отменена пользователем; причина сохранена как история прогона | spec: решения по реализации |
