package ru.alexandrgert.gamespuzzle.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemverTest {
    @Test
    fun parsesVersionWithLeadingV() {
        assertEquals(Semver(1, 2, 3), parseSemver("v1.2.3"))
    }

    @Test
    fun ignoresBuildMetadata() {
        assertEquals(Semver(1, 2, 3), parseSemver("1.2.3+build.7"))
    }

    @Test
    fun comparesVersionsByComponents() {
        assertTrue(parseSemver("v1.2.3") > parseSemver("1.2.0"))
    }
}
