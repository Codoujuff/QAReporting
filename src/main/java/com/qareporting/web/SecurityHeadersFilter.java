package com.qareporting.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * En-têtes de sécurité sur toutes les réponses, et HTTPS obligatoire quand la variable
 * d'environnement QA_FORCE_HTTPS=true est définie (production) : toute requête HTTP est
 * alors redirigée vers le port HTTPS (QA_HTTPS_PORT, 8443 par défaut). Sans la variable,
 * le HTTP reste utilisable (développement, démonstration sur un PC).
 */
@WebFilter(urlPatterns = "/*")
public class SecurityHeadersFilter extends HttpFilter {

    private static final boolean FORCE_HTTPS = "true".equalsIgnoreCase(System.getenv("QA_FORCE_HTTPS"));
    private static final String HTTPS_PORT = System.getenv().getOrDefault("QA_HTTPS_PORT", "8443");

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (FORCE_HTTPS && !request.isSecure()) {
            String query = request.getQueryString();
            String port = "443".equals(HTTPS_PORT) ? "" : ":" + HTTPS_PORT;
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", "https://" + request.getServerName() + port
                    + request.getRequestURI() + (query != null ? "?" + query : ""));
            return;
        }

        response.setHeader("X-Content-Type-Options", "nosniff");          // pas d'interprétation « au jugé » des fichiers
        response.setHeader("X-Frame-Options", "DENY");                     // pas d'affichage dans un cadre (clickjacking)
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                        + "font-src 'self' https://fonts.gstatic.com; script-src 'self' 'unsafe-inline'; "
                        + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'");
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }
        chain.doFilter(request, response);
    }
}
