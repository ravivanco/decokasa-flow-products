package com.decokasa.seguimiento.compartido.domain;

import java.util.Objects;

/**
 * Un dato que debe ser único ya existe, por ejemplo el correo de otro usuario (código DUPLICADO, 409 en la API).
 * Lleva el campo para que el formulario marque el error en su lugar.
 */
public final class DatoDuplicado extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    private final String campo;

    public DatoDuplicado(String campo, String mensaje) {
        super(CodigoError.DUPLICADO, mensaje);
        this.campo = Objects.requireNonNull(campo, "campo");
    }

    public String campo() {
        return campo;
    }
}
