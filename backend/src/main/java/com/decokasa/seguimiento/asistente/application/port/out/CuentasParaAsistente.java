package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Estado vigente de la cuenta, leído de la base en cada llamada (lección A1; ADR-0024 para los países). */
public interface CuentasParaAsistente {

    /** Vacío si el usuario no existe. */
    Optional<EstadoCuenta> estado(UUID usuarioId);

    /** @param activa cuenta activa y con al menos un rol */
    record EstadoCuenta(boolean activa, Set<CodigoRol> roles, Set<CodigoPais> paises) {

        public EstadoCuenta {
            roles = Set.copyOf(roles);
            paises = Set.copyOf(paises);
        }
    }
}
