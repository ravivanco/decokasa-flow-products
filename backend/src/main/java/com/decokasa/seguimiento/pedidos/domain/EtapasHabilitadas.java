package com.decokasa.seguimiento.pedidos.domain;

import java.util.Set;

/**
 * Etapas cuyas acciones están implementadas en esta fase (decisión P-04 del equipo, 2026-10-15). Los datos del flujo
 * A tienen las once etapas, pero en la fase 2 solo se ejecutan acciones con origen en A1 a A4 (incluido A4 → A5).
 * Las acciones de A5 en adelante se habilitan en su fase.
 */
public record EtapasHabilitadas(Set<Integer> numeros) {

    public EtapasHabilitadas {
        numeros = Set.copyOf(numeros);
    }

    /** Fase 2: A1, A2, A3 y A4. */
    public static EtapasHabilitadas faseDos() {
        return new EtapasHabilitadas(Set.of(1, 2, 3, 4));
    }

    public boolean incluye(int numeroEtapa) {
        return numeros.contains(numeroEtapa);
    }
}
