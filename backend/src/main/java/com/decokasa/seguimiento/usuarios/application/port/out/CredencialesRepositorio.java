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

    /** Como {@link #cambiarContrasena}, pero marca la cuenta para que cree su propia contraseña al entrar (V36). */
    void restablecerTemporal(UUID usuarioId, String contrasenaHash, Instant ahora);

    /** La cuenta tiene una contraseña temporal pendiente de cambio (V36). */
    boolean debeCambiar(UUID usuarioId);

    Optional<Instant> cambiadasEn(UUID usuarioId);
}
