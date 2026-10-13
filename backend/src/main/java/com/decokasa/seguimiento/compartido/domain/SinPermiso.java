package com.decokasa.seguimiento.compartido.domain;

/** El solicitante no tiene el rol que exige la operación (código SIN_PERMISO, 403 en la API; R6). */
public final class SinPermiso extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public SinPermiso(String mensaje) {
        super(CodigoError.SIN_PERMISO, mensaje);
    }
}
