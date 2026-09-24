package com.qareporting.web.defect;

import com.qareporting.service.TestService;
import com.qareporting.entity.TestExecution;
import com.qareporting.entity.Test;
import com.qareporting.web.auth.PermissionBean;
import com.qareporting.security.Permissions;
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
    private Long testId;
    private Long executionId;
    private Test sourceTest;

    @Inject
    private TestService testService;
    private Long projectId;
    private List<Project> projectOptions;

    @Inject
    private PermissionBean perm;

    @PostConstruct
    public void init() {
        projectOptions = projectService.findAll();
    }

    /**
     * Ouvert depuis un test en échec (?testId=…&executionId=…) : l'anomalie reprend le
     * projet, la campagne, l'environnement, le résultat attendu, les étapes du test et le
     * résultat obtenu lors de l'exécution — le testeur n'a plus qu'à compléter.
     */
    public void load() {
        if (testId == null || sourceTest != null) {
            return;
        }
        Test test = testService.find(testId);
        if (!testService.canView(sessionAuth.getCurrentUser(), test)) {
            PermissionBean.notFound();
            return;
        }
        sourceTest = test;
        TestExecution execution = testService.findExecution(executionId);
        if (execution != null && !execution.getTest().getId().equals(test.getId())) {
            execution = null;
        }

        projectId = test.getProject() != null ? test.getProject().getId() : null;
        defect.setTitle(I18n.t("defect.fromTest.title", test.getTitle()));
        defect.setExpectedResult(test.getExpectedResult());
        if (execution != null) {
            defect.setActualResult(execution.getActualResult());
        }
        StringBuilder steps = new StringBuilder();
        if (test.getPreconditions() != null && !test.getPreconditions().isBlank()) {
            steps.append(I18n.t("defect.fromTest.preconditions")).append(' ').append(test.getPreconditions()).append('\n');
        }
        if (test.getSteps() != null) {
            int n = 1;
            for (String step : test.getSteps()) {
                steps.append(n++).append(". ").append(step).append('\n');
            }
        }
        defect.setReproductionSteps(steps.toString().strip());
        if (test.getStatus() == Test.Status.failed) {
            defect.setSeverity(Defect.Severity.high);
        }
    }

    public String save() {
        if (!perm.require(Permissions.WORK_ON_DEFECTS)) {
            return null;
        }
        if (projectId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, I18n.t("err.projectRequired"), null));
            return null;
        }
        defect.setProject(projectService.find(projectId));
        if (sourceTest != null) {
            defect.setTest(sourceTest);
            defect.setCampaign(sourceTest.getCampaign());
            TestExecution execution = testService.findExecution(executionId);
            defect.setEnvironment(execution != null && execution.getEnvironment() != null
                    ? execution.getEnvironment() : sourceTest.getEnvironment());
        }
        defectService.createWithHistory(defect, sessionAuth.getCurrentUser());
        return "list.xhtml?faces-redirect=true";
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public Test getSourceTest() {
        return sourceTest;
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
