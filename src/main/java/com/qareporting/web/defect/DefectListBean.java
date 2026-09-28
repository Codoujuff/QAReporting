package com.qareporting.web.defect;

import com.qareporting.web.ListPage;
import com.qareporting.entity.Defect;
import com.qareporting.service.DefectService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class DefectListBean implements Serializable {

    @Inject
    private DefectService defectService;

    @Inject
    private SessionAuth sessionAuth;

    private List<Defect> defects;

    @PostConstruct
    public void init() {
        // La liste est chargée page par page en base, dans load().
    }

    public List<Defect> getDefects() {
        return pageData.getItems();
    }

    // ---------- recherche + pagination (paramètres d'URL q, status, page) ----------

    private final ListPage<Defect> pageData = new ListPage<>();
    private String q;
    private String status;
    private Integer page;

    /** f:viewAction : applique recherche, filtre et page à la liste du périmètre. */
    public void load() {
        var search = defectService.search(sessionAuth.getCurrentUser(), q, validStatus(com.qareporting.entity.Defect.Status.class));
        int first = pageData.prepare(defectService.count(search), page);
        pageData.setItems(defectService.page(search, first, ListPage.PAGE_SIZE));
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

    public ListPage<Defect> getPageData() { return pageData; }
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Defect.Status[] getStatusOptions() {
        return Defect.Status.values();
    }
}
