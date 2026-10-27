package com.decokasa.seguimiento.compartido.domain;

import java.util.Objects;

/**
 * Duración de referencia de una etapa sin plazo definido (columnas definicion_etapa.estimacion_*, V25; ADR-0030). Solo
 * sirve para estimar fechas en el cronograma: no es un plazo de responsabilidad, no genera vencimientos ni alertas y
 * no convierte en aprobado un plazo pendiente del cliente. Java puro (R1).
 */
public record EstimacionDeEtapa(int cantidad, TipoPlazo tipo, Fuente fuente) {

    public EstimacionDeEtapa {
        if (cantidad <= 0) {
            throw new DatoInvalido("Una duración de referencia debe ser mayor que cero.");
        }
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(fuente, "fuente");
    }

    /** De dónde sale la duración. */
    public enum Fuente {
        /** Calculadora de cronograma que ya usaba el cliente (requerimientos 1.8): 31 días de fabricación y 45 de tránsito. */
        CALCULADORA_ORIGINAL,
        /** Tabla 4.5 de requerimientos: 24 h para B2, propuesta del equipo sin confirmar (F6-D4). */
        REQUERIMIENTOS_4_5
    }
}
