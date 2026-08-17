# Games Puzzle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a native Android picture-join puzzle (no empty cells; successful joins snap to the final-picture plan and lock) with a bundled catalog, user imports, GitHub-Release updates, and CI-only APK builds.

**Architecture:** JVM-pure `domain/` owns the board, swap, snap-to-home, lock, shuffle, and records. `data/` loads `catalog.json`, DataStore, and GitHub Releases. Compose `ui/` is a single-activity shell. CI on `main` validates schema, runs unit tests, checks version sync, and `assembleRelease` with a long-lived keystore from GitHub Secrets.

**Tech Stack:** Kotlin 2.0, Jetpack Compose + Material3, Navigation Compose, DataStore Preferences, kotlinx.serialization JSON, JUnit 4, GitHub Actions, AGP 8.7, compileSdk/targetSdk 36.

**Spec:** `docs/superpowers/specs/2026-08-17-games-puzzle-design.md`

## Global Constraints

- minSdk 26; compileSdk 36; targetSdk 36.
- applicationId / namespace: `ru.alexandrgert.gamespuzzle`.
- Builds: GitHub Actions in this repo only. Do not run `assembleRelease` / `assembleDebug` locally unless the user explicitly overrides. `./gradlew test` is allowed.
- Signing: restore keystore from `ANDROID_KEYSTORE_BASE64` + password secrets; fail if secrets missing; never generate a per-run keystore; `*.keystore` stays gitignored.
- Version: canonical semver in `VERSION`; `versionName` matches; `versionCode` monotonic. Start at `0.1.0` / `versionCode 1`.
- UI copy: Russian only in `app/src/main/res/values/strings.xml`. No user-visible string literals in Kotlin.
- Images: public domain / CC redistributable (Wikimedia Commons preferred). Attribution in catalog metadata + Credits screen.
- Portrait phone UI, board width = screen width, tuned for ~20:9 (Poco X7 1220×2712, X8 Pro Max 1280×2772).
- Gradle project lives at **repo root** (`app/`, not `android/app/`).

---

## File map

| Path | Responsibility |
|------|----------------|
| `VERSION` | Canonical semver |
| `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts` | Android app module |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Grid.kt` | `GridSize`, `Cell` |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Board.kt` | Board + lock invariant |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/BoardEngine.kt` | swap, join, snap, shuffle, win |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Records.kt` | best time/moves merge |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/CatalogModels.kt` | catalog enums/data classes |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Semver.kt` | parse/compare |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/data/CatalogJson.kt` | parse `catalog.json` |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/data/GithubReleaseJson.kt` | parse Releases API |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/data/SettingsStore.kt` | DataStore prefs |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/data/RecordsStore.kt` | best results |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/data/UserPuzzlesStore.kt` | imported photos index |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/platform/UserFiles.kt` | app-private image files |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/platform/ApkInstaller.kt` | install downloaded APK |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/MainActivity.kt` | single activity |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/Nav.kt` | routes |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/catalog/CatalogScreen.kt` | filters + thumbs |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/preview/PreviewScreen.kt` | full image + 5×5/6×6 + start |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayScreen.kt` | board, peek, abandon |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/play/PlayViewModel.kt` | apply `MoveResult` |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/settings/SettingsScreen.kt` | stats, update mode, check |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/myphotos/MyPhotosScreen.kt` | import/delete |
| `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/credits/CreditsScreen.kt` | attributions |
| `app/src/main/assets/catalog.json` | bundled catalog |
| `app/src/main/assets/puzzles/`, `thumbs/` | WebP files |
| `app/src/main/res/values/strings.xml` | all UI copy |
| `docs/schemas/catalog.schema.json` | CI schema |
| `scripts/check_version_sync.py` | VERSION vs Gradle |
| `scripts/write_android_keystore_from_env.py` | CI keystore restore |
| `.github/workflows/android.yml` | test + signed APK |
| `app/src/test/java/ru/alexandrgert/gamespuzzle/...` | JUnit |

---

### Task 1: Gradle app skeleton

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `VERSION`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/ru/alexandrgert/gamespuzzle/ui/MainActivity.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/proguard-rules.pro`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: nothing
- Produces: installable *module* (`applicationId ru.alexandrgert.gamespuzzle`); `./gradlew test` runnable (empty tests OK)

- [ ] **Step 1: Extend `.gitignore` for Gradle at repo root**

Append:

```
.gradle/
build/
app/build/
local.properties
*.iml
.idea/
release.keystore
```

- [ ] **Step 2: Write Gradle files**

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "games-puzzle"
include(":app")
```

`build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}
```

`gradle.properties`:

```
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

`VERSION` file contents: `0.1.0`

`app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "ru.alexandrgert.gamespuzzle"
    compileSdk = 36
    defaultConfig {
        applicationId = "ru.alexandrgert.gamespuzzle"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            val ks = rootProject.file("release.keystore")
            if (ks.exists()) {
                storeFile = ks
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (rootProject.file("release.keystore").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

Empty `app/proguard-rules.pro`.

- [ ] **Step 3: Manifest, theme, strings, activity**

`AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
    <application
        android:allowBackup="true"
        android:icon="@android:drawable/ic_menu_gallery"
        android:label="@string/app_name"
        android:theme="@style/Theme.GamesPuzzle"
        android:usesCleartextTraffic="false">
        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:configChanges="orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
```

`app/src/main/res/xml/file_paths.xml`:

```xml
<paths>
    <cache-path name="apks" path="updates/" />
