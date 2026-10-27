package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Optional;
import java.util.UUID;

/** Entrega el estado actual del usuario en la base, sin datos personales ni credenciales. Sin Spring (R2). */
public final class ConsultarResponsablesCasoDeUso implements ConsultarResponsables {

    private final UsuarioRepositorio usuarios;

    public ConsultarResponsablesCasoDeUso(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<EstadoVigente> estado(UUID usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }
        return usuarios.buscarPorId(usuarioId)
                .map(usuario -> new EstadoVigente(usuario.id(), usuario.puedeIniciarSesion(), usuario.roles(),
                        usuario.paises()));
    }
}
