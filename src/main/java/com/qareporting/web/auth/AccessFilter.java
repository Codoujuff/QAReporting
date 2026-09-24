package com.qareporting.web.auth;

import com.qareporting.entity.Role;
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

/**
 * Single combined guard for every /app/* page: requires a logged-in session, and
 * additionally requires the admin role under /app/admin/*. Deliberately one filter
 * rather than two separately-mapped @WebFilters, since relative execution order
 * between multiple annotation-declared filters on overlapping patterns is otherwise
 * unspecified by the Servlet spec.
 */
@WebFilter(urlPatterns = "/app/*")
public class AccessFilter extends HttpFilter {

    @Inject
    private SessionAuth sessionAuth;

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

        chain.doFilter(request, response);
    }
}
