package com.qareporting.security;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues opaque bearer tokens and hashes them for storage — mirrors how
 * Laravel Sanctum only ever persists a token's SHA-256 hash, never the
 * plaintext, so a leaked database dump alone can't be replayed as a token.
 */
@ApplicationScoped
public class TokenService {

    private final SecureRandom random = new SecureRandom();

    public String generatePlainToken() {
        byte[] bytes = new byte[40];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainToken.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
