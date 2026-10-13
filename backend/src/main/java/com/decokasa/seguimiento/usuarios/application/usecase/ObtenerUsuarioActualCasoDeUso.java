package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.usuarios.application.port.in.ObtenerUsuarioActual;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import com.decokasa.seguimiento.usuarios.domain.SesionInvalida;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Usuario de la sesión con la unión de los permisos de sus roles (contrato: GET /auth/yo). */
public final class ObtenerUsuarioActualCasoDeUso implements ObtenerUsuarioActual {

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;

    public ObtenerUsuarioActualCasoDeUso(UsuarioRepositorio usuarios, RolRepositorio roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Override
    public UsuarioActual obtener(UUID usuarioId) {
        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .filter(Usuario::puedeIniciarSesion)
                .orElseThrow(SesionInvalida::new);
        Set<String> permisos = usuario.roles().stream()
                .map(roles::buscarPorCodigo)
                .flatMap(Optional::stream)
                .map(Rol::permisos)
                .flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableSet());
        return new UsuarioActual(usuario, permisos);
    }
}
