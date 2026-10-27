package com.decokasa.seguimiento.usuarios.application.port.in;

import java.util.UUID;

/** Cambio de la propia contraseña con la sesión iniciada (ADR-0033). Revoca todas las sesiones de la cuenta. */
public interface CambiarContrasena {

    void cambiar(UUID usuarioId, String contrasenaActual, String contrasenaNueva, String confirmacion);
}
