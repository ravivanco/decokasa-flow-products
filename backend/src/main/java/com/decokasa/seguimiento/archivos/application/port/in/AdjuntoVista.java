package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Set;
import java.util.UUID;

/**
 * Un adjunto con lo que se muestra de él.
 *
 * @param eventoId evento ARCHIVO_SUBIDO; nulo si no se encontró
 */
public record AdjuntoVista(Adjunto adjunto, UUID eventoId, Persona subidoPor) {

    /** Quien subió el archivo (contrato: UsuarioResumen). */
    public record Persona(UUID id, String nombre, Set<CodigoRol> roles) {
    }
}
