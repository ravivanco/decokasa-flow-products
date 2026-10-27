package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Se reportó una demora en aduana (tipo DEMORA_REPORTADA; F4-D5, provisional del equipo). No mueve el pedido ni cambia
 * plazos: la fecha es una estimación de llegada, no un vencimiento.
 *
 * @param observacion el motivo de la demora
 * @param fechaEstimadaAnterior la estimación vigente antes de esta demora; nula si es la primera
 * @param fechaSolicitada la fecha que escribió la persona, solo si es distinta de la guardada (cayó en fin de semana y
 *        pasó al lunes); nula si coinciden
 * @param nuevaFechaEstimadaLlegada la fecha guardada
 */
public record DemoraReportada(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn,
        String observacion, LocalDate fechaEstimadaAnterior, LocalDate fechaSolicitada,
        LocalDate nuevaFechaEstimadaLlegada, List<UUID> adjuntoIds)
        implements EventoDePedido {

    public DemoraReportada {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(observacion, "observacion");
        Objects.requireNonNull(nuevaFechaEstimadaLlegada, "nuevaFechaEstimadaLlegada");
        if (nuevaFechaEstimadaLlegada.equals(fechaSolicitada)) {
            throw new IllegalArgumentException("La fecha solicitada solo se guarda si es distinta de la efectiva.");
        }
        adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
    }

    @Override
    public UUID pedidoId() {
        return pedido.pedidoId();
    }

    @Override
    public String numeroPedido() {
        return pedido.numeroPedido();
    }

    @Override
    public CodigoPais pais() {
        return pedido.pais();
    }

    @Override
    public TipoFlujo tipoFlujo() {
        return pedido.tipoFlujo();
    }
}
