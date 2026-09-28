package com.qareporting.service;

import com.qareporting.entity.Activity;
import com.qareporting.entity.Campaign;
import com.qareporting.entity.Role;
import com.qareporting.entity.Team;
import com.qareporting.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Règles de travail d'une équipe QA : validation par le lead, campagne figée, rappel, cycle d'une anomalie. */
class WorkflowRulesTest {

    private final ActivityService activities = new ActivityService();
    private final TestService tests = new TestService();
    private final DefectService defects = new DefectService();

    private final Team team = team(1L);
    private final Team otherTeam = team(2L);
    private final User qa = user(10L, Role.QA, team);
    private final User lead = user(20L, Role.QA_LEAD, team);
    private final User otherLead = user(21L, Role.QA_LEAD, otherTeam);
    private final User admin = user(40L, Role.ADMIN, null);

    @Test
    void theLeadValidatesTheTeamsEntriesButNotTheirOwn() {
        Activity fromQa = activity(qa);
        assertTrue(activities.canValidate(lead, fromQa));
        assertFalse(activities.canValidate(otherLead, fromQa));
        assertFalse(activities.canValidate(qa, fromQa));
        assertFalse(activities.canValidate(lead, activity(lead)));
        assertTrue(activities.canValidate(admin, fromQa));
    }

    @Test
    void aTesterLentToAnotherProjectIsValidatedByThatProjectsLead() {
        // Awa (équipe de lead) travaille sur un projet de l'autre équipe : c'est otherLead qui valide.
        Activity lent = activity(qa);
        com.qareporting.entity.Project otherProject = new com.qareporting.entity.Project();
        otherProject.setTeam(otherTeam);
        lent.setProject(otherProject);
        assertTrue(activities.canValidate(otherLead, lent));
        assertFalse(activities.canValidate(lead, lent));
    }

    @Test
    void aValidatedEntryIsFrozenForItsAuthor() {
        Activity entry = activity(qa);
        assertTrue(activities.canEdit(qa, entry));
        entry.setValidatedAt(LocalDateTime.now());
        assertFalse(activities.canEdit(qa, entry));
        assertTrue(activities.canEdit(admin, entry));
        assertFalse(activities.canValidate(lead, entry), "déjà validée");
    }

    @Test
    void testsRunOnlyWhileTheirCampaignIsInProgress() {
        com.qareporting.entity.Test test = new com.qareporting.entity.Test();
        assertNull(tests.executionBlocker(test), "hors campagne : toujours exécutable");
        Campaign campaign = new Campaign();
        test.setCampaign(campaign);
        campaign.setStatus(Campaign.Status.in_progress);
        assertNull(tests.executionBlocker(test));
        campaign.setStatus(Campaign.Status.planned);
        assertEquals("err.campaignNotStarted", tests.executionBlocker(test));
        campaign.setStatus(Campaign.Status.blocked);
        assertEquals("err.campaignBlocked", tests.executionBlocker(test));
        campaign.setStatus(Campaign.Status.completed);
        assertEquals("err.campaignCompleted", tests.executionBlocker(test));
    }

    @Test
    void theReminderIsDueFromItsTimeOnwards() {
        assertFalse(ReminderService.isDue(LocalTime.of(17, 0), LocalTime.of(16, 59)));
        assertTrue(ReminderService.isDue(LocalTime.of(17, 0), LocalTime.of(17, 0)));
        assertTrue(ReminderService.isDue(LocalTime.of(17, 0), LocalTime.of(18, 30)));
        assertFalse(ReminderService.isDue(null, LocalTime.NOON));
    }

    @Test
    void closingWithoutRetestNeedsAReason() {
        com.qareporting.entity.Defect defect = new com.qareporting.entity.Defect();
        defect.setStatus(com.qareporting.entity.Defect.Status.open);
        assertThrows(IllegalArgumentException.class, () -> defects.close(defect, qa, " "));
    }

    @Test
    void aDefectCannotBeMarkedFixedTwice() {
        com.qareporting.entity.Defect defect = new com.qareporting.entity.Defect();
        defect.setStatus(com.qareporting.entity.Defect.Status.fixed);
        assertThrows(IllegalStateException.class, () -> defects.markFixed(defect, qa, null));
        assertThrows(IllegalStateException.class, () -> defects.startProgress(defect, qa, null));
    }

    private static Team team(Long id) {
        Team t = new Team();
        t.setId(id);
        return t;
    }

    private static User user(Long id, String roleName, Team team) {
        Role role = new Role();
        role.setName(roleName);
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setTeam(team);
        return u;
    }

    /** Activité déclarée sur un projet de l'équipe « team » (celle de lead). */
    private Activity activity(User author) {
        Activity a = new Activity();
        a.setUser(author);
        com.qareporting.entity.Project p = new com.qareporting.entity.Project();
        p.setTeam(team);
        a.setProject(p);
        return a;
    }
}
