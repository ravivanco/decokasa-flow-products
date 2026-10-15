package com.decokasa.seguimiento.compartido.adapter.out;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import java.time.Clock;
import java.time.Instant;

/** Adaptador del puerto Reloj con la hora real del sistema (en UTC; la zona de Ecuador la aplica el puerto). */
public final class RelojSistema implements Reloj {

    private final Clock reloj = Clock.systemUTC();

    @Override
    public Instant ahora() {
        return reloj.instant();
    }
}
