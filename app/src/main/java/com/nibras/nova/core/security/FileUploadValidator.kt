package com.nibras.nova.core.security

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ValidatedUploadFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
)

@Singleton
class FileUploadValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MAX_UPLOAD_SIZE_BYTES = 200L * 1024L * 1024L // 200 MB ceiling
    }

    fun validateSelectedUri(uri: Uri): Result<ValidatedUploadFile> {
        // Block raw file:// paths pointing inside our private application sandbox
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = uri.path.orEmpty()
            val appDataDir = context.applicationInfo.dataDir
            if (path.startsWith(appDataDir)) {
                return Result.failure(SecurityException("Uploading internal application sandbox files is forbidden."))
            }
        }

        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        var displayName = "upload_file"
        var sizeBytes = 0L

        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                    displayName = cursor.getString(nameIdx)
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }

        if (sizeBytes > MAX_UPLOAD_SIZE_BYTES) {
            return Result.failure(IllegalArgumentException("File exceeds maximum safe upload limit (200 MB)."))
        }

        return Result.success(
            ValidatedUploadFile(
                uri = uri,
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes
            )
        )
    }
}
