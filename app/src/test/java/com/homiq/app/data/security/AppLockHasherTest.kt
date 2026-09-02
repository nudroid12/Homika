package com.homiq.app.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockHasherTest {
    @Test
    fun hashVerifiesOnlyMatchingPin() {
        val value = AppLockHasher.hashNew("2468")

        assertTrue(
            AppLockHasher.verify(
                "2468",
                value.saltBase64,
                value.hashBase64,
            ),
        )
        assertFalse(
            AppLockHasher.verify(
                "1357",
                value.saltBase64,
                value.hashBase64,
            ),
        )
    }

    @Test
    fun pinRulesAcceptFourToEightDigitsOnly() {
        assertTrue(AppLockService.isValidPin("1234"))
        assertTrue(AppLockService.isValidPin("12345678"))
        assertFalse(AppLockService.isValidPin("123"))
        assertFalse(AppLockService.isValidPin("123456789"))
        assertFalse(AppLockService.isValidPin("12a4"))
    }
}
