package com.nibras.nova.core.security

import com.nibras.nova.core.common.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class FileChecksums(
    val sha256Hex: String,
    val md5Hex: String
)

@Singleton
class FileHashVerifier @Inject constructor(
    private val dispatchers: DispatcherProvider
) {
    suspend fun computeFileChecksums(filePath: String): Result<FileChecksums> = withContext(dispatchers.io) {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist on disk."))
            }
            val sha256 = MessageDigest.getInstance("SHA-256")
            val md5 = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(16 * 1024)
                var read = fis.read(buffer)
                while (read != -1) {
                    sha256.update(buffer, 0, read)
                    md5.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }
            Result.success(
                FileChecksums(
                    sha256Hex = sha256.digest().joinToString("") { "%02x".format(it) },
                    md5Hex = md5.digest().joinToString("") { "%02x".format(it) }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
