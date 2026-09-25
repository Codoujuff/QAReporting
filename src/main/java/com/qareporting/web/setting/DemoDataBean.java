package com.qareporting.web.setting;

import com.qareporting.security.Permissions;
import com.qareporting.service.DemoDataService;
import com.qareporting.web.auth.PermissionBean;
import com.qareporting.web.i18n.I18n;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/** Bouton admin « Charger les données de démonstration » (Paramètres généraux). */
@Named
@RequestScoped
public class DemoDataBean {

    @Inject
    private DemoDataService demoDataService;

    @Inject
    private PermissionBean perm;

    public boolean isLoaded() {
        return demoDataService.isLoaded();
    }

    public String getPassword() {
        return DemoDataService.PASSWORD;
    }

    public String getDomain() {
        return DemoDataService.DOMAIN;
    }

    public String load() {
        if (!perm.require(Permissions.AUDIT_LOG)) { // réservé à l'admin
            return null;
        }
        boolean loaded = demoDataService.load();
        FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                I18n.t(loaded ? "demo.loadedMessage" : "demo.alreadyLoaded"), null));
        return "list.xhtml?faces-redirect=true";
    }
}
