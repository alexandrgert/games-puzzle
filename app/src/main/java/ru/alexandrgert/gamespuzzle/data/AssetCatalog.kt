package ru.alexandrgert.gamespuzzle.data

import android.content.res.AssetManager
import java.io.InputStream
import ru.alexandrgert.gamespuzzle.domain.CatalogFile

object AssetCatalog {
    fun load(assets: AssetManager): CatalogFile = load(assets::open)

    internal fun load(openAsset: (String) -> InputStream): CatalogFile {
        val catalog = runCatching {
            openAsset(CATALOG_PATH).bufferedReader().use { CatalogJson.parse(it.readText()) }
        }.getOrElse {
            return CatalogFile(schemaVersion = CURRENT_SCHEMA_VERSION, puzzles = emptyList())
        }

        return catalog.copy(
            puzzles = catalog.puzzles.filter { puzzle ->
                canOpen(openAsset, puzzle.file) && canOpen(openAsset, puzzle.thumb)
            },
        )
    }

    private fun canOpen(openAsset: (String) -> InputStream, path: String): Boolean =
        runCatching { openAsset(path).use { } }.isSuccess

    private const val CATALOG_PATH = "catalog.json"
    private const val CURRENT_SCHEMA_VERSION = 1
}
