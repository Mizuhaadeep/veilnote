package com.glassnote.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * SHA-256 password hashing for per-note locks.
 *
 * Only the hash is ever persisted in Room and in the `.veil` JSON. There is no
 * plaintext password field.
 */
object PasswordHasher {

    private val HEX = "0123456789abcdef".toCharArray()

    fun sha256(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        val chars = CharArray(bytes.size * 2)
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            chars[index * 2] = HEX[value ushr 4]
            chars[index * 2 + 1] = HEX[value and 0x0F]
        }
        return String(chars)
    }

    fun verify(password: String, expectedHash: String?): Boolean {
        if (expectedHash.isNullOrBlank()) return false
        val actual = sha256(password).toByteArray(StandardCharsets.UTF_8)
        val expected = expectedHash.lowercase().toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.isEqual(actual, expected)
    }
}
