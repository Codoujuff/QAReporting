package com.qareporting.web.project;

import com.qareporting.entity.Project;
import com.qareporting.entity.Team;
import com.qareporting.service.ProjectService;
import com.qareporting.service.TeamService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class ProjectFormBean implements Serializable {

    @Inject
    private ProjectService projectService;

    @Inject
    private TeamService teamService;

    private Long id;
    private Project project;
    private Long teamId;
    private List<Team> teamOptions;

    @PostConstruct
    public void init() {
        teamOptions = teamService.findAll();
    }

    public void load() {
        if (id != null) {
            project = projectService.find(id);
            teamId = project.getTeam() != null ? project.getTeam().getId() : null;
        } else {
            project = new Project();
        }
    }

    public String save() {
        project.setTeam(teamId == null ? null : teamService.find(teamId));
        if (project.getId() == null) {
            projectService.create(project);
        } else {
            projectService.update(project);
        }
        return "list.xhtml?faces-redirect=true";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public List<Team> getTeamOptions() {
        return teamOptions;
    }

    public Project.Status[] getStatusOptions() {
        return Project.Status.values();
    }
}
