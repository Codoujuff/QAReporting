package com.qareporting.service;

import com.qareporting.entity.Activity;
import com.qareporting.entity.Campaign;
import com.qareporting.entity.Defect;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Function;

/**
 * CSV and Excel exports — the Jakarta EE equivalent of the Laravel
 * version's maatwebsite/excel-backed /exports endpoints. Rows always come
 * from the same role-scoped service methods the UI itself would use, never
 * a separate "export view" of the data.
 */
@ApplicationScoped
public class ExportService {

    @Inject
    ActivityService activityService;

    @Inject
    DefectService defectService;

    @Inject
    CampaignService campaignService;

    private static final String[] ACTIVITY_HEADERS = {
            "Date", "Utilisateur", "Projet", "Type", "Tests exécutés", "Passés", "Échoués", "Bloqués", "Non exécutés", "Anomalies"
    };
    private static final String[] DEFECT_HEADERS = {
            "ID", "Titre", "Projet", "Sévérité", "Priorité", "Statut", "Assignée à", "Créée par"
    };
    private static final String[] CAMPAIGN_HEADERS = {
            "ID", "Nom", "Projet", "Version", "Statut", "Début", "Fin", "Responsable"
    };

    public byte[] activitiesCsv(User viewer) {
        return csv(ACTIVITY_HEADERS, activityService.listForUser(viewer), this::activityRow);
    }

    public byte[] activitiesXlsx(User viewer) {
        return xlsx("Activités", ACTIVITY_HEADERS, activityService.listForUser(viewer), this::activityRow);
    }

    public byte[] defectsCsv(User viewer) {
        return csv(DEFECT_HEADERS, defectService.listForUser(viewer), this::defectRow);
    }

    public byte[] defectsXlsx(User viewer) {
        return xlsx("Anomalies", DEFECT_HEADERS, defectService.listForUser(viewer), this::defectRow);
    }

    public byte[] campaignsCsv(User viewer) {
        return csv(CAMPAIGN_HEADERS, campaignService.listForUser(viewer), this::campaignRow);
    }

    public byte[] campaignsXlsx(User viewer) {
        return xlsx("Campagnes", CAMPAIGN_HEADERS, campaignService.listForUser(viewer), this::campaignRow);
    }

    private String[] activityRow(Activity a) {
        return new String[]{
                String.valueOf(a.getActivityDate()),
                a.getUser().getName(),
                a.getProject().getName(),
                a.getActivityType(),
                String.valueOf(a.getTestsExecuted()),
                String.valueOf(a.getPassed()),
                String.valueOf(a.getFailed()),
                String.valueOf(a.getBlocked()),
                String.valueOf(a.getNotRun()),
                String.valueOf(a.getDefectsCount()),
        };
    }

    private String[] defectRow(Defect d) {
        return new String[]{
                String.valueOf(d.getId()),
                d.getTitle(),
                d.getProject().getName(),
                d.getSeverity().name(),
                d.getPriority().name(),
                d.getStatus().name(),
                d.getAssignedTo() != null ? d.getAssignedTo().getName() : "",
                d.getCreatedBy().getName(),
        };
    }

    private String[] campaignRow(Campaign c) {
        return new String[]{
                String.valueOf(c.getId()),
                c.getName(),
                c.getProject().getName(),
                c.getVersion() != null ? c.getVersion() : "",
                c.getStatus().name(),
                c.getStartDate() != null ? c.getStartDate().toString() : "",
                c.getEndDate() != null ? c.getEndDate().toString() : "",
                c.getResponsible() != null ? c.getResponsible().getName() : "",
        };
    }

    private <T> byte[] csv(String[] headers, List<T> rows, Function<T, String[]> mapper) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(";", headers)).append("\n");
        for (T row : rows) {
            String[] values = mapper.apply(row);
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(';');
                sb.append(escapeCsv(values[i]));
            }
            sb.append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private <T> byte[] xlsx(String sheetName, String[] headers, List<T> rows, Function<T, String[]> mapper) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (T item : rows) {
                Row row = sheet.createRow(rowIndex++);
                String[] values = mapper.apply(item);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
