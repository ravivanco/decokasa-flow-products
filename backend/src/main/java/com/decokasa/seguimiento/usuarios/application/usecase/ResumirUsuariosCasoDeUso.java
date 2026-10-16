package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resume a varias personas con una sola lectura. La empresa tiene pocos usuarios, así que se usa el listado completo
 * del repositorio en lugar de agregar una consulta por ids. Sin Spring (R2).
 */
public final class ResumirUsuariosCasoDeUso implements ResumirUsuarios {

    private final UsuarioRepositorio usuarios;

    public ResumirUsuariosCasoDeUso(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Map<UUID, Resumen> resumenes(Collection<UUID> usuarioIds) {
        Set<UUID> buscados = Set.copyOf(usuarioIds);
        Map<UUID, Resumen> resultado = new LinkedHashMap<>();
        if (buscados.isEmpty()) {
            return resultado;
        }
        for (Usuario usuario : usuarios.listar()) {
            if (buscados.contains(usuario.id())) {
                resultado.put(usuario.id(), new Resumen(usuario.id(), usuario.nombre(), usuario.roles(),
                        usuario.puedeIniciarSesion()));
            }
        }
        return resultado;
    }
}
