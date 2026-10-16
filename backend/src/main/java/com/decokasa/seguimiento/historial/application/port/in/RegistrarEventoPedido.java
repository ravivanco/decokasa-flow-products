package com.decokasa.seguimiento.historial.application.port.in;

import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;

/**
 * Inserta en el historial el evento de una acción (R7). Se llama dentro de la transacción de la acción: si la
 * inserción falla, la acción tampoco se guarda.
 */
public interface RegistrarEventoPedido {

    void registrar(EventoDePedido evento);
}
