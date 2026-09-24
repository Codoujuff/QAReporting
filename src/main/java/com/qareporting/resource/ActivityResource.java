package com.qareporting.resource;

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
        return activity == null ? Response.status(404).build() : Response.ok(activity).build();
    }

    @POST
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
        if (existing == null) {
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

    @DELETE
    @Path("/{id}")
    public Response destroy(@PathParam("id") Long id) {
        return activityService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }
}
