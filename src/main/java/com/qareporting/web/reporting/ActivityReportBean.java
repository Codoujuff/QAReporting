package com.qareporting.web.reporting;

import com.qareporting.entity.Activity;
import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import com.qareporting.service.ReportingService;
import com.qareporting.web.auth.SessionAuth;
import com.qareporting.web.i18n.I18n;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rapports d'activité journalier / hebdomadaire / mensuel (cahier des charges §4.7),
 * calculés à la volée à partir des activités déclarées, dans la portée du rôle — mêmes
 * données que GET /api/reporting/daily|weekly|monthly. Période et date passent par l'URL
 * (?period=week&date=2026-09-21) : un rapport se partage et se met en favori.
 */
@Named
@RequestScoped
public class ActivityReportBean {

    public enum Period { day, week, month }

    /** Totaux d'une ligne (un jour, un testeur, ou toute la période). */
    public static final class Totals {
        private final String label;
        private int testsExecuted;
        private int passed;
        private int failed;
        private int blocked;
        private int notRun;
        private int defects;
        private int entries;

        Totals(String label) {
            this.label = label;
        }

        void add(Activity a) {
            testsExecuted += a.getTestsExecuted();
            passed += a.getPassed();
            failed += a.getFailed();
            blocked += a.getBlocked();
            notRun += a.getNotRun();
            defects += a.getDefectsCount();
            entries++;
        }

        public String getLabel() { return label; }
        public int getTestsExecuted() { return testsExecuted; }
        public int getPassed() { return passed; }
        public int getFailed() { return failed; }
        public int getBlocked() { return blocked; }
        public int getNotRun() { return notRun; }
        public int getDefects() { return defects; }
        public int getEntries() { return entries; }

        /** Taux de réussite en % (réussis / exécutés), 0 si rien n'a été exécuté. */
        public double getSuccessRate() {
            return testsExecuted == 0 ? 0 : Math.round(passed * 1000.0 / testsExecuted) / 10.0;
        }

        /** Largeurs (en %) des segments de la barre empilée réussis / échoués / bloqués / non exécutés. */
        public double share(int part) {
            return testsExecuted == 0 ? 0 : part * 100.0 / testsExecuted;
        }

        public double getPassedShare() { return share(passed); }
        public double getFailedShare() { return share(failed); }
        public double getBlockedShare() { return share(blocked); }
        public double getNotRunShare() { return share(notRun); }
    }

    @Inject
    private ReportingService reportingService;

    @Inject
    private SessionAuth sessionAuth;

    private String period = Period.week.name();
    private String date;

    private Period resolvedPeriod;
    private LocalDate anchor;
    private LocalDate start;
    private LocalDate end;
    private Totals total;
    private List<Totals> byDay;
    private List<Totals> byTester;

    /** f:viewAction : calcule le rapport une fois les paramètres d'URL appliqués. */
    public void load() {
        resolvedPeriod = parsePeriod(period);
        anchor = parseDate(date);
        switch (resolvedPeriod) {
            case day -> {
                start = anchor;
                end = anchor;
            }
            case week -> {
                start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = start.plusDays(6);
            }
            case month -> {
                start = YearMonth.from(anchor).atDay(1);
                end = YearMonth.from(anchor).atEndOfMonth();
            }
        }

        User viewer = sessionAuth.getCurrentUser();
        List<Activity> activities = reportingService.activitiesBetween(viewer, start, end);

        total = new Totals(null);
        activities.forEach(total::add);

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern(
                I18n.locale().getLanguage().equals("en") ? "EEE, MMM d" : "EEE d MMM", I18n.locale());
        Map<LocalDate, Totals> days = new LinkedHashMap<>();
        // Semaine : les 7 jours, même vides (on voit les trous). Mois / jour : seulement les jours saisis.
        if (resolvedPeriod == Period.week) {
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                days.put(d, new Totals(capitalize(d.format(dayFmt))));
            }
        }
        activities.stream()
                .sorted(Comparator.comparing(Activity::getActivityDate))
                .forEach(a -> days.computeIfAbsent(a.getActivityDate(),
                        d -> new Totals(capitalize(d.format(dayFmt)))).add(a));
        byDay = new ArrayList<>(days.values());

        Map<String, Totals> testers = new LinkedHashMap<>();
        activities.stream()
                .filter(a -> a.getUser() != null)
                .forEach(a -> testers.computeIfAbsent(a.getUser().getName(), Totals::new).add(a));
        byTester = new ArrayList<>(testers.values());
        byTester.sort(Comparator.comparingInt(Totals::getTestsExecuted).reversed());
    }

    // ---------- navigation ----------

    public String getPreviousDate() {
        return shift(-1).toString();
    }

    public String getNextDate() {
        return shift(1).toString();
    }

    public String getToday() {
        return LocalDate.now().toString();
    }

    public boolean isCurrentPeriod() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(start) && !today.isAfter(end);
    }

    private LocalDate shift(int direction) {
        return switch (resolvedPeriod) {
            case day -> anchor.plusDays(direction);
            case week -> start.plusWeeks(direction);
            case month -> start.plusMonths(direction);
        };
    }

    /** « Semaine 39 · 22 – 28 sept. 2026 », « Septembre 2026 », « Jeudi 24 septembre 2026 ». */
    public String getPeriodLabel() {
        boolean en = I18n.locale().getLanguage().equals("en");
        return switch (resolvedPeriod) {
            case day -> capitalize(anchor.format(DateTimeFormatter.ofPattern(
                    en ? "EEEE, MMMM d, yyyy" : "EEEE d MMMM yyyy", I18n.locale())));
            case week -> I18n.t("activityReport.weekLabel", start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                    start.format(DateTimeFormatter.ofPattern(en ? "MMM d" : "d MMM", I18n.locale())),
                    end.format(DateTimeFormatter.ofPattern(en ? "MMM d, yyyy" : "d MMM yyyy", I18n.locale())));
            case month -> capitalize(start.format(DateTimeFormatter.ofPattern("MMMM yyyy", I18n.locale())));
        };
    }

    /** Le détail par testeur n'a de sens que pour ceux qui voient plusieurs personnes. */
    public boolean isShowTesters() {
        return !sessionAuth.hasRole(Role.QA);
    }

    // ---------- accesseurs ----------

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getResolvedPeriod() { return resolvedPeriod.name(); }
    public String getAnchorDate() { return anchor.toString(); }
    public Totals getTotal() { return total; }
    public List<Totals> getByDay() { return byDay; }
    public List<Totals> getByTester() { return byTester; }

    private static Period parsePeriod(String value) {
        try {
            return value == null ? Period.week : Period.valueOf(value);
        } catch (IllegalArgumentException e) {
            return Period.week;
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
