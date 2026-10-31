package com.nibras.nova.core.common

import android.content.res.AssetManager
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Opens a bundled data asset that is stored as "<name>.gz" in the source tree.
 * The Android build (aapt2) un-gzips such assets and packages them as "<name>", so
 * opening "<name>.gz" throws FileNotFoundException on real builds. This tries both
 * names and only decompresses when the bytes really are gzip.
 */
object Assets {
    fun openText(assets: AssetManager, name: String): InputStream {
        val raw = runCatching { assets.open(name) }.getOrElse { e1 ->
            runCatching { assets.open("$name.gz") }.getOrElse { throw FileNotFoundException("$name(.gz): ${e1.message}") }
        }
        return maybeGunzip(raw)
    }

    fun maybeGunzip(raw: InputStream): InputStream {
        val b = BufferedInputStream(raw)
        b.mark(2)
        val m1 = b.read(); val m2 = b.read()
        b.reset()
        return if (m1 == 0x1f && m2 == 0x8b) GZIPInputStream(b) else b
    }
}
