package com.qareporting.resource;

import java.time.LocalDateTime;
import com.qareporting.security.LoginService;
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

    @Inject
    LoginService loginService;

    @jakarta.ws.rs.core.Context
    jakarta.servlet.http.HttpServletRequest httpRequest;

    @POST
    @Path("/login")
    @Public
    @Transactional
    public Response login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return error(Response.Status.fromStatusCode(422), "Email et mot de passe requis.");
        }
        LoginService.Result result = loginService.authenticate(request.getEmail(), request.getPassword(),
                httpRequest.getRemoteAddr());
        switch (result.status()) {
            case LOCKED:
                return Response.status(429).header("Retry-After", result.minutesLocked() * 60)
                        .entity("{\"message\":\"Trop de tentatives. Réessayez dans " + result.minutesLocked() + " min.\"}")
                        .build();
            case DISABLED:
                return error(Response.Status.FORBIDDEN, "Ce compte a été désactivé.");
            case INVALID:
                return error(Response.Status.UNAUTHORIZED, "Identifiants invalides.");
            default:
                break;
        }
        User user = result.user();
        if (user.isMustChangePassword()) {
            return error(Response.Status.FORBIDDEN,
                    "Mot de passe provisoire : changez-le d'abord depuis l'interface web (Paramètres).");
        }

        String plainToken = tokenService.generatePlainToken();
        AccessToken token = new AccessToken();
        token.setUser(user);
        token.setTokenHash(tokenService.hash(plainToken));
        token.setExpiresAt(LocalDateTime.now().plus(TokenService.LIFETIME));
        em.persist(token);

        return Response.ok(new LoginResponse(plainToken, user)).build();
    }

    @POST
    @Path("/logout")
    @Transactional
    public Response logout() {
        // Révoque le jeton utilisé pour cet appel : il ne pourra plus servir.
        if (currentUser.getTokenId() != null) {
            AccessToken token = em.find(AccessToken.class, currentUser.getTokenId());
            if (token != null) {
                em.remove(token);
            }
        }
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
