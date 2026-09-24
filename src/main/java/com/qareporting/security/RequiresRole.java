package com.qareporting.security;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method/class-level role gate — the interceptor-based equivalent of a
 * Laravel Policy's authorize() check, but declared inline on the resource
 * method instead of a separate Policy class. Example:
 *
 *   @RequiresRole({Role.ADMIN})
 *   public Response destroy(...) { ... }
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresRole {
    @Nonbinding
    String[] value();
}
