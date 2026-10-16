package com.decokasa.seguimiento.compartido.domain;

/**
 * Urgencia de una devolución por producto nuevo (requerimientos 1.4 y 4.2 #11; contrato: NivelUrgencia). El plazo
 * cubre Codificación y Revisión hasta volver a Análisis (A4 / B5). Las horas son del cliente; si son hábiles o
 * corridas sigue pendiente (⏸): el valor provisional documentado es "horas hábiles" (requerimientos §5,
 * arquitectura 8.1) y vive en el módulo de plazos.
 */
public enum NivelUrgencia {
    URGENTE(4),
    PRIORITARIO(12),
    NORMAL(24);

    private final int horas;

    NivelUrgencia(int horas) {
        this.horas = horas;
    }

    public int horas() {
        return horas;
    }
}
