package com.qareporting.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActivityCounterSumTest {

    @Test
    void validWhenSumMatches() {
        Activity activity = new Activity();
        activity.setTestsExecuted(6);
        activity.setPassed(4);
        activity.setFailed(1);
        activity.setBlocked(1);
        activity.setNotRun(0);
        assertTrue(activity.isCounterSumValid());
    }

    @Test
    void invalidWhenSumDoesNotMatch() {
        Activity activity = new Activity();
        activity.setTestsExecuted(6);
        activity.setPassed(4);
        activity.setFailed(1);
        activity.setBlocked(0);
        activity.setNotRun(0);
        assertFalse(activity.isCounterSumValid());
    }
}
