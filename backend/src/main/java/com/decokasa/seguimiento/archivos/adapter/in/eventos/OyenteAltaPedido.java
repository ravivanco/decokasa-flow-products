package com.decokasa.seguimiento.archivos.adapter.in.eventos;

import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasPedidos;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoCreado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Síncrono y en la transacción del alta del pedido (ADR-0027): registra la intención de crear su carpeta de Drive. No
 * llama a Google; si esta escritura falla, el alta se revierte, y si Google falla después, el pedido sigue creado.
 */
@Component
public class OyenteAltaPedido {
    private final GestionarCarpetasPedidos.ProgramarAlta programar;

    public OyenteAltaPedido(GestionarCarpetasPedidos.ProgramarAlta programar) { this.programar = programar; }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void recibir(PedidoCreado evento) { programar.programarAlta(evento.pedidoId(), evento.ocurridoEn()); }
}
