package ru.alexandrgert.gamespuzzle.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Category {
    @SerialName("nature")
    NATURE,

    @SerialName("animals")
    ANIMALS,

    @SerialName("birds")
    BIRDS,

    @SerialName("aquatic")
    AQUATIC,

    @SerialName("trees")
    TREES,

    @SerialName("flowers")
    FLOWERS,
}

@Serializable
enum class Season {
    @SerialName("spring")
    SPRING,

    @SerialName("summer")
    SUMMER,

    @SerialName("autumn")
    AUTUMN,

    @SerialName("winter")
    WINTER,

    @SerialName("any")
    ANY,
}

@Serializable
data class CatalogPuzzle(
    val id: String,
    val file: String,
    val thumb: String,
    val category: Category,
    val season: Season,
    @SerialName("title_ru")
    val titleRu: String,
    val license: String,
    val attribution: String,
    @SerialName("source_url")
    val sourceUrl: String,
)

@Serializable
data class CatalogFile(
    @SerialName("schema_version")
    val schemaVersion: Int,
    val puzzles: List<CatalogPuzzle>,
)
