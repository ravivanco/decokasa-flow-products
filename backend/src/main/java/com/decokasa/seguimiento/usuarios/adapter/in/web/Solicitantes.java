package com.decokasa.seguimiento.usuarios.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Arma el {@link Solicitante} con el id y los roles del token de acceso. Los roles del token solo sirven de primer
 * filtro; el caso de uso vuelve a revisar con el estado actual del usuario en la base (R6, hallazgo A1).
 */
final class Solicitantes {

    private Solicitantes() {
    }

    static Solicitante de(Jwt token) {
        List<String> roles = token.getClaimAsStringList("roles");
        Set<CodigoRol> codigos = roles == null ? Set.of()
                : roles.stream().map(CodigoRol::new).collect(Collectors.toUnmodifiableSet());
        return new Solicitante(UUID.fromString(token.getSubject()), codigos);
    }
}
