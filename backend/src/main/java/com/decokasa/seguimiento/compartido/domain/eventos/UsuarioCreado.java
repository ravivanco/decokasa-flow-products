package com.decokasa.seguimiento.compartido.domain.eventos;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * El admin dio de alta un usuario de DECOKASA. Lo publica el módulo usuarios dentro de la transacción del alta. Desde
 * ADR-0027 ningún módulo crea con él una carpeta global de Drive (las carpetas son por pedido y autor). No es un evento
 * de pedido ni entra en el historial. La carga inicial de usuarios (semilla, ADR-0015) no lo publica.
 */
public record UsuarioCreado(UUID usuarioId, Instant ocurridoEn) {

    public UsuarioCreado {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
    }
}
