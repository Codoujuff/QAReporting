package com.qareporting.security;

import com.qareporting.entity.User;
import jakarta.enterprise.context.RequestScoped;

/**
 * Holds the authenticated User for the lifetime of the current HTTP request.
 * Populated by AuthenticationFilter, read by resources/services — the
 * Jakarta EE equivalent of Laravel's Auth::user() / $request->user().
 */
@RequestScoped
public class CurrentUser {

    private User user;
    private Long tokenId;

    public User get() {
        return user;
    }

    public void set(User user) {
        this.user = user;
    }

    /** Jeton de l'appel API en cours (pour le révoquer à la déconnexion), null côté pages JSF. */
    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    public boolean isAuthenticated() {
        return user != null;
    }

    public boolean hasAnyRole(String... roleNames) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        for (String roleName : roleNames) {
            if (user.getRole().getName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }
}
