package com.qareporting.web.activity;

import com.qareporting.web.ListPage;
import com.qareporting.entity.Activity;
import com.qareporting.service.ActivityService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * "Mon activité" : journal quotidien de tests exécutés. Réutilise
 * ActivityService.listForUser(...), déjà scopé par rôle exactement comme
 * DefectService.listForUser (QA voit le sien, QA Lead son équipe, Manager/Admin tout).
 */
@Named
@ViewScoped
public class ActivityListBean implements Serializable {

    @Inject
    private ActivityService activityService;

    @Inject
    private SessionAuth sessionAuth;

    private List<Activity> activities;

    @PostConstruct
    public void init() {
        // La liste est chargée page par page en base, dans load().
    }

    public boolean canValidate(Activity activity) {
        return activityService.canValidate(sessionAuth.getCurrentUser(), activity);
    }

    /** Le lead valide la déclaration d'un membre de son équipe ; revérifié côté serveur. */
    public String validate(Long activityId) {
        Activity activity = activityService.find(activityId);
        if (!activityService.canValidate(sessionAuth.getCurrentUser(), activity)) {
            com.qareporting.web.auth.PermissionBean.forbidden();
            return null;
        }
        activityService.validateByLead(activity, sessionAuth.getCurrentUser());
        return "list.xhtml?faces-redirect=true";
    }

    public List<Activity> getActivities() {
        return pageData.getItems();
    }

    // ---------- recherche + pagination (paramètres d'URL q, status, page) ----------

    private final ListPage<Activity> pageData = new ListPage<>();
    private String q;
    private String status;
    private Integer page;

    /** f:viewAction : applique recherche, filtre et page à la liste du périmètre. */
    public void load() {
        var search = activityService.search(sessionAuth.getCurrentUser(), q, status);
        int first = pageData.prepare(activityService.count(search), page);
        pageData.setItems(activityService.page(search, first, ListPage.PAGE_SIZE));
    }

    public ListPage<Activity> getPageData() { return pageData; }
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    /**
     * Activity a un champ int "blocked" (compteur) ET un champ boolean "isBlocked"
     * dont le setter est setIsBlocked() — ça rend la propriété "blocked" ambiguë pour
     * l'introspection JavaBean : l'EL de JSF résout #{a.blocked} vers le booléen au
     * lieu du compteur. Cet accesseur explicite appelle la bonne méthode directement
     * (sans ambiguïté en Java) pour un affichage correct dans la vue.
     */
    public int blockedCountOf(Activity activity) {
        return activity.getBlocked();
    }
}
