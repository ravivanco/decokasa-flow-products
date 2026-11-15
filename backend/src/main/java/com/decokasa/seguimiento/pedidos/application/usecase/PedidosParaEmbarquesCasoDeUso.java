package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.EstimacionDeEtapa;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.TipoPlazo;
import com.decokasa.seguimiento.compartido.domain.TransicionNoPermitida;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.embarques.application.port.out.PedidosParaEmbarques;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.AutorizarArchivosDePedido;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.pedidos.domain.PermisosEtapa;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.Optional;
import java.util.UUID;

/**
 * Reglas de pedidos que usa {@code embarques}. Sin transacción propia: corre en la del caso de uso que la llama, y
 * {@code archivos} es la instancia directa, sin envoltura, para que una negativa esperada no marque esa transacción.
 * Sin Spring (R2).
 */
public final class PedidosParaEmbarquesCasoDeUso implements PedidosParaEmbarques {

    private final PedidoRepositorio pedidos;
    private final ConsultaDePedidos consultas;
    private final ConsultarDefinicionFlujo flujos;
    private final AutorizarArchivosDePedido archivos;
    private final ApoyoPedidos apoyo;

    public PedidosParaEmbarquesCasoDeUso(PedidoRepositorio pedidos, ConsultaDePedidos consultas,
            ConsultarDefinicionFlujo flujos, AutorizarArchivosDePedido archivos, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador) {
        this.pedidos = pedidos;
        this.consultas = consultas;
        this.flujos = flujos;
        this.archivos = archivos;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
    }

    @Override
    public Gestion autorizarGestion(Solicitante solicitante, UUID pedidoId) {
        return autorizar(solicitante, pedidoId, true);
    }

    @Override
    public Gestion autorizarRevision(Solicitante solicitante, UUID pedidoId) {
        return autorizar(solicitante, pedidoId, false);
    }

    private Gestion autorizar(Solicitante solicitante, UUID pedidoId, boolean gestion) {
        Pedido pedido = pedidos.buscarParaActualizar(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        ActorVigente actor = apoyo.solicitante(solicitante);
        apoyo.exigirPais(actor, pedido);
        ContratoDeFlujo flujo = flujos.porId(pedido.flujoId());
        ContratoDeEtapa etapa = flujo.etapa(pedido.etapaActual());
        if (pedido.finalizado() || (gestion ? !etapa.gestionaEmbarques() : !etapa.apruebaEmbarques())) {
            throw new TransicionNoPermitida(gestion ? "Los BL solo se gestionan en la etapa de embarques del pedido."
                    : "Esta revisión solo se hace en la etapa de aprobación del pedido.");
        }
        Interviniente interviniente = PermisosEtapa.paraActuar(actual(pedido), etapa.rolResponsable(), actor);
        return new Gestion(referencia(pedido, flujo), interviniente);
    }

    @Override
    public Referencia autorizarConsulta(Solicitante solicitante, UUID pedidoId) {
        ActorVigente actor = apoyo.lector(solicitante);
        Pedido pedido = consultas.porId(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        apoyo.exigirPais(actor, pedido);
        return referencia(pedido, flujos.porId(pedido.flujoId()));
    }

    @Override
    public boolean puedeActuar(Solicitante solicitante, UUID pedidoId) {
        ActorVigente actor = apoyo.lector(solicitante);
        Pedido pedido = consultas.porId(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        CodigoRol rol = flujos.porId(pedido.flujoId()).etapa(pedido.etapaActual()).rolResponsable();
        return !pedido.finalizado() && rol != null && PermisosEtapa.puedeActuar(actual(pedido), rol, actor);
    }

    @Override
    public void autorizarPackingList(Solicitante solicitante, UUID pedidoId, UUID pedidoDelArchivo,
            int etapaDelArchivo) {
        try {
            archivos.autorizarAperturaDesdeEtapaActual(solicitante, pedidoId);
        } catch (SinPermiso sinEtapaActual) {
            archivos.autorizarDescarga(solicitante, pedidoDelArchivo, etapaDelArchivo);
        }
    }

    /** En un flujo con BL, el pedido debe estar en esa etapa; un pedido anterior (v1) se vincula en cualquier etapa. */
    @Override
    public Optional<Referencia> vinculable(Solicitante solicitante, String numero) {
        ActorVigente actor = apoyo.lector(solicitante);
        String limpio = numero == null ? "" : numero.strip().toUpperCase(java.util.Locale.ROOT);
        return consultas.porNumero(limpio)
                .filter(pedido -> actor.veePais(pedido.pais()) && !pedido.finalizado())
                .map(pedido -> referencia(pedido, flujos.porId(pedido.flujoId())))
                .filter(referencia -> !referencia.flujoGestiona() || referencia.etapaGestiona());
    }

    @Override
    public Referencia referencia(UUID pedidoId) {
        Pedido pedido = consultas.porId(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        return referencia(pedido, flujos.porId(pedido.flujoId()));
    }

    private static Referencia referencia(Pedido pedido, ContratoDeFlujo flujo) {
        ContratoDeEtapa etapa = flujo.etapa(pedido.etapaActual());
        boolean flujoGestiona = flujo.etapas().stream().anyMatch(ContratoDeEtapa::gestionaEmbarques);
        return new Referencia(pedido.datosDelPedido(), pedido.flujoId(), pedido.etapaActual(),
                actual(pedido).entradaId(), pedido.finalizado(), flujoGestiona, etapa.gestionaEmbarques(),
                etapa.apruebaEmbarques(), etapa.checklistDocumentos(), transito(flujo));
    }

    private static Integer transito(ContratoDeFlujo flujo) {
        return flujo.etapas().stream()
                .filter(ContratoDeEtapa::registraLlegada)
                .map(ContratoDeEtapa::estimacionDeReferencia)
                .filter(estimacion -> estimacion != null && estimacion.tipo() == TipoPlazo.DIAS_CORRIDOS)
                .map(EstimacionDeEtapa::cantidad)
                .findFirst()
                .orElse(null);
    }

    private static EtapaDePedido actual(Pedido pedido) {
        return pedido.etapas().stream()
                .filter(etapa -> etapa.numero() == pedido.etapaActual())
                .findFirst()
                .orElseThrow();
    }
}
