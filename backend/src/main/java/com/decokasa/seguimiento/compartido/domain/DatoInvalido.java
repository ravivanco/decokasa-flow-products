package com.decokasa.seguimiento.compartido.domain;

/** Un dato no cumple el formato o las reglas del dominio (código VALIDACION, 400 en la API). */
public final class DatoInvalido extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public DatoInvalido(String mensaje) {
        super(CodigoError.VALIDACION, mensaje);
    }
}
