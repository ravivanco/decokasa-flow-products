package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.pedidos.application.port.in.VerificarConsultaDePedido;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.UUID;

/**
 * P-13: primero la cuenta (403) y después la existencia del pedido (404); un pedido de un país no asignado también es
 * 404 (ADR-0024). Sin Spring (R2).
 */
public final class VerificarConsultaCasoDeUso implements VerificarConsultaDePedido {

    private final ConsultaDePedidos consultas;
    private final ApoyoPedidos apoyo;

    public VerificarConsultaCasoDeUso(ConsultaDePedidos consultas, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador) {
        this.consultas = consultas;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
    }

    @Override
    public String numeroVisible(Solicitante solicitante, UUID pedidoId) {
        ActorVigente actor = apoyo.lector(solicitante);
        Pedido pedido = consultas.porId(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        apoyo.exigirPais(actor, pedido);
        return pedido.numero().valor();
    }
}
