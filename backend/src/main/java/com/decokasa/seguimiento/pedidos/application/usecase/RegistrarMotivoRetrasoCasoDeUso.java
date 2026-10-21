package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.RegistrarMotivoRetraso;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.EtapasHabilitadas;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.UUID;

/**
 * F5-D3 (provisional del equipo): el motivo del retraso se guarda con el pedido bloqueado, como una acción, para que no
 * se cruce con un avance o una reentrada. El permiso se decide con el estado, los roles y la asignación vigentes en la
 * base (P-06). El motivo vigente y su evento de historial se confirman o revierten juntos. Sin Spring (R2).
 */
public final class RegistrarMotivoRetrasoCasoDeUso implements RegistrarMotivoRetraso {

    private final PedidoRepositorio pedidos;
    private final ConsultarDefinicionFlujo flujos;
    private final EtapasHabilitadas habilitadas;
    private final Reloj reloj;
    private final ApoyoPedidos apoyo;

    public RegistrarMotivoRetrasoCasoDeUso(PedidoRepositorio pedidos, ConsultarDefinicionFlujo flujos,
            EtapasHabilitadas habilitadas, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj) {
        this.pedidos = pedidos;
        this.flujos = flujos;
        this.habilitadas = habilitadas;
        this.reloj = reloj;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
    }

    @Override
    public UUID registrar(Solicitante solicitante, Comando comando) {
        Pedido pedido = pedidos.buscarParaActualizar(comando.pedidoId())
                .orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        ActorVigente actor = apoyo.solicitante(solicitante);
        ContratoDeFlujo flujo = flujos.porId(pedido.flujoId());
        CodigoRol rol = flujo.etapa(pedido.etapaActual()).rolResponsable();
        UUID eventoId = pedido.registrarMotivoRetraso(comando.etapaEsperada(), comando.entradaEsperadaId(),
                comando.motivo(), rol, habilitadas.para(flujo.etapas()), actor, reloj.ahora()).eventoId();
        pedidos.actualizar(pedido);
        apoyo.publicarEventos(pedido);
        return eventoId;
    }
}
