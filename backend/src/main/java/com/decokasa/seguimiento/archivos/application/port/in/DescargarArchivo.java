package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.io.InputStream;
import java.util.UUID;

/**
 * Descarga a través del servidor (P-17, provisional del equipo). Se separa en dos pasos para no tener una transacción
 * abierta mientras se lee el archivo: primero se autoriza y después se abre, verificando antes tamaño y huella.
 */
public interface DescargarArchivo {

    /**
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el adjunto no existe
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si la persona no puede descargarlo
     */
    Autorizada autorizar(Solicitante solicitante, UUID adjuntoId);

    /**
     * @throws com.decokasa.seguimiento.compartido.domain.ArchivoNoDisponible si falta, está incompleto o su huella no
     *         coincide
     */
    Contenido abrir(Autorizada autorizada);

    record Autorizada(UUID adjuntoId, String nombre, long tamanoBytes, String sha256, String claveLocal) {
    }

    /** El flujo ya verificado; quien lo recibe debe cerrarlo. */
    record Contenido(String nombre, long tamanoBytes, InputStream flujo) {
    }
}
