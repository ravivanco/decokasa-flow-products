package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resume únicamente las personas solicitadas mediante una proyección por lote, sin cargar el listado
 * administrativo de usuarios, países ni credenciales. Sin Spring (R2).
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
        for (UsuarioRepositorio.Resumen usuario : usuarios.resumirPorIds(buscados)) {
            resultado.put(usuario.id(), new Resumen(usuario.id(), usuario.nombre(), usuario.roles(), usuario.activo()));
        }
        return resultado;
    }
}
