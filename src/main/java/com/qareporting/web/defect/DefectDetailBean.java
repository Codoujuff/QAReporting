package com.qareporting.web.defect;

import com.qareporting.entity.Defect;
import com.qareporting.web.i18n.I18n;
import com.qareporting.entity.DefectHistory;
import com.qareporting.entity.User;
import com.qareporting.service.DefectService;
import com.qareporting.service.UserService;
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
public class DefectDetailBean implements Serializable {

    @Inject
    private DefectService defectService;

    @Inject
    private UserService userService;

    @Inject
    private SessionAuth sessionAuth;

    private Long id;
    private Defect defect;
    private List<DefectHistory> history;
    private Long assigneeId;
    private List<User> assigneeOptions;
    private String actionComment;

    @PostConstruct
    public void init() {
        assigneeOptions = userService.findAll();
    }

    public void load() {
        defect = defectService.find(id);
        history = defectService.history(id);
    }

    public boolean isMarkFixedAvailable() {
        return defect.getStatus() != Defect.Status.fixed && defect.getStatus() != Defect.Status.closed;
    }

    public boolean isRetestable() {
        return defect.getStatus() == Defect.Status.fixed;
    }

    public boolean isCloseable() {
        return defect.getStatus() != Defect.Status.closed;
    }

    public boolean isReopenable() {
        return defect.getStatus() == Defect.Status.closed;
    }

    public String assign() {
        if (assigneeId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, I18n.t("err.assigneeRequired"), null));
            return null;
        }
        defectService.assign(defect, userService.find(assigneeId), sessionAuth.getCurrentUser());
        return refresh();
    }

    public String markFixed() {
        defectService.markFixed(defect, sessionAuth.getCurrentUser(), actionComment);
        return refresh();
    }

    public String retest(boolean passed) {
        try {
            defectService.retest(defect, passed, sessionAuth.getCurrentUser(), actionComment);
        } catch (IllegalStateException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, I18n.t("err.retestOnlyFixed"), null));
            return null;
        }
        return refresh();
    }

    public String close() {
        defectService.close(defect, sessionAuth.getCurrentUser(), actionComment);
        return refresh();
    }

    public String reopen() {
        defectService.reopen(defect, sessionAuth.getCurrentUser(), actionComment);
        return refresh();
    }

    private String refresh() {
        return "detail.xhtml?faces-redirect=true&id=" + id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Defect getDefect() {
        return defect;
    }

    public List<DefectHistory> getHistory() {
        return history;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public List<User> getAssigneeOptions() {
        return assigneeOptions;
    }

    public String getActionComment() {
        return actionComment;
    }

    public void setActionComment(String actionComment) {
        this.actionComment = actionComment;
    }
}
