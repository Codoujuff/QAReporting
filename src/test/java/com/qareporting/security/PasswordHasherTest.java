package com.qareporting.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashedPasswordMatchesOriginal() {
        String hash = hasher.hash("password");
        assertTrue(hasher.matches("password", hash));
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        String hash = hasher.hash("password");
        assertFalse(hasher.matches("not-the-password", hash));
    }

    @Test
    void sameInputProducesDifferentHashesEachTime() {
        // BCrypt salts every hash independently — two hashes of the same
        // password must never be equal, or a rainbow-table attack becomes trivial.
        assertNotEquals(hasher.hash("password"), hasher.hash("password"));
    }

    @Test
    void nullHashNeverMatches() {
        assertFalse(hasher.matches("password", null));
    }
}
