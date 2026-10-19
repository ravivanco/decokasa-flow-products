package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.util.Objects;
import java.util.UUID;

/** Solo entrega al proveedor, sin actualizar la base. Debe llamarse fuera de una transacción de base. */
public interface Notificador {
    Entrega enviar(Correo correo);
    record Correo(UUID notificacionId, String destinatario, String asunto, String texto, String messageId) {
        public Correo {
            Objects.requireNonNull(notificacionId);
            destinatario = Notificacion.normalizarCorreo(destinatario);
            if (asunto == null || asunto.isBlank() || asunto.length() > 500 || texto == null || texto.length() > 100000
                    || messageId == null || !messageId.equals("<notificacion-" + notificacionId + "@decokasa.invalid>")) {
                throw new IllegalArgumentException("Correo inválido");
            }
        }
        @Override public String toString() { return "Correo[PROTEGIDO]"; }
    }
    record Entrega(String proveedorId) {
        public Entrega { if (proveedorId == null || proveedorId.isBlank()) { throw new IllegalArgumentException("Entrega sin id"); } }
    }
}
