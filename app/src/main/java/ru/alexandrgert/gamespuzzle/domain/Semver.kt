package ru.alexandrgert.gamespuzzle.domain

data class Semver(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<Semver> {
    override fun compareTo(other: Semver): Int = compareValuesBy(
        this,
        other,
        Semver::major,
        Semver::minor,
        Semver::patch,
    )
}

fun parseSemver(raw: String): Semver {
    val components = raw.removePrefix("v").substringBefore('+').split('.')
    require(components.size == 3) { "Expected semantic version X.Y.Z: $raw" }

    return Semver(
        major = components[0].toInt(),
        minor = components[1].toInt(),
        patch = components[2].toInt(),
    )
}
