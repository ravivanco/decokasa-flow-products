package com.decokasa.seguimiento.flujos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import java.util.Objects;

/**
 * Una etapa de un flujo (modelo-datos.md §3.2). El rol responsable es nulo solo en etapas automáticas como Fin
 * (decisión S-1 del equipo, 2026-10-15). Implementa {@link ContratoDeEtapa}, lo que otros módulos pueden leer (R4).
 * Java puro (R1).
 */
public record DefinicionEtapa(int numero, String nombre, CodigoRol rolResponsable, PlazoDeEtapa plazo,
        boolean automatica, Integer revisaEtapa) implements ContratoDeEtapa {

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
    }

    /** Etapa que no revisa los archivos de otra. */
    public DefinicionEtapa(int numero, String nombre, CodigoRol rolResponsable, PlazoDeEtapa plazo,
            boolean automatica) {
        this(numero, nombre, rolResponsable, plazo, automatica, null);
    }
}
