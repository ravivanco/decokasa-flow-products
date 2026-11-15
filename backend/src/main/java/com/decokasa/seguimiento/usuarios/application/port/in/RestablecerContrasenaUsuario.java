package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.UUID;

/**
 * Admin pone una contraseña temporal a una cuenta (V36, plan B cuando el correo de recuperación no llega). Se cierran
 * sus sesiones, se anulan sus enlaces de recuperación, se quita el bloqueo y la persona debe crear una contraseña propia
 * al iniciar sesión.
 */
public interface RestablecerContrasenaUsuario {

    void restablecer(Solicitante solicitante, UUID usuarioId, String contrasenaTemporal);
}