</paths>
```

Need `androidx.core:core-ktx` for FileProvider — add `implementation("androidx.core:core-ktx:1.13.1")` to `app/build.gradle.kts` dependencies.

`themes.xml`:

```xml
<resources>
    <style name="Theme.GamesPuzzle" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`strings.xml` (initial; later tasks only *add* keys, never hardcode):

```xml
<resources>
    <string name="app_name">Games Puzzle</string>
</resources>
```

`MainActivity.kt`:

```kotlin
package ru.alexandrgert.gamespuzzle.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import ru.alexandrgert.gamespuzzle.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text(stringResource(R.string.app_name)) }
    }
}
```

- [ ] **Step 4: Generate Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.9` (or `./gradlew wrapper` if a wrapper already exists)

Expected: `gradlew`, `gradle/wrapper/gradle-wrapper.properties` with 8.9.

- [ ] **Step 5: Run unit tests**

Run: `./gradlew test --offline` is optional; prefer `./gradlew test`

Expected: BUILD SUCCESSFUL (no tests or empty).

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties app VERSION gradlew gradle .gitignore \
  app/src/main/AndroidManifest.xml app/src/main/java app/src/main/res app/proguard-rules.pro
git commit -m "chore: scaffold Android app module for Games Puzzle"
```

---

### Task 2: Domain models and failed swap

**Files:**
- Create: `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Grid.kt`
- Create: `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Board.kt`
- Create: `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/BoardEngine.kt`
- Test: `app/src/test/java/ru/alexandrgert/gamespuzzle/domain/BoardEngineTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces:
  - `enum class GridSize(val n: Int) { FIVE(5), SIX(6) }`
  - `data class Cell(val row: Int, val col: Int)` with `inBounds(n)`, `index(n)`, `Cell.fromIndex(index, n)`
  - `class Board(size, tiles: IntArray, locked: BooleanArray)` — `tiles[slot] = tileId` (tileId equals home index); `locked[tileId]`
  - `sealed class MoveResult { class Applied(val board: Board); class Reverted(val board: Board) }`
  - `object BoardEngine { fun identityUnlocked(size: GridSize): Board; fun trySwap(board: Board, a: Cell, b: Cell): MoveResult }`
  - Invariant: if `locked[t]` then the slot whose `tiles[slot]==t` is `slot==t`

- [ ] **Step 1: Write failing tests**

```kotlin
package ru.alexandrgert.gamespuzzle.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardEngineTest {
    @Test
    fun identityHasNoEmptyAndAllUnlocked() {
        val b = BoardEngine.identityUnlocked(GridSize.FIVE)
        assertTrue(b.tiles.size == 25)
        assertTrue(b.tiles.toList() == (0 until 25).toList())
        assertTrue(b.locked.all { !it })
    }

    @Test
    fun swapNonJoiningTilesReverts() {
        val start = Board(
            GridSize.FIVE,
            intArrayOf(
                1, 0, 2, 3, 4,
                5, 6, 7, 8, 9,
                10, 11, 12, 13, 14,
                15, 16, 17, 18, 19,
                20, 21, 22, 23, 24,
            ),
            BooleanArray(25),
        )
        val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(4, 4))
        assertTrue(result is MoveResult.Reverted)
        assertArrayEquals(start.tiles, (result as MoveResult.Reverted).board.tiles)
    }

    @Test
    fun swapLockedCellReverts() {
        val locked = BooleanArray(25).also { it[0] = true }
        val start = BoardEngine.identityUnlocked(GridSize.FIVE).let {
            Board(it.size, it.tiles, locked)
        }
        val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(0, 1))
        assertTrue(result is MoveResult.Reverted)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests ru.alexandrgert.gamespuzzle.domain.BoardEngineTest`

Expected: FAIL (unresolved `BoardEngine` / `Board`).

- [ ] **Step 3: Minimal implementation**

`Grid.kt`:

```kotlin
package ru.alexandrgert.gamespuzzle.domain

enum class GridSize(val n: Int) {
    FIVE(5),
    SIX(6),
}

data class Cell(val row: Int, val col: Int) {
    fun inBounds(n: Int): Boolean = row in 0 until n && col in 0 until n
    fun index(n: Int): Int = row * n + col

    companion object {
        fun fromIndex(index: Int, n: Int): Cell = Cell(index / n, index % n)
    }
}
```

`Board.kt`:

```kotlin
package ru.alexandrgert.gamespuzzle.domain

class Board(
    val size: GridSize,
    tiles: IntArray,
    locked: BooleanArray,
) {
    val n: Int = size.n
    val tiles: IntArray = tiles.copyOf()
    val locked: BooleanArray = locked.copyOf()

    init {
        require(this.tiles.size == n * n)
        require(this.locked.size == n * n)
    }

    fun tileAt(cell: Cell): Int = tiles[cell.index(n)]

    fun isLockedTile(tileId: Int): Boolean = locked[tileId]

    fun isLockedCell(cell: Cell): Boolean = locked[tileAt(cell)]

    fun copy(): Board = Board(size, tiles, locked)
}

sealed class MoveResult {
    data class Applied(val board: Board) : MoveResult()
    data class Reverted(val board: Board) : MoveResult()
}
```

`BoardEngine.kt` (revert-only for now):

```kotlin
package ru.alexandrgert.gamespuzzle.domain

