package com.qareporting.web;

import com.qareporting.entity.Activity;
import com.qareporting.web.i18n.I18n;
import com.qareporting.entity.Campaign;
import com.qareporting.entity.Defect;
import com.qareporting.entity.Project;
import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import com.qareporting.service.ActivityService;
import com.qareporting.service.CampaignService;
import com.qareporting.service.DefectService;
import com.qareporting.service.ProjectService;
import com.qareporting.service.TeamService;
import com.qareporting.service.TestService;
import com.qareporting.service.UserService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named
@RequestScoped
public class DashboardBean implements Serializable {

    /** One row of a horizontal bar chart: a category label, its count, and the CSS class carrying its color. */
    private record ChartRow(String label, long value, String colorClass) {
    }

    /**
     * One row of the QA Lead's team table.
     * JSF's EL resolver here only recognizes JavaBean-style {@code getX()} accessors, not the
     * record's plain {@code x()} accessors, so explicit getters are added for use in Facelets.
     */
    public record MemberRow(String name, String initials, long assignedCount, long createdCount) {
        public String getName() {
            return name;
        }

        public String getInitials() {
            return initials;
        }

        public long getAssignedCount() {
            return assignedCount;
        }

        public long getCreatedCount() {
            return createdCount;
        }
    }

    /**
     * One row of the Manager's cross-project breakdown table.
     * See {@link MemberRow} for why explicit getters are needed alongside the record accessors.
     */
    public record ProjectRow(String projectName, long total, long open, long critical) {
        public String getProjectName() {
            return projectName;
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

    /** Une ligne de la carte "Avancement des campagnes" du dashboard QA. */
    public record CampaignProgressRow(String name, int percent) {
        public String getName() {
            return name;
        }

        public int getPercent() {
            return percent;
        }
    }

    @Inject
    private ProjectService projectService;

    @Inject
    private TeamService teamService;

    @Inject
    private DefectService defectService;

    @Inject
    private UserService userService;

    @Inject
    private ActivityService activityService;

    @Inject
    private CampaignService campaignService;

    @Inject
    private TestService testService;

    @Inject
    private SessionAuth sessionAuth;

    private boolean leadView;
    private boolean managerView;
    private boolean qaView;

    private int projectCount;
    private int teamCount;
    private int userCount;
    private int myDefectCount;
    private int openDefectCount;
    private long criticalDefectCount;

    private String defectsByStatusSvg;
    private String defectsBySeveritySvg;
    private String projectsByStatusSvg;

    private List<Defect> recentDefects;
    private List<MemberRow> teamMembers;
    private boolean hasTeam;
    private List<ProjectRow> projectBreakdown;

    private String greetingName;
    private int testsExecutedCount;
    private int passedCount;
    private int failedCount;
    private int blockedCount;
    private double successRate;
    private List<Activity> todaysActivities;
    private String testsByDaySvg;
    private List<CampaignProgressRow> campaignProgress;

    @PostConstruct
    public void init() {
        managerView = sessionAuth.hasRole(Role.MANAGER, Role.ADMIN);
        leadView = sessionAuth.hasRole(Role.QA_LEAD);
        qaView = !managerView && !leadView;

        projectCount = projectService.findAll().size();
        teamCount = teamService.findAll().size();
        userCount = userService.findAll().size();

        User viewer = sessionAuth.getCurrentUser();
        List<Defect> visibleDefects = defectService.listForUser(viewer);
        myDefectCount = visibleDefects.size();
        openDefectCount = (int) visibleDefects.stream()
                .filter(d -> d.getStatus() != Defect.Status.closed)
                .count();

        Map<Defect.Status, Long> byStatus = new LinkedHashMap<>();
        for (Defect.Status s : Defect.Status.values()) {
            byStatus.put(s, 0L);
        }
        for (Defect d : visibleDefects) {
            byStatus.merge(d.getStatus(), 1L, Long::sum);
        }

        Map<Defect.Severity, Long> bySeverity = new LinkedHashMap<>();
        for (Defect.Severity s : Defect.Severity.values()) {
            bySeverity.put(s, 0L);
        }
        for (Defect d : visibleDefects) {
            bySeverity.merge(d.getSeverity(), 1L, Long::sum);
        }
        criticalDefectCount = bySeverity.getOrDefault(Defect.Severity.critical, 0L);

        Map<Project.Status, Long> byProjectStatus = new LinkedHashMap<>();
        for (Project.Status s : Project.Status.values()) {
            byProjectStatus.put(s, 0L);
        }
        for (Project p : projectService.findAll()) {
            byProjectStatus.merge(p.getStatus(), 1L, Long::sum);
        }

        defectsByStatusSvg = buildChart(List.of(
                new ChartRow(I18n.t("enum.Defect.Status.open"), byStatus.get(Defect.Status.open), "bar-default"),
                new ChartRow(I18n.t("enum.Defect.Status.in_progress"), byStatus.get(Defect.Status.in_progress), "bar-default"),
                new ChartRow(I18n.t("enum.Defect.Status.fixed"), byStatus.get(Defect.Status.fixed), "bar-default"),
                new ChartRow(I18n.t("enum.Defect.Status.retest"), byStatus.get(Defect.Status.retest), "bar-default"),
                new ChartRow(I18n.t("enum.Defect.Status.closed"), byStatus.get(Defect.Status.closed), "bar-default"),
                new ChartRow(I18n.t("enum.Defect.Status.reopened"), byStatus.get(Defect.Status.reopened), "bar-default")
        ));

        defectsBySeveritySvg = buildChart(List.of(
                new ChartRow(I18n.t("enum.Defect.Severity.critical"), bySeverity.get(Defect.Severity.critical), "bar-critical"),
                new ChartRow(I18n.t("enum.Defect.Severity.high"), bySeverity.get(Defect.Severity.high), "bar-serious"),
                new ChartRow(I18n.t("enum.Defect.Severity.medium"), bySeverity.get(Defect.Severity.medium), "bar-warning"),
                new ChartRow(I18n.t("enum.Defect.Severity.low"), bySeverity.get(Defect.Severity.low), "bar-good")
        ));

        projectsByStatusSvg = buildChart(List.of(
                new ChartRow(I18n.t("enum.Project.Status.active"), byProjectStatus.get(Project.Status.active), "bar-default"),
                new ChartRow(I18n.t("enum.Project.Status.inactive"), byProjectStatus.get(Project.Status.inactive), "bar-default"),
                new ChartRow(I18n.t("enum.Project.Status.archived"), byProjectStatus.get(Project.Status.archived), "bar-default")
        ));

        if (qaView) {
            recentDefects = visibleDefects.stream()
                    .sorted(Comparator.comparing(Defect::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(5)
                    .toList();

            greetingName = viewer.getName() != null ? viewer.getName().split(" ")[0] : viewer.getName();

            List<Activity> activities = activityService.listForUser(viewer);
            testsExecutedCount = activities.stream().mapToInt(Activity::getTestsExecuted).sum();
            passedCount = activities.stream().mapToInt(Activity::getPassed).sum();
            failedCount = activities.stream().mapToInt(Activity::getFailed).sum();
            blockedCount = activities.stream().mapToInt(Activity::getBlocked).sum();
            successRate = testsExecutedCount == 0 ? 0.0 : 100.0 * passedCount / testsExecutedCount;

            LocalDate today = LocalDate.now();
            todaysActivities = activities.stream()
                    .filter(a -> today.equals(a.getActivityDate()))
                    .toList();

            Map<LocalDate, Integer> testsByDay = new LinkedHashMap<>();
            for (Activity a : activities) {
                if (a.getActivityDate() != null) {
                    testsByDay.merge(a.getActivityDate(), a.getTestsExecuted(), Integer::sum);
                }
            }
            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
            List<ChartRow> dayRows = testsByDay.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .skip(Math.max(0, testsByDay.size() - 7))
                    .map(e -> new ChartRow(e.getKey().format(dayFmt), e.getValue(), "bar-default"))
                    .toList();
            testsByDaySvg = buildChart(dayRows);

            campaignProgress = new ArrayList<>();
            for (Campaign c : campaignService.listForUser(viewer)) {
                List<com.qareporting.entity.Test> campaignTests = testService.findByCampaign(c.getId());
                int percent = 0;
                if (!campaignTests.isEmpty()) {
                    long executed = campaignTests.stream()
                            .filter(t -> t.getStatus() != com.qareporting.entity.Test.Status.not_run)
                            .count();
                    percent = (int) Math.round(100.0 * executed / campaignTests.size());
                }
                campaignProgress.add(new CampaignProgressRow(c.getName(), percent));
            }
        }

        if (leadView) {
            hasTeam = viewer.getTeam() != null;
            teamMembers = new ArrayList<>();
            if (hasTeam) {
                for (User member : userService.findByTeam(viewer.getTeam())) {
                    long assigned = visibleDefects.stream()
                            .filter(d -> d.getAssignedTo() != null && d.getAssignedTo().getId().equals(member.getId()))
                            .count();
                    long created = visibleDefects.stream()
                            .filter(d -> d.getCreatedBy().getId().equals(member.getId()))
                            .count();
                    teamMembers.add(new MemberRow(member.getName(), member.getInitials(), assigned, created));
                }
            }
        }

        if (managerView) {
            Map<String, long[]> byProject = new LinkedHashMap<>();
            for (Defect d : visibleDefects) {
                String name = d.getProject().getName();
                long[] counts = byProject.computeIfAbsent(name, k -> new long[3]);
                counts[0]++;
                if (d.getStatus() != Defect.Status.closed) {
                    counts[1]++;
                }
                if (d.getSeverity() == Defect.Severity.critical) {
                    counts[2]++;
                }
            }
            projectBreakdown = byProject.entrySet().stream()
                    .map(e -> new ProjectRow(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                    .sorted(Comparator.comparingLong(ProjectRow::total).reversed())
                    .toList();
        }
    }

    /** Renders a simple, dependency-free horizontal bar chart as an inline SVG string. */
    private String buildChart(List<ChartRow> rows) {
        final int rowHeight = 44;
        final int barAreaWidth = 350;
        final int barHeight = 20;

        long max = rows.stream().mapToLong(ChartRow::value).max().orElse(0L);

        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox=\"0 0 420 ").append(rows.size() * rowHeight)
                .append("\" width=\"100%\" role=\"img\">");

        int y = 0;
        for (ChartRow row : rows) {
            int barWidth = max == 0 ? 0 : (int) Math.round((double) row.value() / max * barAreaWidth);
            int labelY = y + 12;
            int barY = y + 16;

            svg.append("<text x=\"0\" y=\"").append(labelY).append("\" class=\"chart-label\">")
                    .append(row.label()).append("</text>");
            svg.append("<rect x=\"0\" y=\"").append(barY).append("\" width=\"").append(barWidth)
                    .append("\" height=\"").append(barHeight).append("\" rx=\"4\" class=\"")
                    .append(row.colorClass()).append("\">");
            svg.append("<title>").append(row.label()).append(" : ").append(row.value()).append("</title>");
            svg.append("</rect>");
            svg.append("<text x=\"").append(barWidth + 8).append("\" y=\"").append(barY + barHeight - 5)
                    .append("\" class=\"chart-value\">").append(row.value()).append("</text>");

            y += rowHeight;
        }

        svg.append("</svg>");
        return svg.toString();
    }

    public boolean isQaView() {
        return qaView;
    }

    public boolean isLeadView() {
        return leadView;
    }

    public boolean isManagerView() {
        return managerView;
    }

    public int getProjectCount() {
        return projectCount;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public int getUserCount() {
        return userCount;
    }

    public int getMyDefectCount() {
        return myDefectCount;
    }

    public int getOpenDefectCount() {
        return openDefectCount;
    }

    public long getCriticalDefectCount() {
        return criticalDefectCount;
    }

    public String getDefectsByStatusSvg() {
        return defectsByStatusSvg;
    }

    public String getDefectsBySeveritySvg() {
        return defectsBySeveritySvg;
    }

    public String getProjectsByStatusSvg() {
        return projectsByStatusSvg;
    }

    public List<Defect> getRecentDefects() {
        return recentDefects;
    }

    public boolean isHasTeam() {
        return hasTeam;
    }

    public List<MemberRow> getTeamMembers() {
        return teamMembers;
    }

    public List<ProjectRow> getProjectBreakdown() {
        return projectBreakdown;
    }

    public String getGreetingName() {
        return greetingName;
    }

    public int getTestsExecutedCount() {
        return testsExecutedCount;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public List<Activity> getTodaysActivities() {
        return todaysActivities;
    }

    public String getTestsByDaySvg() {
        return testsByDaySvg;
    }

    public List<CampaignProgressRow> getCampaignProgress() {
        return campaignProgress;
    }
}
