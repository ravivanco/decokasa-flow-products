package com.decokasa.seguimiento.historial.adapter.in.eventos;

import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import com.decokasa.seguimiento.historial.application.port.in.RegistrarEventoPedido;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escucha los eventos de pedido y los inserta en el historial (P-15). Es síncrono y exige la transacción de la acción
 * ({@code MANDATORY}): acción e historial se confirman o se revierten juntos (R7). No usa
 * {@code @TransactionalEventListener(AFTER_COMMIT)}, que dejaría acciones sin historial si la inserción fallara, y
 * sin transacción falla en lugar de escribir un historial suelto.
 */
@Component
public class OyenteEventosPedido {

    private final RegistrarEventoPedido registrar;

    public OyenteEventosPedido(RegistrarEventoPedido registrar) {
        this.registrar = registrar;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void alOcurrir(EventoDePedido evento) {
        registrar.registrar(evento);
    }
}
