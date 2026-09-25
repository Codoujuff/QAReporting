package com.qareporting.security;

import com.qareporting.entity.AccessToken;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Règle de mot de passe, blocage après échecs répétés, expiration des jetons d'API. */
class AccountSecurityTest {

    @Test
    void weakPasswordsAreRefused() {
        assertEquals("err.passwordTooShort", PasswordPolicy.check("abc123", "a@b.c"));
        assertEquals("err.passwordLettersDigits", PasswordPolicy.check("seulementdeslettres", "a@b.c"));
        assertEquals("err.passwordLettersDigits", PasswordPolicy.check("1234567890123", "a@b.c"));
        assertEquals("err.passwordTooCommon", PasswordPolicy.check("Password123", "a@b.c"));
        assertEquals("err.passwordContainsEmail", PasswordPolicy.check("fatou.sarr2026", "fatou.sarr@demo.local"));
        assertNull(PasswordPolicy.check("Recette-Paiement-7", "fatou.sarr@demo.local"));
    }

    @Test
    void anAccountIsLockedAfterFiveFailuresThenReleased() {
        LoginThrottle throttle = new LoginThrottle();
        MutableClock clock = new MutableClock();
        throttle.setClock(clock);

        for (int i = 0; i < 4; i++) {
            throttle.recordFailure("Awa@Demo.local", "10.0.0.1");
        }
        assertEquals(0, throttle.minutesLocked("awa@demo.local", "10.0.0.2"), "4 échecs : pas encore bloqué");
        throttle.recordFailure("awa@demo.local", "10.0.0.1");
        assertEquals(15, throttle.minutesLocked("awa@demo.local", "10.0.0.2"), "bloqué, quelle que soit l'IP");

        clock.advance(Duration.ofMinutes(16));
        assertEquals(0, throttle.minutesLocked("awa@demo.local", "10.0.0.2"), "débloqué après 15 min");
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        LoginThrottle throttle = new LoginThrottle();
        MutableClock clock = new MutableClock();
        throttle.setClock(clock);
        for (int i = 0; i < 4; i++) {
            throttle.recordFailure("moussa@demo.local", null);
        }
        clock.advance(Duration.ofMinutes(20));
        throttle.recordFailure("moussa@demo.local", null);
        assertEquals(0, throttle.minutesLocked("moussa@demo.local", null));
    }

    @Test
    void oneIpTryingManyAccountsIsLockedToo() {
        LoginThrottle throttle = new LoginThrottle();
        throttle.setClock(new MutableClock());
        for (int i = 0; i < 20; i++) {
            throttle.recordFailure("compte" + i + "@demo.local", "203.0.113.9");
        }
        assertTrue(throttle.minutesLocked("nouveau@demo.local", "203.0.113.9") > 0);
    }

    @Test
    void tokensExpireAfterTwelveHoursOrTwoHoursIdle() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 12, 0);
        AccessToken token = new AccessToken();
        token.setCreatedAt(now.minusHours(1));
        token.setExpiresAt(token.getCreatedAt().plus(TokenService.LIFETIME));
        token.setLastUsedAt(now.minusMinutes(10));
        assertFalse(TokenService.isExpired(token, now));

        token.setLastUsedAt(now.minusHours(3));
        assertTrue(TokenService.isExpired(token, now), "inactif depuis plus de 2 h");

        token.setLastUsedAt(now.minusMinutes(1));
        assertTrue(TokenService.isExpired(token, now.plusHours(12)), "plus de 12 h après l'émission");

        token.setExpiresAt(null);
        assertTrue(TokenService.isExpired(token, now), "jeton émis avant la règle : expiré");
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-25T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
