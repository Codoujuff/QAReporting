package com.qareporting.web.test;

import com.qareporting.entity.Test;
import com.qareporting.entity.TestExecution;
import com.qareporting.service.TestService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * Liste des cas de test — mêmes règles que TestResource.index() : tout
 * utilisateur connecté voit tous les tests (seules la création/modification/
 * suppression sont réservées à ADMIN/QA_LEAD côté REST ; l'exécution, elle,
 * est ouverte à tous, donc accessible depuis cet écran QA).
 */
@Named
@ViewScoped
public class TestListBean implements Serializable {

    @Inject
    private TestService testService;

    @Inject
    private SessionAuth sessionAuth;

    private List<Test> tests;

    @PostConstruct
    public void init() {
        tests = testService.findAll();
    }

    /** Réservé à ADMIN côté REST (TestResource.destroy) — le bouton lui-même
     * n'est déjà rendu que pour ce rôle, cette garde couvre un appel direct. */
    public String delete(Test test) {
        if (!sessionAuth.hasRole("admin")) {
            return "list.xhtml?faces-redirect=true";
        }
        testService.delete(test.getId());
        return "list.xhtml?faces-redirect=true";
    }

    public List<Test> getTests() {
        return tests;
    }

    public TestExecution lastExecutionOf(Test test) {
        return testService.lastExecution(test.getId());
    }
}
