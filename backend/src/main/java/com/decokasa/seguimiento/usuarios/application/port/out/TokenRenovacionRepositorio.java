package com.decokasa.seguimiento.usuarios.application.port.out;

import com.decokasa.seguimiento.usuarios.domain.SesionRenovable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Tokens de renovación guardados por su hash (ADR-0002, D3). */
public interface TokenRenovacionRepositorio {

    Optional<SesionRenovable> buscarPorHash(String tokenHash);

    void guardar(SesionRenovable sesion);

    /**
     * Consume el token para rotarlo: lo marca revocado solo si nadie lo hizo antes, en una sola operación atómica.
     * Devuelve {@code false} si otra solicitud ya lo había consumido o revocado, aunque la lectura previa lo viera
     * vigente (hallazgo A2: dos renovaciones simultáneas no pueden emitir dos sucesores).
     */
    boolean consumir(UUID id, Instant ahora);

    /** Revoca todos los tokens sin revocar de un inicio de sesión (rotación o reutilización detectada). */
    void revocarFamilia(UUID familiaId, Instant ahora);

    /** Revoca todos los tokens sin revocar de un usuario (desactivación o cambio de contraseña). */
    void revocarDeUsuario(UUID usuarioId, Instant ahora);
}
