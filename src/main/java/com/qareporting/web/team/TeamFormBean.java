package com.qareporting.web.team;

import com.qareporting.entity.Team;
import com.qareporting.entity.User;
import com.qareporting.service.TeamService;
import com.qareporting.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class TeamFormBean implements Serializable {

    @Inject
    private TeamService teamService;

    @Inject
    private UserService userService;

    private Long id;
    private Team team;
    private Long leadId;
    private List<User> leadOptions;

    @PostConstruct
    public void init() {
        leadOptions = userService.findAll();
    }

    public void load() {
        if (id != null) {
            team = teamService.find(id);
            leadId = team.getLead() != null ? team.getLead().getId() : null;
        } else {
            team = new Team();
        }
    }

    public String save() {
        team.setLead(leadId == null ? null : userService.find(leadId));
        if (team.getId() == null) {
            teamService.create(team);
        } else {
            teamService.update(team);
        }
        return "list.xhtml?faces-redirect=true";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public List<User> getLeadOptions() {
        return leadOptions;
    }
}
