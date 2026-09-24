package com.qareporting.service;

import com.qareporting.entity.Activity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ActivityService.validate() runs entirely on the entity's own fields, no
 * EntityManager needed — so it can be exercised directly without a
 * container, the same server-enforced rule the Laravel version guards with
 * a custom Validator::after hook in ActivityRequest.
 */
class ActivityServiceTest {

    private final ActivityService activityService = new ActivityService();

    private Activity activity(int testsExecuted, int passed, int failed, int blocked, int notRun) {
        Activity activity = new Activity();
        activity.setTestsExecuted(testsExecuted);
        activity.setPassed(passed);
        activity.setFailed(failed);
        activity.setBlocked(blocked);
        activity.setNotRun(notRun);
        return activity;
    }

    @Test
    void validWhenCountersSumToTestsExecuted() {
        Activity activity = activity(10, 7, 2, 0, 1);
        assertNull(activityService.validate(activity));
    }

    @Test
    void rejectsWhenCountersDoNotSumToTestsExecuted() {
        Activity activity = activity(10, 7, 2, 0, 0); // sums to 9, not 10
        String error = activityService.validate(activity);
        assertNotNull(error);
        assertTrue(error.contains("9"));
        assertTrue(error.contains("10"));
    }

    @Test
    void rejectsBlockedTestsWithoutAReason() {
        Activity activity = activity(5, 3, 1, 1, 0);
        activity.setBlockedReason(null);
        assertNotNull(activityService.validate(activity));
    }

    @Test
    void acceptsBlockedTestsWithAReason() {
        Activity activity = activity(5, 3, 1, 1, 0);
        activity.setBlockedReason("Environnement indisponible.");
        assertNull(activityService.validate(activity));
    }

    @Test
    void zeroBlockedNeverRequiresAReason() {
        Activity activity = activity(5, 4, 1, 0, 0);
        assertNull(activityService.validate(activity));
    }
}
