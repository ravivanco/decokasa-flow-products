package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Estado vigente de un usuario para decidir quién actúa o recibe una etapa (paso 2.3; arquitectura.md §3). Lo usa
 * {@code pedidos} sin tocar el dominio de usuarios (R4). Lee la base en cada llamada: los roles del token pueden estar
 * desactualizados hasta 15 minutos y no deciden nada (lección A1).
 */
public interface ConsultarResponsables {

    /** Vacío si el usuario no existe. */
    Optional<EstadoVigente> estado(UUID usuarioId);

    /**
     * @param activo la cuenta está activa y tiene al menos un rol (puede iniciar sesión)
     * @param roles roles actuales en la base
     * @param paises países asignados hoy en la base (usuario_pais; fase 7, ADR-0024)
     */
    record EstadoVigente(UUID usuarioId, boolean activo, Set<CodigoRol> roles, Set<CodigoPais> paises) {

        public EstadoVigente {
            Objects.requireNonNull(usuarioId, "usuarioId");
            roles = Set.copyOf(roles);
            paises = paises == null ? Set.of() : Set.copyOf(paises);
        }

        /**
         * Solo compatibilidad documentada con código y pruebas anteriores a la fase 7 (ADR-0024): equivale a Ecuador
         * asignado. No es una autorización: en producción el estado siempre trae los países leídos de la base
         * ({@code ConsultarResponsablesCasoDeUso}), nunca este constructor.
         */
        public EstadoVigente(UUID usuarioId, boolean activo, Set<CodigoRol> roles) {
            this(usuarioId, activo, roles, Set.of(CodigoPais.EC));
        }
    }
}
