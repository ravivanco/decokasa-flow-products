package com.decokasa.seguimiento.pedidos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.pedidos.domain.NumeroPedido;

/**
 * Reserva el siguiente número de pedido (requerimientos 4.2 #5; decisiones del equipo del 2026-10-15: lectura A,
 * sin exigir ausencia de huecos, NNNN de cuatro dígitos como mínimo, sin truncar ni reiniciar). Corre en la misma
 * transacción que crea el pedido: si esa transacción se revierte, el número no perteneció a ningún pedido confirmado
 * y puede volver a entregarse. El número solo se expone después del commit. El formato sigue provisional (P-03, ⏸).
 */
public interface GeneradorNumeroPedido {

    NumeroPedido siguiente(CodigoPais pais, int anio);
}
