package com.qareporting.web.setting;

import com.qareporting.entity.AppSetting;
import com.qareporting.service.AppSettingService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class AppSettingListBean implements Serializable {

    @Inject
    private AppSettingService appSettingService;

    private List<AppSetting> settings;

    @PostConstruct
    public void init() {
        settings = appSettingService.findAllOrdered();
    }

    public String delete(AppSetting setting) {
        appSettingService.delete(setting.getId());
        return "list.xhtml?faces-redirect=true";
    }

    public List<AppSetting> getSettings() {
        return settings;
    }
}
