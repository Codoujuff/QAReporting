package com.qareporting.web.defect;

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
        defects = defectService.listForUser(sessionAuth.getCurrentUser());
    }

    public List<Defect> getDefects() {
        return defects;
    }
}
