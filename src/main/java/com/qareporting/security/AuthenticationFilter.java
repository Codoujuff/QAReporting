package com.qareporting.security;

import com.qareporting.entity.AccessToken;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

/**
 * Resolves the Bearer token on every request into a User, the same role
 * every request plays in the Laravel version's auth:sanctum middleware
 * group. Endpoints annotated @Public (or whose class is) are let through
 * without a token.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    @Inject
    TokenService tokenService;

    @Inject
    CurrentUser currentUser;

    @Context
    ResourceInfo resourceInfo;

    /**
     * @Transactional lives here, not on touchLastUsed(): that inner call is a
     * plain self-invocation (this.touchLastUsed(...)), which bypasses the CDI
     * proxy entirely — and with it, any interceptor including @Transactional.
     * Discovered by actually calling the deployed endpoint, which failed with
     * "WFLYJPA0060: Transaction is required" on every authenticated request,
     * not by code review.
     */
    @Override
    @Transactional
    public void filter(ContainerRequestContext requestContext) {
        if (isPublic()) {
            return;
        }

        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            reject(requestContext, "Non authentifié.");
            return;
        }

        String plainToken = header.substring("Bearer ".length()).trim();
        String hash = tokenService.hash(plainToken);

        try {
            AccessToken token = em.createQuery(
                            "SELECT t FROM AccessToken t WHERE t.tokenHash = :hash", AccessToken.class)
                    .setParameter("hash", hash)
                    .getSingleResult();

            if (!token.getUser().isActive()) {
                reject(requestContext, "Compte désactivé.");
                return;
            }

            touchLastUsed(token);
            currentUser.set(token.getUser());
        } catch (NoResultException e) {
            reject(requestContext, "Session invalide ou expirée.");
        }
    }

    void touchLastUsed(AccessToken token) {
        token.setLastUsedAt(LocalDateTime.now());
        em.merge(token);
    }

    private boolean isPublic() {
        return resourceInfo.getResourceMethod().isAnnotationPresent(Public.class)
                || resourceInfo.getResourceClass().isAnnotationPresent(Public.class);
    }

    private void reject(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"message\":\"" + message + "\"}")
                        .type("application/json")
                        .build());
    }
}
