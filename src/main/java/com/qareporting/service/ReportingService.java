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
        String scopeClause;
        if (isGlobal(viewer)) {
            scopeClause = "";
        } else if (isTeamScoped(viewer)) {
            scopeClause = " AND t.project.team = :team";
        } else {
            scopeClause = " AND t.assignedTo = :user";
        }

        TypedQuery<Test> testsQuery = em.createQuery("SELECT t FROM Test t WHERE 1=1" + scopeClause, Test.class);
        bindScope(testsQuery, viewer);
        List<Test> tests = testsQuery.getResultList();

        long passed = tests.stream().filter(t -> t.getStatus() == Test.Status.passed).count();
        long failed = tests.stream().filter(t -> t.getStatus() == Test.Status.failed).count();
        long blocked = tests.stream().filter(t -> t.getStatus() == Test.Status.blocked).count();
        long notRun = tests.stream().filter(t -> t.getStatus() == Test.Status.not_run).count();

        String defectScope = isGlobal(viewer) ? ""
                : isTeamScoped(viewer) ? " AND d.project.team = :team"
                : " AND d.assignedTo = :user";
        TypedQuery<Defect> defectsQuery = em.createQuery("SELECT d FROM Defect d WHERE 1=1" + defectScope, Defect.class);
        bindScope(defectsQuery, viewer);
        List<Defect> defects = defectsQuery.getResultList();

        long openDefects = defects.stream().filter(d -> d.getStatus() != Defect.Status.closed).count();
        long criticalDefects = defects.stream().filter(d -> d.getSeverity() == Defect.Severity.critical).count();

        String campaignScope = isGlobal(viewer) ? "" : isTeamScoped(viewer) ? " AND c.project.team = :team" : "";
        TypedQuery<Campaign> campaignsQuery = em.createQuery(
                "SELECT c FROM Campaign c WHERE 1=1" + campaignScope, Campaign.class);
        if (isTeamScoped(viewer)) {
            campaignsQuery.setParameter("team", viewer.getTeam());
        }
        long campaignsInProgress = campaignsQuery.getResultList().stream()
                .filter(c -> c.getStatus() == Campaign.Status.in_progress)
                .count();

        return Json.createObjectBuilder()
                .add("campaigns_in_progress", campaignsInProgress)
                .add("tests_total", tests.size())
                .add("tests_passed", passed)
                .add("tests_failed", failed)
                .add("tests_blocked", blocked)
                .add("tests_not_run", notRun)
                .add("pass_rate", tests.isEmpty() ? 0 : Math.round(passed * 1000.0 / tests.size()) / 10.0)
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
        String scopeClause = isGlobal(viewer) ? ""
                : isTeamScoped(viewer) ? " AND a.user.team = :team"
                : " AND a.user = :user";

        TypedQuery<Activity> query = em.createQuery(
                "SELECT a FROM Activity a WHERE a.activityDate BETWEEN :start AND :end" + scopeClause,
                Activity.class);
        query.setParameter("start", start);
        query.setParameter("end", end);
        bindScope(query, viewer);
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

    private boolean isTeamScoped(User viewer) {
        return viewer.getRole().getName().equals(Role.QA_LEAD) && viewer.getTeam() != null;
    }

    private void bindScope(TypedQuery<?> query, User viewer) {
        if (isTeamScoped(viewer)) {
            query.setParameter("team", viewer.getTeam());
        } else if (!isGlobal(viewer)) {
            query.setParameter("user", viewer);
        }
    }
}
