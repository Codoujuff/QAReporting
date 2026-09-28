package com.qareporting.web.auth;

import com.qareporting.service.PasswordResetService;
import com.qareporting.web.i18n.I18n;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;

/** Pages publiques « Mot de passe oublié » et « Nouveau mot de passe » (lien reçu par e-mail). */
@Named
@ViewScoped
public class PasswordResetBean implements Serializable {

    @Inject
    private transient PasswordResetService resetService;

    private String email;
    private boolean requested;
    private String token;
    private String password;
    private String confirmation;

    /** Envoie le lien — la réponse affichée est la même, que le compte existe ou non. */
    public String request() {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest req = (HttpServletRequest) ec.getRequest();
        resetService.request(email, req.getRemoteAddr(), publicBaseUrl(req),
                (part, link) -> "subject".equals(part) ? I18n.t("reset.mailSubject") : I18n.t("reset.mailBody", link));
        requested = true;
        return null;
    }

    public boolean isTokenValid() {
        return resetService.isValid(token);
    }

    public String reset() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (password == null || !password.equals(confirmation)) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, I18n.t("err.passwordMismatch"), null));
            return null;
        }
        PasswordResetService.ResetResult result = resetService.reset(token, password);
        if (result.status() != PasswordResetService.ResetStatus.OK) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, I18n.t(result.errorKey()), null));
            return null;
        }
        fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, I18n.t("reset.done"), null));
        fc.getExternalContext().getFlash().setKeepMessages(true);
        return "/login.xhtml?faces-redirect=true";
    }

    /** Adresse publique pour le lien : QA_PUBLIC_URL si définie (derrière un proxy), sinon celle de la requête. */
    static String publicBaseUrl(HttpServletRequest req) {
        String configured = System.getenv("QA_PUBLIC_URL");
        if (configured != null && !configured.isBlank()) {
            return configured.replaceAll("/+$", "");
        }
        int port = req.getServerPort();
        boolean defaultPort = ("http".equals(req.getScheme()) && port == 80) || ("https".equals(req.getScheme()) && port == 443);
        return req.getScheme() + "://" + req.getServerName() + (defaultPort ? "" : ":" + port) + req.getContextPath();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isRequested() { return requested; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmation() { return confirmation; }
    public void setConfirmation(String confirmation) { this.confirmation = confirmation; }
}
