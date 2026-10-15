package com.decokasa.seguimiento.compartido.domain;

/** El recurso pedido no existe (código NO_ENCONTRADO, 404 en la API). */
public final class NoEncontrado extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public NoEncontrado(String mensaje) {
        super(CodigoError.NO_ENCONTRADO, mensaje);
    }
}
