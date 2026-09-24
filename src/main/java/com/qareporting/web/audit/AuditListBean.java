package com.qareporting.web.audit;

import com.qareporting.entity.AuditLog;
import com.qareporting.service.AuditService;
import com.qareporting.web.i18n.I18n;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Écran admin « Journal d'audit » : les 200 dernières actions de modification. */
@Named
@RequestScoped
public class AuditListBean {

    static final int MAX_ROWS = 200;

    @Inject
    private AuditService auditService;

    private List<AuditLog> entries;

    @PostConstruct
    public void init() {
        entries = auditService.latest(MAX_ROWS);
    }

    public List<AuditLog> getEntries() {
        return entries;
    }

    public int getMaxRows() {
        return MAX_ROWS;
    }

    public String action(AuditLog log) {
        String label = I18n.find("audit.action." + log.getAction());
        return label != null ? label : log.getAction();
    }

    public String model(AuditLog log) {
        String label = I18n.find("audit.model." + log.getModelType());
        return label != null ? label : log.getModelType();
    }

    /** {"from":"open","to":"fixed"} → « open → fixed » ; autres détails en « clé : valeur ». */
    public String changes(AuditLog log) {
        Map<String, Object> changes = log.getChanges();
        if (changes == null || changes.isEmpty()) {
            return "—";
        }
        if (changes.containsKey("from") || changes.containsKey("to")) {
            return (changes.get("from") == null ? "∅" : changes.get("from")) + " → " + changes.get("to");
        }
        return changes.entrySet().stream()
                .map(e -> e.getKey() + " : " + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
