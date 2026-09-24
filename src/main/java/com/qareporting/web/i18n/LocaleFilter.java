package com.qareporting.web.i18n;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Applique la langue demandée par « ?lang=fr|en » et la mémorise dans un cookie,
 * pour que le choix survive à la déconnexion et à l'expiration de la session.
 * Sans paramètre, une nouvelle session reprend la langue du cookie.
 */
@WebFilter(urlPatterns = "*.xhtml")
public class LocaleFilter extends HttpFilter {

    static final String COOKIE = "qa-lang";
    private static final String APPLIED = LocaleFilter.class.getName() + ".applied";

    @Inject
    private LocaleBean localeBean;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String requested = request.getParameter("lang");
        if (requested != null && LocaleBean.SUPPORTED.contains(requested)) {
            localeBean.setLanguage(requested);
            Cookie cookie = new Cookie(COOKIE, requested);
            cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
            cookie.setMaxAge(60 * 60 * 24 * 365);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            request.getSession().setAttribute(APPLIED, Boolean.TRUE);
        } else if (request.getSession().getAttribute(APPLIED) == null) {
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if (COOKIE.equals(c.getName())) {
                        localeBean.setLanguage(c.getValue());
                    }
                }
            }
            request.getSession().setAttribute(APPLIED, Boolean.TRUE);
        }

        chain.doFilter(request, response);
    }
}