object BoardEngine {
    fun identityUnlocked(size: GridSize): Board {
        val n = size.n
        val count = n * n
        return Board(size, IntArray(count) { it }, BooleanArray(count))
    }

    fun trySwap(board: Board, a: Cell, b: Cell): MoveResult {
        val n = board.n
        if (a == b || !a.inBounds(n) || !b.inBounds(n)) return MoveResult.Reverted(board)
        if (board.isLockedCell(a) || board.isLockedCell(b)) return MoveResult.Reverted(board)
        return MoveResult.Reverted(board)
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew test --tests ru.alexandrgert.gamespuzzle.domain.BoardEngineTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ru/alexandrgert/gamespuzzle/domain app/src/test
git commit -m "feat: add board model and reject non-joining swaps"
```

---

### Task 3: Correct join snaps to home and locks

**Files:**
- Modify: `BoardEngine.kt`
- Modify: `BoardEngineTest.kt`

**Interfaces:**
- Consumes: `Board`, `trySwap`
- Produces: `trySwap` returns `Applied` when two swapped unlocked tiles are orthogonally adjacent **and** `homeOffset(t0,t1)==boardOffset(cell0,cell1)`; those tiles move to slots `t0` and `t1`; previous unlocked occupants of those home slots fill the cells the pair left; both tiles become `locked[t]=true`. If a home slot holds a locked tile that is not one of the pair, `Reverted`.

Correct join predicate:

```kotlin
fun boardOffset(a: Cell, b: Cell): Pair<Int, Int> = (b.row - a.row) to (b.col - a.col)

fun isOrthogonal(off: Pair<Int, Int>): Boolean =
    (kotlin.math.abs(off.first) == 1 && off.second == 0) ||
        (off.first == 0 && kotlin.math.abs(off.second) == 1)

fun isCorrectJoin(n: Int, cellA: Cell, tileA: Int, cellB: Cell, tileB: Int): Boolean {
    val bo = boardOffset(cellA, cellB)
    if (!isOrthogonal(bo)) return false
    val homeA = Cell.fromIndex(tileA, n)
    val homeB = Cell.fromIndex(tileB, n)
    return boardOffset(homeA, homeB) == bo
}
```

- [ ] **Step 1: Write failing test**

Add to `BoardEngineTest.kt`:

```kotlin
@Test
fun joiningPairAwayFromHomeSnapsAndLocks() {
    // 3x3 would be easier but spec is 5x5: tiles 0 and 1 are horizontal neighbours.
    val tiles = IntArray(25) { it }
    // Place tile 0 at (2,2)=12 and tile 1 at (2,3)=13 (correct relative edge, not home)
    tiles[0] = 12
    tiles[1] = 13
    tiles[12] = 0
    tiles[13] = 1
    val start = Board(GridSize.FIVE, tiles, BooleanArray(25))
    val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(0, 1))
    // After this swap, cells (0,0) and (0,1) hold tiles 0 and 1 in some order.
    // Homes of 0 and 1 are (0,0) and (0,1) — already home after swap if order matches.
    assertTrue(result is MoveResult.Applied)
    val board = (result as MoveResult.Applied).board
    assertTrue(board.tiles[0] == 0)
    assertTrue(board.tiles[1] == 1)
    assertTrue(board.locked[0] && board.locked[1])
}

@Test
fun joiningPairWrongOrderDoesNotJoin() {
    val tiles = IntArray(25) { it }
    val start = Board(GridSize.FIVE, tiles, BooleanArray(25))
    // Swap 0 and 2 (not neighbours): no join
    val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(0, 2))
    assertTrue(result is MoveResult.Reverted)
}
```

Fix the first test to actually start *away* from home:

```kotlin
@Test
fun joiningPairAwayFromHomeSnapsAndLocks() {
    val tiles = IntArray(25) { it }
    // Put tiles 0 and 1 at (4,0) and (4,1) — correct edge, not home
    val p0 = Cell(4, 0).index(5)
    val p1 = Cell(4, 1).index(5)
    tiles[p0] = 0
    tiles[p1] = 1
    tiles[0] = 20 // whatever was at (4,0)
    tiles[1] = 21
    val start = Board(GridSize.FIVE, tiles, BooleanArray(25))
    val result = BoardEngine.trySwap(start, Cell(4, 0), Cell(4, 1))
    // They already share the correct edge; trySwap of the two cells they occupy
    // still evaluates the join after a no-op swap of the same two tiles.
    assertTrue(result is MoveResult.Applied)
    val board = (result as MoveResult.Applied).board
    assertTrue(board.tiles[0] == 0 && board.tiles[1] == 1)
    assertTrue(board.locked[0] && board.locked[1])
    assertTrue(board.tiles[p0] == 20 && board.tiles[p1] == 21)
}
```

Note: swapping a cell with itself is reverted. The player “confirms” a join by swapping two *different* cells that after swap hold a joining pair. For the away-from-home case, start with 0 at (4,0) and 1 at (4,2) (not adjacent), and 7 at (4,1). Swap (4,0) with (4,1): then 0 at (4,1) and 7 at (4,0); 1 still at (4,2) — may not join.

Clean scenario: **before** swap, tile 0 at (4,0), tile 1 at (4,1) already adjacent with correct edge. Spec says the move is a swap of two unlocked tiles that *results* in a join. If they already join, swapping them with each other flips them to 1 then 0 — **wrong** edge, should revert. So the player must swap a *third* tile? Re-read spec.

Spec: “Two unlocked tiles share a correct relative edge **after** the swap → both snap to home.”

So the swap *creates* the adjacent correct pair. Example: 0 at (4,0), 1 at (3,1), filler at (4,1). Swap (3,1) and (4,1): now 1 at (4,1) next to 0 at (4,0), offset (0,+1), homes of 0 and 1 are (0,0) and (0,1), same offset → snap 0 and 1 to homes, displace occupants of homes into (4,0) and (4,1).

Use that as the test.

```kotlin
@Test
fun joiningPairAwayFromHomeSnapsAndLocks() {
    val tiles = IntArray(25) { it }
    // (4,0)=slot 20 has tile 0; (3,1)=slot 16 has tile 1; (4,1)=slot 21 has tile 21
    tiles[20] = 0
    tiles[16] = 1
    tiles[0] = 20
    tiles[1] = 16
    val start = Board(GridSize.FIVE, tiles, BooleanArray(25))
    val result = BoardEngine.trySwap(start, Cell(3, 1), Cell(4, 1))
    assertTrue(result is MoveResult.Applied)
    val board = (result as MoveResult.Applied).board
    assertTrue(board.tiles[0] == 0 && board.tiles[1] == 1)
    assertTrue(board.locked[0] && board.locked[1])
    assertTrue(!board.locked[21])
}
```

After swap: slot 16 has 21, slot 21 has 1, slot 20 has 0. Tiles 0 at (4,0) and 1 at (4,1) — correct join. Snap to 0 and 1. Occupants of homes were 20 and 16. Those go to slots 20 and 21 (the pair’s cells). Implement pairing: leftover sources `[20, 21]`, leftover occupants `[20, 16]` mapped in that order → slot20=20, slot21=16.

- [ ] **Step 2: Run test, expect FAIL** (still always Reverted)

- [ ] **Step 3: Implement join + snap in `BoardEngine.trySwap`**

After the locked/bounds checks, copy board, swap indices `ia, ib`, then:

```kotlin
val cellA = a
val cellB = b
val tA = next.tileAt(cellA)
val tB = next.tileAt(cellB)
if (isCorrectJoin(n, cellA, tA, cellB, tB)) {
    val snapped = snapPairToHome(next, tA, tB) ?: return MoveResult.Reverted(board)
    return MoveResult.Applied(snapped)
}
return MoveResult.Reverted(board)
```

`snapPairToHome`:

```kotlin
fun snapPairToHome(board: Board, t0: Int, t1: Int): Board? {
    val n = board.n
    val h0 = t0
    val h1 = t1
    val occ0 = board.tiles[h0]
    val occ1 = board.tiles[h1]
    if (occ0 != t0 && board.isLockedTile(occ0)) return null
    if (occ1 != t1 && board.isLockedTile(occ1)) return null
    var p0 = -1
    var p1 = -1
    for (i in board.tiles.indices) {
        if (board.tiles[i] == t0) p0 = i
        if (board.tiles[i] == t1) p1 = i
    }
    val involved = linkedSetOf(p0, p1, h0, h1)
    val next = board.copy()
    next.tiles[h0] = t0
    next.tiles[h1] = t1
    val leftoverSlots = listOf(p0, p1).filter { it != h0 && it != h1 }
    val leftoverTiles = buildList {
        if (occ0 != t0 && occ0 != t1) add(occ0)
        if (occ1 != t1 && occ1 != t0) add(occ1)
    }
    if (leftoverSlots.size != leftoverTiles.size) return null
    leftoverSlots.zip(leftoverTiles).forEach { (slot, tile) -> next.tiles[slot] = tile }
    next.locked[t0] = true
    next.locked[t1] = true
    return next
}
```

Also handle the no-op-adjacent already-correct case only via a swap that *creates* the adjacency (as in the test). Do not special-case swapping a cell with itself.

- [ ] **Step 4: Run tests — PASS**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ru/alexandrgert/gamespuzzle/domain/BoardEngine.kt app/src/test
git commit -m "feat: snap joining pair to home cells and lock"
```

