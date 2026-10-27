package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.Set;
import java.util.UUID;

/**
 * Reemplaza código, nombre y permisos de un rol; solo el admin (contrato: PUT /roles/{rolId}).
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si el solicitante no es admin
 * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el rol no existe
 * @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado si el código es de otro rol
 */
public interface ActualizarRol {

    Rol actualizar(Solicitante solicitante, Comando comando);

    record Comando(UUID rolId, CodigoRol codigo, String nombre, Set<String> permisos) {
    }
}
