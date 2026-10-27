package com.decokasa.seguimiento.pedidos.adapter.in.notificaciones;

import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.notificaciones.application.port.out.EtapasDePedido;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Expone a notificaciones las etapas del pedido con la versión de su flujo y la asignación vigente en esta transacción
 * (ADR-0032). Vive en pedidos para que la dependencia vaya de pedidos a notificaciones y nunca al revés (R9).
 */
@Component
public class EtapasParaNotificacionesAdaptador implements EtapasDePedido {

    private final ConsultaDePedidos pedidos;
    private final ConsultarDefinicionFlujo flujos;

    public EtapasParaNotificacionesAdaptador(ConsultaDePedidos pedidos, ConsultarDefinicionFlujo flujos) {
        this.pedidos = pedidos;
        this.flujos = flujos;
    }

    @Override
    public Optional<Contexto> contexto(UUID pedidoId, Integer etapaOrigen, int etapaDestino) {
        Optional<Pedido> encontrado = pedidos.porId(pedidoId);
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        Pedido pedido = encontrado.get();
        ContratoDeFlujo flujo = flujos.porId(pedido.flujoId());
        ContratoDeEtapa destino = etapa(flujo, etapaDestino);
        if (destino == null) {
            return Optional.empty();
        }
        ContratoDeEtapa origen = etapaOrigen == null ? null : etapa(flujo, etapaOrigen);
        UUID asignado = pedido.etapas().stream()
                .filter(e -> e.numero() == etapaDestino)
                .map(EtapaDePedido::asignadoA)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        return Optional.of(new Contexto(origen == null ? null : origen.nombre(), destino.nombre(),
                destino.rolResponsable(), destino.automatica(), asignado));
    }

    private static ContratoDeEtapa etapa(ContratoDeFlujo flujo, int numero) {
        return flujo.etapas().stream().filter(e -> e.numero() == numero).findFirst().orElse(null);
    }
}
