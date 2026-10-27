package com.decokasa.seguimiento.usuarios.application.port.in;

/** Recuperación de contraseña por correo (ADR-0033). Público: no requiere sesión. */
public interface RecuperarContrasena {

    /** Registra una solicitud si corresponde. No dice si la cuenta existe: nunca falla por eso. */
    void solicitar(String correo);

    /** Fija una contraseña nueva con el token del enlace. No inicia sesión. */
    void restablecer(String token, String contrasenaNueva, String confirmacion);
}
