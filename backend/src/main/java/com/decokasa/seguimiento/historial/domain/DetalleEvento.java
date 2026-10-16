package com.decokasa.seguimiento.historial.domain;

import com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion;
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
 */
public record DetalleEvento(Integer etapaDestino, MotivoDevolucion motivoDevolucion, Boolean urgenciaTerminada,
        UUID asignadoAnterior, UUID asignadoA) {

    static final DetalleEvento VACIO = new DetalleEvento(null, null, null, null, null);

    static DetalleEvento movimiento(int etapaDestino, boolean urgenciaTerminada) {
        return new DetalleEvento(etapaDestino, null, urgenciaTerminada ? Boolean.TRUE : null, null, null);
    }
}
