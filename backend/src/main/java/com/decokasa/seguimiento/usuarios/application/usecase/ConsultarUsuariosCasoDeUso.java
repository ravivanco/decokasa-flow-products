package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.List;
import java.util.UUID;

/** Lista y detalle de usuarios para el admin (R6). */
public final class ConsultarUsuariosCasoDeUso implements ConsultarUsuarios {

    private final UsuarioRepositorio usuarios;
    private final Autorizacion autorizacion;

    public ConsultarUsuariosCasoDeUso(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public List<Usuario> listar(Solicitante solicitante, Boolean activo) {
        autorizacion.exigirAdmin(solicitante);
        return usuarios.listar().stream()
                .filter(usuario -> activo == null || usuario.activo() == activo)
                .toList();
    }

    @Override
    public Usuario obtener(Solicitante solicitante, UUID usuarioId) {
        autorizacion.exigirAdmin(solicitante);
        return usuarios.buscarPorId(usuarioId).orElseThrow(() -> new NoEncontrado("El usuario no existe."));
    }
}
