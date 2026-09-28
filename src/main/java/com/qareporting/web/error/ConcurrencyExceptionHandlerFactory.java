package com.qareporting.web.error;

import com.qareporting.web.i18n.I18n;
import jakarta.faces.FacesException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Deux personnes enregistrent le même élément : la seconde écriture est refusée par le
 * verrou optimiste (@Version). Au lieu d'une page d'erreur 500, on affiche un message
 * clair et on recharge la page (avec ses paramètres) sur la version à jour.
 */
public class ConcurrencyExceptionHandlerFactory extends ExceptionHandlerFactory {

    public ConcurrencyExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
        super(wrapped);
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new Handler(getWrapped().getExceptionHandler());
    }

    /** Vrai si la chaîne de causes contient un conflit de version (JPA ou Hibernate). */
    public static boolean isConcurrentModification(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof jakarta.persistence.OptimisticLockException
                    || c.getClass().getName().startsWith("org.hibernate.StaleObjectState")
                    || c.getClass().getName().equals("org.hibernate.StaleStateException")) {
                return true;
            }
            if (c.getCause() == c) {
                break;
            }
        }
        return false;
    }

    private static final class Handler extends ExceptionHandlerWrapper {

        Handler(ExceptionHandler wrapped) {
            super(wrapped);
        }

        @Override
        public void handle() throws FacesException {
            Iterator<ExceptionQueuedEvent> events = getUnhandledExceptionQueuedEvents().iterator();
            while (events.hasNext()) {
                Throwable error = events.next().getContext().getException();
                if (!isConcurrentModification(error)) {
                    continue;
                }
                events.remove();
                FacesContext fc = FacesContext.getCurrentInstance();
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, I18n.t("err.concurrentModification"), null));
                fc.getExternalContext().getFlash().setKeepMessages(true);
                String viewId = fc.getViewRoot().getViewId();
                String url = fc.getApplication().getViewHandler().getRedirectURL(fc, viewId, Map.of(), true);
                try {
                    fc.getExternalContext().redirect(url);
                } catch (IOException e) {
                    throw new FacesException(e);
                }
                fc.responseComplete();
            }
            getWrapped().handle();
        }
    }
}
