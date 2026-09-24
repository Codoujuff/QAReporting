package com.qareporting.service;

import com.qareporting.entity.Project;
import com.qareporting.entity.Role;
import com.qareporting.entity.Team;
import com.qareporting.entity.User;

import java.util.Objects;

/**
 * Petits prédicats communs aux méthodes canView(...) des services. Ils reprennent,
 * objet par objet, les mêmes règles que les requêtes listForUser(...) : un objet
 * absent de la liste d'un utilisateur ne doit pas non plus s'ouvrir par son id.
 * Comparaison par id, car les entités peuvent venir de contextes de persistance différents.
 */
final class Scope {

    private Scope() {
    }

    static boolean seesEverything(User viewer) {
        String role = roleOf(viewer);
        return Role.ADMIN.equals(role) || Role.MANAGER.equals(role);
    }

    static boolean isLeadWithTeam(User viewer) {
        return Role.QA_LEAD.equals(roleOf(viewer)) && viewer.getTeam() != null;
    }

    static boolean sameUser(User a, User b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }

    static boolean inTeam(Project project, Team team) {
        return project != null && project.getTeam() != null && team != null
                && Objects.equals(project.getTeam().getId(), team.getId());
    }

    private static String roleOf(User user) {
        return user == null || user.getRole() == null ? null : user.getRole().getName();
    }
}
