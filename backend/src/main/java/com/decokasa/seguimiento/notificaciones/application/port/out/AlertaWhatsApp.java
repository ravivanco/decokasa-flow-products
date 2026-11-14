package com.decokasa.seguimiento.notificaciones.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Alerta de WhatsApp para un responsable. Hoy solo se simula: se guarda la fila y no hay llamadas de red ni teléfonos.
 * La conexión real espera a confirmar proveedor, número y credenciales.
 */
public interface AlertaWhatsApp {

    AlertaWhatsApp APAGADA = (pedidoId, eventoId, usuarioId, texto, ahora) -> { };

    void simular(UUID pedidoId, UUID eventoId, UUID usuarioId, String texto, Instant ahora);
}
