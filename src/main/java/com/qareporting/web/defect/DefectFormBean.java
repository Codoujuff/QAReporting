package com.qareporting.web.defect;

import com.qareporting.entity.Defect;
import com.qareporting.web.i18n.I18n;
import com.qareporting.entity.Project;
import com.qareporting.service.DefectService;
import com.qareporting.service.ProjectService;
import com.qareporting.web.auth.SessionAuth;
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
public class DefectFormBean implements Serializable {

    @Inject
    private DefectService defectService;

    @Inject
    private ProjectService projectService;

    @Inject
    private SessionAuth sessionAuth;

    private Defect defect = new Defect();
    private Long projectId;
    private List<Project> projectOptions;

    @PostConstruct
    public void init() {
        projectOptions = projectService.findAll();
    }

    public String save() {
        if (projectId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, I18n.t("err.projectRequired"), null));
            return null;
        }
        defect.setProject(projectService.find(projectId));
        defectService.createWithHistory(defect, sessionAuth.getCurrentUser());
        return "list.xhtml?faces-redirect=true";
    }

    public Defect getDefect() {
        return defect;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public List<Project> getProjectOptions() {
        return projectOptions;
    }

    public Defect.Severity[] getSeverityOptions() {
        return Defect.Severity.values();
    }

    public Defect.Priority[] getPriorityOptions() {
        return Defect.Priority.values();
    }
}
