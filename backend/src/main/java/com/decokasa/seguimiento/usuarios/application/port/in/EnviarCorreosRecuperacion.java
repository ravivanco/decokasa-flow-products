package com.decokasa.seguimiento.usuarios.application.port.in;

/** Una pasada del trabajador que envía los enlaces de recuperación (ADR-0033). Devuelve cuántas solicitudes trató. */
public interface EnviarCorreosRecuperacion {

    int procesar();
}
