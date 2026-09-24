package com.qareporting.security;

import com.qareporting.entity.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** La matrice des permissions du cahier des charges (§6), ligne par ligne. */
class PermissionsTest {

    @Test
    void onlyLeadAndAdminManageCampaigns() {
        assertTrue(Permissions.allows(Permissions.MANAGE_CAMPAIGNS, Role.QA_LEAD));
        assertTrue(Permissions.allows(Permissions.MANAGE_CAMPAIGNS, Role.ADMIN));
        assertFalse(Permissions.allows(Permissions.MANAGE_CAMPAIGNS, Role.QA));
        assertFalse(Permissions.allows(Permissions.MANAGE_CAMPAIGNS, Role.MANAGER));
    }

    @Test
    void managerNeitherRunsTestsNorLogsActivity() {
        assertFalse(Permissions.allows(Permissions.EXECUTE_TESTS, Role.MANAGER));
        assertFalse(Permissions.allows(Permissions.LOG_ACTIVITY, Role.MANAGER));
        assertTrue(Permissions.allows(Permissions.EXECUTE_TESTS, Role.QA));
        assertTrue(Permissions.allows(Permissions.LOG_ACTIVITY, Role.QA));
    }

    @Test
    void onlyLeadAndAdminAssignDefects() {
        assertTrue(Permissions.allows(Permissions.ASSIGN_DEFECTS, Role.QA_LEAD));
        assertTrue(Permissions.allows(Permissions.ASSIGN_DEFECTS, Role.ADMIN));
        assertFalse(Permissions.allows(Permissions.ASSIGN_DEFECTS, Role.QA));
        assertFalse(Permissions.allows(Permissions.ASSIGN_DEFECTS, Role.MANAGER));
    }

    @Test
    void qaWritesTestsButOnlyTheLeadDistributesThem() {
        assertTrue(Permissions.allows(Permissions.MANAGE_TESTS, Role.QA));
        assertFalse(Permissions.allows(Permissions.ASSIGN_TESTS, Role.QA));
        assertTrue(Permissions.allows(Permissions.ASSIGN_TESTS, Role.QA_LEAD));
        assertFalse(Permissions.allows(Permissions.DELETE, Role.QA));
    }

    @Test
    void unknownOrMissingRoleIsNeverAllowed() {
        assertFalse(Permissions.allows(Permissions.EXECUTE_TESTS, (String) null));
        assertFalse(Permissions.allows(Permissions.EXECUTE_TESTS, "guest"));
    }
}
