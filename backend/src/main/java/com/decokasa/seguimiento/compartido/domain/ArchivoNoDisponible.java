package com.decokasa.seguimiento.compartido.domain;

/**
 * El archivo de un adjunto falta en el almacenamiento, está incompleto o su huella no coincide (código
 * ARCHIVO_NO_DISPONIBLE, 409 en la API). Nunca se entrega ni se acepta como válido (P-05, P-17).
 */
public final class ArchivoNoDisponible extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public ArchivoNoDisponible(String mensaje) {
        super(CodigoError.ARCHIVO_NO_DISPONIBLE, mensaje);
    }
}
