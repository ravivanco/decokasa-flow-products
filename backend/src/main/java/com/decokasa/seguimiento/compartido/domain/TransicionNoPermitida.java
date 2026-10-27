package com.decokasa.seguimiento.compartido.domain;

/**
 * La acción no se puede ejecutar en la etapa actual (código TRANSICION_NO_PERMITIDA, 409 en la API): no existe en la
 * definición del flujo, la etapa no está habilitada en esta fase o sus requisitos documentales siguen pendientes.
 */
public final class TransicionNoPermitida extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public TransicionNoPermitida(String mensaje) {
        super(CodigoError.TRANSICION_NO_PERMITIDA, mensaje);
    }
}
