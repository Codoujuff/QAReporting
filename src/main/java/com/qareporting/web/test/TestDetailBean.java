package com.qareporting.web.test;

import com.qareporting.web.auth.PermissionBean;
import com.qareporting.security.Permissions;
import com.qareporting.entity.Environment;
import com.qareporting.web.i18n.I18n;
import com.qareporting.entity.Test;
import com.qareporting.entity.TestExecution;
import com.qareporting.service.EnvironmentService;
import com.qareporting.service.TestService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * Détail d'un cas de test : informations, historique des exécutions et
 * formulaire d'exécution. Même règle que TestResource.execute() : chaque
 * exécution crée une ligne d'historique immuable et met à jour le statut
 * courant du test — jamais d'écrasement du résultat précédent.
 */
@Named
@ViewScoped
public class TestDetailBean implements Serializable {

    @Inject
    private TestService testService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private SessionAuth sessionAuth;

    private Long id;
    private Test test;
    private Long failedExecution;
    private List<TestExecution> executions;
    private List<Environment> environmentOptions;

    private Test.Status resultStatus;
    private String actualResult;
    private Long environmentId;
    private String duration;

    @Inject
    private PermissionBean perm;

    @PostConstruct
    public void init() {
        environmentOptions = environmentService.findAll();
    }

    /** Clé du message qui explique pourquoi le test ne peut pas être exécuté, ou null. */
    public String getExecutionBlocker() {
        return testService.executionBlocker(test);
    }

    public Long getFailedExecution() {
        return failedExecution;
    }

    public void setFailedExecution(Long failedExecution) {
        this.failedExecution = failedExecution;
    }

    public void load() {
        test = id == null ? null : testService.find(id);
        if (!testService.canView(sessionAuth.getCurrentUser(), test)) {
            test = null;
            PermissionBean.notFound();
            return;
        }
        executions = testService.executions(id);
        environmentId = test.getEnvironment() != null ? test.getEnvironment().getId() : null;
    }

    public String execute() {
        if (!perm.require(Permissions.EXECUTE_TESTS)) {
            return null;
        }
        if (!testService.canView(sessionAuth.getCurrentUser(), testService.find(id))) {
            PermissionBean.notFound();
            return null;
        }
        String blocker = testService.executionBlocker(testService.find(id));
        if (blocker != null) {
            addError(I18n.t(blocker));
            return null;
        }
        if (resultStatus == null) {
            addError(I18n.t("err.resultRequired"));
            return null;
        }

        Environment environment = environmentId != null ? environmentService.find(environmentId) : test.getEnvironment();
        TestExecution execution = testService.execute(test, resultStatus, actualResult,
                sessionAuth.getCurrentUser(), environment, duration);

        // Un échec propose aussitôt de déclarer l'anomalie (cahier des charges §4.4).
        String failed = resultStatus == Test.Status.failed ? "&failedExecution=" + execution.getId() : "";
        return "detail.xhtml?faces-redirect=true&id=" + id + failed;
    }

    private void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public List<Test.Status> getStatusOptions() {
        return List.of(Test.Status.values());
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

    public List<TestExecution> getExecutions() {
        return executions;
    }

    public List<Environment> getEnvironmentOptions() {
        return environmentOptions;
    }

    public Test.Status getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(Test.Status resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
