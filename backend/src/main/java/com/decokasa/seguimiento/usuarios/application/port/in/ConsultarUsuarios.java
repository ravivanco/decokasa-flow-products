package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.List;
import java.util.UUID;

/**
 * Lista y detalle de usuarios; solo el admin (contrato: GET /usuarios y GET /usuarios/{id}).
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si el solicitante no es admin
 */
public interface ConsultarUsuarios {

    /** @param activo filtro opcional; {@code null} devuelve todos */
    List<Usuario> listar(Solicitante solicitante, Boolean activo);

    /** @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si no existe */
    Usuario obtener(Solicitante solicitante, UUID usuarioId);
}
