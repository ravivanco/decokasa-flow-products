package com.decokasa.seguimiento.usuarios.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Contraseña y sello de credenciales de la cuenta (ADR-0033). */
public interface CredencialesRepositorio {

    /**
     * Guarda el hash nuevo, sella las credenciales en {@code ahora}, reinicia el bloqueo y sube la versión de la cuenta
     * (una edición administrativa abierta antes recibe 409). Requiere la transacción del llamador.
     */
    void cambiarContrasena(UUID usuarioId, String contrasenaHash, Instant ahora);

    Optional<Instant> cambiadasEn(UUID usuarioId);
}
