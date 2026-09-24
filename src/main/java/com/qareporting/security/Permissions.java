package com.qareporting.security;

import com.qareporting.entity.Role;
import com.qareporting.entity.User;

import java.util.Set;

/**
 * Matrice des permissions du cahier des charges (§6), en un seul endroit, partagée par
 * l'API REST et l'interface JSF pour qu'aucune des deux ne soit plus permissive que l'autre.
 * La portée des données (quel objet un rôle peut voir) est vérifiée à part, par les
 * méthodes canView(...) des services.
 */
public final class Permissions {

    /** Créer une campagne et faire évoluer son statut. */
    public static final Set<String> MANAGE_CAMPAIGNS = Set.of(Role.ADMIN, Role.QA_LEAD);
    /**
     * Créer / modifier un cas de test : les QA rédigent leurs propres cas (un test qu'ils
     * créent leur est assigné d'office, ils ne modifient que ceux qui leur sont assignés).
     * Supprimer reste réservé à l'admin (DELETE) pour ne pas perdre l'historique d'exécution.
     */
    public static final Set<String> MANAGE_TESTS = Set.of(Role.ADMIN, Role.QA_LEAD, Role.QA);
    /** Répartir les tests entre testeurs : rôle du QA Lead (et de l'admin). */
    public static final Set<String> ASSIGN_TESTS = Set.of(Role.ADMIN, Role.QA_LEAD);
    public static final Set<String> DELETE = Set.of(Role.ADMIN);
    public static final Set<String> EXECUTE_TESTS = Set.of(Role.ADMIN, Role.QA_LEAD, Role.QA);
    public static final Set<String> ASSIGN_DEFECTS = Set.of(Role.ADMIN, Role.QA_LEAD);
    public static final Set<String> LOG_ACTIVITY = Set.of(Role.ADMIN, Role.QA_LEAD, Role.QA);
    /** Déclarer une anomalie et la faire avancer (corrigée, fermée, revérifiée...). */
    public static final Set<String> WORK_ON_DEFECTS = Set.of(Role.ADMIN, Role.QA_LEAD, Role.QA);
    public static final Set<String> TEAM_VIEW = Set.of(Role.QA_LEAD);
    public static final Set<String> AUDIT_LOG = Set.of(Role.ADMIN);

    private Permissions() {
    }

    public static boolean allows(Set<String> allowed, String roleName) {
        return roleName != null && allowed.contains(roleName);
    }

    public static boolean allows(Set<String> allowed, User user) {
        return user != null && user.getRole() != null && allows(allowed, user.getRole().getName());
    }
}
