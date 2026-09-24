package com.qareporting.web;

import com.qareporting.web.i18n.I18n;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.time.LocalDate;

/**
 * Libellés traduits des valeurs techniques (enums, noms de rôles, types d'activité)
 * affichées dans l'IHM : sans lui, les badges et listes montraient « in_progress »,
 * « not_run », « qa_lead »... Les textes vivent dans messages_{fr,en}.properties ;
 * la clé d'un enum est préfixée par son entité (« enum.Campaign.Status.blocked » vs
 * « enum.Test.Status.blocked ») pour accorder le genre en français.
 * Usage : #{labels.of(d.status)}, #{labels.role(u.role.name)}.
 */
@Named
@ApplicationScoped
public class Labels {

    /** Libellé d'une valeur d'enum ; la valeur brute si elle n'est pas répertoriée. */
    public String of(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof Enum<?> e) {
            Class<?> type = e.getDeclaringClass();
            String owner = type.getEnclosingClass() != null ? type.getEnclosingClass().getSimpleName() + "." : "";
            return orRaw("enum." + owner + type.getSimpleName() + "." + e.name(), e.name());
        }
        return value.toString();
    }

    public String role(String roleName) {
        return roleName == null ? "—" : orRaw("role." + roleName, roleName);
    }

    /** Description d'un rôle système traduite ; sinon celle saisie en base. */
    public String roleDescription(String roleName, String stored) {
        return roleName == null ? stored : orRaw("roleDesc." + roleName, stored);
    }

    /**
     * Les types d'activité sont stockés en base sous leur libellé français (valeurs
     * historiques de la version Laravel) : on traduit l'affichage sans toucher aux données.
     */
    public String activityType(String stored) {
        return stored == null ? "—" : orRaw("activityType." + stored, stored);
    }

    /** Commentaire d'historique : « Assignée à X. » est généré par l'application, donc traduit. */
    public String historyComment(String stored) {
        if (stored == null || stored.isBlank()) { // EL passe "" pour un null
            return "—";
        }
        if (stored.startsWith("Assignée à ") && stored.endsWith(".")) {
            return I18n.t("history.assignedTo", stored.substring("Assignée à ".length(), stored.length() - 1));
        }
        return stored;
    }

    /** Taille lisible : 830 o, 12,4 Ko, 3,1 Mo. */
    public String fileSize(long bytes) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(I18n.locale());
        nf.setMaximumFractionDigits(1);
        boolean en = "en".equals(I18n.locale().getLanguage());
        if (bytes < 1024) {
            return bytes + (en ? " B" : " o");
        }
        if (bytes < 1024 * 1024) {
            return nf.format(bytes / 1024.0) + (en ? " KB" : " Ko");
        }
        return nf.format(bytes / (1024.0 * 1024)) + (en ? " MB" : " Mo");
    }

    /** Date du jour, affichée dans l'en-tête. */
    public LocalDate getToday() {
        return LocalDate.now();
    }

    private static String orRaw(String key, String raw) {
        String label = I18n.find(key);
        return label != null ? label : raw;
    }
}
