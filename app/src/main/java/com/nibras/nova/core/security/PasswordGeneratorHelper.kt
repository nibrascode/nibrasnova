package com.nibras.nova.core.security

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log2

data class GeneratedPasswordResult(
    val password: String,
    val entropyBits: Double,
    val strengthLabel: String
)

/**
 * Stage 14.4: Cryptographic High-Entropy Password & Passphrase Generator.
 * Uses java.security.SecureRandom with character-set guarantees and calculates Shannon/pool entropy bits.
 */
@Singleton
class PasswordGeneratorHelper @Inject constructor() {

    private val secureRandom = SecureRandom()

    private val lowercaseChars = "abcdefghijkmnopqrstuvwxyz"
    private val uppercaseChars = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val digitChars = "23456789"
    private val symbolChars = "!@#\$%^&*()-_=+[]{}?"

    private val dicewareWords = listOf(
        "nova", "orbit", "cipher", "quantum", "cobalt", "velvet", "falcon", "zenith",
        "aurora", "matrix", "nebula", "prism", "vortex", "glacier", "obsidian", "titanium",
        "phoenix", "horizon", "cascade", "mercury", "eclipse", "solstice", "paradox", "beacon"
    )

    fun generateStrongPassword(
        length: Int = 20,
        includeUppercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): GeneratedPasswordResult {
        val safeLen = length.coerceIn(8, 64)
        val poolBuilder = StringBuilder(lowercaseChars)
        val mandatoryChars = mutableListOf(lowercaseChars[secureRandom.nextInt(lowercaseChars.length)])

        if (includeUppercase) {
            poolBuilder.append(uppercaseChars)
            mandatoryChars.add(uppercaseChars[secureRandom.nextInt(uppercaseChars.length)])
        }
        if (includeDigits) {
            poolBuilder.append(digitChars)
            mandatoryChars.add(digitChars[secureRandom.nextInt(digitChars.length)])
        }
        if (includeSymbols) {
            poolBuilder.append(symbolChars)
            mandatoryChars.add(symbolChars[secureRandom.nextInt(symbolChars.length)])
        }

        val pool = poolBuilder.toString()
        val resultChars = CharArray(safeLen)
        for (i in 0 until safeLen) {
            resultChars[i] = if (i < mandatoryChars.size) {
                mandatoryChars[i]
            } else {
                pool[secureRandom.nextInt(pool.length)]
            }
        }

        // Fisher-Yates cryptographic shuffle
        for (i in resultChars.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = resultChars[i]
            resultChars[i] = resultChars[j]
            resultChars[j] = tmp
        }

        val entropy = safeLen * log2(pool.length.toDouble())
        val label = when {
            entropy >= 110.0 -> L.s(R.string.t_military_post_quantum_grade_bits, entropy.toInt())
            entropy >= 80.0 -> L.s(R.string.t_very_strong_bits, entropy.toInt())
            entropy >= 60.0 -> L.s(R.string.t_strong_bits, entropy.toInt())
            else -> L.s(R.string.t_moderate_bits, entropy.toInt())
        }

        return GeneratedPasswordResult(
            password = String(resultChars),
            entropyBits = entropy,
            strengthLabel = label
        )
    }

    fun generateMemorablePassphrase(wordCount: Int = 4): String {
        val count = wordCount.coerceIn(3, 8)
        val picked = (1..count).map {
            dicewareWords[secureRandom.nextInt(dicewareWords.size)]
        }
        val num = secureRandom.nextInt(900) + 100
        return picked.joinToString("-") + "-$num"
    }
}
