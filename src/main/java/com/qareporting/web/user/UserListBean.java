package com.qareporting.web.user;

import com.qareporting.entity.User;
import com.qareporting.service.UserService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class UserListBean implements Serializable {

    @Inject
    private UserService userService;

    @Inject
    private SessionAuth sessionAuth;

    private List<User> users;

    @PostConstruct
    public void init() {
        users = userService.findAll();
    }

    public String delete(User user) {
        if (!user.getId().equals(sessionAuth.getUserId())) {
            userService.delete(user.getId());
        }
        return "list.xhtml?faces-redirect=true";
    }

    public List<User> getUsers() {
        return users;
    }
}
