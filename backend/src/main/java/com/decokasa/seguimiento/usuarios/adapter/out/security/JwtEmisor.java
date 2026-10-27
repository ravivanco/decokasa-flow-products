package com.decokasa.seguimiento.usuarios.adapter.out.security;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.port.out.EmisorTokens;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Token de acceso JWT firmado con HS256 (ADR-0002): {@code iss=decokasa}, {@code sub} = id del usuario y
 * {@code roles} = todos sus roles (uno o varios). Los permisos se vuelven a validar en el servidor.
 */
public final class JwtEmisor implements EmisorTokens {

    public static final String EMISOR = "decokasa";

    private final JwtEncoder codificador;

    public JwtEmisor(SecretKey clave) {
        this.codificador = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Override
    public String emitirAcceso(Usuario usuario, Instant emitidoEn, Duration duracion) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.id().toString())
                .issuedAt(emitidoEn)
                .expiresAt(emitidoEn.plus(duracion))
                .claim("roles", usuario.roles().stream().map(CodigoRol::valor).sorted().toList())
                // ADR-0033: instante exacto de emisión, para invalidar los tokens anteriores a un cambio de contraseña.
                .claim("emitido_ms", emitidoEn.toEpochMilli())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
