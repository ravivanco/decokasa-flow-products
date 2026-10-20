package com.decokasa.seguimiento.compartido.domain.flujo;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;

/**
 * Lo que otro módulo puede leer de una etapa de un flujo (R4: contrato compartido, como los eventos de P-15). Lo
 * implementa el dominio de {@code flujos}; {@code pedidos} lo usa sin conocer ese dominio. Java puro (R1).
 */
public interface ContratoDeEtapa {

    int numero();

    String nombre();

    /** Nulo solo en etapas automáticas, como Fin (S-1). */
    CodigoRol rolResponsable();

    PlazoDeEtapa plazo();

    /** Etapa cuyos archivos revisa esta etapa (P-17); nula si no revisa ninguna. Se declara en los datos del flujo. */
    Integer revisaEtapa();

    /**
     * Etapa de llegada a aduana (ADR-0018): al aprobarla, el pedido registra la llegada. Se declara en los datos del
     * flujo; no se deduce de otras reglas.
     */
    default boolean registraLlegada() {
        return false;
    }
}
