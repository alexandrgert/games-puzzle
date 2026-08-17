# Android release signing in CI

Как **не** генерировать новый keystore на каждый CI run и как документировать для пользователей смену ключа.

## Проблема

Ephemeral debug/release keystore в CI → каждый APK подписан **другим** ключом → Android не даёт обновить поверх установленной версии (conflicting signatures).

## Решение

### One-time keystore

1. Сгенерировать release keystore **локально один раз** (не в git).
2. Сохранить в GitHub Secrets:
   - `ANDROID_KEYSTORE_BASE64` — base64 файла `.jks`/`.keystore`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`

### CI workflow

```yaml
- name: Restore Android keystore
  env:
    ANDROID_KEYSTORE_BASE64: ${{ secrets.ANDROID_KEYSTORE_BASE64 }}
  run: python scripts/write_android_keystore_from_env.py
```

Скрипт:

- декодирует base64 → `android/release.keystore` (path в `.gitignore`);
- **fail fast** если secret пуст в CI release job;
- не перезаписывает существующий локальный keystore dev-машины без env.

### Gradle

`android/app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../release.keystore")
        storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("ANDROID_KEY_ALIAS")
        keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    }
}
```

Release build type использует `signingConfigs.getByName("release")`.

## Локальная разработка

- Debug builds — debug keystore по умолчанию.
- Release APK локально — только при явном override пользователя; keystore из env или local file.

## User communication

Если ранее выкладывались APK с ephemeral signing:

- В release notes: **удалить старое приложение**, установить новое.
- Play Store Internal Testing — upload key отдельно от app signing key (Google Play App Signing).

## Secrets hygiene

| Do | Don't |
|----|-------|
| GitHub Secrets для CI | keystore в git |
| `.gitignore` для `release.keystore` | пароли в workflow yaml plaintext |
| rotate secrets при компрометации | логировать base64 decode |

## Checklist перед первым signed CI APK

- [ ] Keystore создан и backup в offline storage
- [ ] Secrets добавлены в repo settings
- [ ] CI job падает без secrets (не silent fallback на debug)
- [ ] Release notes объясняют reinstall при смене ключа
- [ ] `versionName`/`versionCode` synced с canonical semver

## Связанные материалы

- Skill: `multi-platform-ci-release`
- Rule: `multi-platform-version-sync.mdc`
- Doc: `multi-platform-build-release.md`
