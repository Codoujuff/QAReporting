package com.qareporting.service;

import com.qareporting.entity.Campaign;
import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CampaignService extends AbstractCrudService<Campaign, Long> {

    @Inject
    NotificationService notificationService;

    @Override
    protected Class<Campaign> entityClass() {
        return Campaign.class;
    }

    /** Same visibility rule as reporting: QA/QA Lead see their team's campaigns, Manager/Admin everything. */
    public List<Campaign> listForUser(User viewer) {
        String roleName = viewer.getRole().getName();
        if (roleName.equals(Role.MANAGER) || roleName.equals(Role.ADMIN) || viewer.getTeam() == null) {
            return findAll();
        }
        return em.createQuery("SELECT c FROM Campaign c WHERE c.project.team = :team", Campaign.class)
                .setParameter("team", viewer.getTeam())
                .getResultList();
    }

    /**
     * Same trigger as the Laravel version: a status change on a campaign
     * auto-notifies every active member of the owning project's team.
     */
    @Transactional
    public Campaign changeStatus(Campaign campaign, Campaign.Status newStatus) {
        Campaign.Status previous = campaign.getStatus();
        campaign.setStatus(newStatus);
        Campaign saved = update(campaign);

        if (previous != newStatus && saved.getProject() != null && saved.getProject().getTeam() != null) {
            String title = switch (newStatus) {
                case in_progress -> "Campagne démarrée";
                case completed -> "Campagne terminée";
                case blocked -> "Blocage";
                case planned -> "Campagne planifiée";
            };
            String message = saved.getName() + " est passée à \"" + newStatus + "\".";
            notificationService.notifyTeam(saved.getProject().getTeam(),
                    "campaign_" + newStatus, title, message);
        }

        return saved;
    }
}
