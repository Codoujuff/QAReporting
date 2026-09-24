package com.qareporting.web.profile;

import com.qareporting.entity.Activity;
import com.qareporting.entity.User;
import com.qareporting.service.ActivityService;
import com.qareporting.service.DefectService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * "Mon profil" : synthèse en lecture seule de l'activité de l'utilisateur
 * (tests exécutés, taux de réussite, anomalies visibles, activité récente).
 * L'édition du compte (nom, mot de passe, préférences) vit désormais dans
 * "Paramètres" (UserSettingsBean), comme dans la maquette.
 */
@Named
@ViewScoped
public class ProfileBean implements Serializable {

    @Inject
    private SessionAuth sessionAuth;

    @Inject
    private ActivityService activityService;

    @Inject
    private DefectService defectService;

    private User user;
    private int testsExecutedCount;
    private int passedCount;
    private int failedCount;
    private double successRate;
    private int anomaliesCount;
    private List<Activity> recentActivities;

    @PostConstruct
    public void init() {
        user = sessionAuth.getCurrentUser();

        List<Activity> activities = activityService.listForUser(user);
        testsExecutedCount = activities.stream().mapToInt(Activity::getTestsExecuted).sum();
        passedCount = activities.stream().mapToInt(Activity::getPassed).sum();
        failedCount = activities.stream().mapToInt(Activity::getFailed).sum();
        successRate = testsExecutedCount == 0 ? 0.0 : 100.0 * passedCount / testsExecutedCount;

        anomaliesCount = defectService.listForUser(user).size();

        recentActivities = activities.stream()
                .sorted(Comparator.comparing(Activity::getActivityDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();
    }

    public User getUser() {
        return user;
    }

    public int getTestsExecutedCount() {
        return testsExecutedCount;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public int getAnomaliesCount() {
        return anomaliesCount;
    }

    public List<Activity> getRecentActivities() {
        return recentActivities;
    }
}
