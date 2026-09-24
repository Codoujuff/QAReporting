package com.qareporting.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.mindrot.jbcrypt.BCrypt;

/** Equivalent of Laravel's Hash::make()/Hash::check(). */
@ApplicationScoped
public class PasswordHasher {

    public String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public boolean matches(String plainPassword, String hash) {
        return hash != null && BCrypt.checkpw(plainPassword, hash);
    }
}
