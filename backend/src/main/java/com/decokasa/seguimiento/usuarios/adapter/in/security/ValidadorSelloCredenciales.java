package com.decokasa.seguimiento.usuarios.adapter.in.security;

import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarSelloCredenciales;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rechaza el JWT de acceso emitido antes del último cambio de contraseña de su cuenta (ADR-0033): tras cambiarla o
 * restablecerla, ningún token anterior autoriza solicitudes. Usa {@code emitido_ms} y, en los tokens anteriores a
 * V28, {@code iat}. Sin sello (nunca cambió) o sin cuenta, no decide: lo hacen los demás validadores y el caso de uso.
 */
public final class ValidadorSelloCredenciales implements OAuth2TokenValidator<Jwt> {

    public static final String CLAIM = "emitido_ms";

    private static final OAuth2Error SESION_REVOCADA = new OAuth2Error("invalid_token",
            "La sesión ya no es válida; inicia sesión de nuevo.", null);

    private final ConsultarSelloCredenciales sello;

    public ValidadorSelloCredenciales(ConsultarSelloCredenciales sello) {
        this.sello = sello;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        UUID usuarioId;
        try {
            usuarioId = UUID.fromString(token.getSubject());
        } catch (RuntimeException sinSujetoValido) {
            return OAuth2TokenValidatorResult.success();
        }
        Optional<Instant> cambio = sello.cambiadasEn(usuarioId);
        if (cambio.isEmpty()) {
            return OAuth2TokenValidatorResult.success();
        }
        Instant emitido = emitido(token);
        return emitido != null && !emitido.isBefore(cambio.get())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(SESION_REVOCADA);
    }

    private static Instant emitido(Jwt token) {
        Object milisegundos = token.getClaims().get(CLAIM);
        if (milisegundos instanceof Number numero) {
            return Instant.ofEpochMilli(numero.longValue());
        }
        return token.getIssuedAt();
    }
}
