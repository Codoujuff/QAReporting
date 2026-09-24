package com.qareporting.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JAX-RS resource method (or whole class) as reachable without a
 * Bearer token — everything else requires authentication by default. Used
 * on login/forgot-password/reset-password, mirroring the three routes left
 * outside the auth:sanctum group in the Laravel version's routes/api.php.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Public {
}
