package com.qareporting.web.campaign;

import com.qareporting.entity.Campaign;
import com.qareporting.entity.Environment;
import com.qareporting.entity.Project;
import com.qareporting.entity.User;
import com.qareporting.service.CampaignService;
import com.qareporting.service.EnvironmentService;
import com.qareporting.service.ProjectService;
import com.qareporting.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * Création d'une campagne de test (pas d'édition dans cette itération, même
 * convention que DefectFormBean : création manuelle validée, pas de Bean Validation).
 */
@Named
@ViewScoped
public class CampaignFormBean implements Serializable {

    @Inject
    private CampaignService campaignService;

    @Inject
    private ProjectService projectService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private UserService userService;

    private final Campaign campaign = new Campaign();
    private Long projectId;
    private Long environmentId;
    private Long responsibleId;

    private List<Project> projectOptions;
    private List<Environment> environmentOptions;
    private List<User> userOptions;

    @PostConstruct
    public void init() {
        projectOptions = projectService.findAll();
        environmentOptions = environmentService.findAll();
        userOptions = userService.findAll();
        campaign.setStatus(Campaign.Status.planned);
    }

    public String save() {
        if (projectId == null) {
            addError("Le projet est obligatoire.");
            return null;
        }
        if (campaign.getName() == null || campaign.getName().isBlank()) {
            addError("Le nom de la campagne est obligatoire.");
            return null;
        }

        campaign.setProject(projectService.find(projectId));
        campaign.setEnvironment(environmentId == null ? null : environmentService.find(environmentId));
        campaign.setResponsible(responsibleId == null ? null : userService.find(responsibleId));

        campaignService.create(campaign);
        return "list.xhtml?faces-redirect=true";
    }

    private void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public Long getResponsibleId() {
        return responsibleId;
    }

    public void setResponsibleId(Long responsibleId) {
        this.responsibleId = responsibleId;
    }

    public List<Project> getProjectOptions() {
        return projectOptions;
    }

    public List<Environment> getEnvironmentOptions() {
        return environmentOptions;
    }

    public List<User> getUserOptions() {
        return userOptions;
    }
}
