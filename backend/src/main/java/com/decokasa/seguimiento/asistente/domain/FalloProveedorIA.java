package com.decokasa.seguimiento.asistente.domain;

/**
 * Fallo del proveedor de IA, clasificado sin cuerpo, cabeceras ni mensajes del proveedor (pueden repetir parte de la
 * petición). La causa decide qué se hace con la reserva de presupuesto (ADR-0022).
 */
public final class FalloProveedorIA extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Causa {
        /** Rechazo confirmado antes de procesar (400, 401, 403, 404, 413, 422): no se cobró. */
        RECHAZADA,
        /** 429: límite de frecuencia del proveedor. No se reintenta. */
        LIMITE_PROVEEDOR,
        /**
         * Límite de gasto del workspace o de la organización (ADR-0022, respaldo externo): no se cobró, no se reintenta y
         * la IA se desactiva hasta que un admin la reactive.
         */
        LIMITE_GASTO,
        /** Tiempo agotado, conexión cortada, 5xx o respuesta inesperada: pudo cobrarse. */
        INCIERTA
    }

    private final Causa causa;

    public FalloProveedorIA(Causa causa) {
        super("Proveedor de IA: " + causa.name());
        this.causa = causa;
    }

    public Causa causa() {
        return causa;
    }

    /** Criterio conservador de ADR-0022: un resultado dudoso se da por consumido. */
    public boolean posibleCobro() {
        return causa == Causa.INCIERTA;
    }
}
