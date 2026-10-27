package com.decokasa.seguimiento.asistente.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Conversación de una persona sobre **un único pedido** (F7-D1 opción C, ADR-0021). Solo la ve su autor. Vence a los 30
 * días en el piloto (F7-D5) y entonces se borra con sus mensajes.
 *
 * @param numeroPedido y {@code pais} copian los del pedido, que no cambian (V2, pedido_proteger_identidad)
 * @param preguntas preguntas ya respondidas y guardadas
 */
public record ConversacionIA(UUID id, UUID usuarioId, UUID pedidoId, String numeroPedido, CodigoPais pais,
        Instant creadaEn, Instant expiraEn, int preguntas) {

    /** F7-D2 (aprobado por Richard el 2026-10-22): 10 preguntas por conversación. */
    public static final int LIMITE_PREGUNTAS = 10;

    public ConversacionIA {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(pedidoId, "pedidoId");
        Objects.requireNonNull(numeroPedido, "numeroPedido");
        Objects.requireNonNull(pais, "pais");
        Objects.requireNonNull(creadaEn, "creadaEn");
        Objects.requireNonNull(expiraEn, "expiraEn");
        if (!expiraEn.isAfter(creadaEn)) {
            throw new IllegalArgumentException("Vencimiento inválido");
        }
        if (preguntas < 0 || preguntas > LIMITE_PREGUNTAS) {
            throw new IllegalArgumentException("Preguntas inválidas");
        }
    }

    public static ConversacionIA nueva(UUID id, UUID usuarioId, UUID pedidoId, String numeroPedido, CodigoPais pais,
            Instant ahora, Duration conservacion) {
        return new ConversacionIA(id, usuarioId, pedidoId, numeroPedido, pais, ahora, ahora.plus(conservacion), 0);
    }

    public boolean esDe(UUID usuario) {
        return usuarioId.equals(usuario);
    }

    public boolean vigente(Instant ahora) {
        return ahora.isBefore(expiraEn);
    }

    public int preguntasRestantes() {
        return Math.max(0, LIMITE_PREGUNTAS - preguntas);
    }
}
