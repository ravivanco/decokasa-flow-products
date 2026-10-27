package com.decokasa.seguimiento.notificaciones.adapter.in.eventos;

import com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado;
import com.decokasa.seguimiento.notificaciones.application.port.in.EncolarNotificacion;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Síncrono y obligatorio: un fallo revierte también la transición y el historial. No llama a Google. */
@Component
public class OyenteEventosNotificacion {
    private final EncolarNotificacion encolar;
    public OyenteEventosNotificacion(EncolarNotificacion encolar) { this.encolar = encolar; }
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void recibir(EventoPedidoRegistrado evento) { encolar.encolar(evento); }
}
