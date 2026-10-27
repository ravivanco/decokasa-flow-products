package com.decokasa.seguimiento.pedidos.domain;

/**
 * Estado de una etapa en la línea de tiempo del pedido (contrato: EstadoEtapa; columna etapa_pedido.estado).
 * DEVUELTA: la etapa se cerró, pero el pedido volvió a una etapa anterior.
 */
public enum EstadoEtapa {
    PENDIENTE,
    ACTUAL,
    COMPLETADA,
    DEVUELTA
}
