package com.decokasa.seguimiento.usuarios.application.port.in;

/**
 * Cierra la sesión: revoca la familia del token de renovación (ADR-0002, D3b). Sin token, o con uno desconocido,
 * no hace nada; el contrato responde 204 igual.
 */
public interface CerrarSesion {

    void cerrar(String tokenRenovacion);
}
