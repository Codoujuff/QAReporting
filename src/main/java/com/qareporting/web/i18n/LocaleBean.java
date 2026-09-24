package com.qareporting.web.i18n;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

/**
 * Langue d'affichage de la session (français par défaut, anglais en option).
 * Changée par un simple lien GET « ?lang=en » intercepté par {@link LocaleFilter},
 * appliquée à chaque vue par le f:view du layout.
 */
@Named
@SessionScoped
public class LocaleBean implements Serializable {

    public static final List<String> SUPPORTED = List.of("fr", "en");

    private Locale locale = Locale.FRENCH;

    public Locale getLocale() {
        return locale;
    }

    public String getLanguage() {
        return locale.getLanguage();
    }

    public void setLanguage(String language) {
        if (SUPPORTED.contains(language)) {
            locale = Locale.forLanguageTag(language);
        }
    }

    public boolean isEnglish() {
        return "en".equals(locale.getLanguage());
    }
}
