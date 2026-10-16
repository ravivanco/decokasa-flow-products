package com.decokasa.seguimiento.compartido.application.port.out;

import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;

/**
 * Puerto de salida para publicar los eventos de un pedido (arquitectura.md §3; P-15). Se llama dentro de la
 * transacción de la acción: los oyentes que deben fallar junto con ella (el historial, R7) corren en esa misma
 * transacción, así que si uno falla, la acción se revierte. El envío diferido (outbox, ADR-0012) sigue propuesto y no
 * se implementa aquí.
 */
public interface PublicadorEventos {

    void publicar(EventoDePedido evento);
}
