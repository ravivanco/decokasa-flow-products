package com.decokasa.seguimiento.compartido.domain;

/**
 * Otra persona cambió el recurso desde que el cliente lo leyó: la versión enviada ya no es la vigente (código
 * VERSION_DESACTUALIZADA, 409 en la API; D10). No se aplica ningún cambio y no lleva campo: hay que volver a leer.
 */
public final class VersionDesactualizada extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public VersionDesactualizada(String mensaje) {
        super(CodigoError.VERSION_DESACTUALIZADA, mensaje);
    }
}
