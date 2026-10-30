package com.nibras.nova.core.security

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NovaBackupBookmark(
    val title: String,
    val url: String
)

@Serializable
data class NovaBackupShortcut(
    val title: String,
    val url: String
)

@Serializable
data class NovaBackupUserScript(
    val name: String,
    val matchDomainPattern: String,
    val scriptType: String,
    val codeContent: String,
    val isEnabled: Boolean
)

@Serializable
data class NovaBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val bookmarks: List<NovaBackupBookmark> = emptyList(),
    val shortcuts: List<NovaBackupShortcut> = emptyList(),
    val userScripts: List<NovaBackupUserScript> = emptyList(),
    val cookieWhitelistDomains: List<String> = emptyList(),
    val rssFeedUrls: List<String> = emptyList()
)

@Serializable
data class EncryptedNovaArchive(
    val magic: String = "NIBRAS_NOVA_AES256_GCM_V1",
    val saltBase64: String,
    val ivBase64: String,
    val ciphertextBase64: String
)

@Singleton
class EncryptedBackupManager @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    companion object {
        private const val PBKDF2_ITERATIONS = 60_000
        private const val KEY_LENGTH_BITS = 256
        private const val GCM_TAG_BITS = 128
    }

    fun encryptBackupPayload(payload: NovaBackupPayload, passphrase: String): Result<String> {
        if (passphrase.length < 4) {
            return Result.failure(IllegalArgumentException("Backup password must be at least 4 characters."))
        }
        return try {
            val random = SecureRandom()
            val salt = ByteArray(16).also { random.nextBytes(it) }
            val iv = ByteArray(12).also { random.nextBytes(it) }

            val secretKey = deriveAesKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))

            val plainBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
            val cipherBytes = cipher.doFinal(plainBytes)

            val envelope = EncryptedNovaArchive(
                saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
                ciphertextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            )
            Result.success(json.encodeToString(envelope))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decryptBackupPayload(encryptedJson: String, passphrase: String): Result<NovaBackupPayload> {
        return try {
            val envelope = json.decodeFromString<EncryptedNovaArchive>(encryptedJson.trim())
            val salt = Base64.decode(envelope.saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(envelope.ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(envelope.ciphertextBase64, Base64.NO_WRAP)

            val secretKey = deriveAesKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))

            val plainBytes = cipher.doFinal(cipherBytes)
            val payload = json.decodeFromString<NovaBackupPayload>(String(plainBytes, Charsets.UTF_8))
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(SecurityException("Decryption failed: Invalid password or corrupted .novabackup archive.", e))
        }
    }

    private fun deriveAesKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
