package com.qareporting.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService();

    @Test
    void generatedTokensAreUnique() {
        assertNotEquals(tokenService.generatePlainToken(), tokenService.generatePlainToken());
    }

    @Test
    void hashingIsDeterministic() {
        String token = tokenService.generatePlainToken();
        assertEquals(tokenService.hash(token), tokenService.hash(token));
    }

    @Test
    void differentTokensHashDifferently() {
        String a = tokenService.generatePlainToken();
        String b = tokenService.generatePlainToken();
        assertNotEquals(tokenService.hash(a), tokenService.hash(b));
    }

    @Test
    void hashNeverEqualsThePlainToken() {
        // The whole point of hashing before storage: a stolen DB dump must
        // not contain anything directly replayable as a bearer token.
        String token = tokenService.generatePlainToken();
        assertNotEquals(token, tokenService.hash(token));
    }
}
