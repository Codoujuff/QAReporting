package com.qareporting.resource;

import com.qareporting.dto.UserRequest;
import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import com.qareporting.security.CurrentUser;
import com.qareporting.security.PasswordHasher;
import com.qareporting.security.RequiresRole;
import com.qareporting.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    CurrentUser currentUser;

    @GET
    @RequiresRole({Role.ADMIN, Role.QA_LEAD})
    public Response index() {
        return Response.ok(userService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    public Response show(@PathParam("id") Long id) {
        User user = userService.find(id);
        return user == null ? Response.status(404).build() : Response.ok(user).build();
    }

    @POST
    @RequiresRole({Role.ADMIN})
    public Response store(UserRequest request) {
        if (userService.findByEmail(request.getEmail()) != null) {
            return Response.status(409).entity("{\"message\":\"Cet email est déjà utilisé.\"}").build();
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return Response.status(422).entity("{\"message\":\"Le mot de passe est requis.\"}").build();
        }

        User user = new User();
        user.setName(request.getName());
        user.setInitials(request.getInitials());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordHasher.hash(request.getPassword()));
        user.setRole(userService.findRole(request.getRoleId()));
        user.setTeam(request.getTeamId() != null ? userService.findTeam(request.getTeamId()) : null);
        user.setActive(request.getActive() == null || request.getActive());

        return Response.status(201).entity(userService.create(user)).build();
    }

    /**
     * Admins can update any field on any account. A user editing their own
     * account can only ever change name/initials/password — role and team
     * are silently ignored, never trusted from the request body, matching
     * UserPolicy::update in the Laravel version.
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, UserRequest request) {
        User existing = userService.find(id);
        if (existing == null) {
            return Response.status(404).build();
        }

        boolean isAdmin = currentUser.hasAnyRole(Role.ADMIN);
        boolean isSelf = currentUser.get() != null && currentUser.get().getId().equals(id);

        if (!isAdmin && !isSelf) {
            return Response.status(403).entity("{\"message\":\"Action non autorisée.\"}").build();
        }

        existing.setName(request.getName());
        existing.setInitials(request.getInitials());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPasswordHash(passwordHasher.hash(request.getPassword()));
        }

        if (isAdmin) {
            if (request.getRoleId() != null) {
                existing.setRole(userService.findRole(request.getRoleId()));
            }
            existing.setTeam(request.getTeamId() != null ? userService.findTeam(request.getTeamId()) : null);
            if (request.getActive() != null) {
                existing.setActive(request.getActive());
            }
        }

        return Response.ok(userService.update(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @RequiresRole({Role.ADMIN})
    public Response destroy(@PathParam("id") Long id) {
        return userService.delete(id) ? Response.noContent().build() : Response.status(404).build();
    }
}
