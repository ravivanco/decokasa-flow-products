package com.decokasa.seguimiento.historial.domain;

import com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Detalle propio de cada tipo de evento (columna evento_pedido.datos, jsonb). Cada campo es nulo cuando el tipo no lo
 * usa.
 *
 * @param etapaDestino etapa a la que pasó el pedido en un movimiento
 * @param motivoDevolucion por qué se devolvió (PEDIDO_DEVUELTO)
 * @param urgenciaTerminada el movimiento devolvió el pedido a la etapa de retorno y terminó la urgencia
 * @param asignadoAnterior quién tenía la etapa antes de reasignarla; nulo si nadie
 * @param asignadoA quién la recibió (ETAPA_REASIGNADA)
 * @param claseVencimiento ETAPA o URGENCIA (PLAZO_VENCIDO, paso 5.2)
 * @param venceEn vencimiento que se incumplió (PLAZO_VENCIDO)
 * @param entradaId entrada de la etapa (PLAZO_VENCIDO de ETAPA y MOTIVO_RETRASO_REGISTRADO)
 * @param urgenciaActivacionId entrada que activó la urgencia (PLAZO_VENCIDO de URGENCIA)
 */
public record DetalleEvento(Integer etapaDestino, MotivoDevolucion motivoDevolucion, Boolean urgenciaTerminada,
        UUID asignadoAnterior, UUID asignadoA, LocalDate fechaEstimadaAnterior, LocalDate fechaEstimadaSolicitada,
        LocalDate nuevaFechaEstimadaLlegada, UUID bodegaId, String claseVencimiento, Instant venceEn,
        UUID entradaId, UUID urgenciaActivacionId) {

    static final DetalleEvento VACIO = new DetalleEvento(null, null, null, null, null);

    /** Sin datos de vencimiento: todos los tipos salvo PLAZO_VENCIDO y MOTIVO_RETRASO_REGISTRADO. */
    public DetalleEvento(Integer etapaDestino, MotivoDevolucion motivoDevolucion, Boolean urgenciaTerminada,
            UUID asignadoAnterior, UUID asignadoA, LocalDate fechaEstimadaAnterior, LocalDate fechaEstimadaSolicitada,
            LocalDate nuevaFechaEstimadaLlegada, UUID bodegaId) {
        this(etapaDestino, motivoDevolucion, urgenciaTerminada, asignadoAnterior, asignadoA, fechaEstimadaAnterior,
                fechaEstimadaSolicitada, nuevaFechaEstimadaLlegada, bodegaId, null, null, null, null);
    }

    /** PLAZO_VENCIDO (5.2): qué plazo venció y cuándo, con la entrada o la activación de urgencia que lo identifica. */
    static DetalleEvento vencimiento(String clase, Instant venceEn, UUID entradaId, UUID urgenciaActivacionId) {
        return new DetalleEvento(null, null, null, null, null, null, null, null, null, clase, venceEn, entradaId,
                urgenciaActivacionId);
    }

    /** MOTIVO_RETRASO_REGISTRADO (5.2): la entrada a la que pertenece el motivo; el texto va en la observación. */
    static DetalleEvento motivo(UUID entradaId) {
        return new DetalleEvento(null, null, null, null, null, null, null, null, null, null, null, entradaId, null);
    }

    /** Sin fechas de demora: todos los tipos salvo DEMORA_REPORTADA. */
    public DetalleEvento(Integer etapaDestino, MotivoDevolucion motivoDevolucion, Boolean urgenciaTerminada,
            UUID asignadoAnterior, UUID asignadoA) {
        this(etapaDestino, motivoDevolucion, urgenciaTerminada, asignadoAnterior, asignadoA, null, null, null, null);
    }

    /** Sin bodega: todos los tipos salvo BODEGA_ASIGNADA. */
    public DetalleEvento(Integer etapaDestino, MotivoDevolucion motivoDevolucion, Boolean urgenciaTerminada,
            UUID asignadoAnterior, UUID asignadoA, LocalDate fechaEstimadaAnterior, LocalDate fechaEstimadaSolicitada,
            LocalDate nuevaFechaEstimadaLlegada) {
        this(etapaDestino, motivoDevolucion, urgenciaTerminada, asignadoAnterior, asignadoA, fechaEstimadaAnterior,
                fechaEstimadaSolicitada, nuevaFechaEstimadaLlegada, null);
    }

    /** BODEGA_ASIGNADA (F4-D2): la bodega elegida al aprobar Bodega. */
    static DetalleEvento bodega(UUID bodegaId) {
        return new DetalleEvento(null, null, null, null, null, null, null, null, bodegaId);
    }

    /**
     * DEMORA_REPORTADA (F4-D5): la estimación vigente antes de la demora (nula si es la primera), la fecha que escribió
     * la persona solo si es distinta de la guardada (pasó al lunes) y la fecha guardada.
     */
    static DetalleEvento demora(LocalDate anterior, LocalDate solicitada, LocalDate nueva) {
        return new DetalleEvento(null, null, null, null, null, anterior, solicitada, nueva);
    }

    static DetalleEvento movimiento(int etapaDestino, boolean urgenciaTerminada) {
        return new DetalleEvento(etapaDestino, null, urgenciaTerminada ? Boolean.TRUE : null, null, null);
    }
}
