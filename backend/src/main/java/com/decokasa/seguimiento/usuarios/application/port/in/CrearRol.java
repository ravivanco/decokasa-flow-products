package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.Set;

/**
 * Alta de un rol; solo el admin (requisitos 1.3 y 4.6). La lista de permisos sigue pendiente de definir.
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si el solicitante no es admin
 * @throws com.decokasa.seguimiento.compartido.domain.DatoInvalido nombre vacío o de más de 100 caracteres
 * @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado código ya registrado
 */
public interface CrearRol {

    Rol crear(Solicitante solicitante, Comando comando);

    record Comando(CodigoRol codigo, String nombre, Set<String> permisos) {
    }
}
