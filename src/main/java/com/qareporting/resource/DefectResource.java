package com.qareporting.resource;

import com.qareporting.dto.AssignRequest;
import com.qareporting.dto.CommentRequest;
import com.qareporting.dto.RetestRequest;
import com.qareporting.entity.Defect;
import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import com.qareporting.security.CurrentUser;
import com.qareporting.security.RequiresRole;
import com.qareporting.service.DefectService;
import com.qareporting.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/defects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DefectResource {

    @Inject
    DefectService defectService;

    @Inject
    UserService userService;

    @Inject
    CurrentUser currentUser;

    @GET
    public Response index() {
        return Response.ok(defectService.listForUser(currentUser.get())).build();
    }

    @GET
    @Path("/{id}")
    public Response show(@PathParam("id") Long id) {
        Defect defect = defectService.find(id);
        return defectService.canView(currentUser.get(), defect)
                ? Response.ok(defect).build() : Response.status(404).build();
    }

    @GET
    @Path("/{id}/history")
    public Response history(@PathParam("id") Long id) {
        if (!defectService.canView(currentUser.get(), defectService.find(id))) {
            return Response.status(404).build();
        }
        return Response.ok(defectService.history(id)).build();
    }

    @POST
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response store(Defect defect) {
        return Response.status(201).entity(defectService.createWithHistory(defect, currentUser.get())).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Defect incoming) {
        Defect existing = defectService.find(id);
        if (existing == null || !defectService.canView(currentUser.get(), existing)) {
            return Response.status(404).build();
        }
        existing.setTitle(incoming.getTitle());
        existing.setDescription(incoming.getDescription());
        existing.setSeverity(incoming.getSeverity());
        existing.setPriority(incoming.getPriority());
        existing.setExpectedResult(incoming.getExpectedResult());
        existing.setActualResult(incoming.getActualResult());
        existing.setReproductionSteps(incoming.getReproductionSteps());
        existing.setBrowser(incoming.getBrowser());
        existing.setOs(incoming.getOs());
        existing.setVersion(incoming.getVersion());
        existing.setDevice(incoming.getDevice());
        return Response.ok(defectService.update(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response destroy(@PathParam("id") Long id) {
        return defectService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }

    @POST
    @Path("/{id}/assign")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD})
    public Response assign(@PathParam("id") Long id, AssignRequest request) {
        Defect defect = defectService.find(id);
        User assignee = request == null ? null : userService.find(request.getUserId());
        if (defect == null || !defectService.canView(currentUser.get(), defect) || assignee == null) {
            return Response.status(404).build();
        }
        if (!defectService.canBeAssignedTo(defect, assignee)) {
            return Response.status(422).entity("{\"message\":\"L'assignation est réservée aux testeurs de l'équipe du projet.\"}").build();
        }
        return Response.ok(defectService.assign(defect, assignee, currentUser.get())).build();
    }

    @POST
    @Path("/{id}/retest")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response retest(@PathParam("id") Long id, RetestRequest request) {
        Defect defect = defectService.find(id);
        if (defect == null || !defectService.canView(currentUser.get(), defect)) {
            return Response.status(404).build();
        }
        try {
            Defect updated = defectService.retest(defect, request.isPassed(), currentUser.get(), request.getComment());
            return Response.ok(updated).build();
        } catch (IllegalStateException e) {
            return Response.status(422).entity("{\"message\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/{id}/close")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response close(@PathParam("id") Long id, CommentRequest request) {
        Defect defect = defectService.find(id);
        if (defect == null || !defectService.canView(currentUser.get(), defect)) {
            return Response.status(404).build();
        }
        String comment = request == null ? null : request.getComment();
        try {
            return Response.ok(defectService.close(defect, currentUser.get(), comment)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity("{\"message\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/{id}/start")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response start(@PathParam("id") Long id, CommentRequest request) {
        return transition(id, d -> defectService.startProgress(d, currentUser.get(), request == null ? null : request.getComment()));
    }

    @POST
    @Path("/{id}/start-retest")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response startRetest(@PathParam("id") Long id, CommentRequest request) {
        return transition(id, d -> defectService.startRetest(d, currentUser.get(), request == null ? null : request.getComment()));
    }

    private Response transition(Long id, java.util.function.Function<Defect, Defect> action) {
        Defect defect = defectService.find(id);
        if (defect == null || !defectService.canView(currentUser.get(), defect)) {
            return Response.status(404).build();
        }
        try {
            return Response.ok(action.apply(defect)).build();
        } catch (IllegalStateException e) {
            return Response.status(422).entity("{\"message\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/{id}/reopen")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response reopen(@PathParam("id") Long id, CommentRequest request) {
        Defect defect = defectService.find(id);
        if (defect == null || !defectService.canView(currentUser.get(), defect)) {
            return Response.status(404).build();
        }
        String comment = request == null ? null : request.getComment();
        return Response.ok(defectService.reopen(defect, currentUser.get(), comment)).build();
    }
}
