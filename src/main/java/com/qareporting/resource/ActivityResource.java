package com.qareporting.resource;

import com.qareporting.security.RequiresRole;
import com.qareporting.entity.Role;
import com.qareporting.entity.Activity;
import com.qareporting.security.CurrentUser;
import com.qareporting.service.ActivityService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/activities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActivityResource {

    @Inject
    ActivityService activityService;

    @Inject
    CurrentUser currentUser;

    @GET
    public Response index() {
        return Response.ok(activityService.listForUser(currentUser.get())).build();
    }

    @GET
    @Path("/{id}")
    public Response show(@PathParam("id") Long id) {
        Activity activity = activityService.find(id);
        return activityService.canView(currentUser.get(), activity)
                ? Response.ok(activity).build() : Response.status(404).build();
    }

    @POST
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response store(Activity activity) {
        activity.setUser(currentUser.get());
        String error = activityService.validate(activity);
        if (error != null) {
            return Response.status(422).entity("{\"message\":\"" + error + "\"}").build();
        }
        return Response.status(201).entity(activityService.createValidated(activity)).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Activity incoming) {
        Activity existing = activityService.find(id);
        if (!activityService.canEdit(currentUser.get(), existing)) {
            return Response.status(404).build();
        }
        existing.setProject(incoming.getProject());
        existing.setCampaign(incoming.getCampaign());
        existing.setEnvironment(incoming.getEnvironment());
        existing.setActivityType(incoming.getActivityType());
        existing.setTestsExecuted(incoming.getTestsExecuted());
        existing.setPassed(incoming.getPassed());
        existing.setFailed(incoming.getFailed());
        existing.setBlocked(incoming.getBlocked());
        existing.setNotRun(incoming.getNotRun());
        existing.setDefectsCount(incoming.getDefectsCount());
        existing.setBlockedReason(incoming.getBlockedReason());
        existing.setComment(incoming.getComment());
        existing.setDuration(incoming.getDuration());
        existing.setActivityDate(incoming.getActivityDate());

        String error = activityService.validate(existing);
        if (error != null) {
            return Response.status(422).entity("{\"message\":\"" + error + "\"}").build();
        }
        existing.setIsBlocked(existing.getBlocked() > 0);
        return Response.ok(activityService.update(existing)).build();
    }

    /** Validation d'une déclaration par le QA Lead de l'équipe (ou l'admin). */
    @POST
    @Path("/{id}/validate")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD})
    public Response validate(@PathParam("id") Long id) {
        Activity activity = activityService.find(id);
        if (!activityService.canView(currentUser.get(), activity)) {
            return Response.status(404).build();
        }
        if (!activityService.canValidate(currentUser.get(), activity)) {
            return Response.status(403).build();
        }
        return Response.ok(activityService.validateByLead(activity, currentUser.get())).build();
    }

    @DELETE
    @Path("/{id}")
    public Response destroy(@PathParam("id") Long id) {
        if (!activityService.canEdit(currentUser.get(), activityService.find(id))) {
            return Response.status(404).build();
        }
        return activityService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }
}
