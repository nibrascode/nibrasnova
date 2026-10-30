package com.nibras.nova.core.security

import com.nibras.nova.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FileHashVerifierTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    @Test
    fun `computeFileChecksums calculates exact SHA-256 and MD5 hex digests`() = runTest {
        val tempFile = File.createTempFile("nova_hash_test", ".txt")
        tempFile.writeText("NibrasNovaBrowser", Charsets.UTF_8)

        val verifier = FileHashVerifier(dispatchers)
        val result = verifier.computeFileChecksums(tempFile.absolutePath)

        assertTrue(result.isSuccess)
        val sums = result.getOrThrow()
        assertEquals(64, sums.sha256Hex.length)
        assertEquals(32, sums.md5Hex.length)
        tempFile.delete()
    }
}
