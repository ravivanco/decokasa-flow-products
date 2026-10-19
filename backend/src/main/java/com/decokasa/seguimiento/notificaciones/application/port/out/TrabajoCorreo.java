package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Cada método confirma una transacción corta independiente; nunca contiene HTTP. */
public interface TrabajoCorreo {
    Optional<Notificacion> reclamar(Instant ahora, Instant hasta, UUID propietario);
    boolean vigente(UUID id, UUID propietario, Instant ahora);
    boolean enviada(UUID id, UUID propietario, String proveedorId, Instant ahora);
    boolean fallo(UUID id, UUID propietario, Causa causa, Instant ahora);
    /** Adapters without durable retry support conservatively stop instead. */
    default boolean reintentar(UUID id, UUID propietario, Causa causa, Instant siguiente, Instant ahora) {
        return fallo(id,propietario,causa,ahora);
    }
}
