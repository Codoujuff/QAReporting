package com.qareporting.web.role;

import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import com.qareporting.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class RoleListBean implements Serializable {

    /**
     * A role row with the number of users currently holding it.
     * JSF's EL resolver here only recognizes JavaBean-style {@code getX()} accessors, not the
     * record's plain {@code x()} accessors, so explicit getters are added for use in Facelets.
     */
    public record RoleRow(String name, String description, long userCount) {
        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public long getUserCount() {
            return userCount;
        }
    }

    @Inject
    private UserService userService;

    private List<RoleRow> roles;

    @PostConstruct
    public void init() {
        List<Role> allRoles = userService.listRoles();
        List<User> allUsers = userService.findAll();
        roles = new ArrayList<>();
        for (Role role : allRoles) {
            long count = allUsers.stream().filter(u -> u.getRole().getId().equals(role.getId())).count();
            roles.add(new RoleRow(role.getName(), role.getDescription(), count));
        }
    }

    public List<RoleRow> getRoles() {
        return roles;
    }
}
