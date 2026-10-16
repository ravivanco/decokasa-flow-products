package com.decokasa.seguimiento.compartido.domain;

/**
 * La etapa que la persona veía ya no es la actual (código ETAPA_CAMBIO, 409 en la API): otra persona movió el pedido.
 * No se aplica ningún cambio; la pantalla recarga el pedido.
 */
public final class EtapaCambio extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public EtapaCambio(String mensaje) {
        super(CodigoError.ETAPA_CAMBIO, mensaje);
    }
}
