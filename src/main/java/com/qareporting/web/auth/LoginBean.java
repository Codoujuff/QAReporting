package com.qareporting.web.auth;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@RequestScoped
public class LoginBean implements Serializable {

    @Inject
    private SessionAuth sessionAuth;

    private String email;
    private String password;
    private String next;

    public String login() {
        if (sessionAuth.login(email, password)) {
            if (next != null && next.startsWith("/app/")) {
                return next + "?faces-redirect=true";
            }
            // Chaque rôle atterrit sur son écran de référence, comme dans la maquette.
            if (sessionAuth.hasRole("admin")) {
                return "/app/admin/users/list.xhtml?faces-redirect=true";
            }
            return "/app/dashboard.xhtml?faces-redirect=true";
        }

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, sessionAuth.getErrorMessage(), null));
        return null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }
}
