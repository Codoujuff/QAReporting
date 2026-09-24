package com.qareporting.resource;

import com.qareporting.security.CurrentUser;
import com.qareporting.entity.Campaign;
import com.qareporting.entity.Role;
import com.qareporting.security.RequiresRole;
import com.qareporting.service.CampaignService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/campaigns")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CampaignResource {

    @Inject
    CampaignService campaignService;

    @Inject
    CurrentUser currentUser;

    @GET
    public Response index() {
        return Response.ok(campaignService.listForUser(currentUser.get())).build();
    }

    @GET
    @Path("/{id}")
    public Response show(@PathParam("id") Long id) {
        Campaign campaign = campaignService.find(id);
        return campaignService.canView(currentUser.get(), campaign)
                ? Response.ok(campaign).build() : Response.status(404).build();
    }

    @POST
    @RequiresRole({Role.ADMIN, Role.QA_LEAD})
    public Response store(Campaign campaign) {
        return Response.status(201).entity(campaignService.create(campaign)).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD})
    public Response update(@PathParam("id") Long id, Campaign incoming) {
        Campaign existing = campaignService.find(id);
        if (existing == null || !campaignService.canView(currentUser.get(), existing)) {
            return Response.status(404).build();
        }

        existing.setName(incoming.getName());
        existing.setVersion(incoming.getVersion());
        existing.setDescription(incoming.getDescription());
        existing.setEnvironment(incoming.getEnvironment());
        existing.setStartDate(incoming.getStartDate());
        existing.setEndDate(incoming.getEndDate());
        existing.setResponsible(incoming.getResponsible());

        if (incoming.getStatus() != null && incoming.getStatus() != existing.getStatus()) {
            return Response.ok(campaignService.changeStatus(existing, incoming.getStatus())).build();
        }
        return Response.ok(campaignService.update(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response destroy(@PathParam("id") Long id) {
        return campaignService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }
}
