package com.qareporting.web.user;

import com.qareporting.entity.Role;
import com.qareporting.entity.Team;
import com.qareporting.entity.User;
import com.qareporting.security.PasswordHasher;
import com.qareporting.service.TeamService;
import com.qareporting.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class UserFormBean implements Serializable {

    @Inject
    private UserService userService;

    @Inject
    private TeamService teamService;

    @Inject
    private PasswordHasher passwordHasher;

    private Long id;
    private User user;
    private String password;
    private Long roleId;
    private Long teamId;
    private List<Role> roleOptions;
    private List<Team> teamOptions;

    @PostConstruct
    public void init() {
        roleOptions = userService.listRoles();
        teamOptions = teamService.findAll();
    }

    public void load() {
        if (id != null) {
            user = userService.find(id);
            roleId = user.getRole() != null ? user.getRole().getId() : null;
            teamId = user.getTeam() != null ? user.getTeam().getId() : null;
        } else {
            user = new User();
            user.setActive(true);
        }
    }

    public String save() {
        if (roleId == null) {
            addError("Le rôle est obligatoire.");
            return null;
        }

        boolean isCreate = user.getId() == null;

        if (isCreate) {
            if (userService.findByEmail(user.getEmail()) != null) {
                addError("Cet email est déjà utilisé.");
                return null;
            }
            if (password == null || password.isBlank()) {
                addError("Le mot de passe est requis.");
                return null;
            }
            user.setPasswordHash(passwordHasher.hash(password));
        } else if (password != null && !password.isBlank()) {
            user.setPasswordHash(passwordHasher.hash(password));
        }

        user.setRole(userService.findRole(roleId));
        user.setTeam(teamId == null ? null : userService.findTeam(teamId));

        if (isCreate) {
            userService.create(user);
        } else {
            userService.update(user);
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

    public User getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public List<Role> getRoleOptions() {
        return roleOptions;
    }

    public List<Team> getTeamOptions() {
        return teamOptions;
    }
}
