package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.ReasignarEtapa;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;

/**
 * El admin reasigna la etapa actual. Bloquea el pedido como una acción (P-11): una reasignación y un avance
 * simultáneos se atienden de uno en uno, y la reasignación de una etapa que ya avanzó responde 409 ETAPA_CAMBIO.
 * Sin Spring (R2).
 */
public final class ReasignarEtapaCasoDeUso implements ReasignarEtapa {

    private final PedidoRepositorio pedidos;
    private final ConsultarDefinicionFlujo flujos;
    private final Reloj reloj;
    private final ApoyoPedidos apoyo;

    public ReasignarEtapaCasoDeUso(PedidoRepositorio pedidos, ConsultarDefinicionFlujo flujos,
            ConsultarResponsables responsables, CalcularVencimiento vencimientos, PublicadorEventos publicador,
            Reloj reloj) {
        this.pedidos = pedidos;
        this.flujos = flujos;
        this.reloj = reloj;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
    }

    @Override
    public Pedido reasignar(Solicitante solicitante, Comando comando) {
        Pedido pedido = pedidos.buscarParaActualizar(comando.pedidoId())
                .orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        ActorVigente admin = apoyo.solicitante(solicitante);
        // Si la persona no existe, el dominio responde 400 en usuarioId (no puede recibir la etapa).
        ActorVigente candidato = comando.usuarioId() == null ? null
                : apoyo.persona(comando.usuarioId()).orElse(null);
        // Rol de la etapa actual en la versión del flujo del pedido. Si numeroEtapa no es la actual, el dominio
        // responde 409 antes de usarlo.
        // Solo el contrato compartido del flujo, no sus clases internas (R4; 2.B1-D01).
        ContratoDeFlujo flujo = flujos.porId(pedido.flujoId());
        CodigoRol rolResponsable = flujo.etapa(pedido.etapaActual()).rolResponsable();
        boolean cambio = pedido.reasignar(comando.numeroEtapa(), rolResponsable, admin, candidato, comando.motivo(),
                reloj.ahora()).isPresent();
        if (cambio) {
            pedidos.actualizar(pedido);
            apoyo.publicarEventos(pedido);
        }
        return pedido;
    }
}
