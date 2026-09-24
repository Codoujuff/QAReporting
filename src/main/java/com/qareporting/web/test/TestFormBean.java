package com.qareporting.web.test;

import com.qareporting.web.auth.PermissionBean;
import com.qareporting.security.Permissions;
import com.qareporting.entity.Campaign;
import com.qareporting.web.i18n.I18n;
import com.qareporting.entity.Environment;
import com.qareporting.entity.Project;
import com.qareporting.entity.Test;
import com.qareporting.entity.User;
import com.qareporting.service.CampaignService;
import com.qareporting.service.EnvironmentService;
import com.qareporting.service.ProjectService;
import com.qareporting.service.TestService;
import com.qareporting.service.UserService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Création/modification d'un cas de test, ouverte à QA, QA Lead et Admin.
 * Note : côté REST, TestResource.store()/update() réserve cette action à
 * ADMIN/QA_LEAD (@RequiresRole) — cet écran JSF appelle TestService directement
 * sans passer par la couche REST, donc il applique volontairement sa propre règle,
 * plus permissive, à la demande explicite du produit (QA doit pouvoir créer ses
 * propres tests). AccessFilter ne connaît pas cette règle fine (il ne bloque que
 * /app/admin/*), donc la garde est refaite ici.
 */
@Named
@ViewScoped
public class TestFormBean implements Serializable {

    @Inject
    private TestService testService;

    @Inject
    private ProjectService projectService;

    @Inject
    private CampaignService campaignService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private UserService userService;

    @Inject
    private SessionAuth sessionAuth;

    private Long id;
    private Test test;
    private Long projectId;
    private Long campaignId;
    private Long environmentId;
    private Long assignedToId;
    private String stepsText;

    private List<Project> projectOptions;
    private List<Campaign> campaignOptions;
    private List<Environment> environmentOptions;
    private List<User> userOptions;

    @Inject
    private PermissionBean perm;

    @PostConstruct
    public void init() {
        projectOptions = projectService.findAll();
        campaignOptions = campaignService.findAll();
        environmentOptions = environmentService.findAll();
        userOptions = userService.findAll();
    }

    public String load() {
        if (!perm.require(Permissions.MANAGE_TESTS)) {
            return null;
        }

        if (id != null) {
            test = testService.find(id);
            if (!testService.canView(sessionAuth.getCurrentUser(), test)) {
                PermissionBean.notFound();
                return null;
            }
            projectId = test.getProject() != null ? test.getProject().getId() : null;
            campaignId = test.getCampaign() != null ? test.getCampaign().getId() : null;
            environmentId = test.getEnvironment() != null ? test.getEnvironment().getId() : null;
            assignedToId = test.getAssignedTo() != null ? test.getAssignedTo().getId() : null;
            stepsText = test.getSteps() != null ? String.join("\n", test.getSteps()) : "";
        } else {
            test = new Test();
            stepsText = "";
        }
        return null;
    }

    public String save() {
        if (!perm.require(Permissions.MANAGE_TESTS)) {
            return null;
        }
        if (id != null && !testService.canView(sessionAuth.getCurrentUser(), testService.find(id))) {
            PermissionBean.notFound();
            return null;
        }
        if (projectId == null) {
            addError(I18n.t("err.projectRequired"));
            return null;
        }
        if (test.getTitle() == null || test.getTitle().isBlank()) {
            addError(I18n.t("err.titleRequired"));
            return null;
        }

        test.setProject(projectService.find(projectId));
        test.setCampaign(campaignId == null ? null : campaignService.find(campaignId));
        test.setEnvironment(environmentId == null ? null : environmentService.find(environmentId));
        if (perm.has(Permissions.ASSIGN_TESTS)) {
            test.setAssignedTo(assignedToId == null ? null : userService.find(assignedToId));
        } else if (test.getId() == null) {
            // Un QA qui rédige un cas de test en devient le testeur ; il ne réassigne pas.
            test.setAssignedTo(sessionAuth.getCurrentUser());
        }

        List<String> steps = new ArrayList<>();
        if (stepsText != null) {
            for (String line : stepsText.split("\\R")) {
                if (!line.isBlank()) {
                    steps.add(line.trim());
                }
            }
        }
        test.setSteps(steps);

        if (test.getId() == null) {
            testService.create(test);
        } else {
            testService.update(test);
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

    public Test getTest() {
        return test;
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

    public Long getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getStepsText() {
        return stepsText;
    }

    public void setStepsText(String stepsText) {
        this.stepsText = stepsText;
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

    public List<User> getUserOptions() {
        return userOptions;
    }
}
