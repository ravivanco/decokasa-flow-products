package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import java.util.Map;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Traduce a {@link DatoDuplicado} las violaciones de las tres restricciones únicas de la tabla usuario (V1), con el
 * campo repetido que pide el contrato (D9). Hace falta cuando dos altas o ediciones simultáneas pasan la comprobación
 * previa del caso de uso y la base rechaza la segunda (hallazgo M2, duplicados). Solo se traduce una violación de
 * unicidad (SQLState 23505) de una restricción conocida; cualquier otro error de integridad se deja seguir y termina
 * en un 500 sin detalles. El mensaje es el del dominio: no lleva nombres ni textos de la base.
 */
final class RestriccionesUnicasUsuario {

    private static final String VIOLACION_DE_UNICIDAD = "23505";

    private record Duplicado(String campo, String mensaje) {
    }

    private static final Map<String, Duplicado> CONOCIDAS = Map.of(
            "usuario_correo_unico", new Duplicado("correo", "Ya existe un usuario con ese correo."),
            "usuario_usuario_unico", new Duplicado("usuario", "Ya existe un usuario con ese nombre de usuario."),
            "usuario_telefono_unico", new Duplicado("telefono", "Ya existe un usuario con ese teléfono."));

    private RestriccionesUnicasUsuario() {
    }

    static Optional<DatoDuplicado> comoDuplicado(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                if (!VIOLACION_DE_UNICIDAD.equals(violacion.getSQLState()) || violacion.getConstraintName() == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(CONOCIDAS.get(violacion.getConstraintName()))
                        .map(duplicado -> new DatoDuplicado(duplicado.campo(), duplicado.mensaje()));
            }
        }
        return Optional.empty();
    }
}
