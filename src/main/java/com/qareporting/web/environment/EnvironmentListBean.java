package com.qareporting.web.environment;

import com.qareporting.entity.Environment;
import com.qareporting.service.EnvironmentService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class EnvironmentListBean implements Serializable {

    @Inject
    private EnvironmentService environmentService;

    private List<Environment> environments;

    @PostConstruct
    public void init() {
        environments = environmentService.findAll();
    }

    public String delete(Environment environment) {
        environmentService.delete(environment.getId());
        return "list.xhtml?faces-redirect=true";
    }

    public List<Environment> getEnvironments() {
        return environments;
    }
}
