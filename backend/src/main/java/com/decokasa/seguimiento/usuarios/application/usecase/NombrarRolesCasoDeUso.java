package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.port.in.NombrarRoles;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Sin Spring (R2). */
public final class NombrarRolesCasoDeUso implements NombrarRoles {

    private final RolRepositorio roles;

    public NombrarRolesCasoDeUso(RolRepositorio roles) {
        this.roles = roles;
    }

    @Override
    public Map<CodigoRol, String> nombres(Collection<CodigoRol> codigos) {
        Set<CodigoRol> buscados = Set.copyOf(codigos);
        Map<CodigoRol, String> resultado = new LinkedHashMap<>();
        if (buscados.isEmpty()) {
            return resultado;
        }
        for (Rol rol : roles.listar()) {
            if (buscados.contains(rol.codigo())) {
                resultado.put(rol.codigo(), rol.nombre());
            }
        }
        return resultado;
    }
}
