package com.qareporting.resource;

import com.qareporting.security.Permissions;
import com.qareporting.dto.ExecuteTestRequest;
import com.qareporting.entity.Environment;
import com.qareporting.entity.Role;
import com.qareporting.entity.Test;
import com.qareporting.entity.TestExecution;
import com.qareporting.security.CurrentUser;
import com.qareporting.security.RequiresRole;
import com.qareporting.service.EnvironmentService;
import com.qareporting.service.TestService;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/tests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResource {

    @Inject
    TestService testService;

    @Inject
    EnvironmentService environmentService;

    @Inject
    CurrentUser currentUser;

    @GET
    public Response index() {
        return Response.ok(testService.listForUser(currentUser.get())).build();
    }

    @GET
    @Path("/{id}")
    public Response show(@PathParam("id") Long id) {
        Test test = testService.find(id);
        return testService.canView(currentUser.get(), test)
                ? Response.ok(test).build() : Response.status(404).build();
    }

    @POST
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response store(Test test) {
        if (!Permissions.allows(Permissions.ASSIGN_TESTS, currentUser.get())) {
            test.setAssignedTo(currentUser.get()); // un QA rédige ses propres cas
        }
        return Response.status(201).entity(testService.create(test)).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response update(@PathParam("id") Long id, Test incoming) {
        Test existing = testService.find(id);
        if (existing == null || !testService.canView(currentUser.get(), existing)) {
            return Response.status(404).build();
        }
        existing.setTitle(incoming.getTitle());
        existing.setDescription(incoming.getDescription());
        existing.setPreconditions(incoming.getPreconditions());
        existing.setSteps(incoming.getSteps());
        existing.setExpectedResult(incoming.getExpectedResult());
        existing.setType(incoming.getType());
        existing.setCampaign(incoming.getCampaign());
        existing.setEnvironment(incoming.getEnvironment());
        if (Permissions.allows(Permissions.ASSIGN_TESTS, currentUser.get())) {
            existing.setAssignedTo(incoming.getAssignedTo());
        }
        return Response.ok(testService.update(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response destroy(@PathParam("id") Long id) {
        return testService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }

    /**
     * Records a result and, like the Laravel version's TestController::execute,
     * tells the caller whether this result should prompt the client to open
     * the "create a defect from this failure" flow.
     */
    @POST
    @Path("/{id}/execute")
    @RequiresRole({Role.ADMIN, Role.QA_LEAD, Role.QA})
    public Response execute(@PathParam("id") Long id, ExecuteTestRequest request) {
        Test test = testService.find(id);
        if (test == null || !testService.canView(currentUser.get(), test)) {
            return Response.status(404).build();
        }
        if (request == null || request.getStatus() == null) {
            return Response.status(422).entity("{\"message\":\"Le résultat est requis.\"}").build();
        }

        Environment environment = request.getEnvironmentId() != null
                ? environmentService.find(request.getEnvironmentId())
                : test.getEnvironment();

        TestExecution execution = testService.execute(
                test, request.getStatus(), request.getActualResult(),
                currentUser.get(), environment, request.getDuration());

        boolean shouldCreateDefect = request.getStatus() == Test.Status.failed;

        return Response.ok(Json.createObjectBuilder()
                        .add("execution_id", execution.getId())
                        .add("status", execution.getStatus().name())
                        .add("should_create_defect", shouldCreateDefect)
                        .build().toString())
                .build();
    }

    @GET
    @Path("/{id}/executions")
    public Response executions(@PathParam("id") Long id) {
        if (!testService.canView(currentUser.get(), testService.find(id))) {
            return Response.status(404).build();
        }
        return Response.ok(testService.executions(id)).build();
    }
}
