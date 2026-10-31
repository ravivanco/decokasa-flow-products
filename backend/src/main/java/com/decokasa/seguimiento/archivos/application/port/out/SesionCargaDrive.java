package com.decokasa.seguimiento.archivos.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * ADR-0034: lo que se recuerda de la sesión reanudable de Drive de una carga (V31). Solo identificadores y la URI de
 * sesión cifrada; nunca el contenido. Cada operación es una sentencia corta, sin red.
 */
public interface SesionCargaDrive {

    Optional<Estado> leer(UUID cargaId);

    /** Fija el ID reservado y la carpeta solo si todavía no había (un reintento conserva los primeros). */
    void reservar(UUID cargaId, String driveId, String carpetaId);

    void guardarSesion(UUID cargaId, byte[] sesionCifrada);

    /** Tras registrar el adjunto: la URI de sesión ya no sirve y es una credencial; se borra (ID y carpeta quedan). */
    void olvidarSesion(UUID cargaId);

    /**
     * La sesión de Drive se perdió o el archivo se descartó: la carga vuelve a empezar (offset 0, sin fragmentos) y se
     * olvidan sesión, ID y carpeta. Nunca toca una carga COMPLETADA.
     */
    void reiniciar(UUID cargaId);

    record Estado(String driveId, String carpetaId, byte[] sesionCifrada) {
        public Estado { sesionCifrada = sesionCifrada == null ? null : sesionCifrada.clone(); }
        @Override public byte[] sesionCifrada() { return sesionCifrada == null ? null : sesionCifrada.clone(); }
        @Override public String toString() { return "EstadoCargaDrive[PROTEGIDO]"; }
    }
}
