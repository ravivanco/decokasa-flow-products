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
        this("Este documento está guardado en Drive y Google no respondió. Intenta de nuevo en unos minutos; "
                + "si continúa, avisa al admin.");
    }

    private DriveNoDisponible(String mensaje) {
        super(CodigoError.GOOGLE_NO_DISPONIBLE, mensaje);
    }

    /** ADR-0034: Google falló durante una carga directa a Drive. Lo confirmado se conserva y el reintento no duplica. */
    public static DriveNoDisponible alSubir() {
        return new DriveNoDisponible("Google Drive no respondió y el documento todavía no quedó guardado. Lo ya "
                + "enviado se conserva: vuelve a pulsar Subir con el mismo archivo y la carga continuará sin duplicarse.");
    }

    /** ADR-0034: la cuenta central de Google no está autorizada o caducó; solo el admin puede reconectarla. */
    public static DriveNoDisponible sinAutorizacion() {
        return new DriveNoDisponible("La conexión con Google Drive no está autorizada en este momento y el documento no "
                + "se guardó. Avisa al admin para que pulse «Conectar Google Drive» en Usuarios y vuelve a intentarlo.");
    }
}
