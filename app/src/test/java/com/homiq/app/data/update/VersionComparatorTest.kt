package com.homiq.app.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun newerPatchIsDetected() {
        assertTrue(VersionComparator.isNewer("v1.0.1", "1.0.0"))
    }

    @Test
    fun equalVersionIsNotNewer() {
        assertFalse(VersionComparator.isNewer("v1.0.0", "1.0.0"))
    }

    @Test
    fun olderReleaseIsNotNewer() {
        assertFalse(VersionComparator.isNewer("0.9.9", "1.0.0"))
    }

    @Test
    fun brandedTagIsNormalized() {
        assertTrue(VersionComparator.isNewer("Homika-v1.2.0", "1.1.9"))
    }
}
