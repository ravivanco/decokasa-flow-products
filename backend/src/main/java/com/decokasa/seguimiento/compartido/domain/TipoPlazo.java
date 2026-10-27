package com.decokasa.seguimiento.compartido.domain;

/**
 * Cómo se cuenta un plazo (contrato: TipoPlazo; columna definicion_etapa.plazo_tipo). Hábil significa de lunes a
 * viernes con la hora de Ecuador (ADR-0006). La jornada dentro del día y los feriados siguen pendientes (⏸).
 */
public enum TipoPlazo {
    HORAS_HABILES,
    DIAS_HABILES,
    DIAS_CORRIDOS
}
