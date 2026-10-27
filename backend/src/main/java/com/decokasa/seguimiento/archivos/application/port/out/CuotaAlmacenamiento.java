package com.decokasa.seguimiento.archivos.application.port.out;

import java.util.UUID;

/** Short metadata transactions only. Never moves/deletes bytes or changes their owner. */
public interface CuotaAlmacenamiento {
    void reservar(UUID adjuntoId, UUID propietario, long bytes);
    void confirmar(UUID adjuntoId, UUID propietario, long bytes);
}
