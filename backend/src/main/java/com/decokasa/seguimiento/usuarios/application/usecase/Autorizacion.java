package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Usuario;

/**
 * Permisos de la gestión de usuarios y roles, revisados dentro del caso de uso (R6). En la fase 1 la regla es
 * "solo el admin" (requisitos 1.3, 4.2 #3 y 4.6). Se decide con el estado actual del solicitante en la base (activo y
 * con el rol admin), no con los roles del token: un token de acceso sigue vigente hasta 15 minutos después de
 * desactivar la cuenta o quitarle el rol, y no debe servir para reactivarse ni repartir el rol admin (hallazgo A1).
 */
final class Autorizacion {

    static final CodigoRol ADMIN = new CodigoRol("admin");

    private final UsuarioRepositorio usuarios;

    Autorizacion(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    void exigirAdmin(Solicitante solicitante) {
        boolean esAdminHoy = solicitante != null && solicitante.usuarioId() != null
                && usuarios.buscarPorId(solicitante.usuarioId())
                        .filter(Usuario::puedeIniciarSesion)
                        .filter(usuario -> usuario.tieneRol(ADMIN))
                        .isPresent();
        if (!esAdminHoy) {
            throw new SinPermiso("Solo el admin gestiona usuarios y roles.");
        }
    }
}
