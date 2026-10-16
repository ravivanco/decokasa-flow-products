package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Collection;
import java.util.Map;

/**
 * Nombre visible de cada rol (tabla rol), para textos de otros módulos como el resumen de etapa. Solo lectura y sin
 * control de acceso: el nombre de un rol no es un dato sensible y quien lo usa ya autorizó la consulta.
 */
public interface NombrarRoles {

    /** Los códigos que no existen no aparecen en el resultado. */
    Map<CodigoRol, String> nombres(Collection<CodigoRol> codigos);
}