---

### Task 4: Attach an unlocked tile to a locked group

**Files:**
- Modify: `BoardEngine.kt`, `BoardEngineTest.kt`

**Interfaces:**
- Consumes: `trySwap` after Task 3
- Produces: if after swap (and case 1 did not fire), any swapped tile that sits on its home slot and has an orthogonal neighbour cell whose tile `u` is locked and `isCorrectJoin(home(t), t, home(u), u)` — equivalently neighbour cell is home of a locked correct neighbour — then `locked[t]=true` and `Applied`. Swap already placed it; do not move the locked group.

- [ ] **Step 1: Failing test**

```kotlin
@Test
fun attachToLockedNeighbourLocksInPlace() {
    val tiles = IntArray(25) { it }
    val locked = BooleanArray(25).also {
        it[0] = true
        it[1] = true
    }
    // tile 2 belongs at (0,2). Put it at (4,4); put 24 at (0,2)
    tiles[24] = 2
    tiles[2] = 24
    val start = Board(GridSize.FIVE, tiles, locked)
    val result = BoardEngine.trySwap(start, Cell(4, 4), Cell(0, 2))
    assertTrue(result is MoveResult.Applied)
    val board = (result as MoveResult.Applied).board
    assertTrue(board.tiles[2] == 2)
    assertTrue(board.locked[2])
    assertTrue(board.locked[0] && board.locked[1])
    assertTrue(board.tiles[24] == 24)
}

@Test
fun cannotSwapOntoLockedTile() {
    val locked = BooleanArray(25).also { it[0] = true }
    val start = Board(GridSize.FIVE, IntArray(25) { it }, locked)
    val result = BoardEngine.trySwap(start, Cell(1, 0), Cell(0, 0))
    assertTrue(result is MoveResult.Reverted)
}
```

