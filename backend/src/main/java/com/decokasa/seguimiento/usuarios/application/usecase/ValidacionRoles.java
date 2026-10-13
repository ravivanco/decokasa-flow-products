package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import java.util.Set;

/** Reglas comunes de los roles: existen, y nombre y permisos dentro de los límites (modelo-datos.md 3.1). */
final class ValidacionRoles {

    private ValidacionRoles() {
    }

    /** Cada rol asignado debe existir en el catálogo. La lista vacía la rechaza el dominio. */
    static void exigirExistentes(RolRepositorio roles, Set<CodigoRol> codigos) {
        if (codigos == null) {
            return;
        }
        for (CodigoRol codigo : codigos) {
            if (roles.buscarPorCodigo(codigo).isEmpty()) {
                throw new DatoInvalido("roles", "El rol " + codigo.valor() + " no existe.");
            }
        }
    }

    static String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 100) {
            throw new DatoInvalido("nombre", "El nombre del rol debe tener de 1 a 100 caracteres.");
        }
        return nombre.strip();
    }

    static Set<String> permisosValidos(Set<String> permisos) {
        if (permisos == null) {
            return Set.of();
        }
        for (String permiso : permisos) {
            if (permiso == null || permiso.isBlank() || permiso.length() > 100) {
                throw new DatoInvalido("permisos", "Cada permiso debe tener de 1 a 100 caracteres.");
            }
        }
        return Set.copyOf(permisos);
    }
}
