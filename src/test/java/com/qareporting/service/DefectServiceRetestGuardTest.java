package com.qareporting.service;

import com.qareporting.entity.Defect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * retest() must reject any defect that isn't currently FIXED before it ever
 * touches the EntityManager — this test exploits that ordering to check the
 * guard without a running persistence context, the same case the Laravel
 * test suite covers as "retest again on a non-fixed defect correctly 422s".
 */
class DefectServiceRetestGuardTest {

    private final DefectService defectService = new DefectService();

    @Test
    void retestingAnOpenDefectIsRejected() {
        Defect defect = new Defect();
        defect.setStatus(Defect.Status.open);
        assertThrows(IllegalStateException.class,
                () -> defectService.retest(defect, true, null, "comment"));
    }

    @Test
    void retestingAnAlreadyClosedDefectIsRejected() {
        Defect defect = new Defect();
        defect.setStatus(Defect.Status.closed);
        assertThrows(IllegalStateException.class,
                () -> defectService.retest(defect, true, null, "comment"));
    }
}