- [ ] **Step 2: Run — FAIL** (attach currently Reverted)

- [ ] **Step 3: After case 1, add case 2**

```kotlin
fun lockIfHomeAgainstLocked(board: Board, tileId: Int): Boolean {
    val n = board.n
    val home = Cell.fromIndex(tileId, n)
    if (board.tiles[tileId] != tileId) return false
    val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    for ((dr, dc) in dirs) {
        val nb = Cell(home.row + dr, home.col + dc)
        if (!nb.inBounds(n)) continue
        val u = board.tileAt(nb)
        if (!board.isLockedTile(u)) continue
        if (isCorrectJoin(n, home, tileId, nb, u)) return true
    }
    return false
}
```

In `trySwap`, after swap, if case 1 fails:

```kotlin
val nextLocked = next.copy()
var any = false
for (t in listOf(tA, tB)) {
    if (lockIfHomeAgainstLocked(nextLocked, t)) {
        nextLocked.locked[t] = true
        any = true
    }
}
return if (any) MoveResult.Applied(nextLocked) else MoveResult.Reverted(board)
```

- [ ] **Step 4: Run — PASS**

- [ ] **Step 5: Commit**

```bash
git commit -am "feat: lock a tile that lands home beside a locked group"
```

---

### Task 5: Shuffle, resultative probe, win

**Files:**
- Modify: `BoardEngine.kt`, `BoardEngineTest.kt`

**Interfaces:**
- Produces:
  - `fun isWin(board: Board): Boolean` — every `tiles[i]==i && locked[i]`
  - `fun hasResultativeSwap(board: Board): Boolean` — some pair of unlocked cells yields `Applied`
  - `fun shuffle(size: GridSize, random: java.util.Random): Board` — permutation ≠ identity, all unlocked, `hasResultativeSwap`; retry

- [ ] **Step 1: Failing tests**

```kotlin
@Test
fun identityIsWinOnlyWhenLocked() {
    val unlocked = BoardEngine.identityUnlocked(GridSize.FIVE)
    assertTrue(!BoardEngine.isWin(unlocked))
    val locked = Board(unlocked.size, unlocked.tiles, BooleanArray(25) { true })
    assertTrue(BoardEngine.isWin(locked))
}

@Test
fun shuffleNeverIdentityAndHasAMove() {
    val r = java.util.Random(1L)
    repeat(20) {
        val b = BoardEngine.shuffle(GridSize.FIVE, r)
        assertTrue(b.tiles.toList() != (0 until 25).toList())
        assertTrue(b.locked.all { !it })
        assertTrue(BoardEngine.hasResultativeSwap(b))
        assertTrue(!BoardEngine.isWin(b))
    }
}
```

- [ ] **Step 2: Run — FAIL**

- [ ] **Step 3: Implement**

```kotlin
fun isWin(board: Board): Boolean {
    for (i in board.tiles.indices) {
        if (board.tiles[i] != i || !board.locked[i]) return false
    }
    return true
}

fun hasResultativeSwap(board: Board): Boolean {
    val n = board.n
    val cells = mutableListOf<Cell>()
    for (r in 0 until n) for (c in 0 until n) {
        val cell = Cell(r, c)
        if (!board.isLockedCell(cell)) cells.add(cell)
    }
    for (i in cells.indices) for (j in i + 1 until cells.size) {
        if (trySwap(board, cells[i], cells[j]) is MoveResult.Applied) return true
    }
    return false
}

fun shuffle(size: GridSize, random: java.util.Random): Board {
    val n = size.n
    val count = n * n
    repeat(500) {
        val tiles = IntArray(count) { it }
        for (i in count - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = tiles[i]
            tiles[i] = tiles[j]
            tiles[j] = tmp
        }
        if (tiles.toList() == (0 until count).toList()) continue
        val board = Board(size, tiles, BooleanArray(count))
        if (hasResultativeSwap(board)) return board
    }
    error("shuffle failed to find a playable permutation")
}
```

- [ ] **Step 4: Run — PASS**

- [ ] **Step 5: Commit**

```bash
git commit -am "feat: shuffle playable boards and detect win"
```

---

### Task 6: Records merge

**Files:**
- Create: `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/Records.kt`
- Test: `app/src/test/java/ru/alexandrgert/gamespuzzle/domain/RecordsTest.kt`

**Interfaces:**
- Produces: `data class BestRecord(val bestTimeMs: Long?, val bestMoves: Int?)` and `fun mergeRecord(previous: BestRecord?, timeMs: Long, moves: Int): BestRecord` — independently keep min time and min moves.

- [ ] **Step 1: Failing test**

```kotlin
@Test
fun mergeKeepsIndependentBests() {
    val a = mergeRecord(null, 5000, 40)
    val b = mergeRecord(a, 8000, 20)
    assertEquals(5000L, b.bestTimeMs)
    assertEquals(20, b.bestMoves)
}
```

- [ ] **Step 2: FAIL**

- [ ] **Step 3:**

