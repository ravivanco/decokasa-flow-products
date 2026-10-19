package com.decokasa.seguimiento.archivos.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Exclusión de una copia entre procesos que comparten el volumen aceptado de fase 2. */
public interface BloqueoCopiaLocal {
    Optional<Bloqueo> intentar(UUID adjuntoId);
    interface Bloqueo extends AutoCloseable { @Override void close(); }
}
