package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarRoles;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.List;

/** Lista de roles para el admin (R6). */
public final class ConsultarRolesCasoDeUso implements ConsultarRoles {

    private final RolRepositorio roles;
    private final Autorizacion autorizacion;

    public ConsultarRolesCasoDeUso(RolRepositorio roles, UsuarioRepositorio usuarios) {
        this.roles = roles;
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public List<Rol> listar(Solicitante solicitante) {
        autorizacion.exigirAdmin(solicitante);
        return roles.listar();
    }
}
