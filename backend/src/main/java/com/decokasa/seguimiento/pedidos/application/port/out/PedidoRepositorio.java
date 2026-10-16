package com.decokasa.seguimiento.pedidos.application.port.out;

import com.decokasa.seguimiento.pedidos.domain.Pedido;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia del agregado pedido (tablas pedido y etapa_pedido de V2). Un pedido no se borra (trigger de V2).
 */
public interface PedidoRepositorio {

    /**
     * Lee el pedido con su fila bloqueada hasta el final de la transacción ({@code SELECT … FOR UPDATE}) y con lo
     * último confirmado. Dos acciones simultáneas sobre el mismo pedido se atienden de una en una (P-11).
     */
    Optional<Pedido> buscarParaActualizar(UUID pedidoId);

    void insertar(Pedido pedido);

    /** Guarda los cambios de un pedido leído con {@link #buscarParaActualizar} en esta misma transacción. */
    void actualizar(Pedido pedido);
}
