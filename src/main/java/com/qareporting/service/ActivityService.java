package com.qareporting.service;

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
    public List<Activity> listForUser(User viewer) {
        String roleName = viewer.getRole().getName();
        if (roleName.equals(Role.MANAGER) || roleName.equals(Role.ADMIN)) {
            return findAll();
        }
        if (roleName.equals(Role.QA_LEAD) && viewer.getTeam() != null) {
            return em.createQuery(
                            "SELECT a FROM Activity a WHERE a.user.team = :team ORDER BY a.activityDate DESC",
                            Activity.class)
                    .setParameter("team", viewer.getTeam())
                    .getResultList();
        }
        return em.createQuery(
                        "SELECT a FROM Activity a WHERE a.user = :user ORDER BY a.activityDate DESC",
                        Activity.class)
                .setParameter("user", viewer)
                .getResultList();
    }

    public boolean canView(User viewer, Activity activity) {
        if (viewer == null || activity == null) {
            return false;
        }
        if (Scope.seesEverything(viewer)) {
            return true;
        }
        if (Scope.sameUser(activity.getUser(), viewer)) {
            return true;
        }
        return Scope.isLeadWithTeam(viewer) && activity.getUser() != null && activity.getUser().getTeam() != null
                && viewer.getTeam().getId().equals(activity.getUser().getTeam().getId());
    }

    /** Une déclaration d'activité ne se modifie / supprime que par son auteur (ou l'admin). */
    public boolean canEdit(User viewer, Activity activity) {
        return activity != null && viewer != null
                && (Scope.sameUser(activity.getUser(), viewer)
                    || (viewer.getRole() != null && Role.ADMIN.equals(viewer.getRole().getName())));
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
