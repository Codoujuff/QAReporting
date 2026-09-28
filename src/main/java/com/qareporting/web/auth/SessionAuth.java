package com.qareporting.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.faces.context.ExternalContext;
import com.qareporting.security.LoginService;
import com.qareporting.entity.User;
import com.qareporting.web.i18n.I18n;
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
    private boolean mustChangePassword;

    @Inject
    private LoginService loginService;

    /**
     * Connexion par la page JSF : mêmes vérifications que l'API (LoginService), puis
     * nouvel identifiant de session — un identifiant fixé avant la connexion (fixation de
     * session) ne donne ainsi jamais accès au compte.
     */
    public boolean login(String email, String password) {
        errorMessage = null;
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) ec.getRequest();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            errorMessage = I18n.t("err.credentialsRequired");
            return false;
        }
        LoginService.Result result = loginService.authenticate(email, password, request.getRemoteAddr());
        switch (result.status()) {
            case LOCKED -> errorMessage = I18n.t("err.tooManyAttempts", result.minutesLocked());
            case DISABLED -> errorMessage = I18n.t("err.accountDisabled");
            case INVALID -> errorMessage = I18n.t("err.invalidCredentials");
            default -> { }
        }
        if (!result.ok()) {
            return false;
        }

        request.changeSessionId();
        User user = result.user();
        this.userId = user.getId();
        this.userName = user.getName();
        this.userInitials = user.getInitials();
        this.roleName = user.getRole().getName();
        this.mustChangePassword = user.isMustChangePassword();
        return true;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void passwordChanged() {
        this.mustChangePassword = false;
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
        mustChangePassword = false;
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
        return notificationService.countUnread(user);
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
