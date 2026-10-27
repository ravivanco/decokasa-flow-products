package com.decokasa.seguimiento.archivos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/**
 * El documento solo puede leerse de Drive y Google no respondió o la conexión central caducó (ADR-0028). Es recuperable:
 * no cambia el estado de la copia. Mensaje cerrado, sin datos del proveedor, tokens ni IDs.
 */
public final class DriveNoDisponible extends ErrorDeDominio {
    private static final long serialVersionUID = 1L;

    public DriveNoDisponible() {
        super(CodigoError.GOOGLE_NO_DISPONIBLE,
                "Este documento está guardado en Drive y Google no respondió. Intenta de nuevo en unos minutos; "
                        + "si continúa, avisa al admin.");
    }
}
