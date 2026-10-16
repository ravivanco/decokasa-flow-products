package com.decokasa.seguimiento.plazos.domain;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.plazos.application.usecase.CalcularVencimientoCasoDeUso;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalendarioFaseDosTest {
    private final CalcularVencimientoCasoDeUso calculo=new CalcularVencimientoCasoDeUso(
            new CalendarioLaboral(ZoneId.of("America/Guayaquil")));

    // Jornada de 24 h lunes-viernes y sin feriados: P-09 provisional, no decisión del cliente.
    @ParameterizedTest @CsvSource({
        "2026-10-16T23:00:00-05:00,4,2026-10-19T03:00:00-05:00",
        "2026-10-16T23:00:00-05:00,12,2026-10-19T11:00:00-05:00",
        "2026-10-16T23:00:00-05:00,24,2026-10-19T23:00:00-05:00",
        "2026-10-16T20:00:00-05:00,4,2026-10-19T00:00:00-05:00",
        "2026-10-17T10:00:00-05:00,4,2026-10-19T04:00:00-05:00",
        "2026-10-18T10:00:00-05:00,24,2026-10-20T00:00:00-05:00",
        "2026-10-15T22:00:00-05:00,4,2026-10-16T02:00:00-05:00"
    })
    void horasHabilesProvisionalesCruzanFinDeSemana(String inicio,int horas,String esperado) {
        assertThat(calculo.deEtapa(OffsetDateTime.parse(inicio).toInstant(),
                new PlazoDeEtapa.Definido(horas,TipoPlazo.HORAS_HABILES)))
                .contains(OffsetDateTime.parse(esperado).toInstant());
    }
    @Test void tresDiasHabilesConservanHoraEcuador() {
        assertThat(calculo.deEtapa(Instant.parse("2026-10-16T15:00:00Z"),
                new PlazoDeEtapa.Definido(3,TipoPlazo.DIAS_HABILES)))
                .contains(Instant.parse("2026-10-21T15:00:00Z"));
    }
    @Test void diasCorridosMuevenVencimientoDeSabadoALunes() {
        assertThat(calculo.deEtapa(Instant.parse("2026-10-16T15:00:00Z"),
                new PlazoDeEtapa.Definido(1,TipoPlazo.DIAS_CORRIDOS)))
                .contains(Instant.parse("2026-10-19T15:00:00Z"));
    }
    @Test void sinPlazoYPendienteSonTiposDistintosSinInventarVencimiento() {
        Instant inicio=Instant.parse("2026-10-15T15:00:00Z");
        assertThat(calculo.deEtapa(inicio,new PlazoDeEtapa.SinPlazo())).isEmpty();
        assertThat(calculo.deEtapa(inicio,new PlazoDeEtapa.Pendiente())).isEmpty();
        assertThat(new PlazoDeEtapa.SinPlazo()).isNotEqualTo(new PlazoDeEtapa.Pendiente());
        assertThatThrownBy(() -> new PlazoDeEtapa.Definido(0,TipoPlazo.HORAS_HABILES)).isInstanceOf(DatoInvalido.class);
    }
    @ParameterizedTest @CsvSource({"URGENTE,4", "PRIORITARIO,12", "NORMAL,24"})
    void urgenciasUsanHorasHabilesProvisionales(NivelUrgencia nivel,int horas) {
        assertThat(nivel.horas()).isEqualTo(horas);
        assertThat(calculo.deUrgencia(Instant.parse("2026-10-17T04:00:00Z"),nivel))
                .isEqualTo(Instant.parse("2026-10-17T04:00:00Z").plusSeconds((horas+48)*3600L));
    }
}
