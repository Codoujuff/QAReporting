package com.qareporting.service;

import java.time.LocalDate;
import com.qareporting.entity.Activity;
import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ActivityService extends AbstractCrudService<Activity, Long> {

    @Override
    protected Class<Activity> entityClass() {
        return Activity.class;
    }

    /**
     * Same visibility rule as the Laravel version's role-scoped reporting:
     * QA sees only their own activity, QA Lead sees their whole team,
     * Manager/Admin see everything.
     */
    /**
     * QA : ses déclarations ; QA Lead : les siennes et toutes celles faites sur ses projets
     * (y compris par des testeurs d'autres équipes) ; Manager / Admin : toutes.
     */
    public List<Activity> listForUser(User viewer) {
        if (Scope.seesEverything(viewer)) {
            return em.createQuery("SELECT a FROM Activity a ORDER BY a.activityDate DESC", Activity.class).getResultList();
        }
        if (Scope.isLead(viewer)) {
            var q = em.createQuery("SELECT a FROM Activity a WHERE a.user = :scopeUser OR "
                    + Scope.projectClause("a.project", viewer) + " ORDER BY a.activityDate DESC", Activity.class);
            Scope.bindProjectScope(q, viewer);
            return q.getResultList();
        }
        return em.createQuery(
                        "SELECT a FROM Activity a WHERE a.user = :user ORDER BY a.activityDate DESC",
                        Activity.class)
                .setParameter("user", viewer)
                .getResultList();
    }

    public SearchQuery search(User viewer, String q, String status) {
        SearchQuery s = new SearchQuery("FROM Activity x JOIN x.user u JOIN x.project p LEFT JOIN x.campaign c");
        if (!Scope.seesEverything(viewer)) {
            s.scope(Scope.isLead(viewer) ? "(x.user = :scopeUser OR " + Scope.projectClause("x.project", viewer) + ")"
                    : "x.user = :scopeUser", viewer);
        }
        if ("toValidate".equals(status)) {
            s.and("x.validatedAt IS NULL");
        } else if ("validated".equals(status)) {
            s.and("x.validatedAt IS NOT NULL");
        }
        return s.text(q, null, "", "u.name", "p.name", "c.name", "x.activityType");
    }

    public long count(SearchQuery s) {
        return s.count(em, "x");
    }

    public List<Activity> page(SearchQuery s, int first, int size) {
        return s.page(em, "x", Activity.class, "x.activityDate DESC, x.id DESC", first, size);
    }

    public boolean canView(User viewer, Activity activity) {
        if (viewer == null || activity == null) {
            return false;
        }
        if (Scope.seesEverything(viewer) || Scope.sameUser(activity.getUser(), viewer)) {
            return true;
        }
        return Scope.isLead(viewer) && Scope.onProject(activity.getProject(), viewer);
    }

    /** Une déclaration d'activité ne se modifie / supprime que par son auteur (ou l'admin). */
    public boolean canEdit(User viewer, Activity activity) {
        if (activity == null || viewer == null) {
            return false;
        }
        if (isAdmin(viewer)) {
            return true;
        }
        // Une fois validée par le lead, la déclaration est figée pour son auteur.
        return Scope.sameUser(activity.getUser(), viewer) && !activity.isValidated();
    }

    /**
     * Le QA Lead valide les déclarations des membres de son équipe (pas les siennes : on ne
     * valide pas son propre travail) ; l'admin peut tout valider.
     */
    /**
     * La déclaration est validée par le QA Lead de l'équipe du PROJET sur lequel on a
     * travaillé — un testeur prêté à un autre projet est validé par le lead de ce projet —
     * jamais par son auteur ; l'admin peut tout valider.
     */
    public boolean canValidate(User viewer, Activity activity) {
        if (viewer == null || activity == null || activity.isValidated() || activity.getUser() == null) {
            return false;
        }
        if (isAdmin(viewer)) {
            return true;
        }
        return Scope.isLeadWithTeam(viewer)
                && !Scope.sameUser(activity.getUser(), viewer)
                && Scope.inTeam(activity.getProject(), viewer.getTeam());
    }

    @Transactional
    public Activity validateByLead(Activity activity, User lead) {
        if (!canValidate(lead, activity)) {
            throw new IllegalStateException("Validation non autorisée.");
        }
        activity.setValidatedBy(lead);
        activity.setValidatedAt(java.time.LocalDateTime.now());
        Activity merged = em.merge(activity);
        audit.record("validated", merged);
        return merged;
    }

    /** Déclarations de l'équipe du lead encore à valider (hors les siennes). */
    /** Déclarations faites sur les projets de l'équipe du lead et encore à valider (hors les siennes). */
    public long countToValidate(User lead) {
        if (!Scope.isLeadWithTeam(lead)) {
            return 0;
        }
        return em.createQuery("SELECT COUNT(a) FROM Activity a WHERE a.project.team = :team "
                        + "AND a.user <> :lead AND a.validatedAt IS NULL", Long.class)
                .setParameter("team", lead.getTeam())
                .setParameter("lead", lead)
                .getSingleResult();
    }

    /** Déclaration du jour déjà faite ? (utilisé par le rappel quotidien) */
    public boolean hasActivityOn(User user, LocalDate day) {
        return em.createQuery("SELECT COUNT(a) FROM Activity a WHERE a.user = :user AND a.activityDate = :day", Long.class)
                .setParameter("user", user)
                .setParameter("day", day)
                .getSingleResult() > 0;
    }

    private static boolean isAdmin(User user) {
        return user.getRole() != null && Role.ADMIN.equals(user.getRole().getName());
    }

    /**
     * Same two server-enforced rules as the Laravel ActivityRequest validator:
     * passed+failed+blocked+not_run must equal tests_executed, and a blocked
     * count above zero requires a reason. Never trusted to client-side
     * validation alone.
     */
    public String validate(Activity activity) {
        int sum = activity.getPassed() + activity.getFailed() + activity.getBlocked() + activity.getNotRun();
        if (sum != activity.getTestsExecuted()) {
            return "La somme des résultats (" + sum + ") ne correspond pas au nombre de tests exécutés ("
                    + activity.getTestsExecuted() + ").";
        }
        if (activity.getBlocked() > 0 && (activity.getBlockedReason() == null || activity.getBlockedReason().isBlank())) {
            return "Un motif de blocage est requis lorsque des tests sont bloqués.";
        }
        return null;
    }

    @Transactional
    public Activity createValidated(Activity activity) {
        activity.setIsBlocked(activity.getBlocked() > 0);
        em.persist(activity);
        audit.record(AuditService.CREATED, activity);
        return activity;
    }
}
