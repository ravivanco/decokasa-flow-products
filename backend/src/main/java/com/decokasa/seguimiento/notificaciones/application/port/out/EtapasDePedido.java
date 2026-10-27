package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que notificaciones necesita saber del pedido para avisar (ADR-0032). Lo implementa pedidos (la dependencia va de
 * pedidos a notificaciones, R9) con la versión del flujo del pedido y su estado en esta transacción.
 */
public interface EtapasDePedido {

    /** Vacío si el pedido no existe. */
    Optional<Contexto> contexto(UUID pedidoId, Integer etapaOrigen, int etapaDestino);

    /**
     * @param rolDestino      rol responsable de la etapa de destino; nulo si es automática
     * @param asignadoDestino persona asignada individualmente a la etapa de destino; nula si no hay
     */
    record Contexto(String nombreOrigen, String nombreDestino, CodigoRol rolDestino, boolean destinoAutomatico,
            UUID asignadoDestino) {
    }
}
