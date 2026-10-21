package com.decokasa.seguimiento.notificaciones.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Cola y registro único: destinatario null conserva un aviso aún no resoluble, nunca entregable. */
public record Notificacion(UUID id, UUID pedidoId, UUID eventoId, String destinatario, String plantilla,
        String asunto, Map<String, String> datos, Estado estado, int intentos, Instant proximoIntentoEn,
        String ultimoError, Instant creadaEn, Instant enviadaEn, String proveedorId, String messageId,
        boolean resultadoIncierto) {
    public enum Estado { PENDIENTE, ENVIADA, FALLIDA }
    public Notificacion {
        Objects.requireNonNull(id); Objects.requireNonNull(pedidoId); Objects.requireNonNull(eventoId);
        Objects.requireNonNull(estado); Objects.requireNonNull(creadaEn); Objects.requireNonNull(messageId);
        datos = Map.copyOf(datos);
        if (destinatario != null) { destinatario = normalizarCorreo(destinatario); }
        if (asunto == null || asunto.isBlank() || asunto.length() > 500
                || plantilla == null || plantilla.isBlank() || plantilla.length() > 100 || intentos < 0) {
            throw new IllegalArgumentException("Metadatos de notificación inválidos");
        }
    }
    @Override public String toString() { return "Notificacion[id=" + id + ", estado=" + estado + "]"; }
    public static String normalizarCorreo(String valor) {
        String correo = Objects.requireNonNull(valor).strip().toLowerCase(Locale.ROOT);
        if (correo.length() > 254 || !correo.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9.-]+\\.[a-z]{2,}")) {
            throw new IllegalArgumentException("Destinatario inválido");
        }
        return correo;
    }
    public static Notificacion pendiente(UUID pedidoId, UUID eventoId, String destinatario,
            Map<String,String> datos, Instant ahora) {
        return pendiente(pedidoId, eventoId, destinatario, datos, ahora, "TRANSICION", "Cambio de etapa de un pedido");
    }
    /** Con plantilla y asunto propios (VENCIMIENTO, paso 5.2). */
    public static Notificacion pendiente(UUID pedidoId, UUID eventoId, String destinatario,
            Map<String,String> datos, Instant ahora, String plantilla, String asunto) {
        UUID id = UUID.randomUUID();
        return new Notificacion(id, pedidoId, eventoId, destinatario, plantilla, asunto,
                datos, Estado.PENDIENTE, 0, ahora, null, ahora, null, null,
                "<notificacion-" + id + "@decokasa.invalid>", false);
    }
}
