package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.usuarios.application.port.in.IdentificarUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Optional;
import java.util.UUID;

/** Solo el nombre de usuario; sin Spring (R2). */
public final class IdentificarUsuariosCasoDeUso implements IdentificarUsuarios {

    private final UsuarioRepositorio usuarios;

    public IdentificarUsuariosCasoDeUso(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<String> nombreDeUsuario(UUID usuarioId) {
        return usuarios.buscarPorId(usuarioId).map(Usuario::usuario);
    }
}
