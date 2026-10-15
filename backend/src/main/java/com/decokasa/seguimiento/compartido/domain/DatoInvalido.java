package com.decokasa.seguimiento.compartido.domain;

/**
 * Un dato no cumple el formato o las reglas del dominio (código VALIDACION, 400 en la API). Si se conoce el campo,
 * la API lo informa en {@code errores[].campo} para que el formulario lo marque.
 */
public final class DatoInvalido extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    private final String campo;

    public DatoInvalido(String mensaje) {
        this(null, mensaje);
    }

    public DatoInvalido(String campo, String mensaje) {
        super(CodigoError.VALIDACION, mensaje);
        this.campo = campo;
    }

    /** Campo del contrato al que se refiere el error, o {@code null} si no corresponde a uno. */
    public String campo() {
        return campo;
    }
}
