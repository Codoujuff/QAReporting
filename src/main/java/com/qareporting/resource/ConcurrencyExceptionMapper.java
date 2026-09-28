package com.qareporting.resource;

import com.qareporting.web.error.ConcurrencyExceptionHandlerFactory;
import jakarta.transaction.TransactionalException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * API : une écriture sur une version périmée répond 409 Conflict (à recharger puis
 * rejouer) au lieu d'une erreur 500. Le conflit arrive soit tel quel, soit enveloppé par
 * l'intercepteur @Transactional au moment du commit.
 */
@Provider
public class ConcurrencyExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException e) {
        if (e instanceof jakarta.ws.rs.WebApplicationException wae) {
            return wae.getResponse(); // comportement normal de JAX-RS, inchangé
        }
        if (ConcurrencyExceptionHandlerFactory.isConcurrentModification(e)) {
            return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON)
                    .entity("{\"message\":\"Cet élément a été modifié entre-temps : rechargez-le puis recommencez.\"}")
                    .build();
        }
        if (e instanceof TransactionalException || e.getCause() != null) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Erreur API", e);
        }
        return Response.serverError().type(MediaType.APPLICATION_JSON)
                .entity("{\"message\":\"Erreur interne.\"}").build();
    }
}
