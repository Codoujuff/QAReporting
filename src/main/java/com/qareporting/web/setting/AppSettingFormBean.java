package com.qareporting.web.setting;

import com.qareporting.entity.AppSetting;
import com.qareporting.web.i18n.I18n;
import com.qareporting.service.AppSettingService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@ViewScoped
public class AppSettingFormBean implements Serializable {

    @Inject
    private AppSettingService appSettingService;

    private Long id;
    private AppSetting setting;

    public void load() {
        setting = id != null ? appSettingService.find(id) : new AppSetting();
    }

    public String save() {
        if (setting.getSettingKey() == null || setting.getSettingKey().isBlank()) {
            addError(I18n.t("err.keyRequired"));
            return null;
        }

        AppSetting existing = appSettingService.findByKey(setting.getSettingKey());
        if (existing != null && !existing.getId().equals(setting.getId())) {
            addError(I18n.t("err.keyTaken"));
            return null;
        }

        if (setting.getId() == null) {
            appSettingService.create(setting);
        } else {
            appSettingService.update(setting);
        }
        return "list.xhtml?faces-redirect=true";
    }

    private void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppSetting getSetting() {
        return setting;
    }
}
