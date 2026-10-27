package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Etapas cuyas acciones están habilitadas (decisión P-04 del equipo, 2026-10-15).
 *
 * <p>Desde la fase 6 (paso 6.4, F6-D3) la aplicación usa {@link #segunDefinicion()}: se habilitan las etapas manuales
 * (con rol responsable y no automáticas) de la definición del flujo de cada pedido. Así B11 Incidencias se habilita en
 * B sin habilitar el número 11 en A, donde es Fin, y ninguna etapa automática (A11, B12) admite acciones manuales.
 * Antes de consultar {@link #incluye(int)}, el caso de uso la concreta con {@link #para(Collection)} o
 * {@link #paraEtapa(int, boolean)}; sin concretar, {@code incluye} falla en lugar de habilitar algo.
 *
 * <p>Los conjuntos fijos por número (fases 2 a 4, solo flujo A) se conservan para las pruebas existentes; con ellos,
 * {@code para} y {@code paraEtapa} devuelven el mismo conjunto.
 *
 * @param porDefinicion la habilitación se calcula con la definición del flujo del pedido
 */
public record EtapasHabilitadas(Set<Integer> numeros, boolean porDefinicion) {

    public EtapasHabilitadas {
        numeros = Set.copyOf(numeros);
        if (porDefinicion && !numeros.isEmpty()) {
            throw new IllegalArgumentException("La habilitación según la definición no lleva números fijos.");
        }
    }

    /** Conjunto fijo de números de etapa (fases 2 a 4; pruebas). */
    public EtapasHabilitadas(Set<Integer> numeros) {
        this(numeros, false);
    }

    /** Fase 6: las etapas manuales de la definición del flujo de cada pedido. */
    public static EtapasHabilitadas segunDefinicion() {
        return new EtapasHabilitadas(Set.of(), true);
    }

    /**
     * La habilitación concreta para un pedido, con las etapas de la definición de su flujo: las que tienen rol
     * responsable y no son automáticas. Un conjunto fijo se devuelve igual.
     */
    public EtapasHabilitadas para(Collection<? extends ContratoDeEtapa> etapasDelFlujo) {
        if (!porDefinicion) {
            return this;
        }
        return new EtapasHabilitadas(etapasDelFlujo.stream()
                .filter(etapa -> etapa.rolResponsable() != null && !etapa.automatica())
                .map(ContratoDeEtapa::numero)
                .collect(Collectors.toSet()));
    }

    /**
     * La habilitación concreta de una sola etapa, cuando el caso de uso ya resolvió la acción con la definición (la
     * resolución solo existe para etapas no automáticas: {@code DefinicionFlujo} lo exige). Un conjunto fijo se
     * devuelve igual.
     *
     * @param manual la etapa tiene rol responsable en la definición
     */
    public EtapasHabilitadas paraEtapa(int numeroEtapa, boolean manual) {
        if (!porDefinicion) {
            return this;
        }
        return new EtapasHabilitadas(manual ? Set.of(numeroEtapa) : Set.of());
    }

    /** Fase 2: A1, A2, A3 y A4. */
    public static EtapasHabilitadas faseDos() {
        return new EtapasHabilitadas(Set.of(1, 2, 3, 4));
    }


    /**
     * Fase 4, paso 4.4: A1 a A9. Autorización de salida (A8) y Bodega (A9, con la bodega elegida al aprobar; F4-D2).
     * A9 → A10 sigue bloqueada mientras los documentos de Bodega estén pendientes del cliente (F4-D7). Incidencias (A10)
     * y lo que sigue se habilitan en el paso 4.5.
     */
    public static EtapasHabilitadas faseCuatroHastaBodega() {
        return new EtapasHabilitadas(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    public boolean incluye(int numeroEtapa) {
        if (porDefinicion) {
            throw new IllegalStateException(
                    "Concreta la habilitación con la definición del flujo del pedido antes de consultarla.");
        }
        return numeros.contains(numeroEtapa);
    }
}
