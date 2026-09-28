package com.qareporting.resource;

import com.qareporting.entity.Project;
import com.qareporting.entity.Role;
import com.qareporting.security.RequiresRole;
import com.qareporting.service.ProjectService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @Inject
    com.qareporting.service.UserService userService;

    @Inject
    ProjectService projectService;

    @GET
    public Response index() {
        return Response.ok(projectService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    public Response show(@PathParam("id") Long id) {
        Project project = projectService.find(id);
        return project == null ? Response.status(404).build() : Response.ok(project).build();
    }

    @POST
    @RequiresRole({Role.ADMIN})
    public Response store(Project project) {
        applyMembers(project, project.getRequestedMemberIds());
        return Response.status(201).entity(projectService.create(project)).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response update(@PathParam("id") Long id, Project incoming) {
        Project existing = projectService.find(id);
        if (existing == null) {
            return Response.status(404).build();
        }
        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setStatus(incoming.getStatus());
        existing.setTeam(incoming.getTeam());
        if (incoming.getRequestedMemberIds() != null) {
            applyMembers(existing, incoming.getRequestedMemberIds());
        }
        return Response.ok(projectService.update(existing)).build();
    }

    /** « memberIds » : testeurs affectés au projet (un QA peut être sur plusieurs projets). */
    private void applyMembers(Project project, java.util.List<Long> ids) {
        java.util.Set<com.qareporting.entity.User> members = new java.util.HashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                com.qareporting.entity.User u = userService.find(id);
                if (u != null) {
                    members.add(u);
                }
            }
        }
        project.setMembers(members);
    }

    @DELETE
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response destroy(@PathParam("id") Long id) {
        return projectService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }
}
