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

        test.setStatus(result);
        em.merge(test);

        return execution;
    }
}
