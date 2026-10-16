package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.AutorizarArchivosDePedido;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.EtapasHabilitadas;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Reglas de archivos que dependen del pedido. Subir: P-06 sobre la entrada actual (P-18), con el pedido bloqueado como
 * en una acción. Ver: P-13. Descargar: P-17 (admin, quien actúa en la etapa del archivo o en la etapa que la revisa,
 * según los datos del flujo; provisional del equipo). Sin Spring (R2).
 */
public final class AutorizarArchivosCasoDeUso implements AutorizarArchivosDePedido {

    private final PedidoRepositorio pedidos;
    private final ConsultaDePedidos consultas;
    private final ConsultarDefinicionFlujo flujos;
    private final EtapasHabilitadas habilitadas;
    private final ApoyoPedidos apoyo;

    public AutorizarArchivosCasoDeUso(PedidoRepositorio pedidos, ConsultaDePedidos consultas,
            ConsultarDefinicionFlujo flujos, EtapasHabilitadas habilitadas, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador) {
        this.pedidos = pedidos;
        this.consultas = consultas;
        this.flujos = flujos;
        this.habilitadas = habilitadas;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
    }

    @Override
    public AutorizacionSubida autorizarSubida(Solicitante solicitante, UUID pedidoId, int etapa,
            UUID entradaEsperadaId) {
        Pedido pedido = pedidos.buscarParaActualizar(pedidoId)
                .orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        ActorVigente actor = apoyo.solicitante(solicitante);
        CodigoRol rol = flujos.porId(pedido.flujoId()).etapa(pedido.etapaActual()).rolResponsable();
        Interviniente interviniente = pedido.autorizarSubida(etapa, entradaEsperadaId, rol, habilitadas, actor);
        UUID entradaId = pedido.etapas().stream()
                .filter(etapaDePedido -> etapaDePedido.numero() == pedido.etapaActual())
                .map(EtapaDePedido::entradaId)
                .findFirst()
                .orElseThrow();
        return new AutorizacionSubida(pedido.datosDelPedido(), interviniente, entradaId);
    }

    @Override
    public void autorizarConsulta(Solicitante solicitante, UUID pedidoId) {
        apoyo.lector(solicitante);
        if (consultas.porId(pedidoId).isEmpty()) {
            throw new NoEncontrado("El pedido no existe.");
        }
    }

    @Override
    public void autorizarDescarga(Solicitante solicitante, UUID pedidoId, int etapaDelArchivo) {
        ActorVigente actor = apoyo.solicitante(solicitante);
        Pedido pedido = consultas.porId(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        ContratoDeFlujo flujo = flujos.porId(pedido.flujoId());
        Map<Integer, CodigoRol> revisoras = new HashMap<>();
        for (ContratoDeEtapa etapa : flujo.etapas()) {
            if (Objects.equals(etapa.revisaEtapa(), etapaDelArchivo) && etapa.rolResponsable() != null) {
                revisoras.put(etapa.numero(), etapa.rolResponsable());
            }
        }
        CodigoRol rolDelArchivo = flujo.etapa(etapaDelArchivo).rolResponsable();
        if (!pedido.puedeDescargar(etapaDelArchivo, rolDelArchivo, revisoras, actor)) {
            throw new SinPermiso("No puedes descargar este archivo.");
        }
    }
}
