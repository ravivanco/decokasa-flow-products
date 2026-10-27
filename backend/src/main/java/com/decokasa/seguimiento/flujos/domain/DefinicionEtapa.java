package com.decokasa.seguimiento.flujos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.EstimacionDeEtapa;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import java.util.Objects;

/**
 * Una etapa de un flujo (modelo-datos.md §3.2). El rol responsable es nulo solo en etapas automáticas como Fin
 * (decisión S-1 del equipo, 2026-10-15). Implementa {@link ContratoDeEtapa}, lo que otros módulos pueden leer (R4).
 * La duración de referencia (V25, ADR-0030) solo existe en etapas con plazo pendiente: un plazo definido siempre manda.
 * Java puro (R1).
 */
public record DefinicionEtapa(int numero, String nombre, CodigoRol rolResponsable, PlazoDeEtapa plazo,
        boolean automatica, Integer revisaEtapa, boolean registraLlegada, EstimacionDeEtapa estimacionDeReferencia)
        implements ContratoDeEtapa {

    public DefinicionEtapa {
        if (numero < 1 || numero > 30) {
            throw new DatoInvalido("El número de etapa debe estar entre 1 y 30.");
        }
        if (nombre == null || nombre.isBlank() || nombre.length() > 100) {
            throw new DatoInvalido("El nombre de la etapa debe tener entre 1 y 100 caracteres.");
        }
        if (rolResponsable == null && !automatica) {
            throw new DatoInvalido("Solo una etapa automática puede no tener rol responsable.");
        }
        Objects.requireNonNull(plazo, "plazo");
        if (revisaEtapa != null && (revisaEtapa < 1 || revisaEtapa > 30 || revisaEtapa == numero)) {
            throw new DatoInvalido("Una etapa solo revisa otra etapa existente del mismo flujo.");
        }
        if (registraLlegada && automatica) {
            throw new DatoInvalido("Una etapa automática no registra la llegada: nadie da su check.");
        }
        if (estimacionDeReferencia != null && !(plazo instanceof PlazoDeEtapa.Pendiente)) {
            throw new DatoInvalido("Solo una etapa con plazo pendiente tiene duración de referencia.");
        }
    }

    /** Etapa sin duración de referencia (V25). */
    public DefinicionEtapa(int numero, String nombre, CodigoRol rolResponsable, PlazoDeEtapa plazo,
            boolean automatica, Integer revisaEtapa, boolean registraLlegada) {
        this(numero, nombre, rolResponsable, plazo, automatica, revisaEtapa, registraLlegada, null);
    }

    /** Etapa que no registra la llegada a aduana (ADR-0018). */
    public DefinicionEtapa(int numero, String nombre, CodigoRol rolResponsable, PlazoDeEtapa plazo,
            boolean automatica, Integer revisaEtapa) {
        this(numero, nombre, rolResponsable, plazo, automatica, revisaEtapa, false, null);
    }

    /** Etapa que no revisa los archivos de otra. */
    public DefinicionEtapa(int numero, String nombre, CodigoRol rolResponsable, PlazoDeEtapa plazo,
            boolean automatica) {
        this(numero, nombre, rolResponsable, plazo, automatica, null, false, null);
    }
}
