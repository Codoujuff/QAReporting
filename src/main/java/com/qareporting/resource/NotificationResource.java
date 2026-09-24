package com.qareporting.resource;

import com.qareporting.security.CurrentUser;
import com.qareporting.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @Inject
    NotificationService notificationService;

    @Inject
    CurrentUser currentUser;

    @GET
    public Response index() {
        return Response.ok(notificationService.listFor(currentUser.get())).build();
    }

    @POST
    @Path("/{id}/read")
    public Response markRead(@PathParam("id") Long id) {
        return notificationService.markRead(id, currentUser.get())
                ? Response.noContent().build()
                : Response.status(404).build();
    }

    @POST
    @Path("/read-all")
    public Response markAllRead() {
        notificationService.markAllRead(currentUser.get());
        return Response.noContent().build();
    }
}
