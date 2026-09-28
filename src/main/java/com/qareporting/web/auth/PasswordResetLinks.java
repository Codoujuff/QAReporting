package com.qareporting.web.auth;

import jakarta.servlet.http.HttpServletRequest;

/** Adresse publique de l'application, pour les liens envoyés par e-mail. */
public final class PasswordResetLinks {

    private PasswordResetLinks() {
    }

    public static String baseUrl(HttpServletRequest request) {
        return PasswordResetBean.publicBaseUrl(request);
    }
}
