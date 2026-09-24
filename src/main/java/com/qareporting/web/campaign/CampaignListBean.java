package com.qareporting.web.campaign;

import com.qareporting.entity.Campaign;
import com.qareporting.entity.Test;
import com.qareporting.service.CampaignService;
import com.qareporting.service.TestService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * Liste des campagnes de test visibles par l'utilisateur (QA/QA Lead voient les
 * campagnes de leur équipe, Manager/Admin tout — CampaignService.listForUser existant),
 * avec les transitions de statut qui déclenchent déjà une notification à toute
 * l'équipe du projet (CampaignService.changeStatus).
 */
@Named
@ViewScoped
public class CampaignListBean implements Serializable {

    @Inject
    private CampaignService campaignService;

    @Inject
    private TestService testService;

    @Inject
    private SessionAuth sessionAuth;

    private List<Campaign> campaigns;

    @PostConstruct
    public void init() {
        load();
    }

    private void load() {
        campaigns = campaignService.listForUser(sessionAuth.getCurrentUser());
    }

    public boolean isStartable(Campaign campaign) {
        return campaign.getStatus() == Campaign.Status.planned;
    }

    public boolean isCompletable(Campaign campaign) {
        return campaign.getStatus() == Campaign.Status.in_progress;
    }

    public boolean isBlockable(Campaign campaign) {
        return campaign.getStatus() == Campaign.Status.in_progress;
    }

    public boolean isResumable(Campaign campaign) {
        return campaign.getStatus() == Campaign.Status.blocked;
    }

    /** Part des tests de la campagne déjà exécutés (statut différent de "not_run"), en %. */
    public int getProgress(Campaign campaign) {
        List<Test> tests = testService.findByCampaign(campaign.getId());
        if (tests.isEmpty()) {
            return 0;
        }
        long executed = tests.stream().filter(t -> t.getStatus() != Test.Status.not_run).count();
        return (int) Math.round(100.0 * executed / tests.size());
    }

    public String start(Campaign campaign) {
        campaignService.changeStatus(campaign, Campaign.Status.in_progress);
        return refresh();
    }

    public String complete(Campaign campaign) {
        campaignService.changeStatus(campaign, Campaign.Status.completed);
        return refresh();
    }

    public String block(Campaign campaign) {
        campaignService.changeStatus(campaign, Campaign.Status.blocked);
        return refresh();
    }

    public String resume(Campaign campaign) {
        campaignService.changeStatus(campaign, Campaign.Status.in_progress);
        return refresh();
    }

    private String refresh() {
        return "list.xhtml?faces-redirect=true";
    }

    public List<Campaign> getCampaigns() {
        return campaigns;
    }
}
