package com.qareporting.web.auth;

import com.qareporting.entity.User;
import com.qareporting.security.PasswordHasher;
import com.qareporting.service.NotificationService;
import com.qareporting.service.UserService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Session-scoped login state for the JSF frontend. Deliberately independent from the
 * REST API's bearer-token mechanism (TokenService/CurrentUser/AuthenticationFilter),
 * which is request-scoped and has no session concept. Mirrors AuthResource.login()'s
 * validation order/messages so both entry points behave identically for the same
 * credentials.
 *
 * Stores only primitives, never the User entity itself, to avoid caching a stale
 * managed/detached entity across requests.
 */
@Named
@SessionScoped
public class SessionAuth implements Serializable {

    @Inject
    private UserService userService;

    @Inject
    private PasswordHasher passwordHasher;

    @Inject
    private NotificationService notificationService;

    private Long userId;
    private String userName;
    private String userInitials;
    private String roleName;
    private String errorMessage;

    public boolean login(String email, String password) {
        errorMessage = null;

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            errorMessage = "Email et mot de passe requis.";
            return false;
        }

        User user = userService.findByEmail(email);
        if (user == null) {
            errorMessage = "Identifiants invalides.";
            return false;
        }
        if (!user.isActive()) {
            errorMessage = "Ce compte a été désactivé.";
            return false;
        }
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            errorMessage = "Identifiants invalides.";
            return false;
        }

        this.userId = user.getId();
        this.userName = user.getName();
        this.userInitials = user.getInitials();
        this.roleName = user.getRole().getName();
        return true;
    }

    /** Refreshes the cached display name/initials after a self-service profile edit. */
    public void syncDisplayName(User user) {
        this.userName = user.getName();
        this.userInitials = user.getInitials();
    }

    public String logout() {
        userId = null;
        userName = null;
        userInitials = null;
        roleName = null;
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login.xhtml?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return userId != null;
    }

    public boolean hasRole(String... roleNames) {
        if (roleName == null) {
            return false;
        }
        for (String candidate : roleNames) {
            if (roleName.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    /** Re-fetches a fresh managed User on demand — never cache the entity itself in session. */
    public User getCurrentUser() {
        return userId == null ? null : userService.find(userId);
    }

    /** Pour le badge de la cloche dans l'en-tête — recalculé à chaque affichage. */
    public long getUnreadNotificationCount() {
        User user = getCurrentUser();
        if (user == null) {
            return 0;
        }
        return notificationService.listFor(user).stream()
                .filter(n -> n.getReadAt() == null)
                .count();
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserInitials() {
        return userInitials;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
