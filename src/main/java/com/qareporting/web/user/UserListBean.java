package com.qareporting.web.user;

import com.qareporting.web.ListPage;
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

    /**
     * On ne supprime pas un compte : ses anomalies, exécutions et l'historique y font
     * référence (et la traçabilité l'exige). On le désactive ; il ne peut plus se
     * connecter, ses jetons d'API sont révoqués, et on peut le réactiver.
     */
    public String toggleActive(User user) {
        if (!user.getId().equals(sessionAuth.getUserId())) {
            userService.setActive(user.getId(), !user.isActive());
        }
        return "list.xhtml?faces-redirect=true";
    }

    public List<User> getUsers() {
        return pageData.getItems();
    }

    // ---------- recherche + pagination (paramètres d'URL q, status, page) ----------

    private final ListPage<User> pageData = new ListPage<>();
    private String q;
    private String status;
    private Integer page;

    /** f:viewAction : applique recherche, filtre et page à la liste du périmètre. */
    public void load() {
        pageData.apply(users, q, u -> u.getName() + " " + u.getEmail() + " " + (u.getRole() != null ? u.getRole().getName() : "") + " " + (u.getTeam() != null ? u.getTeam().getName() : ""), null, page);
    }

    public ListPage<User> getPageData() { return pageData; }
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
}