```kotlin
package ru.alexandrgert.gamespuzzle.domain

data class BestRecord(val bestTimeMs: Long?, val bestMoves: Int?)

fun mergeRecord(previous: BestRecord?, timeMs: Long, moves: Int): BestRecord {
    val time = listOfNotNull(previous?.bestTimeMs, timeMs).minOrNull()
    val mv = listOfNotNull(previous?.bestMoves, moves).minOrNull()
    return BestRecord(time, mv)
}
```

- [ ] **Step 4: PASS**

- [ ] **Step 5: Commit** `feat: merge best time and moves independently`

---

### Task 7: Catalog schema, parser, twelve placeholders

**Files:**
- Create: `docs/schemas/catalog.schema.json`
- Create: `app/src/main/java/ru/alexandrgert/gamespuzzle/domain/CatalogModels.kt`
- Create: `app/src/main/java/ru/alexandrgert/gamespuzzle/data/CatalogJson.kt`
- Create: `app/src/main/assets/catalog.json`
- Create: `scripts/gen_placeholder_puzzles.py`
- Create: `app/src/test/java/ru/alexandrgert/gamespuzzle/data/CatalogJsonTest.kt`
- Create: `app/src/test/resources/catalog_min.json`

**Interfaces:**
- Produces: `enum class Category { NATURE, ANIMALS, BIRDS, AQUATIC, TREES, FLOWERS }` JSON names `nature|animals|birds|aquatic|trees|flowers`
- `enum class Season { SPRING, SUMMER, AUTUMN, WINTER, ANY }` JSON `spring|summer|autumn|winter|any`
- `data class CatalogPuzzle(id, file, thumb, category, season, titleRu, license, attribution, sourceUrl)`
- `data class CatalogFile(schemaVersion: Int, puzzles: List<CatalogPuzzle>)`
- `object CatalogJson { fun parse(text: String): CatalogFile }` using kotlinx.serialization
- Parser skips nothing; tests use a fixture with all six categories and four seasons represented (12 entries). Missing asset at *runtime* is a later UI concern.

JSON schema `required` on those fields; `schema_version` integer; `puzzles` array.

- [ ] **Step 1: Write schema + failing parser test** with `catalog_min.json` of 12 rows (ids `ph-01` … `ph-12`, categories cycling the six, seasons cycling four, files `puzzles/ph-01.webp`).

- [ ] **Step 2: FAIL**

- [ ] **Step 3: Implement models + `CatalogJson.parse`.** kotlinx.serialization: add `@Serializable` with `@SerialName` for enums. `ignoreUnknownKeys = true`.

- [ ] **Step 4: Generate 12 solid-color WebP** via `scripts/gen_placeholder_puzzles.py` (Pillow): 1200×1200 puzzles, 256×256 thumbs, distinct colors, write under `app/src/main/assets/puzzles/` and `thumbs/`. Copy `catalog_min.json` to `assets/catalog.json`.

- [ ] **Step 5: `./gradlew test --tests ru.alexandrgert.gamespuzzle.data.CatalogJsonTest` PASS**

- [ ] **Step 6: Commit** `feat: add catalog schema, parser, and 12 placeholder images`

---

### Task 8: Semver and GitHub release parser

**Files:**
- Create: `domain/Semver.kt`, `data/GithubReleaseJson.kt`
- Test: `domain/SemverTest.kt`, `data/GithubReleaseJsonTest.kt`
- Test fixture: `app/src/test/resources/github_release_latest.json`

**Interfaces:**
- `data class Semver(val major: Int, val minor: Int, val patch: Int) : Comparable<Semver>`
- `fun parseSemver(raw: String): Semver` — strip leading `v`, take `X.Y.Z`, ignore `+build`
- `data class ReleaseInfo(val tag: String, val body: String, val apkUrl: String?)`
- `object GithubReleaseJson { fun parseLatest(json: String): ReleaseInfo }` — `tag_name`, `body`, first asset whose `name` ends with `.apk` → `browser_download_url`
- `data class UpdateCheckResult(ok, error, current: Semver, latest: Semver?, changelog: String, apkAssetUrl: String?, updateAvailable: Boolean)`
- `fun evaluateUpdate(current: Semver, release: ReleaseInfo): UpdateCheckResult` — `updateAvailable = latest > current`; always set `changelog = release.body` (empty string if missing)

- [ ] **Step 1–4:** TDD parse `v1.2.3` vs `1.2.0`; fixture JSON with one apk asset; `evaluateUpdate` sets `updateAvailable` false when equal and still copies body.

- [ ] **Step 5: Commit** `feat: parse GitHub Releases and compare semver`

---

### Task 9: Navigation, catalog, preview screens

**Files:**
- Create: `ui/Nav.kt`, `ui/catalog/CatalogScreen.kt`, `ui/preview/PreviewScreen.kt`
- Modify: `MainActivity.kt`, `strings.xml`
- Create: `data/AssetCatalog.kt` — `fun load(assets: AssetManager): CatalogFile` reads `catalog.json`, **drops** entries whose `file` or `thumb` is missing (`assets.open` try/catch), never crashes.

**Interfaces:**
- Routes: `catalog`, `preview/{id}/{source}` where `source` is `builtin` or `user`, `play/{id}/{source}/{n}`, `settings`, `myphotos`, `credits`
- Catalog filters: selected `Category?` (null = all) and `Season?` (null = all seasons; `ANY` photos always match a season filter). Grid of thumbs. Buttons to settings, my photos, credits.
- Preview: full image, toggle `GridSize.FIVE` / `SIX` (both enabled), button start → `play`.

