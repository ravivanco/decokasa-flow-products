package com.decokasa.seguimiento.compartido.domain;

/** Clasificación cerrada para recuperación futura. No contiene cuerpo HTTP, tokens o causas del proveedor. */
public final class FalloIntegracionGoogle extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public enum Causa { DESACTIVADA, AUTORIZACION_REQUERIDA, CUOTA, TRANSITORIA, PERMANENTE,
        RESULTADO_INCIERTO, RESPUESTA_INVALIDA, TRANSACCION_ACTIVA, SESION_CADUCADA, CONFLICTO_REMOTO,
        METADATOS_DIVERGENTES, ALMACEN_LOCAL, LEASE_PERDIDO, CONFIGURACION }
    private final Causa causa;
    private final boolean rechazoConfirmado;
    private final java.time.Duration esperaMinima;
    public FalloIntegracionGoogle(Causa causa) {
        this(causa,java.time.Duration.ZERO);
    }
    public FalloIntegracionGoogle(Causa causa, java.time.Duration esperaMinima) {
        this(causa,esperaMinima,false);
    }
    private FalloIntegracionGoogle(Causa causa, java.time.Duration esperaMinima, boolean rechazoConfirmado) {
        super("Integración Google: " + causa.name());
        this.causa = causa;
        this.rechazoConfirmado = rechazoConfirmado;
        this.esperaMinima=esperaMinima.isNegative() ? java.time.Duration.ZERO : esperaMinima;
    }
    public static FalloIntegracionGoogle rechazoTransitorioConfirmado(Causa causa, java.time.Duration espera) {
        if (causa!=Causa.CUOTA && causa!=Causa.TRANSITORIA) {
            throw new IllegalArgumentException("Solo un rechazo transitorio confirmado permite reintentar");
        }
        return new FalloIntegracionGoogle(causa,espera,true);
    }
    public boolean rechazoConfirmado() { return rechazoConfirmado; }
    public Causa causa() { return causa; }
    public java.time.Duration esperaMinima() { return esperaMinima; }
}
