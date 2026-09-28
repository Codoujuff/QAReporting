package com.qareporting.web.test;

import com.qareporting.web.ListPage;
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
        // La liste est chargée page par page en base, dans load().
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
        return pageData.getItems();
    }

    // ---------- recherche + pagination (paramètres d'URL q, status, page) ----------

    private final ListPage<Test> pageData = new ListPage<>();
    private String q;
    private String status;
    private Integer page;

    /** f:viewAction : applique recherche, filtre et page à la liste du périmètre. */
    public void load() {
        var search = testService.search(sessionAuth.getCurrentUser(), q, validStatus(com.qareporting.entity.Test.Status.class));
        int first = pageData.prepare(testService.count(search), page);
        pageData.setItems(testService.page(search, first, ListPage.PAGE_SIZE));
    }

    /** Le statut de l'URL s'il désigne bien une valeur de l'enum, sinon null (paramètre trafiqué). */
    private <E extends Enum<E>> String validStatus(Class<E> type) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (E value : type.getEnumConstants()) {
            if (value.name().equals(status)) {
                return status;
            }
        }
        return null;
    }

    public ListPage<Test> getPageData() { return pageData; }
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Test.Status[] getStatusOptions() {
        return Test.Status.values();
    }

    public TestExecution lastExecutionOf(Test test) {
        return testService.lastExecution(test.getId());
    }
}
