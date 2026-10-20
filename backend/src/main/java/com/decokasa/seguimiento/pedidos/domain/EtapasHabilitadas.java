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

    /**
     * Fase 4, paso 4.2: A1 a A6. Fabricación (A5) avanza a Embarque con observación y Embarque (A6) acepta archivos y
     * su acción, que sigue bloqueada mientras sus documentos estén pendientes del cliente (F4-D4, S-2). Aduana (A7) y lo
     * que sigue se habilitan en los pasos 4.3 a 4.5.
     */
    public static EtapasHabilitadas faseCuatroHastaEmbarque() {
        return new EtapasHabilitadas(Set.of(1, 2, 3, 4, 5, 6));
    }

    /**
     * Fase 4, paso 4.3: A1 a A7. Aduana (A7) reporta demoras y da el check de llegada (A7 → A8). Las acciones de A8 en
     * adelante se habilitan en los pasos 4.4 y 4.5. A7 solo se alcanza si A6 avanza, y A6 sigue bloqueada mientras sus
     * documentos estén pendientes (F4-D4).
     */
    public static EtapasHabilitadas faseCuatroHastaAduana() {
        return new EtapasHabilitadas(Set.of(1, 2, 3, 4, 5, 6, 7));
    }

    /**
     * Fase 4, paso 4.4: A1 a A9. Autorización de salida (A8) y Bodega (A9, con la bodega elegida al aprobar; F4-D2).
     * A9 → A10 sigue bloqueada mientras los documentos de Bodega estén pendientes del cliente (F4-D7). Incidencias (A10)
     * y lo que sigue se habilitan en el paso 4.5.
     */
    public static EtapasHabilitadas faseCuatroHastaBodega() {
        return new EtapasHabilitadas(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    /**
     * Paso 4.5: A1 a A10. A6, A9 y A10 conservan sus requisitos documentales pendientes.
     * A11 no se habilita: se completa automáticamente al confirmar A10 cuando sus requisitos estén definidos.
     */
    public static EtapasHabilitadas faseCuatroHastaIncidencias() {
        return new EtapasHabilitadas(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    public boolean incluye(int numeroEtapa) {
        return numeros.contains(numeroEtapa);
    }
}
