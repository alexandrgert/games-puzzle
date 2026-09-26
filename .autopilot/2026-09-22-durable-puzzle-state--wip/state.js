window.STATE =
{
  "slug": "durable-puzzle-state",
  "dir": "2026-09-22-durable-puzzle-state--wip",
  "title": "Устойчивое сохранение партии и выпуск APK",
  "mode": "semi",
  "depth": "normal",
  "polish": null,
  "tier": "T2",
  "briefFile": "2026-09-22-brief.md",
  "memoryFile": "AGENTS.md",
  "skillDir": "/srv/cloudcli/workers/2/home/.codex/skills/cloudcli-autopilot",
  "startedAt": "2026-09-22T12:48:10+03:00",
  "updatedAt": "2026-09-22T16:45:48+03:00",
  "finishedAt": "2026-09-22T16:45:48+03:00",
  "stages": [
    { "id": "preflight", "status": "done", "startedAt": "2026-09-22T12:48:10+03:00", "finishedAt": "2026-09-22T12:52:00+03:00" },
    { "id": "manifest", "status": "done", "startedAt": "2026-09-22T12:52:00+03:00", "finishedAt": "2026-09-22T12:54:00+03:00" },
    { "id": "briefing", "status": "skipped", "note": "вопросов не потребовалось" },
    { "id": "spec", "status": "done", "startedAt": "2026-09-22T12:54:00+03:00", "finishedAt": "2026-09-22T13:00:00+03:00" },
    { "id": "plan", "status": "done", "startedAt": "2026-09-22T13:00:00+03:00", "finishedAt": "2026-09-22T13:07:00+03:00", "note": "3 таска, ярус T2" },
    { "id": "build", "status": "done", "startedAt": "2026-09-22T13:07:00+03:00", "finishedAt": "2026-09-22T15:20:00+03:00", "note": "4 из 4 тасков готовы" },
    { "id": "review", "status": "done", "startedAt": "2026-09-22T13:24:00+03:00", "finishedAt": "2026-09-22T15:20:00+03:00", "note": "проверено 4 из 4" },
    { "id": "final", "status": "done", "startedAt": "2026-09-22T15:20:00+03:00", "finishedAt": "2026-09-22T16:45:48+03:00" }
  ],
  "requirements": {
    "total": 10, "done": 9, "inTicket": 0, "inSpec": 0,
    "placeholder": 0, "deferred": 0, "dropped": 0
  },
  "tickets": [
    { "id": "01", "title": "Устойчивое сохранение партии и фоновый таймер", "requirements": ["R01", "R02", "R03"], "blockedBy": [], "wave": 1, "zone": ["domain", "data", "ui/play", "play-tests"], "status": "done", "startedAt": "2026-09-22T13:11:00+03:00", "finishedAt": "2026-09-22T14:20:00+03:00", "retries": 0, "repairs": 1, "repairFindings": ["Добавить lifecycle-level тест маршрутизации ON_STOP/ON_START через экранный observer"], "handoffs": 0, "files": ["PlaySession.kt", "ActivePlaySessionStore.kt", "PlayViewModel.kt", "PlayScreen.kt", "PlaySessionTest.kt", "ActivePlaySessionStoreTest.kt", "PlayViewModelTest.kt", "PlayLifecycleTest.kt"], "tests": { "declared": 95, "executed": 0, "failed": 0 }, "commit": null, "concerns": ["runtime suite blocked by sandbox"] },
    { "id": "02", "title": "Регрессии поля, каталог и debug CI", "requirements": ["R04", "R05", "R06", "G01"], "blockedBy": [], "wave": 1, "zone": ["domain-tests", "catalog-tests", "github-workflows"], "status": "done", "startedAt": "2026-09-22T13:11:00+03:00", "finishedAt": "2026-09-22T14:42:00+03:00", "retries": 2, "repairs": 1, "repairFindings": ["CI должен запускаться для каждого push/PR; APK должен собираться только GitHub Actions"], "handoffs": 0, "files": ["BoardEngineTest.kt", "ShippedCatalogTest.kt", ".github/workflows/android.yml"], "tests": { "declared": 95, "executed": 0, "failed": 0 }, "commit": null, "concerns": ["runtime suite должен подтвердить GitHub Actions", "локальная APK-сборка запрещена G01"] },
    { "id": "03", "title": "Черновик накопительных release notes", "requirements": ["R07"], "blockedBy": ["01"], "wave": 2, "zone": ["docs"], "status": "done", "startedAt": "2026-09-22T14:20:00+03:00", "finishedAt": "2026-09-22T14:50:00+03:00", "retries": 0, "repairs": 1, "repairFindings": ["Добавить накопительную историю релизов 0.3.0, 0.2.0 и 0.1.0"], "handoffs": 0, "files": ["docs/github-release-draft.md"], "tests": { "documentChecks": "passed" }, "commit": null, "concerns": [] },
    { "id": "04", "title": "Надёжность постоянного сохранения", "requirements": ["R01", "R02", "R03"], "blockedBy": ["01"], "wave": 3, "zone": ["data", "ui/play", "persistence-tests"], "status": "done", "startedAt": "2026-09-22T15:00:00+03:00", "finishedAt": "2026-09-22T15:20:00+03:00", "retries": 0, "repairs": 1, "repairFindings": ["Тест должен отдельно блокировать первую save и доказать, что вторая не стартовала", "Ошибка чтения store не должна оставлять PlayViewModel.start в starting=true"], "handoffs": 0, "files": ["ActivePlaySessionStore.kt", "PlayViewModel.kt", "ActivePlaySessionStoreTest.kt", "PlayViewModelTest.kt"], "tests": { "staticSeams": "passed", "runtime": "GitHub CI pending" }, "commit": null, "concerns": [] }
  ],
  "singlePass": null,
  "tests": { "runtime": "GitHub CI pending", "staticReviews": "passed", "failed": 0 },
  "debt": { "placeholders": [], "assumptions": [], "emptyEnv": [] },
  "additions": [],
  "coverage": { "found": 2, "fixed": 2, "deferred": 0 },
  "concerns": ["resolved: catalog test confirmed against shipped assets", "dropped: BoardEngine is the public domain seam for cascade behaviour", "resolved: store tests use public API", "resolved: tick no longer writes durably", "resolved: codec uses IO dispatcher", "resolved: ordering test blocks only first save", "dropped: local APK build cancelled by user; APK is GitHub-only"],
  "reviewers": { "manifestSpec": "/root/manifest_spec_review", "craft": "/root/craft_review" },
  "blind": { "implemented": 8, "partial": 0, "missing": 0, "runtimeVerified": false, "blocker": "APK и Gradle unit tests должны быть подтверждены GitHub Actions; локальная APK-сборка отменена пользователем" }
}
