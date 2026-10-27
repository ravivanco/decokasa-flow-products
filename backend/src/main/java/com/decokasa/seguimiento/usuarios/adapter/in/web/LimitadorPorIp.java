package com.decokasa.seguimiento.usuarios.adapter.in.web;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DemasiadasSolicitudes;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite por IP en memoria del proceso, con ventana fija (ADR-0033). Se pierde al reiniciar y, detrás de un proxy sin
 * cabeceras reenviadas de confianza, la IP es la del proxy: el límite pasa a ser global. Complementa, no sustituye,
 * al límite por cuenta y al bloqueo D2.
 */
final class LimitadorPorIp {

    private static final int MAXIMO_CLAVES = 10_000;

    private record Ventana(Instant inicio, int usadas) {
    }

    private final int maximo;
    private final Duration ventana;
    private final Reloj reloj;
    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();

    LimitadorPorIp(int maximo, Duration ventana, Reloj reloj) {
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    void consumir(String ip) {
        Instant ahora = reloj.ahora();
        if (ventanas.size() > MAXIMO_CLAVES) {
            ventanas.entrySet().removeIf(entrada -> !entrada.getValue().inicio().plus(ventana).isAfter(ahora));
        }
        Ventana actual = ventanas.compute(ip == null ? "desconocida" : ip, (clave, anterior) ->
                anterior == null || !anterior.inicio().plus(ventana).isAfter(ahora)
                        ? new Ventana(ahora, 1)
                        : new Ventana(anterior.inicio(), anterior.usadas() + 1));
        if (actual.usadas() > maximo) {
            throw new DemasiadasSolicitudes(Duration.between(ahora, actual.inicio().plus(ventana)).toSeconds());
        }
    }
}
