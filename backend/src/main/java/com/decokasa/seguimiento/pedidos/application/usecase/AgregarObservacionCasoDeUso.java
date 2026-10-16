package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.ValidarAdjuntosParaAccion;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.AgregarObservacion;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.EtapasHabilitadas;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.UUID;

/**
 * P-19 (provisional del equipo): agrega una observación con el pedido bloqueado, para que no se cruce con una acción
 * que cambie la etapa. El permiso se decide con el estado, los roles y la asignación vigentes en la base. No guarda el
 * pedido: no cambia su versión, su etapa ni sus plazos; solo publica el evento, que el historial inserta en la misma
 * transacción. Sin Spring (R2).
 */
public final class AgregarObservacionCasoDeUso implements AgregarObservacion {

    private final PedidoRepositorio pedidos;
    private final ConsultarDefinicionFlujo flujos;
    private final EtapasHabilitadas habilitadas;
    private final Reloj reloj;
    private final ApoyoPedidos apoyo;
    private final ValidarAdjuntosParaAccion adjuntos;

    public AgregarObservacionCasoDeUso(PedidoRepositorio pedidos, ConsultarDefinicionFlujo flujos,
            EtapasHabilitadas habilitadas, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj, ValidarAdjuntosParaAccion adjuntos) {
        this.pedidos = pedidos;
        this.flujos = flujos;
        this.habilitadas = habilitadas;
        this.reloj = reloj;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
        this.adjuntos = adjuntos;
    }

    @Override
    public UUID agregar(Solicitante solicitante, Comando comando) {
        Pedido pedido = pedidos.buscarParaActualizar(comando.pedidoId())
                .orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        ActorVigente actor = apoyo.solicitante(solicitante);
        CodigoRol rol = flujos.porId(pedido.flujoId()).etapa(pedido.etapaActual()).rolResponsable();
        // Antes de revisar archivos, el permiso y la entrada: quien no puede actuar no aprende nada de los
        // identificadores, y un archivo solo cuenta si es de la entrada actual.
        pedido.autorizarObservacion(comando.etapaEsperada(), comando.entradaEsperadaId(), rol, habilitadas, actor);
        if (!comando.adjuntoIds().isEmpty()) {
            UUID entradaId = pedido.etapas().stream()
                    .filter(etapa -> etapa.numero() == pedido.etapaActual())
                    .map(EtapaDePedido::entradaId)
                    .findFirst()
                    .orElseThrow();
            adjuntos.tiposValidos(pedido.id(), pedido.etapaActual(), entradaId, comando.adjuntoIds());
        }
        UUID eventoId = pedido.agregarObservacion(comando.etapaEsperada(), comando.entradaEsperadaId(),
                comando.texto(), comando.adjuntoIds(), rol, habilitadas, actor, reloj.ahora()).eventoId();
        apoyo.publicarEventos(pedido);
        return eventoId;
    }
}
