package com.decokasa.seguimiento.archivos.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Marca histórica de subida de un adjunto ({@code adjunto.subido_en}), guardada por el servidor al confirmarlo. Fija la
 * carpeta del día en Drive (ADR-0027); no cambia con reintentos ni con la hora de la copia.
 */
@FunctionalInterface
public interface FechasDeSubida {
    Optional<Instant> subidoEn(UUID adjuntoId);
}
