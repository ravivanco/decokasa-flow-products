package com.decokasa.seguimiento.compartido.adapter.out;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import java.time.Duration;
import java.time.Instant;

/** Reloj de prueba: marca la hora que diga la prueba y se puede adelantar (R5). */
public final class RelojFijo implements Reloj {

    private Instant ahora;

    public RelojFijo(Instant ahora) {
        this.ahora = ahora;
    }

    @Override
    public Instant ahora() {
        return ahora;
    }

    public void adelantar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }
}
