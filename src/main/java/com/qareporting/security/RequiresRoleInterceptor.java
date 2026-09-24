package com.qareporting.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

@RequiresRole({})
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RequiresRoleInterceptor {

    @Inject
    CurrentUser currentUser;

    @AroundInvoke
    public Object checkRole(InvocationContext context) throws Exception {
        RequiresRole binding = context.getMethod().getAnnotation(RequiresRole.class);
        if (binding == null) {
            binding = context.getTarget().getClass().getAnnotation(RequiresRole.class);
        }

        if (!currentUser.isAuthenticated()) {
            throw new NotAuthorizedException("Non authentifié.");
        }
        if (binding != null && binding.value().length > 0 && !currentUser.hasAnyRole(binding.value())) {
            throw new ForbiddenException("Vous n'avez pas les droits pour effectuer cette action.");
        }

        return context.proceed();
    }
}
