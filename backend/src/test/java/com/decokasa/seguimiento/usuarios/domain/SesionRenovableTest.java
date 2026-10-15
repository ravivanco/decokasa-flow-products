package com.decokasa.seguimiento.usuarios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// Token de renovación con vencimiento absoluto de 8 horas (ADR-0002, D3).
class SesionRenovableTest {

    private static final Instant INICIO = Instant.parse("2026-10-12T14:00:00Z");

    @Test
    void estaVigenteAntesDeSuVencimiento() {
        SesionRenovable sesion = sesion();

        assertThat(sesion.estaRevocada()).isFalse();
        assertThat(sesion.estaVigente(INICIO.plus(Duration.ofHours(7).plusMinutes(59)))).isTrue();
    }

    @Test
    void venceALasOchoHoras() {
        assertThat(sesion().estaVigente(INICIO.plus(Duration.ofHours(8)))).isFalse();
    }

    @Test
    void unaSesionRevocadaNoEstaVigente() {
        SesionRenovable sesion = sesion();

        sesion.revocar(INICIO.plus(Duration.ofMinutes(5)));

        assertThat(sesion.estaRevocada()).isTrue();
        assertThat(sesion.estaVigente(INICIO.plus(Duration.ofMinutes(6)))).isFalse();
    }

    @Test
    void revocarDosVecesConservaLaPrimeraFecha() {
        SesionRenovable sesion = sesion();

        sesion.revocar(INICIO.plus(Duration.ofMinutes(5)));
        sesion.revocar(INICIO.plus(Duration.ofMinutes(9)));

        assertThat(sesion.revocadoEn()).isEqualTo(INICIO.plus(Duration.ofMinutes(5)));
    }

    private static SesionRenovable sesion() {
        return new SesionRenovable(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hash-ficticio",
                INICIO, INICIO.plus(Duration.ofHours(8)), null);
    }
}
