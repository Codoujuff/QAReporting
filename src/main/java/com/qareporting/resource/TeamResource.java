package com.qareporting.resource;

import com.qareporting.entity.Role;
import com.qareporting.entity.Team;
import com.qareporting.security.RequiresRole;
import com.qareporting.service.TeamService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/teams")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TeamResource {

    @Inject
    TeamService teamService;

    @GET
    public Response index() {
        return Response.ok(teamService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    public Response show(@PathParam("id") Long id) {
        Team team = teamService.find(id);
        return team == null ? Response.status(404).build() : Response.ok(team).build();
    }

    @POST
    @RequiresRole({Role.ADMIN})
    public Response store(Team team) {
        return Response.status(201).entity(teamService.create(team)).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response update(@PathParam("id") Long id, Team incoming) {
        Team existing = teamService.find(id);
        if (existing == null) {
            return Response.status(404).build();
        }
        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setLead(incoming.getLead());
        return Response.ok(teamService.update(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response destroy(@PathParam("id") Long id) {
        return teamService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }
}
