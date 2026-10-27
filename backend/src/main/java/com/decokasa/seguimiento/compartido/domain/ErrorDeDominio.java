package com.decokasa.seguimiento.compartido.domain;

import java.util.Objects;

/**
 * Error de una regla de negocio. Lleva un código estable del contrato y un mensaje para la persona usuaria.
 * Es Java puro (R1): no conoce HTTP; el adaptador web decide el estado de la respuesta.
 */
public abstract class ErrorDeDominio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CodigoError codigo;

    protected ErrorDeDominio(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = Objects.requireNonNull(codigo, "codigo");
    }

    public CodigoError codigo() {
        return codigo;
    }
}
