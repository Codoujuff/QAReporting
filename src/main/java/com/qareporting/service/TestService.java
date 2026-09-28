package com.qareporting.service;

import com.qareporting.entity.Environment;
import com.qareporting.entity.Test;
import com.qareporting.entity.TestExecution;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class TestService extends AbstractCrudService<Test, Long> {

    @Override
    protected Class<Test> entityClass() {
        return Test.class;
    }

    /** QA : ses tests (assignés) ; QA Lead : ceux de son équipe ; Manager/Admin : tous. */
    /** QA : ses tests (assignés) ; QA Lead : ceux de ses projets ; Manager / Admin : tous. */
    public List<Test> listForUser(User viewer) {
        if (Scope.seesEverything(viewer)) {
            return findAll();
        }
        if (Scope.isLead(viewer)) {
            var q = em.createQuery("SELECT t FROM Test t WHERE " + Scope.projectClause("t.project", viewer), Test.class);
            Scope.bindProjectScope(q, viewer);
            return q.getResultList();
        }
        return em.createQuery("SELECT t FROM Test t WHERE t.assignedTo = :user", Test.class)
                .setParameter("user", viewer)
                .getResultList();
    }

    public boolean canView(User viewer, Test test) {
        if (viewer == null || test == null) {
            return false;
        }
        if (Scope.seesEverything(viewer)) {
            return true;
        }
        if (Scope.isLead(viewer)) {
            return Scope.onProject(test.getProject(), viewer);
        }
        return Scope.sameUser(test.getAssignedTo(), viewer);
    }

    /**
     * Pourquoi un test ne peut pas être exécuté maintenant, ou null s'il le peut. Un test
     * rattaché à une campagne ne s'exécute que pendant la campagne (statut « en cours ») :
     * une campagne terminée est figée, ses résultats ne doivent plus bouger.
     * Renvoie une clé de traduction, utilisée par l'interface comme par l'API.
     */
    public String executionBlocker(Test test) {
        if (test == null || test.getCampaign() == null) {
            return null;
        }
        return switch (test.getCampaign().getStatus()) {
            case in_progress -> null;
            case planned -> "err.campaignNotStarted";
            case blocked -> "err.campaignBlocked";
            case completed -> "err.campaignCompleted";
        };
    }

    public TestExecution findExecution(Long executionId) {
        return executionId == null ? null : em.find(TestExecution.class, executionId);
    }

    public SearchQuery search(User viewer, String q, String status) {
        SearchQuery s = new SearchQuery("FROM Test t JOIN t.project p LEFT JOIN t.campaign c LEFT JOIN t.assignedTo a");
        if (!Scope.seesEverything(viewer)) {
            s.scope(Scope.isLead(viewer) ? Scope.projectClause("t.project", viewer) : "t.assignedTo = :scopeUser", viewer);
        }
        if (status != null && !status.isBlank()) {
            s.and("t.status = :status").param("status", Test.Status.valueOf(status));
        }
        return s.text(q, "t.id", "t", "t.title", "p.name", "c.name", "a.name");
    }

    public long count(SearchQuery s) {
        return s.count(em, "t");
    }

    public List<Test> page(SearchQuery s, int first, int size) {
        return s.page(em, "t", Test.class, "t.id DESC", first, size);
    }

    public List<TestExecution> executions(Long testId) {
        return em.createQuery(
                        "SELECT e FROM TestExecution e WHERE e.test.id = :testId ORDER BY e.executedAt DESC",
                        TestExecution.class)
                .setParameter("testId", testId)
                .getResultList();
    }

    /** Dernière exécution d'un test, ou null s'il n'a jamais été exécuté. */
    public TestExecution lastExecution(Long testId) {
        List<TestExecution> results = em.createQuery(
                        "SELECT e FROM TestExecution e WHERE e.test.id = :testId ORDER BY e.executedAt DESC",
                        TestExecution.class)
                .setParameter("testId", testId)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /** Tests d'une campagne — utilisé pour calculer sa progression (part de tests déjà exécutés). */
    /**
     * Progression (%) de chaque campagne — part des tests déjà exécutés (statut autre que
     * not_run) — calculée en UNE requête groupée, au lieu d'une requête par campagne.
     */
    public java.util.Map<Long, Integer> progressByCampaign(java.util.Collection<Long> campaignIds) {
        java.util.Map<Long, Integer> progress = new java.util.HashMap<>();
        if (campaignIds == null || campaignIds.isEmpty()) {
            return progress;
        }
        List<Object[]> rows = em.createQuery("SELECT t.campaign.id, COUNT(t), "
                        + "SUM(CASE WHEN t.status <> :notRun THEN 1 ELSE 0 END) "
                        + "FROM Test t WHERE t.campaign.id IN :ids GROUP BY t.campaign.id", Object[].class)
                .setParameter("notRun", Test.Status.not_run)
                .setParameter("ids", campaignIds)
                .getResultList();
        for (Object[] row : rows) {
            long total = ((Number) row[1]).longValue();
            long executed = row[2] == null ? 0 : ((Number) row[2]).longValue();
            progress.put((Long) row[0], total == 0 ? 0 : (int) Math.round(100.0 * executed / total));
        }
        return progress;
    }

    public List<Test> findByCampaign(Long campaignId) {
        return em.createQuery("SELECT t FROM Test t WHERE t.campaign.id = :campaignId", Test.class)
                .setParameter("campaignId", campaignId)
                .getResultList();
    }

    /**
     * Records one execution as a new, immutable history row (never
     * overwrites a previous result — same rule as the Laravel version) and
     * updates the test's current/last-known status. Returns the created
     * TestExecution so the caller can tell the client whether the result
     * was a failure (the "should_create_defect" handoff to the frontend).
     */
    @Transactional
    public TestExecution execute(Test test, Test.Status result, String actualResult,
                                  User executedBy, Environment environment, String duration) {
        TestExecution execution = new TestExecution();
        execution.setTest(test);
        execution.setStatus(result);
        execution.setActualResult(actualResult);
        execution.setExecutedBy(executedBy);
        execution.setEnvironment(environment);
        execution.setDuration(duration);
        em.persist(execution);

        Test.Status previous = test.getStatus();
        test.setStatus(result);
        Test merged = em.merge(test);
        audit.record("executed", merged, AuditService.transition(previous, result));

        return execution;
    }
}
