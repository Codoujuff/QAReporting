package com.qareporting.web.reporting;

import com.qareporting.entity.Defect;
import com.qareporting.entity.User;
import com.qareporting.service.DefectService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Synthèse tabulaire + export CSV des anomalies visibles par l'utilisateur connecté.
 * Complète le tableau de bord (graphiques) par une vue exportable, comme demandé
 * dans le cahier des charges (rapports). La portée des données réutilise
 * {@link DefectService#listForUser(User)} — QA voit les siennes, QA Lead son équipe,
 * Manager/Admin tout.
 */
@Named
@ViewScoped
public class ReportingBean implements Serializable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Une ligne de synthèse : un libellé (statut, sévérité, projet, testeur assigné) et ses compteurs. */
    public record SummaryRow(String label, long total, long open, long critical) {
        public String getLabel() {
            return label;
        }

        public long getTotal() {
            return total;
        }

        public long getOpen() {
            return open;
        }

        public long getCritical() {
            return critical;
        }
    }

    @Inject
    private DefectService defectService;

    @Inject
    private SessionAuth sessionAuth;

    private List<Defect> defects;

    private long totalCount;
    private long openCount;
    private long closedCount;
    private long criticalCount;

    private List<SummaryRow> byStatus;
    private List<SummaryRow> bySeverity;
    private List<SummaryRow> byProject;
    private List<SummaryRow> byAssignee;

    @PostConstruct
    public void init() {
        User viewer = sessionAuth.getCurrentUser();
        defects = defectService.listForUser(viewer);

        totalCount = defects.size();
        openCount = defects.stream().filter(d -> d.getStatus() != Defect.Status.closed).count();
        closedCount = defects.stream().filter(d -> d.getStatus() == Defect.Status.closed).count();
        criticalCount = defects.stream().filter(d -> d.getSeverity() == Defect.Severity.critical).count();

        byStatus = groupBy(defects, d -> statusLabel(d.getStatus()));
        bySeverity = groupBy(defects, d -> severityLabel(d.getSeverity()));
        byProject = groupBy(defects, d -> d.getProject().getName());
        byAssignee = groupBy(defects, d -> d.getAssignedTo() != null ? d.getAssignedTo().getName() : "Non assignée");
    }

    private List<SummaryRow> groupBy(List<Defect> source, java.util.function.Function<Defect, String> keyFn) {
        Map<String, long[]> counts = new LinkedHashMap<>();
        for (Defect d : source) {
            String key = keyFn.apply(d);
            long[] c = counts.computeIfAbsent(key, k -> new long[3]);
            c[0]++;
            if (d.getStatus() != Defect.Status.closed) {
                c[1]++;
            }
            if (d.getSeverity() == Defect.Severity.critical) {
                c[2]++;
            }
        }
        List<SummaryRow> rows = new ArrayList<>();
        for (Map.Entry<String, long[]> e : counts.entrySet()) {
            rows.add(new SummaryRow(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]));
        }
        rows.sort(Comparator.comparingLong(SummaryRow::total).reversed());
        return rows;
    }

    private String statusLabel(Defect.Status status) {
        return switch (status) {
            case open -> "Ouverte";
            case in_progress -> "En cours";
            case fixed -> "Corrigée";
            case retest -> "En retest";
            case closed -> "Fermée";
            case reopened -> "Réouverte";
        };
    }

    private String severityLabel(Defect.Severity severity) {
        return switch (severity) {
            case critical -> "Critique";
            case high -> "Élevée";
            case medium -> "Moyenne";
            case low -> "Faible";
        };
    }

    /** Génère et envoie un export CSV (compatible Excel, BOM UTF-8) des anomalies visibles. */
    public void exportCsv() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.responseReset();
        ec.setResponseContentType("text/csv; charset=UTF-8");
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"rapport-anomalies.csv\"");

        StringBuilder csv = new StringBuilder();
        csv.append("Titre;Projet;Sévérité;Statut;Assignée à;Créée par;Date de création\n");
        for (Defect d : defects) {
            csv.append(csvEscape(d.getTitle())).append(';')
                    .append(csvEscape(d.getProject().getName())).append(';')
                    .append(severityLabel(d.getSeverity())).append(';')
                    .append(statusLabel(d.getStatus())).append(';')
                    .append(d.getAssignedTo() != null ? csvEscape(d.getAssignedTo().getName()) : "—").append(';')
                    .append(csvEscape(d.getCreatedBy().getName())).append(';')
                    .append(d.getCreatedAt() != null ? d.getCreatedAt().format(DATE_FMT) : "").append('\n');
        }

        try (OutputStream out = ec.getResponseOutputStream()) {
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // BOM — force Excel à détecter l'UTF-8
            out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (java.io.IOException e) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Échec de l'export CSV.", null));
            return;
        }
        fc.responseComplete();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(";") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getOpenCount() {
        return openCount;
    }

    public long getClosedCount() {
        return closedCount;
    }

    public long getCriticalCount() {
        return criticalCount;
    }

    public List<SummaryRow> getByStatus() {
        return byStatus;
    }

    public List<SummaryRow> getBySeverity() {
        return bySeverity;
    }

    public List<SummaryRow> getByProject() {
        return byProject;
    }

    public List<SummaryRow> getByAssignee() {
        return byAssignee;
    }
}
