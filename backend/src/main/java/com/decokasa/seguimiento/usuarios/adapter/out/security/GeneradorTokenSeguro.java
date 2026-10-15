package com.decokasa.seguimiento.usuarios.adapter.out.security;

import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Tokens de renovación de 32 bytes al azar; en la base solo se guarda su SHA-256 (ADR-0002). */
public final class GeneradorTokenSeguro implements GeneradorTokenRenovacion {

    private final SecureRandom azar = new SecureRandom();

    @Override
    public String nuevo() {
        byte[] bytes = new byte[32];
        azar.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", error);
        }
    }
}
