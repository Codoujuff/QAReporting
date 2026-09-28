package com.qareporting.service;

import com.qareporting.entity.Activity;
import com.qareporting.entity.Campaign;
import com.qareporting.entity.Defect;
import com.qareporting.entity.Project;
import com.qareporting.entity.Role;
import com.qareporting.entity.Team;
import com.qareporting.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Portée des données par rôle, pour l'ouverture d'un objet par son id : un QA ne voit
 * que ses anomalies / tests, un QA Lead ceux de son équipe, Manager et Admin tout.
 * Les méthodes canView(...) ne touchent pas à l'EntityManager, d'où le test sans conteneur.
 */
class ScopeTest {

    private final DefectService defects = new DefectService();
    private final TestService tests = new TestService();
    private final CampaignService campaigns = new CampaignService();
    private final ActivityService activities = new ActivityService();

    private final Team teamA = team(1L);
    private final Team teamB = team(2L);
    private final User qa = user(10L, Role.QA, teamA);
    private final User otherQa = user(11L, Role.QA, teamA);
    private final User lead = user(20L, Role.QA_LEAD, teamA);
    private final User manager = user(30L, Role.MANAGER, null);
    private final User admin = user(40L, Role.ADMIN, null);

    @Test
    void qaSeesOnlyDefectsTheyCreatedOrAreAssigned() {
        assertTrue(defects.canView(qa, defect(teamA, qa, otherQa)));
        assertTrue(defects.canView(qa, defect(teamA, otherQa, qa)));
        assertFalse(defects.canView(qa, defect(teamA, otherQa, otherQa)));
    }

    @Test
    void leadSeesTheirTeamDefectsOnly() {
        assertTrue(defects.canView(lead, defect(teamA, otherQa, null)));
        assertFalse(defects.canView(lead, defect(teamB, otherQa, null)));
    }

    @Test
    void managerAndAdminSeeEverything() {
        Defect elsewhere = defect(teamB, otherQa, null);
        assertTrue(defects.canView(manager, elsewhere));
        assertTrue(defects.canView(admin, elsewhere));
    }

    @Test
    void qaSeesOnlyTestsAssignedToThem() {
        com.qareporting.entity.Test mine = test(teamA, qa);
        com.qareporting.entity.Test notMine = test(teamA, otherQa);
        assertTrue(tests.canView(qa, mine));
        assertFalse(tests.canView(qa, notMine));
        assertTrue(tests.canView(lead, notMine));
    }

    @Test
    void campaignsAreScopedToTheTeam() {
        Campaign other = new Campaign();
        other.setProject(project(teamB));
        assertFalse(campaigns.canView(qa, other));
        assertTrue(campaigns.canView(manager, other));
    }

    @Test
    void onlyTheAuthorOrAdminEditsAnActivity() {
        Activity activity = new Activity();
        activity.setUser(qa);
        activity.setProject(project(teamA));
        assertTrue(activities.canEdit(qa, activity));
        assertTrue(activities.canEdit(admin, activity));
        assertFalse(activities.canEdit(lead, activity));
        assertTrue(activities.canView(lead, activity));
    }

    @Test
    void aQaAssignedToAnotherTeamsProjectSeesItsCampaigns() {
        // Un QA peut être affecté à plusieurs projets, y compris ceux d'une autre équipe.
        Project other = project(teamB);
        Campaign campaign = new Campaign();
        campaign.setProject(other);
        assertFalse(campaigns.canView(qa, campaign));
        other.getMembers().add(qa);
        assertTrue(campaigns.canView(qa, campaign));
    }

    @Test
    void aLeadMemberOfAnotherProjectSeesItsDefects() {
        Project other = project(teamB);
        Defect d = defect(teamB, otherQa, null);
        d.setProject(other);
        assertFalse(defects.canView(lead, d));
        other.getMembers().add(lead);
        assertTrue(defects.canView(lead, d));
    }

    @Test
    void nothingIsVisibleWithoutAUserOrAnObject() {
        assertFalse(defects.canView(null, defect(teamA, qa, qa)));
        assertFalse(defects.canView(admin, null));
    }

    private static Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }

    private static User user(Long id, String roleName, Team team) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setTeam(team);
        return user;
    }

    private static Project project(Team team) {
        Project project = new Project();
        project.setTeam(team);
        return project;
    }

    private static Defect defect(Team team, User createdBy, User assignedTo) {
        Defect defect = new Defect();
        defect.setProject(project(team));
        defect.setCreatedBy(createdBy);
        defect.setAssignedTo(assignedTo);
        return defect;
    }

    private static com.qareporting.entity.Test test(Team team, User assignedTo) {
        com.qareporting.entity.Test test = new com.qareporting.entity.Test();
        test.setProject(project(team));
        test.setAssignedTo(assignedTo);
        return test;
    }
}
