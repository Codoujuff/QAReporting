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
        return defect;
    }

    @Transactional
    public Defect assign(Defect defect, User assignee, User actor) {
        defect.setAssignedTo(assignee);
        em.merge(defect);
        recordHistory(defect, defect.getStatus(), defect.getStatus(), actor,
                "Assignée à " + assignee.getName() + ".");
        notificationService.notify(assignee, "defect_assigned", "Anomalie assignée",
                defect.getTitle() + " vous a été assignée.");
        return defect;
    }

    /** Only a FIXED defect can be retested — anything else is a 422, enforced here rather
     *  than trusted to the client, same guarantee as the Laravel version's controller check. */
    @Transactional
    public Defect retest(Defect defect, boolean passed, User actor, String comment) {
        if (defect.getStatus() != Defect.Status.fixed) {
            throw new IllegalStateException("Seule une anomalie au statut FIXED peut être retestée.");
        }
        Defect.Status previous = defect.getStatus();
        Defect.Status next = passed ? Defect.Status.closed : Defect.Status.reopened;
        defect.setStatus(next);
        em.merge(defect);
        recordHistory(defect, previous, next, actor, comment);
        return defect;
    }

    @Transactional
    public Defect markFixed(Defect defect, User actor, String comment) {
        Defect.Status previous = defect.getStatus();
        defect.setStatus(Defect.Status.fixed);
        em.merge(defect);
        recordHistory(defect, previous, Defect.Status.fixed, actor, comment);
        notificationService.notify(defect.getCreatedBy(), "defect_fixed", "Anomalie corrigée",
                defect.getTitle() + " est passée à \"corrigée\".");
        return defect;
    }

    @Transactional
    public Defect close(Defect defect, User actor, String comment) {
        Defect.Status previous = defect.getStatus();
        defect.setStatus(Defect.Status.closed);
        em.merge(defect);
        recordHistory(defect, previous, Defect.Status.closed, actor, comment);
        return defect;
    }

    @Transactional
    public Defect reopen(Defect defect, User actor, String comment) {
        Defect.Status previous = defect.getStatus();
        defect.setStatus(Defect.Status.reopened);
        em.merge(defect);
        recordHistory(defect, previous, Defect.Status.reopened, actor, comment);
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
