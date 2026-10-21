package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarCronograma;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.domain.DemoraAduana;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entrega a plazos los datos del pedido para su cronograma, con la visibilidad de P-13 (provisional del equipo): quien
 * tiene cuenta activa con roles vigentes ve cualquier pedido. Las fechas reales salen de la línea de tiempo del pedido;
 * la llegada real y la estimada, de los datos de aduana (ADR-0018, F4-D5). Sin Spring (R2).
 */
public final class ConsultarCronogramaCasoDeUso implements ConsultarCronograma {

    private final ConsultaDePedidos consultas;
    private final ConsultarDefinicionFlujo flujos;
    private final CalcularCronograma cronogramas;
    private final ApoyoPedidos apoyo;

    public ConsultarCronogramaCasoDeUso(ConsultaDePedidos consultas, ConsultarDefinicionFlujo flujos,
            CalcularCronograma cronogramas, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador) {
        this.consultas = consultas;
        this.flujos = flujos;
        this.cronogramas = cronogramas;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
    }

    @Override
    public CalcularCronograma.Resultado cronograma(Solicitante solicitante, UUID pedidoId) {
        apoyo.lector(solicitante);
        Pedido pedido = consultas.porId(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        ContratoDeFlujo flujo = flujos.porId(pedido.flujoId());
        List<CalcularCronograma.EtapaDelPedido> etapas = new ArrayList<>();
        for (ContratoDeEtapa definicion : flujo.etapas()) {
            EtapaDePedido etapa = pedido.etapas().stream()
                    .filter(e -> e.numero() == definicion.numero())
                    .findFirst()
                    .orElse(null);
            etapas.add(new CalcularCronograma.EtapaDelPedido(definicion.numero(), definicion.nombre(),
                    definicion.plazo(), definicion.registraLlegada(), etapa == null ? null : etapa.entradaEn(),
                    salida(pedido, etapa)));
        }
        return cronogramas.calcular(new CalcularCronograma.Datos(pedido.id(), pedido.creadoEn(), pedido.etapaActual(),
                pedido.finalizado(), etapas, pedido.llegadaAduanaEn().orElse(null),
                pedido.demora().map(DemoraAduana::nuevaFechaEstimadaLlegada).orElse(null)));
    }

    /**
     * Salida registrada de la etapa. La etapa final automática (Fin) no tiene salida: termina cuando el pedido se
     * finaliza, y esa es su fecha real.
     */
    private static Instant salida(Pedido pedido, EtapaDePedido etapa) {
        if (etapa == null) {
            return null;
        }
        if (etapa.salidaEn() == null && pedido.finalizado() && etapa.numero() == pedido.etapaActual()) {
            return pedido.finalizadoEn();
        }
        return etapa.salidaEn();
    }
}
