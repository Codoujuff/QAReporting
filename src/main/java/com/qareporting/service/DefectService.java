package com.qareporting.service;

import com.qareporting.entity.Defect;
import com.qareporting.entity.DefectHistory;
import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class DefectService extends AbstractCrudService<Defect, Long> {

    @Inject
    NotificationService notificationService;

    @Override
    protected Class<Defect> entityClass() {
        return Defect.class;
    }

    /**
     * Testeurs à qui l'anomalie peut être assignée : les QA et QA Lead actifs de l'équipe
     * du projet (tous les QA / QA Lead actifs si le projet n'a pas d'équipe). Ni le Manager
     * ni l'Admin ne corrigent ou ne revérifient d'anomalie.
     */
    public List<User> assignableUsers(Defect defect) {
        boolean teamless = defect == null || defect.getProject() == null || defect.getProject().getTeam() == null;
        var query = em.createQuery("SELECT u FROM User u WHERE u.active = true AND u.role.name IN :roles"
                        + (teamless ? "" : " AND u.team = :team") + " ORDER BY u.name", User.class)
                .setParameter("roles", List.of(Role.QA, Role.QA_LEAD));
        if (!teamless) {
            query.setParameter("team", defect.getProject().getTeam());
        }
        return query.getResultList();
    }

    public boolean canBeAssignedTo(Defect defect, User assignee) {
        return assignee != null && assignableUsers(defect).stream().anyMatch(u -> u.getId().equals(assignee.getId()));
    }

    /** Même règle que listForUser, pour un seul objet (ouverture par id). */
    public boolean canView(User viewer, Defect defect) {
        if (viewer == null || defect == null) {
            return false;
        }
        if (Scope.seesEverything(viewer)) {
            return true;
        }
        if (Scope.isLeadWithTeam(viewer)) {
            return Scope.inTeam(defect.getProject(), viewer.getTeam());
        }
        return Scope.sameUser(defect.getAssignedTo(), viewer) || Scope.sameUser(defect.getCreatedBy(), viewer);
    }

    /** Same visibility rule as reporting: QA sees their own, QA Lead their team's, Manager/Admin everything. */
    public List<Defect> listForUser(User viewer) {
        String roleName = viewer.getRole().getName();
        if (roleName.equals(Role.MANAGER) || roleName.equals(Role.ADMIN)) {
            return findAll();
        }
        if (roleName.equals(Role.QA_LEAD) && viewer.getTeam() != null) {
            return em.createQuery("SELECT d FROM Defect d WHERE d.project.team = :team", Defect.class)
                    .setParameter("team", viewer.getTeam())
                    .getResultList();
        }
        return em.createQuery("SELECT d FROM Defect d WHERE d.assignedTo = :user OR d.createdBy = :user", Defect.class)
                .setParameter("user", viewer)
                .getResultList();
    }

    public List<DefectHistory> history(Long defectId) {
        return em.createQuery(
                        "SELECT h FROM DefectHistory h WHERE h.defect.id = :id ORDER BY h.createdAt ASC",
                        DefectHistory.class)
                .setParameter("id", defectId)
                .getResultList();
    }

    @Transactional
    public Defect createWithHistory(Defect defect, User createdBy) {
        defect.setCreatedBy(createdBy);
        defect.setStatus(Defect.Status.open);
        em.persist(defect);
        recordHistory(defect, null, Defect.Status.open, createdBy, null);
        audit.record(AuditService.CREATED, defect);
        return defect;
    }

    @Transactional
    public Defect assign(Defect defect, User assignee, User actor) {
        defect.setAssignedTo(assignee);
        em.merge(defect);
        recordHistory(defect, defect.getStatus(), defect.getStatus(), actor,
                "Assignée à " + assignee.getName() + ".");
        audit.record("assigned", defect, java.util.Map.of("assignee", assignee.getName()));
        notificationService.notify(assignee, "defect_assigned", "Anomalie assignée",
                defect.getTitle() + " vous a été assignée.");
        return defect;
    }

    /** Only a FIXED defect can be retested — anything else is a 422, enforced here rather
     *  than trusted to the client, same guarantee as the Laravel version's controller check. */
    @Transactional
    public Defect retest(Defect defect, boolean passed, User actor, String comment) {
        if (defect.getStatus() != Defect.Status.fixed && defect.getStatus() != Defect.Status.retest) {
            throw new IllegalStateException("Seule une anomalie corrigée (FIXED ou RETEST) peut être revérifiée.");
        }
        Defect.Status previous = defect.getStatus();
        Defect.Status next = passed ? Defect.Status.closed : Defect.Status.reopened;
        defect.setStatus(next);
        em.merge(defect);
        recordHistory(defect, previous, next, actor, comment);
        audit.record(passed ? "retest_passed" : "retest_failed", defect, AuditService.transition(previous, next));
        return defect;
    }

    /** Ouverte / rouverte → en cours : quelqu'un (le développeur, via le testeur) s'en occupe. */
    @Transactional
    public Defect startProgress(Defect defect, User actor, String comment) {
        requireStatus(defect, Defect.Status.open, Defect.Status.reopened);
        return transition(defect, Defect.Status.in_progress, actor, comment);
    }

    /** Corrigée → à revérifier : un testeur a commencé la revérification. */
    @Transactional
    public Defect startRetest(Defect defect, User actor, String comment) {
        requireStatus(defect, Defect.Status.fixed);
        return transition(defect, Defect.Status.retest, actor, comment);
    }

    private Defect transition(Defect defect, Defect.Status next, User actor, String comment) {
        Defect.Status previous = defect.getStatus();
        defect.setStatus(next);
        em.merge(defect);
        recordHistory(defect, previous, next, actor, comment);
        audit.record("status_changed", defect, AuditService.transition(previous, next));
        return defect;
    }

    private static void requireStatus(Defect defect, Defect.Status... allowed) {
        for (Defect.Status s : allowed) {
            if (defect.getStatus() == s) {
                return;
            }
        }
        throw new IllegalStateException("Transition impossible depuis le statut " + defect.getStatus() + ".");
    }

    @Transactional
    public Defect markFixed(Defect defect, User actor, String comment) {
        requireStatus(defect, Defect.Status.open, Defect.Status.in_progress, Defect.Status.reopened);
        Defect.Status previous = defect.getStatus();
        defect.setStatus(Defect.Status.fixed);
        em.merge(defect);
        recordHistory(defect, previous, Defect.Status.fixed, actor, comment);
        audit.record("status_changed", defect, AuditService.transition(previous, Defect.Status.fixed));
        notificationService.notify(defect.getCreatedBy(), "defect_fixed", "Anomalie corrigée",
                defect.getTitle() + " est passée à \"corrigée\".");
        return defect;
    }

    @Transactional
    public Defect close(Defect defect, User actor, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Un commentaire est obligatoire pour fermer une anomalie sans revérification.");
        }
        Defect.Status previous = defect.getStatus();
        defect.setStatus(Defect.Status.closed);
        em.merge(defect);
        recordHistory(defect, previous, Defect.Status.closed, actor, comment);
        audit.record("status_changed", defect, AuditService.transition(previous, Defect.Status.closed));
        return defect;
    }

    @Transactional
    public Defect reopen(Defect defect, User actor, String comment) {
        Defect.Status previous = defect.getStatus();
        defect.setStatus(Defect.Status.reopened);
        em.merge(defect);
        recordHistory(defect, previous, Defect.Status.reopened, actor, comment);
        audit.record("status_changed", defect, AuditService.transition(previous, Defect.Status.reopened));
        return defect;
    }

    private void recordHistory(Defect defect, Defect.Status oldStatus, Defect.Status newStatus,
                                User user, String comment) {
        DefectHistory history = new DefectHistory();
        history.setDefect(defect);
        history.setUser(user);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setComment(comment);
        em.persist(history);
    }
}
