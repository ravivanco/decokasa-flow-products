package com.decokasa.seguimiento.compartido.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

// R5 (arquitectura.md): la fecha y la hora salen siempre del puerto Reloj, en la zona de Ecuador.
class RelojSistemaTest {

    private final Reloj reloj = new RelojSistema();

    @Test
    void usaLaZonaHorariaDeEcuador() {
        assertThat(reloj.zona()).isEqualTo(ZoneId.of("America/Guayaquil"));
    }

    @Test
    void devuelveElInstanteActual() {
        Instant antes = Instant.now();
        Instant ahora = reloj.ahora();
        assertThat(Duration.between(antes, ahora)).isBetween(Duration.ZERO, Duration.ofSeconds(5));
    }

    @Test
    void calculaLaFechaYHoraLocalDeEcuador() {
        assertThat(reloj.ahoraEnEcuador().getZone()).isEqualTo(ZoneId.of("America/Guayaquil"));
    }
}
