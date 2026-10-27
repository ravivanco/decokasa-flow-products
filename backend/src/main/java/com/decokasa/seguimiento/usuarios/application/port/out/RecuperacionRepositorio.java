package com.decokasa.seguimiento.usuarios.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Solicitudes de recuperación de contraseña (ADR-0033, V28). Solo guarda el hash del token. Las mutaciones usan la
 * transacción del llamador.
 */
public interface RecuperacionRepositorio {

    /** Solicitudes de la cuenta creadas desde ese instante (límite por hora). */
    int contarDesde(UUID usuarioId, Instant desde);

    void registrar(UUID id, UUID usuarioId, Instant creadaEn, Instant expiraEn);

    /** Solicitud enviada (o en envío), sin usar ni anular y sin caducar, con ese hash; bloquea su fila. */
    Optional<Vigente> bloquearVigente(String tokenHash, Instant ahora);

    void marcarUsada(UUID id, Instant ahora);

    /** Anula las solicitudes aún utilizables de la cuenta (pendientes, en envío o enviadas). */
    int anularDeUsuario(UUID usuarioId, Instant ahora);

    /** Pendientes que caducaron sin salir: no se enviarán nunca. */
    int anularCaducadas(Instant ahora);

    /**
     * Reclama la pendiente más antigua sin caducar, la pasa a ENVIANDO con el hash del token nuevo y devuelve los datos
     * vigentes de la cuenta para el correo. Una sola solicitud por vez y sin reintentos: nunca se envía dos veces.
     */
    Optional<Reclamada> reclamar(String tokenHash, Instant ahora);

    void marcarEnviada(UUID id, Instant ahora);

    void marcarFallida(UUID id, String codigo, Instant ahora);

    void anular(UUID id, Instant ahora);

    record Vigente(UUID id, UUID usuarioId) {
    }

    /** {@code activa}: la cuenta sigue activa y con roles; si no, la solicitud se anula sin enviar. */
    record Reclamada(UUID id, UUID usuarioId, Instant expiraEn, String correo, String nombre, boolean activa) {

        @Override
        public String toString() {
            return "Reclamada[id=" + id + "]";
        }
    }
}
