package com.qareporting.web.project;

import com.qareporting.entity.Project;
import com.qareporting.service.ProjectService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class ProjectListBean implements Serializable {

    @Inject
    private ProjectService projectService;

    private List<Project> projects;

    @PostConstruct
    public void init() {
        projects = projectService.findAll();
    }

    public String delete(Project project) {
        projectService.delete(project.getId());
        return "list.xhtml?faces-redirect=true";
    }

    public List<Project> getProjects() {
        return projects;
    }
}
