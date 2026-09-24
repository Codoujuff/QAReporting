package com.qareporting.web.activity;

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
        activities = activityService.listForUser(sessionAuth.getCurrentUser());
    }

    public List<Activity> getActivities() {
        return activities;
    }

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
