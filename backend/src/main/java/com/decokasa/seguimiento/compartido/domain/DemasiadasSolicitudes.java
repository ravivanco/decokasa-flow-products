package com.decokasa.seguimiento.compartido.domain;

/**
 * Se superó un límite de solicitudes por origen (ADR-0033; 429 en la API con {@code Retry-After}). No dice nada de la
 * cuenta afectada.
 */
public final class DemasiadasSolicitudes extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    private final long reintentarEnSegundos;

    public DemasiadasSolicitudes(long reintentarEnSegundos) {
        super(CodigoError.DEMASIADAS_SOLICITUDES, "Demasiadas solicitudes. Espera unos minutos e intenta de nuevo.");
        this.reintentarEnSegundos = Math.max(1, reintentarEnSegundos);
    }

    public long reintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
