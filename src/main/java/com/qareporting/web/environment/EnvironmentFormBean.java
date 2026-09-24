package com.qareporting.web.environment;

import com.qareporting.entity.Environment;
import com.qareporting.service.EnvironmentService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@ViewScoped
public class EnvironmentFormBean implements Serializable {

    @Inject
    private EnvironmentService environmentService;

    private Long id;
    private Environment environment;

    public void load() {
        environment = id != null ? environmentService.find(id) : new Environment();
    }

    public String save() {
        if (environment.getId() == null) {
            environmentService.create(environment);
        } else {
            environmentService.update(environment);
        }
        return "list.xhtml?faces-redirect=true";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Environment.Status[] getStatusOptions() {
        return Environment.Status.values();
    }
}
