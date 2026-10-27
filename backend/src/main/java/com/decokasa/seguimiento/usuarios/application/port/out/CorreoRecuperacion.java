package com.decokasa.seguimiento.usuarios.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrega el correo con el enlace de recuperación (ADR-0033). Lo implementa el módulo de notificaciones con Gmail. Se
 * llama fuera de toda transacción; un error se lanza como excepción y no se reintenta.
 */
public interface CorreoRecuperacion {

    void enviar(UUID solicitudId, String destinatario, String nombre, String enlace, Instant expiraEn);
}
