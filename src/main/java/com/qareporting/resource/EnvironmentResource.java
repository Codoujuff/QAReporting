package com.qareporting.resource;

import com.qareporting.entity.Environment;
import com.qareporting.entity.Role;
import com.qareporting.security.RequiresRole;
import com.qareporting.service.EnvironmentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvironmentResource {

    @Inject
    EnvironmentService environmentService;

    @GET
    public Response index() {
        return Response.ok(environmentService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    public Response show(@PathParam("id") Long id) {
        Environment env = environmentService.find(id);
        return env == null ? Response.status(404).build() : Response.ok(env).build();
    }

    @POST
    @RequiresRole({Role.ADMIN})
    public Response store(Environment environment) {
        return Response.status(201).entity(environmentService.create(environment)).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response update(@PathParam("id") Long id, Environment incoming) {
        Environment existing = environmentService.find(id);
        if (existing == null) {
            return Response.status(404).build();
        }
        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setUrl(incoming.getUrl());
        existing.setStatus(incoming.getStatus());
        return Response.ok(environmentService.update(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response destroy(@PathParam("id") Long id) {
        return environmentService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }
}
