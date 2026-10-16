package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import java.util.UUID;

/**
 * El admin reasigna la etapa actual (contrato: PUT /pedidos/{id}/etapas/{n}/asignacion). Regla provisional del
 * equipo (P-07): solo a personas activas con el rol responsable; reasignar a otro rol sigue pendiente del cliente.
 */
public interface ReasignarEtapa {

    Pedido reasignar(Solicitante solicitante, Comando comando);

    record Comando(UUID pedidoId, int numeroEtapa, UUID usuarioId, String motivo) {
    }
}
