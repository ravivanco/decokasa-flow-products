package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearRol;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.Set;
import java.util.UUID;

/** Alta de roles por el admin (R6; requisitos 1.3 y 4.6). */
public final class CrearRolCasoDeUso implements CrearRol {

    private final RolRepositorio roles;
    private final Autorizacion autorizacion;

    public CrearRolCasoDeUso(RolRepositorio roles, UsuarioRepositorio usuarios) {
        this.roles = roles;
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public Rol crear(Solicitante solicitante, Comando comando) {
        autorizacion.exigirAdmin(solicitante);
        String nombre = ValidacionRoles.nombreValido(comando.nombre());
        Set<String> permisos = ValidacionRoles.permisosValidos(comando.permisos());
        if (roles.buscarPorCodigo(comando.codigo()).isPresent()) {
            throw new DatoDuplicado("codigo", "Ya existe un rol con ese código.");
        }
        Rol rol = new Rol(UUID.randomUUID(), comando.codigo(), nombre, permisos);
        roles.guardar(rol);
        return rol;
    }
}
