package com.qareporting.web.auth;

import com.qareporting.entity.Role;
import com.qareporting.security.CurrentUser;
import com.qareporting.security.Permissions;
import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

/**
 * Single combined guard for every /app/* page: requires a logged-in session, and
 * additionally requires the admin role under /app/admin/*. Deliberately one filter
 * rather than two separately-mapped @WebFilters, since relative execution order
 * between multiple annotation-declared filters on overlapping patterns is otherwise
 * unspecified by the Servlet spec.
 *
 * Applique aussi la matrice des permissions aux écrans de saisie (PAGE_RULES) — les
 * actions des beans la revérifient, ce filtre évite simplement d'afficher un formulaire
 * inutilisable — et renseigne CurrentUser pour que le journal d'audit connaisse l'auteur
 * des actions JSF comme de celles de l'API REST.
 */
@WebFilter(urlPatterns = "/app/*")
public class AccessFilter extends HttpFilter {

    private static final Map<String, Set<String>> PAGE_RULES = Map.of(
            "/app/campaigns/form.xhtml", Permissions.MANAGE_CAMPAIGNS,
            "/app/tests/form.xhtml", Permissions.MANAGE_TESTS,
            "/app/activity/form.xhtml", Permissions.LOG_ACTIVITY,
            "/app/defects/form.xhtml", Permissions.WORK_ON_DEFECTS,
            "/app/team.xhtml", Permissions.TEAM_VIEW
    );

    @Inject
    private SessionAuth sessionAuth;

    @Inject
    private CurrentUser currentUser;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!sessionAuth.isLoggedIn()) {
            String path = request.getServletPath();
            String query = request.getQueryString();
            String next = path + (query != null ? "?" + query : "");
            String encodedNext = URLEncoder.encode(next, StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/login.xhtml?next=" + encodedNext);
            return;
        }

        if (request.getServletPath().startsWith("/app/admin/") && !sessionAuth.hasRole(Role.ADMIN)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Set<String> allowed = PAGE_RULES.get(request.getServletPath());
        if (allowed != null && !Permissions.allows(allowed, sessionAuth.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        com.qareporting.entity.User user = sessionAuth.getCurrentUser();
        if (user == null || !user.isActive()) {
            // Compte désactivé (ou supprimé) pendant la session : on coupe tout de suite.
            request.getSession().invalidate();
            response.sendRedirect(request.getContextPath() + "/login.xhtml");
            return;
        }
        if (sessionAuth.isMustChangePassword() && !"/app/settings.xhtml".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/app/settings.xhtml");
            return;
        }
        currentUser.set(user);

        chain.doFilter(request, response);
    }
}
