package com.qareporting.web.i18n;

import jakarta.enterprise.inject.spi.CDI;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Accès aux traductions depuis le code Java (messages d'erreur, libellés de graphiques...).
 * Même fichier que le « msg » des pages : com/qareporting/i18n/messages_{fr,en}.properties.
 *
 * Attention : une clé utilisée AVEC arguments passe par MessageFormat, où l'apostrophe
 * doit être doublée (« l''activité ») ; sans argument, le texte est rendu tel quel.
 */
public final class I18n {

    public static final String BUNDLE = "com.qareporting.i18n.messages";

    private I18n() {
    }

    public static Locale locale() {
        try {
            return CDI.current().select(LocaleBean.class).get().getLocale();
        } catch (RuntimeException noActiveSession) {
            return Locale.FRENCH;
        }
    }

    public static String t(String key, Object... args) {
        return t(locale(), key, args);
    }

    /** Traduction, ou {@code null} si la clé n'existe pas (pour les libellés facultatifs). */
    public static String find(String key) {
        try {
            return ResourceBundle.getBundle(BUNDLE, locale()).getString(key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    static String t(Locale locale, String key, Object... args) {
        String pattern;
        try {
            pattern = ResourceBundle.getBundle(BUNDLE, locale).getString(key);
        } catch (MissingResourceException e) {
            return "???" + key + "???";
        }
        return args.length == 0 ? pattern : new MessageFormat(pattern, locale).format(args);
    }
}
