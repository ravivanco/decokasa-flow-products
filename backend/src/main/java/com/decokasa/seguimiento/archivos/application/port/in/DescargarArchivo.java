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

    /**
     * @param driveId ID de Drive persistido, solo si la copia está confirmada (ADR-0028); lo resuelve el servidor a
     *        partir del adjunto, nunca llega del navegador. Nulo si no hay copia confirmada.
     */
    record Autorizada(UUID adjuntoId, String nombre, long tamanoBytes, String sha256, String claveLocal,
            String driveId) {
        /** Sin copia de Drive: compatibilidad con las pruebas anteriores a ADR-0028. */
        public Autorizada(UUID adjuntoId, String nombre, long tamanoBytes, String sha256, String claveLocal) {
            this(adjuntoId, nombre, tamanoBytes, sha256, claveLocal, null);
        }

        @Override public String toString() { return "Autorizada[" + adjuntoId + "]"; }
    }

    /** El flujo ya verificado; quien lo recibe debe cerrarlo. */
    record Contenido(String nombre, long tamanoBytes, InputStream flujo) {
    }
}
