package com.qareporting.service;

import com.qareporting.entity.Activity;
import com.qareporting.entity.Campaign;
import com.qareporting.entity.Defect;
import com.qareporting.entity.Role;
import com.qareporting.entity.Test;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

/**
 * All numbers here are computed from real rows at query time — never
 * cached or hand-entered — same guarantee as the Laravel version's
 * ReportingController. Visibility is scoped the same way too: QA sees
 * their own data, QA Lead their team's, Manager/Admin everything.
 */
@ApplicationScoped
public class ReportingService {

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    public JsonObject dashboard(User viewer) {
        // Tout est compté par la base (COUNT / GROUP BY) : rien n'est chargé en mémoire,
        // quel que soit le nombre de tests ou d'anomalies.
        String testScope = scope(viewer, "t.project", "t.assignedTo = :scopeUser");
        TypedQuery<Object[]> byStatus = em.createQuery(
                "SELECT t.status, COUNT(t) FROM Test t WHERE 1=1" + testScope + " GROUP BY t.status", Object[].class);
        bind(byStatus, testScope, viewer);
        java.util.Map<Test.Status, Long> counts = new java.util.EnumMap<>(Test.Status.class);
        for (Object[] row : byStatus.getResultList()) {
            counts.put((Test.Status) row[0], (Long) row[1]);
        }
        long passed = counts.getOrDefault(Test.Status.passed, 0L);
        long failed = counts.getOrDefault(Test.Status.failed, 0L);
        long blocked = counts.getOrDefault(Test.Status.blocked, 0L);
        long notRun = counts.getOrDefault(Test.Status.not_run, 0L);
        long total = passed + failed + blocked + notRun;

        String defectScope = scope(viewer, "d.project", "(d.assignedTo = :scopeUser OR d.createdBy = :scopeUser)");
        long openDefects = count("SELECT COUNT(d) FROM Defect d WHERE d.status <> :closed" + defectScope,
                defectScope, viewer, "closed", Defect.Status.closed);
        long criticalDefects = count("SELECT COUNT(d) FROM Defect d WHERE d.severity = :critical" + defectScope,
                defectScope, viewer, "critical", Defect.Severity.critical);

        String campaignScope = isGlobal(viewer) ? "" : " AND " + Scope.projectClause("c.project", viewer);
        long campaignsInProgress = count("SELECT COUNT(c) FROM Campaign c WHERE c.status = :running" + campaignScope,
                campaignScope, viewer, "running", Campaign.Status.in_progress);

        long testsTotal = total;
        return Json.createObjectBuilder()
                .add("campaigns_in_progress", campaignsInProgress)
                .add("tests_total", testsTotal)
                .add("tests_passed", passed)
                .add("tests_failed", failed)
                .add("tests_blocked", blocked)
                .add("tests_not_run", notRun)
                .add("pass_rate", testsTotal == 0 ? 0 : Math.round(passed * 1000.0 / testsTotal) / 10.0)
                .add("defects_open", openDefects)
                .add("defects_critical", criticalDefects)
                .build();
    }

    public JsonObject daily(User viewer, LocalDate date) {
        List<Activity> activities = scopedActivities(viewer, date, date);
        return summarize(activities);
    }

    public JsonObject weekly(User viewer, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        List<Activity> activities = scopedActivities(viewer, weekStart, weekEnd);

        JsonArrayBuilder days = Json.createArrayBuilder();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            List<Activity> dayActivities = activities.stream()
                    .filter(a -> a.getActivityDate().equals(day))
                    .toList();
            days.add(Json.createObjectBuilder(summarize(dayActivities)).add("date", day.toString()));
        }

        return Json.createObjectBuilder(summarize(activities))
                .add("week", weekStart.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear()))
                .add("days", days)
                .build();
    }

    public JsonObject monthly(User viewer, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<Activity> activities = scopedActivities(viewer, start, end);

        JsonArrayBuilder days = Json.createArrayBuilder();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            LocalDate current = day;
            List<Activity> dayActivities = activities.stream()
                    .filter(a -> a.getActivityDate().equals(current))
                    .toList();
            if (!dayActivities.isEmpty()) {
                days.add(Json.createObjectBuilder(summarize(dayActivities)).add("date", current.toString()));
            }
        }

        return Json.createObjectBuilder(summarize(activities))
                .add("month", month.toString())
                .add("days", days)
                .build();
    }

    /** Activités déclarées entre deux dates (incluses), dans la portée du rôle — pour les rapports JSF. */
    public List<Activity> activitiesBetween(User viewer, LocalDate start, LocalDate end) {
        return scopedActivities(viewer, start, end);
    }

    private List<Activity> scopedActivities(User viewer, LocalDate start, LocalDate end) {
        // Lead : ses déclarations et toutes celles faites sur ses projets ; QA : les siennes.
        String scopeClause = isGlobal(viewer) ? ""
                : Scope.isLead(viewer) ? " AND (a.user = :scopeUser OR " + Scope.projectClause("a.project", viewer) + ")"
                : " AND a.user = :scopeUser";

        TypedQuery<Activity> query = em.createQuery(
                "SELECT a FROM Activity a WHERE a.activityDate BETWEEN :start AND :end" + scopeClause,
                Activity.class);
        query.setParameter("start", start);
        query.setParameter("end", end);
        bind(query, scopeClause, viewer);
        return query.getResultList();
    }

    private JsonObject summarize(List<Activity> activities) {
        int testsExecuted = activities.stream().mapToInt(Activity::getTestsExecuted).sum();
        int passed = activities.stream().mapToInt(Activity::getPassed).sum();
        int failed = activities.stream().mapToInt(Activity::getFailed).sum();
        int blocked = activities.stream().mapToInt(Activity::getBlocked).sum();
        int notRun = activities.stream().mapToInt(Activity::getNotRun).sum();
        int defectsCount = activities.stream().mapToInt(Activity::getDefectsCount).sum();

        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("tests_executed", testsExecuted)
                .add("passed", passed)
                .add("failed", failed)
                .add("blocked", blocked)
                .add("not_run", notRun)
                .add("defects_count", defectsCount);
        return builder.build();
    }

    private boolean isGlobal(User viewer) {
        String role = viewer.getRole().getName();
        return role.equals(Role.MANAGER) || role.equals(Role.ADMIN);
    }

    /** Clause de périmètre : rien (Manager / Admin), les projets du lead, ou la condition « à moi » du QA. */
    private String scope(User viewer, String projectPath, String ownCondition) {
        if (isGlobal(viewer)) {
            return "";
        }
        return Scope.isLead(viewer) ? " AND " + Scope.projectClause(projectPath, viewer) : " AND " + ownCondition;
    }

    private void bind(jakarta.persistence.Query query, String clause, User viewer) {
        if (clause.contains(":scopeTeam")) {
            query.setParameter("scopeTeam", viewer.getTeam());
        }
        if (clause.contains(":scopeUser")) {
            query.setParameter("scopeUser", viewer);
        }
    }

    private long count(String jpql, String clause, User viewer, String param, Object value) {
        TypedQuery<Long> q = em.createQuery(jpql, Long.class).setParameter(param, value);
        bind(q, clause, viewer);
        return q.getSingleResult();
    }
}
