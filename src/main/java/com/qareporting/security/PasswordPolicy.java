package com.qareporting.security;

import java.util.Locale;
import java.util.Set;

/**
 * Règle de mot de passe, appliquée partout où un mot de passe est choisi (création par
 * l'admin, écran Paramètres, API) : 10 caractères minimum, au moins une lettre et un
 * chiffre, pas l'adresse e-mail ni un mot de passe courant. Volontairement simple :
 * la longueur protège mieux que des règles de complexité difficiles à retenir.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;

    private static final Set<String> COMMON = Set.of(
            "password", "password1", "password123", "motdepasse", "motdepasse1", "azerty", "azertyuiop",
            "azerty123", "qwerty", "qwerty123", "123456789", "1234567890", "admin", "admin123",
            "administrateur", "bienvenue", "bienvenue1", "welcome", "welcome1", "soleil", "doudou",
            "qareporting", "qa-reporting", "demo2026");

    private PasswordPolicy() {
    }

    /** Clé de traduction de l'erreur, ou null si le mot de passe est acceptable. */
    public static String check(String password, String email) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "err.passwordTooShort";
        }
        if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            return "err.passwordLettersDigits";
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower)) {
            return "err.passwordTooCommon";
        }
        if (email != null && !email.isBlank()) {
            String local = email.toLowerCase(Locale.ROOT).split("@")[0];
            if (local.length() >= 3 && lower.contains(local)) {
                return "err.passwordContainsEmail";
            }
        }
        return null;
    }
}
