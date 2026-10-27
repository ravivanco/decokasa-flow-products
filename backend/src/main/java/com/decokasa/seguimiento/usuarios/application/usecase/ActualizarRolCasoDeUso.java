package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarRol;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.Set;

/** Reemplazo de los datos de un rol por el admin (R6; contrato: PUT /roles/{rolId}). */
public final class ActualizarRolCasoDeUso implements ActualizarRol {

    private final RolRepositorio roles;
    private final Autorizacion autorizacion;

    public ActualizarRolCasoDeUso(RolRepositorio roles, UsuarioRepositorio usuarios) {
        this.roles = roles;
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public Rol actualizar(Solicitante solicitante, Comando comando) {
        autorizacion.exigirAdmin(solicitante);
        Rol actual = roles.buscarPorId(comando.rolId()).orElseThrow(() -> new NoEncontrado("El rol no existe."));
        String nombre = ValidacionRoles.nombreValido(comando.nombre());
        Set<String> permisos = ValidacionRoles.permisosValidos(comando.permisos());
        roles.buscarPorCodigo(comando.codigo())
                .filter(otro -> !otro.id().equals(actual.id()))
                .ifPresent(otro -> {
                    throw new DatoDuplicado("codigo", "Ya existe un rol con ese código.");
                });
        Rol actualizado = new Rol(actual.id(), comando.codigo(), nombre, permisos);
        roles.guardar(actualizado);
        return actualizado;
    }
}
