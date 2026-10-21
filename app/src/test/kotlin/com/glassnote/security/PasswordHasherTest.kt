package com.glassnote.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun sha256_matchesKnownDigest() {
        assertEquals(
            "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4",
            PasswordHasher.sha256("1234"),
        )
    }

    @Test
    fun verify_acceptsMatchingPassword() {
        assertTrue(PasswordHasher.verify("1234", PasswordHasher.sha256("1234")))
    }

    @Test
    fun verify_rejectsWrongPasswordAndNullHash() {
        assertFalse(PasswordHasher.verify("wrong", PasswordHasher.sha256("1234")))
        assertFalse(PasswordHasher.verify("1234", null))
    }
}
