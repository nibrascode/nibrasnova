package com.nibras.nova.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class AssetsTest {
    @Test fun plainAndGzipBothRead() {
        val text = "AZ\tBakı\n"
        assertEquals(text, Assets.maybeGunzip(ByteArrayInputStream(text.toByteArray())).reader().readText())
        val gz = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(text.toByteArray()) } }.toByteArray()
        assertEquals(text, Assets.maybeGunzip(ByteArrayInputStream(gz)).reader().readText())
    }
}
