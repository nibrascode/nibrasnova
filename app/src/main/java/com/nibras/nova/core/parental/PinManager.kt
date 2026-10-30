package com.nibras.nova.core.parental

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Key-value storage abstraction so the logic is unit-testable. */
interface KvStore {
    fun getString(k: String): String?; fun putString(k: String, v: String?)
    fun getLong(k: String, d: Long): Long; fun putLong(k: String, v: Long)
}

sealed class PinResult {
    data object Ok : PinResult()
    data class Wrong(val attemptsLeft: Int) : PinResult()
    data class LockedOut(val untilMs: Long) : PinResult()
    data object NoPin : PinResult()
}

/**
 * Parental PIN: 4–6 digits, PBKDF2-HMAC-SHA256 with a random 16-byte salt. Stored on device only.
 * After 5 wrong attempts: lockout 30 s, doubling on each further lockout (max 1 h).
 */
class PinManager(private val kv: KvStore, private val clock: () -> Long = System::currentTimeMillis) {
    companion object {
        const val ITERATIONS = 60_000
        const val MAX_ATTEMPTS = 5
        const val BASE_LOCK_MS = 30_000L
        const val MAX_LOCK_MS = 3_600_000L
        fun isValidPin(pin: String) = pin.length in 4..6 && pin.all { it in '0'..'9' }

        fun hash(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
            val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
            return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
        }
    }
    private val b64e = Base64.getEncoder(); private val b64d = Base64.getDecoder()

    val hasPin: Boolean get() = kv.getString("pin_hash") != null

    fun setPin(pin: String) {
        require(isValidPin(pin)) { "PIN must be 4–6 digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        kv.putString("pin_salt", b64e.encodeToString(salt))
        kv.putString("pin_hash", b64e.encodeToString(hash(pin, salt)))
        kv.putLong("pin_iter", ITERATIONS.toLong())
        resetFailures(); kv.putLong("pin_lock_level", 0)
    }

    fun clearPin() { kv.putString("pin_hash", null); kv.putString("pin_salt", null); resetFailures(); kv.putLong("pin_lock_level", 0) }

    fun lockedUntil(): Long = kv.getLong("pin_lock_until", 0).takeIf { it > clock() } ?: 0L

    fun verify(pin: String): PinResult {
        val h = kv.getString("pin_hash") ?: return PinResult.NoPin
        val until = lockedUntil(); if (until > 0) return PinResult.LockedOut(until)
        val salt = b64d.decode(kv.getString("pin_salt") ?: return PinResult.NoPin)
        val iter = kv.getLong("pin_iter", ITERATIONS.toLong()).toInt()
        val ok = isValidPin(pin) && MessageDigest.isEqual(hash(pin, salt, iter), b64d.decode(h))
        if (ok) { resetFailures(); kv.putLong("pin_lock_level", 0); return PinResult.Ok }
        val fails = kv.getLong("pin_fails", 0) + 1
        if (fails >= MAX_ATTEMPTS) {
            val level = kv.getLong("pin_lock_level", 0)
            val dur = (BASE_LOCK_MS shl level.toInt().coerceAtMost(7)).coerceAtMost(MAX_LOCK_MS)
            val t = clock() + dur
            kv.putLong("pin_lock_until", t); kv.putLong("pin_lock_level", level + 1); kv.putLong("pin_fails", 0)
            return PinResult.LockedOut(t)
        }
        kv.putLong("pin_fails", fails)
        return PinResult.Wrong((MAX_ATTEMPTS - fails).toInt())
    }

    private fun resetFailures() { kv.putLong("pin_fails", 0); kv.putLong("pin_lock_until", 0) }
}
