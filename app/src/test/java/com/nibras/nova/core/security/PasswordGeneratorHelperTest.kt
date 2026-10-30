package com.nibras.nova.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasswordGeneratorHelperTest {

    private lateinit var generator: PasswordGeneratorHelper

    @Before
    fun setUp() {
        generator = PasswordGeneratorHelper()
    }

    @Test
    fun `generateStrongPassword produces requested length and high entropy`() {
        val result = generator.generateStrongPassword(
            length = 24,
            includeUppercase = true,
            includeDigits = true,
            includeSymbols = true
        )
        assertEquals(24, result.password.length)
        assertTrue(result.entropyBits > 120.0)
    }

    @Test
    fun `generateMemorablePassphrase produces hyphenated multi-word passphrase`() {
        val phrase = generator.generateMemorablePassphrase(4)
        val parts = phrase.split("-")
        assertEquals(5, parts.size) // 4 words + 3-digit suffix
    }
}
