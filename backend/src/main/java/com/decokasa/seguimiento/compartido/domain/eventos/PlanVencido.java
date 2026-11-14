package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * La etapa actual superó la fecha planificada al crear el pedido (tipo PLAN_VENCIDO, evento del sistema, V34). Es
 * distinto de PLAZO_VENCIDO, que cuenta desde la entrada a la etapa. Se registra una sola vez por pedido y etapa.
 */
public record PlanVencido(DatosDelPedido pedido, int etapa, String nombreEtapa, LocalDate fechaPlanificada,
        Instant ocurridoEn) implements EventoDePedido {

    public PlanVencido {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(fechaPlanificada, "fechaPlanificada");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
    }

    public String texto() {
        return "La etapa " + (nombreEtapa == null ? "" : nombreEtapa + " ") + "superó su fecha planificada ("
                + fechaPlanificada.getDayOfMonth() + "/" + fechaPlanificada.getMonthValue() + "/"
                + fechaPlanificada.getYear() + ").";
    }

    @Override public UUID pedidoId() { return pedido.pedidoId(); }

    @Override public String numeroPedido() { return pedido.numeroPedido(); }

    @Override public CodigoPais pais() { return pedido.pais(); }

    @Override public TipoFlujo tipoFlujo() { return pedido.tipoFlujo(); }

    @Override public Interviniente actor() { return null; }
}
