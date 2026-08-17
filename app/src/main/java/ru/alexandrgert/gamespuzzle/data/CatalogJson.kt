package ru.alexandrgert.gamespuzzle.data

import kotlinx.serialization.json.Json
import ru.alexandrgert.gamespuzzle.domain.CatalogFile

object CatalogJson {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(text: String): CatalogFile = json.decodeFromString(text)
}
