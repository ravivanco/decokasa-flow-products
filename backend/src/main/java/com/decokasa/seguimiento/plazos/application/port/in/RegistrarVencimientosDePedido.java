package com.decokasa.seguimiento.plazos.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Registra los vencimientos de un pedido en una sola transacción: bloqueo, comprobación, evento PLAZO_VENCIDO
 * (historial y outbox) y aviso único. Si algo falla, no queda nada a medias. Paso 5.2.
 */
public interface RegistrarVencimientosDePedido {

    /** @return cuántos avisos nuevos registró (0, 1 o 2) */
    int registrar(UUID pedidoId, Instant ahora);
}
