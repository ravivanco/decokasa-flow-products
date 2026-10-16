package com.decokasa.seguimiento.historial.application.port.out;

import com.decokasa.seguimiento.historial.domain.EventoPedido;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida del historial. Solo inserta y consulta: no hay operación para modificar ni borrar (R7). En la base,
 * los triggers de V2 rechazan UPDATE, DELETE y TRUNCATE sobre evento_pedido.
 */
public interface HistorialRepositorio {

    void insertar(EventoPedido evento);

    /** Eventos del pedido del más antiguo al más reciente. Lo usará la consulta del historial (paso 2.4). */
    List<EventoPedido> listarPorPedido(UUID pedidoId);
}
