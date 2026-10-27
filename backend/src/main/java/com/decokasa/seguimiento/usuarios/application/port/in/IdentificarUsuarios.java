package com.decokasa.seguimiento.usuarios.application.port.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Nombre de usuario (login) de una persona, para el nombre visible de su carpeta de Drive (ADR-0026). Sin correo,
 * teléfono ni credenciales. Lo usan otros módulos sin tocar el dominio de usuarios (R4).
 */
@FunctionalInterface
public interface IdentificarUsuarios {

    /** Vacío si el usuario no existe. */
    Optional<String> nombreDeUsuario(UUID usuarioId);
}
