package com.decokasa.seguimiento.usuarios.application.port.in;

/**
 * Renueva el acceso con un token de renovación y lo rota (ADR-0002, D3): hasta 8 horas desde el inicio de sesión.
 *
 * @throws com.decokasa.seguimiento.usuarios.domain.SesionInvalida token desconocido, revocado (se revoca toda la
 *         familia) o vencido, o usuario desactivado o sin roles
 */
public interface RenovarSesion {

    SesionIniciada renovar(String tokenRenovacion);
}
