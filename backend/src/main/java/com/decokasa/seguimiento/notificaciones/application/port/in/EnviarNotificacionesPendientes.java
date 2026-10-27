package com.decokasa.seguimiento.notificaciones.application.port.in;

/** Una pasada acotada; no autoriza activar Google ni resolver destinatarios históricos. */
public interface EnviarNotificacionesPendientes {
    int procesar();
}
