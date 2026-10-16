package com.decokasa.seguimiento.compartido.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Arma el {@link Solicitante} con el id y los roles del token de acceso, para los controladores de pedidos y archivos.
 * Los roles del token solo sirven de primer filtro: los casos de uso deciden con el estado actual de la persona en la
 * base (R6, lección A1). Mismo criterio que {@code usuarios.adapter.in.web.Solicitantes}, que es privado de ese módulo.
 */
public final class SolicitantesWeb {

    private SolicitantesWeb() {
    }

    public static Solicitante de(Jwt token) {
        List<String> roles = token.getClaimAsStringList("roles");
        Set<CodigoRol> codigos = roles == null ? Set.of()
                : roles.stream().map(CodigoRol::new).collect(Collectors.toUnmodifiableSet());
        return new Solicitante(UUID.fromString(token.getSubject()), codigos);
    }
}
