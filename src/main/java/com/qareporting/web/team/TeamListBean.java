package com.qareporting.web.team;

import com.qareporting.entity.Team;
import com.qareporting.service.TeamService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class TeamListBean implements Serializable {

    @Inject
    private TeamService teamService;

    private List<Team> teams;

    @PostConstruct
    public void init() {
        teams = teamService.findAll();
    }

    public String delete(Team team) {
        teamService.delete(team.getId());
        return "list.xhtml?faces-redirect=true";
    }

    public List<Team> getTeams() {
        return teams;
    }
}
