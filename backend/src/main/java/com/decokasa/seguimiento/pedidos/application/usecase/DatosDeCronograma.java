package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.pedidos.domain.DemoraAduana;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Datos de un pedido para plazos (cronograma y planificación original; ADR-0030). Las fechas reales salen de la línea
 * de tiempo del pedido; la llegada real y la estimada, de los datos de aduana (ADR-0018, F4-D5); las duraciones, de la
 * versión del flujo que sigue el pedido. Sin Spring (R2).
 */
final class DatosDeCronograma {

    private DatosDeCronograma() {
    }

    /** @param ahora momento del cálculo según el reloj del servidor */
    static CalcularCronograma.Datos de(Pedido pedido, ContratoDeFlujo flujo, Instant ahora) {
        List<CalcularCronograma.EtapaDelPedido> etapas = new ArrayList<>();
        for (ContratoDeEtapa definicion : flujo.etapas()) {
            EtapaDePedido etapa = pedido.etapas().stream()
                    .filter(e -> e.numero() == definicion.numero())
                    .findFirst()
                    .orElse(null);
            etapas.add(new CalcularCronograma.EtapaDelPedido(definicion.numero(), definicion.nombre(),
                    definicion.plazo(), definicion.registraLlegada(), etapa == null ? null : etapa.entradaEn(),
                    salida(pedido, etapa), definicion.estimacionDeReferencia()));
        }
        return new CalcularCronograma.Datos(pedido.id(), pedido.creadoEn(), pedido.etapaActual(), pedido.finalizado(),
                etapas, pedido.llegadaAduanaEn().orElse(null),
                pedido.demora().map(DemoraAduana::nuevaFechaEstimadaLlegada).orElse(null), ahora);
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
