package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla token_renovacion: solo el hash del token (ADR-0002, D3). Solo vive en el adaptador (R3). */
@Entity
@Table(name = "token_renovacion")
public class TokenRenovacionEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(name = "familia_id", updatable = false)
    private UUID familiaId;

    @Column(name = "token_hash", updatable = false)
    private String tokenHash;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @Column(name = "expira_en", updatable = false)
    private Instant expiraEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    protected TokenRenovacionEntity() {
    }

    TokenRenovacionEntity(UUID id, UUID usuarioId, UUID familiaId, String tokenHash, Instant creadoEn,
            Instant expiraEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.familiaId = familiaId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    void setRevocadoEn(Instant revocadoEn) {
        this.revocadoEn = revocadoEn;
    }

    UUID getId() {
        return id;
    }

    UUID getUsuarioId() {
        return usuarioId;
    }

    UUID getFamiliaId() {
        return familiaId;
    }

    String getTokenHash() {
        return tokenHash;
    }

    Instant getCreadoEn() {
        return creadoEn;
    }

    Instant getExpiraEn() {
        return expiraEn;
    }

    Instant getRevocadoEn() {
        return revocadoEn;
    }
}
