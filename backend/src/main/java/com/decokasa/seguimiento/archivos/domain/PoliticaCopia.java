package com.decokasa.seguimiento.archivos.domain;

import java.time.Duration;
import java.util.List;

/** Inicial (1) + reintentos (2,3,4). El sondeo no duerme ni crea intentos adicionales. */
public record PoliticaCopia(Duration lease, List<Duration> esperas) {
    public PoliticaCopia {
        esperas=List.copyOf(esperas);
        if (lease==null || lease.compareTo(Duration.ofSeconds(60))<0 || lease.compareTo(Duration.ofMinutes(30))>0
                || esperas.size()!=3) { throw new IllegalArgumentException("Política de copia inválida"); }
        Duration previa=Duration.ZERO;
        for (Duration espera:esperas) {
            if (espera==null || espera.compareTo(previa)<=0 || espera.compareTo(Duration.ofDays(1))>0) {
                throw new IllegalArgumentException("Se requieren tres esperas positivas crecientes");
            }
            previa=espera;
        }
    }
    public Duration esperaDespuesDe(int intento) {
        if (intento<1 || intento>3) { throw new IllegalArgumentException("No hay más reintentos"); }
        return esperas.get(intento-1);
    }
}
