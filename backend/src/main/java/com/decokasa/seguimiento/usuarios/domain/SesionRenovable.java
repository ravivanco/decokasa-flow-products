package com.decokasa.seguimiento.usuarios.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Un token de renovación guardado (tabla token_renovacion; ADR-0002, D3). Solo se guarda su hash. Todos los tokens
 * de un mismo inicio de sesión forman una familia con el mismo vencimiento absoluto: inicio de sesión + 8 horas.
 */
public final class SesionRenovable {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID familiaId;
    private final String tokenHash;
    private final Instant creadoEn;
    private final Instant expiraEn;
    private Instant revocadoEn;

    public SesionRenovable(UUID id, UUID usuarioId, UUID familiaId, String tokenHash, Instant creadoEn,
            Instant expiraEn, Instant revocadoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.familiaId = familiaId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
        this.revocadoEn = revocadoEn;
    }

    /** Nuevo token de una familia: el de un inicio de sesión o el de una rotación (que hereda el vencimiento). */
    public static SesionRenovable nueva(UUID usuarioId, UUID familiaId, String tokenHash, Instant ahora,
            Instant expiraEn) {
        return new SesionRenovable(UUID.randomUUID(), usuarioId, familiaId, tokenHash, ahora, expiraEn, null);
    }

    public boolean estaRevocada() {
        return revocadoEn != null;
    }

    /** Sin revocar y antes de su vencimiento absoluto. */
    public boolean estaVigente(Instant ahora) {
        return !estaRevocada() && ahora.isBefore(expiraEn);
    }

    /** Marca el token como revocado; si ya lo estaba, conserva la primera fecha. */
    public void revocar(Instant ahora) {
        if (revocadoEn == null) {
            revocadoEn = ahora;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public UUID familiaId() {
        return familiaId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public Instant expiraEn() {
        return expiraEn;
    }

    public Instant revocadoEn() {
        return revocadoEn;
    }
}
