package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Objects;
import java.util.UUID;

/**
 * Quién hizo una acción y con qué rol intervino (R7: actor y rol en cada evento). El rol es el responsable de la etapa
 * o {@code admin} si la persona actuó como admin; se guarda tal cual aunque después cambien sus roles.
 */
public record Interviniente(UUID usuarioId, CodigoRol rol) {

    public Interviniente {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(rol, "rol");
    }
}
