package com.qareporting.web.activity;

import com.qareporting.web.auth.PermissionBean;
import com.qareporting.security.Permissions;
import com.qareporting.entity.Activity;
import com.qareporting.web.i18n.I18n;
import com.qareporting.entity.Campaign;
import com.qareporting.entity.Environment;
import com.qareporting.entity.Project;
import com.qareporting.service.ActivityService;
import com.qareporting.service.CampaignService;
import com.qareporting.service.EnvironmentService;
import com.qareporting.service.ProjectService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Saisie d'une nouvelle entrée de "Mon activité" (journal quotidien de tests).
 * Toute la validation métier (somme des compteurs, motif de blocage requis)
 * est déléguée à ActivityService.validate(...) — jamais dupliquée côté vue,
 * même convention que DefectFormBean.
 */
@Named
@ViewScoped
public class ActivityFormBean implements Serializable {

    @Inject
    private ActivityService activityService;

    @Inject
    private ProjectService projectService;

    @Inject
    private CampaignService campaignService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private SessionAuth sessionAuth;

    private final Activity activity = new Activity();
    private Long projectId;
    private Long campaignId;
    private Long environmentId;

    /**
     * Activity a un champ int "blocked" (compteur) ET un champ boolean "isBlocked"
     * dont le setter est setIsBlocked() — ça crée une propriété "blocked" ambiguë
     * pour l'introspection JavaBean que l'EL de JSF ne sait pas résoudre en écriture
     * (contrairement aux appels Java directs, qui ciblent la méthode exacte).
     * On binde donc la vue sur ce champ à part, copié vers l'entité au save().
     */
    private int blockedCount;

    private List<Project> projectOptions;
    private List<Campaign> campaignOptions;
    private List<Environment> environmentOptions;
    private final List<String> activityTypeOptions = List.of(
            "Exécution de tests", "Revue de tests", "Analyse d'anomalies", "Support");

    @Inject
    private PermissionBean perm;

    @PostConstruct
    public void init() {
        projectOptions = projectService.findAll();
        campaignOptions = campaignService.listForUser(sessionAuth.getCurrentUser());
        environmentOptions = environmentService.findAll();
        activity.setActivityDate(LocalDate.now());
    }

    public String save() {
        if (!perm.require(Permissions.LOG_ACTIVITY)) {
            return null;
        }
        if (projectId == null) {
            addError(I18n.t("err.projectRequired"));
            return null;
        }
        if (activity.getActivityDate() == null) {
            addError(I18n.t("err.dateRequired"));
            return null;
        }
        if (activity.getActivityType() == null || activity.getActivityType().isBlank()) {
            addError(I18n.t("err.activityTypeRequired"));
            return null;
        }

        activity.setProject(projectService.find(projectId));
        activity.setCampaign(campaignId == null ? null : campaignService.find(campaignId));
        activity.setEnvironment(environmentId == null ? null : environmentService.find(environmentId));
        activity.setUser(sessionAuth.getCurrentUser());
        activity.setBlocked(blockedCount);

        String error = activityService.validate(activity);
        if (error != null) {
            int sum = activity.getPassed() + activity.getFailed() + activity.getBlocked() + activity.getNotRun();
            error = sum != activity.getTestsExecuted()
                    ? I18n.t("err.activitySum", sum, activity.getTestsExecuted())
                    : I18n.t("err.blockedReasonRequired");
        }
        if (error != null) {
            addError(error);
            return null;
        }

        activityService.createValidated(activity);
        return "list.xhtml?faces-redirect=true";
    }

    private void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public Activity getActivity() {
        return activity;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(int blockedCount) {
        this.blockedCount = blockedCount;
    }

    public List<Project> getProjectOptions() {
        return projectOptions;
    }

    public List<Campaign> getCampaignOptions() {
        return campaignOptions;
    }

    public List<Environment> getEnvironmentOptions() {
        return environmentOptions;
    }

    public List<String> getActivityTypeOptions() {
        return activityTypeOptions;
    }
}
