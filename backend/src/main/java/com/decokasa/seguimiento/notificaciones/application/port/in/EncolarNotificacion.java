package com.decokasa.seguimiento.notificaciones.application.port.in;

import com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado;

/** Debe invocarse dentro de la transacción que insertó el evento del historial. */
public interface EncolarNotificacion {
    void encolar(EventoPedidoRegistrado evento);
}
