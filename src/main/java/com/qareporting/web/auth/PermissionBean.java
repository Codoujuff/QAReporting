package com.qareporting.web.auth;

import com.qareporting.security.Permissions;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * La matrice {@link Permissions} vue depuis les pages et les beans JSF :
 * #{perm.manageCampaigns} masque un bouton, perm.require(...) refuse l'action côté
 * serveur même si quelqu'un poste le formulaire à la main.
 */
@Named("perm")
@RequestScoped
public class PermissionBean {

    @Inject
    private SessionAuth sessionAuth;

    public boolean isManageCampaigns() { return has(Permissions.MANAGE_CAMPAIGNS); }
    public boolean isManageTests() { return has(Permissions.MANAGE_TESTS); }
    public boolean isAssignTests() { return has(Permissions.ASSIGN_TESTS); }
    public boolean isDelete() { return has(Permissions.DELETE); }
    public boolean isExecuteTests() { return has(Permissions.EXECUTE_TESTS); }
    public boolean isAssignDefects() { return has(Permissions.ASSIGN_DEFECTS); }
    public boolean isLogActivity() { return has(Permissions.LOG_ACTIVITY); }
    public boolean isWorkOnDefects() { return has(Permissions.WORK_ON_DEFECTS); }

    public boolean has(Set<String> allowed) {
        return Permissions.allows(allowed, sessionAuth.getRoleName());
    }

    /** Refuse (403) si le rôle connecté n'a pas la permission ; renvoie true si l'action peut continuer. */
    public boolean require(Set<String> allowed) {
        if (has(allowed)) {
            return true;
        }
        sendError(403);
        return false;
    }

    public static void forbidden() {
        sendError(403);
    }

    /** 404 plutôt que 403 pour un objet hors périmètre : on ne révèle pas qu'il existe. */
    public static void notFound() {
        sendError(404);
    }

    private static void sendError(int status) {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        try {
            ec.responseSendError(status, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        fc.responseComplete();
    }
}
