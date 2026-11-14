package com.decokasa.seguimiento.embarques.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.time.LocalDate;

/**
 * Llegada estimada de un BL. Manual si la escribe la persona; automática si hay salida y tránsito de referencia (el
 * dato de la etapa de llegada del flujo, 45 días corridos en Ecuador); sin datos, «Llegada por definir». Java puro (R1).
 */
public record LlegadaEstimada(LocalDate fecha, Origen origen, Integer transitoDias) {

    public enum Origen {
        AUTOMATICA,
        MANUAL,
        SIN_DATOS
    }

    /**
     * @param manual fecha escrita por la persona; se ignora si pide volver a la estimación automática
     * @param origenPedido AUTOMATICA para volver a calcularla; nulo o MANUAL conserva la fecha manual
     * @param transitoDias tránsito de referencia; nulo si el flujo no lo tiene
     */
    public static LlegadaEstimada resolver(LocalDate salida, LocalDate manual, Origen origenPedido,
            Integer transitoDias) {
        if (manual != null && origenPedido != Origen.AUTOMATICA) {
            if (salida != null && manual.isBefore(salida)) {
                throw new DatoInvalido("llegadaEstimada", "La llegada no puede ser anterior a la salida.");
            }
            return new LlegadaEstimada(manual, Origen.MANUAL, transitoDias);
        }
        if (salida != null && transitoDias != null) {
            return new LlegadaEstimada(salida.plusDays(transitoDias), Origen.AUTOMATICA, transitoDias);
        }
        return new LlegadaEstimada(null, Origen.SIN_DATOS, transitoDias);
    }
}