Strings to add (Russian): `action_settings`, `action_my_photos`, `action_credits`, `action_start`, `grid_5`, `grid_6`, `filter_all`, `category_*`, `season_*`.

- [ ] **Step 1:** Add strings.
- [ ] **Step 2:** Implement `NavHost` in `MainActivity`.
- [ ] **Step 3:** Catalog + Preview composables; load thumbs with `BitmapFactory.decodeStream(assets.open(path))`.
- [ ] **Step 4:** `./gradlew test` still PASS (no UI tests required).
- [ ] **Step 5: Commit** `feat: add catalog filters and puzzle preview`

---

### Task 10: Play screen and PlayViewModel

**Files:**
- Create: `ui/play/PlayViewModel.kt`, `ui/play/PlayScreen.kt`
- Test: `ui/play/PlayViewModelTest.kt` (JVM: construct VM with a fake bitmap slicer or test only engine wiring via a thin `PlayState`)

**Interfaces:**
- `data class PlayState(val board: Board, val selected: Cell?, val elapsedMs: Long, val moves: Int, val won: Boolean, val peek: Boolean, val lastReverted: Boolean)`
- `class PlayViewModel` :
  - `fun start(size: GridSize, random: Random)` — `board = BoardEngine.shuffle`
  - `fun onCell(cell: Cell)` — if locked, ignore; if `selected==null` set selected; if same cell clear; else `trySwap`; on `Applied` clear selection, `moves++` if stats enabled (stats flag passed in constructor); on `Reverted` set `lastReverted` (UI animates back — for v1, just clear selection and flash); if `isWin` set `won`
  - Peek toggles `peek`; does not increment moves
- PlayScreen: `n×n` `Canvas`/`Image` grid, each cell the cropped bitmap of `tileId` (crop home rectangle of the source bitmap). Locked cells drawn normally (already at home). Selected cell outlined. Button `Картинка` (`action_peek`). If stats enabled show time + moves. Back: `AlertDialog` abandon (`confirm_abandon`). On `won` navigate to win dialog.

Bitmap slice: `fun tileBitmap(src: Bitmap, tileId: Int, n: Int): Bitmap` — home row/col of tileId, `src` scaled square.

- [ ] **Step 1:** JVM tests: shuffle start not win; two-step attach sequence using engine through a `PlayController` if ViewModel needs Android — extract `class PlaySession(size, statsEnabled, random)` in `domain/PlaySession.kt` without Android, test that; ViewModel wraps it.

Add `PlaySession` in domain:

```kotlin
class PlaySession(
    val size: GridSize,
    val statsEnabled: Boolean,
    random: java.util.Random,
) {
    var board: Board = BoardEngine.shuffle(size, random)
        private set
    var selected: Cell? = null
        private set
    var moves: Int = 0
        private set
    fun tap(cell: Cell): MoveResult? { /* selection + trySwap; increment moves only on Applied && statsEnabled */ }
    fun isWin(): Boolean = BoardEngine.isWin(board)
}
```

TDD `PlaySession` then thin ViewModel.

- [ ] **Step 2–4:** implement session + UI
- [ ] **Step 5: Commit** `feat: play session with lock-in-place puzzle board`

---

### Task 11: Win dialog, settings, DataStore records

**Files:**
- Create: `data/SettingsStore.kt`, `data/RecordsStore.kt`, `ui/settings/SettingsScreen.kt`, `ui/play/WinDialog.kt`
- Modify: `strings.xml`, `PlayScreen.kt`

**Interfaces:**
- Settings keys: `stats_enabled` default **false**; `update_download_mode` `confirm` | `immediate` default `confirm`; `last_update_check_at` string ISO; `dismissed_update_version` string
- Records: preferences key `"rec:${puzzleId}:${n}"` value `"timeMs,moves"`; `suspend fun load/save`
- Win dialog: `win_title`; if stats on show time, moves, `win_new_best_time` / `win_new_best_moves` when `mergeRecord` improved a field; buttons `win_again` (same id+size), `win_catalog`
- Settings: switches for stats and download mode; version `stringResource` + `VERSION`/`versionName`; button `action_check_update` (no-op until Task 14)

- [ ] **Step 1:** Write `RecordsStore` merge using `mergeRecord` — unit test a pure helper `fun serialize/parse BestRecord` if store is Android-only.
- [ ] **Step 2:** Settings + win UI
- [ ] **Step 3: Commit** `feat: settings for stats and persist best results`

---

### Task 12: User photos import and delete

**Files:**
- Create: `platform/UserFiles.kt`, `data/UserPuzzlesStore.kt`, `ui/myphotos/MyPhotosScreen.kt`

**Interfaces:**
- `user_puzzles.json` in `filesDir`: `{ "puzzles": [ { "id": "user:uuid", "file": "user/uuid.webp" } ] }`
- Import: `ActivityResultContracts.GetContent()` `image/*` → decode Bitmap → if `min(w,h) < 512` snackbar `error_image_too_small` and delete nothing → else center-crop square, compress WebP 90% to `filesDir/user/{uuid}.webp`, append index. On decode fail: `error_image_open`.
- Delete: remove file, index row, and records keys `user:{uuid}:5` and `:6`.
- Catalog screen section or My photos list; preview/play `source=user` loads from `UserFiles`.

