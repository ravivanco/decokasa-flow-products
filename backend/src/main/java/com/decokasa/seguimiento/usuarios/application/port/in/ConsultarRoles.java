package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.List;

/**
 * Lista de roles; solo el admin (contrato: GET /roles).
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si el solicitante no es admin
 */
public interface ConsultarRoles {

    List<Rol> listar(Solicitante solicitante);
}
