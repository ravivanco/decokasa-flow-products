package com.decokasa.seguimiento.notificaciones.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Cola de alertas de WhatsApp con proveedor. Cada operación corre en su propia transacción corta. */
public interface ColaWhatsApp {

    /** Mensaje listo para enviar. @param telefono el registrado en el usuario; puede faltar el código de país */
    record Pendiente(UUID id, String telefono, String texto) {
        @Override public String toString() { return "Pendiente[" + id + "]"; }
    }

    /** Marca como ENVIANDO hasta {@code lote} pendientes y los devuelve. */
    List<Pendiente> reclamar(int lote);

    void enviada(UUID id, String proveedorId, Instant ahora);

    void fallida(UUID id, String causa, Instant ahora);

    /** Proveedor de WhatsApp. Se llama fuera de toda transacción. @return identificador del mensaje en el proveedor */
    interface Mensajero {
        String enviar(String telefono, String texto);
    }
}
