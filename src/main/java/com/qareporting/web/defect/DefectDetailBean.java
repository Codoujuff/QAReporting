package com.qareporting.web.defect;

import jakarta.faces.context.ExternalContext;
import jakarta.servlet.http.Part;
import com.qareporting.service.AttachmentService;
import com.qareporting.entity.Attachment;
import com.qareporting.web.auth.PermissionBean;
import com.qareporting.security.Permissions;
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

    @Inject
    private AttachmentService attachmentService;

    private List<Attachment> attachments;
    private transient Part upload;

    @Inject
    private PermissionBean perm;

    @PostConstruct
    public void init() {
        assigneeOptions = userService.findAll();
    }

    /** Une anomalie hors du périmètre du rôle répond 404, comme si elle n'existait pas. */
    public void load() {
        defect = id == null ? null : defectService.find(id);
        if (!defectService.canView(sessionAuth.getCurrentUser(), defect)) {
            defect = null;
            PermissionBean.notFound();
            return;
        }
        history = defectService.history(id);
        attachments = attachmentService.listFor(defect);
    }

    // ---------- pièces jointes ----------

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public Part getUpload() {
        return upload;
    }

    public void setUpload(Part upload) {
        this.upload = upload;
    }

    public boolean canDelete(Attachment attachment) {
        return attachmentService.canDelete(sessionAuth.getCurrentUser(), attachment);
    }

    /** Mêmes contrôles que POST /api/defects/{id}/attachments : type autorisé, 10 Mo maximum. */
    public String uploadAttachment() {
        if (!allowed(Permissions.WORK_ON_DEFECTS)) {
            return null;
        }
        if (upload == null || upload.getSize() == 0) {
            addError(I18n.t("err.attachmentMissing"));
            return null;
        }
        String mimeType = upload.getContentType() == null ? "application/octet-stream"
                : upload.getContentType().split(";")[0].trim().toLowerCase();
        if (!AttachmentService.ALLOWED_MIME_TYPES.contains(mimeType)) {
            addError(I18n.t("err.attachmentType", mimeType));
            return null;
        }
        if (upload.getSize() > AttachmentService.MAX_SIZE_BYTES) {
            addError(I18n.t("err.attachmentTooBig"));
            return null;
        }
        try (java.io.InputStream in = upload.getInputStream()) {
            attachmentService.store(defectService.find(id), sessionAuth.getCurrentUser(),
                    upload.getSubmittedFileName(), mimeType, in.readAllBytes());
        } catch (java.io.IOException e) {
            addError(I18n.t("err.attachmentRead"));
            return null;
        }
        return refresh();
    }

    public void download(Long attachmentId) {
        Attachment attachment = attachmentService.find(attachmentId);
        if (attachment == null || !attachment.getDefect().getId().equals(id)
                || !defectService.canView(sessionAuth.getCurrentUser(), attachment.getDefect())) {
            PermissionBean.notFound();
            return;
        }
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        byte[] content = attachmentService.read(attachment);
        ec.responseReset();
        ec.setResponseContentType(attachment.getMimeType());
        ec.setResponseContentLength(content.length);
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + attachment.getFilename() + "\"");
        try (java.io.OutputStream out = ec.getResponseOutputStream()) {
            out.write(content);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        fc.responseComplete();
    }

    public String deleteAttachment(Long attachmentId) {
        Attachment attachment = attachmentService.find(attachmentId);
        if (attachment == null || !attachment.getDefect().getId().equals(id)
                || !canDelete(attachment)) {
            PermissionBean.forbidden();
            return null;
        }
        attachmentService.delete(attachmentId);
        return refresh();
    }

    private void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    /** Garde commune aux actions : rôle autorisé et anomalie toujours dans le périmètre. */
    private boolean allowed(java.util.Set<String> permission) {
        if (!perm.require(permission)) {
            return false;
        }
        if (!defectService.canView(sessionAuth.getCurrentUser(), defectService.find(id))) {
            PermissionBean.notFound();
            return false;
        }
        return true;
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
        if (!allowed(Permissions.ASSIGN_DEFECTS)) {
            return null;
        }
        if (assigneeId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, I18n.t("err.assigneeRequired"), null));
            return null;
        }
        defectService.assign(defect, userService.find(assigneeId), sessionAuth.getCurrentUser());
        return refresh();
    }

    public String markFixed() {
        if (!allowed(Permissions.WORK_ON_DEFECTS)) {
            return null;
        }
        defectService.markFixed(defect, sessionAuth.getCurrentUser(), actionComment);
        return refresh();
    }

    public String retest(boolean passed) {
        if (!allowed(Permissions.WORK_ON_DEFECTS)) {
            return null;
        }
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
        if (!allowed(Permissions.WORK_ON_DEFECTS)) {
            return null;
        }
        defectService.close(defect, sessionAuth.getCurrentUser(), actionComment);
        return refresh();
    }

    public String reopen() {
        if (!allowed(Permissions.WORK_ON_DEFECTS)) {
            return null;
        }
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
