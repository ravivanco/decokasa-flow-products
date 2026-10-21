package com.decokasa.seguimiento.pedidos.domain;
import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class Fase5ReentradaTest {
    @Test void reentrarLimpiaMotivoReiniciaPlazoYConservaAsignacion(){
        Instant antes=Instant.parse("2026-10-16T15:00:00Z"),ahora=Instant.parse("2026-10-20T15:00:00Z");UUID entrada=UUID.randomUUID(),persona=UUID.randomUUID();
        var etapa=new EtapaDePedido(UUID.randomUUID(),8,EstadoEtapa.DEVUELTA,persona,antes,antes.plusSeconds(100),antes.plusSeconds(1),0,entrada,"Motivo anterior ficticio");
        etapa.entrar(ahora,ahora.plusSeconds(86400));assertThat(etapa.motivoRetraso()).isNull();assertThat(etapa.entradaId()).isNotEqualTo(entrada);assertThat(etapa.reentradas()).isEqualTo(1);assertThat(etapa.entradaEn()).isEqualTo(ahora);assertThat(etapa.venceEn()).isEqualTo(ahora.plusSeconds(86400));assertThat(etapa.asignadoA()).isEqualTo(persona);assertThat(etapa.salidaEn()).isNull();
    }
}
