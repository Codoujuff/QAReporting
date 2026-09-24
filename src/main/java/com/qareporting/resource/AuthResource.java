package com.qareporting.resource;

import com.qareporting.dto.LoginRequest;
import com.qareporting.dto.LoginResponse;
import com.qareporting.entity.AccessToken;
import com.qareporting.entity.User;
import com.qareporting.security.CurrentUser;
import com.qareporting.security.PasswordHasher;
import com.qareporting.security.Public;
import com.qareporting.security.TokenService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Mirrors AuthController from the Laravel version: /login, /logout, /me.
 * /forgot-password and /reset-password are intentionally not ported yet —
 * they depend on a real mail transport, out of scope for this phase.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    TokenService tokenService;

    @Inject
    CurrentUser currentUser;

    @POST
    @Path("/login")
    @Public
    @Transactional
    public Response login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return Response.status(422)
                    .entity("{\"message\":\"Email et mot de passe requis.\"}")
                    .build();
        }

        User user;
        try {
            user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", request.getEmail())
                    .getSingleResult();
        } catch (NoResultException e) {
            return error(Response.Status.UNAUTHORIZED, "Identifiants invalides.");
        }

        if (!user.isActive()) {
            return error(Response.Status.FORBIDDEN, "Ce compte a été désactivé.");
        }

        if (!passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            return error(Response.Status.UNAUTHORIZED, "Identifiants invalides.");
        }

        String plainToken = tokenService.generatePlainToken();
        AccessToken token = new AccessToken();
        token.setUser(user);
        token.setTokenHash(tokenService.hash(plainToken));
        em.persist(token);

        return Response.ok(new LoginResponse(plainToken, user)).build();
    }

    @POST
    @Path("/logout")
    @Transactional
    public Response logout() {
        // The current token was already resolved by AuthenticationFilter; a full
        // implementation revokes it here. Left as a follow-up once token lookup
        // is threaded through the filter into the request context.
        return Response.noContent().build();
    }

    @GET
    @Path("/me")
    public Response me() {
        if (!currentUser.isAuthenticated()) {
            return error(Response.Status.UNAUTHORIZED, "Non authentifié.");
        }
        return Response.ok(currentUser.get()).build();
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity("{\"message\":\"" + message + "\"}")
                .build();
    }
}
