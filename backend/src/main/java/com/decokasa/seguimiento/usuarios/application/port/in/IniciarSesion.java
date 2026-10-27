package com.decokasa.seguimiento.usuarios.application.port.in;

/**
 * Inicio de sesión con correo, nombre de usuario o teléfono y contraseña (requisito 4.2 #1, ADR-0002).
 *
 * @throws com.decokasa.seguimiento.usuarios.domain.CredencialesInvalidas contraseña incorrecta o de más de 72
 *         bytes, usuario inexistente, desactivado o sin roles (siempre el mismo error)
 * @throws com.decokasa.seguimiento.usuarios.domain.CuentaBloqueada tras 5 intentos fallidos, durante 15 minutos
 */
public interface IniciarSesion {

    SesionIniciada iniciar(String identificador, String contrasena);
}