- [ ] **Step 1–3:** implement
- [ ] **Step 4: Commit** `feat: import and delete user puzzle photos`

---

### Task 13: Credits

**Files:**
- Create: `ui/credits/CreditsScreen.kt`
- Modify: `strings.xml`

List each builtin `CatalogPuzzle` as `titleRu — attribution — license` plus `source_url` as clickable `https` text. User photos not listed.

- [ ] **Commit** `feat: show bundled photo attributions`

---

### Task 14: Update check, changelog, download, install

**Files:**
- Create: `data/UpdateChecker.kt`, `platform/ApkInstaller.kt`
- Modify: `SettingsScreen.kt`, `AndroidManifest.xml` (already has install permission)
- Test: reuse `evaluateUpdate` (already Task 8)

**Interfaces:**
- `UpdateChecker.check(client: OkHttpClient, current: Semver): UpdateCheckResult` GET `https://api.github.com/repos/alexandrgert/games-puzzle/releases/latest` with `User-Agent: games-puzzle/<version>`. On IO/HTTP: `ok=false`, `error` mapped to `error_offline` in UI (do not parse body as changelog).
- Settings: always show `result.changelog` in a dialog when `ok` (including “already latest”: title `update_current` + body). If `updateAvailable && mode==immediate` start download after dialog open. If `confirm`, button `action_download`.
- Download APK to `cacheDir/updates/games-puzzle.apk`; on failure delete partial file; snackbar `error_download`.
- `ApkInstaller.install(context, file)` via FileProvider URI + `ACTION_VIEW` / `ACTION_INSTALL_PACKAGE`. If blocked, snackbar `error_install_permission`.

- [ ] **Commit** `feat: check GitHub Releases, show changelog, download APK`

---

### Task 15: CI, version sync, keystore restore

**Files:**
- Create: `scripts/check_version_sync.py`, `scripts/write_android_keystore_from_env.py`, `.github/workflows/android.yml`
- Create: `docs/github-release-v0.1.0.md` (changelog body for first tag; do not publish until user asks)

**Interfaces:**
- `check_version_sync.py` reads `VERSION`, `app/build.gradle.kts` `versionName` and `versionCode`; exit 0 prints `OK`; else exit 1
- `write_android_keystore_from_env.py` writes `release.keystore` from `ANDROID_KEYSTORE_BASE64`; **exits 1 if env empty** (CI release job)
- Workflow on `push`/`pull_request` to `main`:
  1. checkout
  2. Python: validate `catalog.json` against `docs/schemas/catalog.schema.json` (`pip install jsonschema`)
  3. `python scripts/check_version_sync.py`
  4. setup JDK 17, Android SDK
  5. `./gradlew test`
  6. `python scripts/write_android_keystore_from_env.py` then `./gradlew assembleRelease` with env passwords — **only if secrets present**; if secrets missing, skip assemble and still succeed tests on PRs; on `main` default branch **fail** assemble when secrets missing (no debug-key fallback)
  7. upload artifact `games-puzzle-${VERSION}.apk`

Do **not** run `assembleRelease` on the agent laptop.

- [ ] **Step 1:** Write both scripts with a local dry-run: `python scripts/check_version_sync.py` → `OK`
- [ ] **Step 2:** Workflow YAML
- [ ] **Step 3: Commit** `ci: validate catalog, tests, and signed APK on GitHub`

---

### Task 16: Replace placeholders with 12 Wikimedia photos

**Files:**
- Modify: `app/src/main/assets/catalog.json`, `puzzles/*.webp`, `thumbs/*.webp`

Pick 12 Russia-themed Commons files (PD or CC BY), covering all six categories and all four seasons at least once. For each: download original, resize long edge 1200 square-crop, WebP, thumb 256, fill `title_ru`, `license`, `attribution`, `source_url`. Do not use copyrighted stock.

If a file’s license cannot be verified, skip it and choose another. Keep `id` stable (`kvasny-altai-spring`, etc.).

- [ ] **Step 1:** Verify licenses on the file pages
- [ ] **Step 2:** Convert and update catalog
- [ ] **Step 3:** `python -c` jsonschema validate + `./gradlew test`
- [ ] **Step 4: Commit** `content: replace placeholders with 12 free Russia photos`

Do not grow to 200–300 in this plan; catalog remains data-only.

---

## Self-review (spec coverage)

| Spec item | Task |
|-----------|------|
| No empty cells, swap, revert | 2 |
| Join → snap home + lock, group does not travel | 3 |
| Attach to locked group at home | 4 |
| Shuffle playable, win all locked | 5 |
| Optional stats + bests | 6, 11 |
| Catalog JSON + filters + seasons | 7, 9 |
| 12 then 200–300 without code change | 7, 16 (growth = more JSON later) |
| Preview then shuffle on start | 9, 10 |
| 5×5 and 6×6 always | 9 |
| User import/delete | 12 |
| Credits / attribution | 13 |
| Update changelog + download modes | 8, 14 |
| CI-only signed APK, VERSION sync | 1, 15 |
| Russian strings / i18n slots | 1, 9+ |
| Poco portrait width board | 10 |
| Peek button, abandon dialog | 10 |
| minSdk 26 / id `ru.alexandrgert.gamespuzzle` | 1 |
| No local assemble | 15 + Global Constraints |

No TBD/TODO placeholders remain in tasks. Names `trySwap`, `MoveResult`, `CatalogJson.parse`, `evaluateUpdate`, `PlaySession` are consistent across tasks.
