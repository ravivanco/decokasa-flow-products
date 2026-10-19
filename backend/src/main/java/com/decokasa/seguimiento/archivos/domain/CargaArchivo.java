package com.decokasa.seguimiento.archivos.domain;

import java.time.Instant;
import java.util.UUID;

/** Sesión técnica; RECIBIENDO nunca es un Adjunto ni satisface un requisito documental. */
public record CargaArchivo(UUID id, UUID usuarioId, UUID claveIdempotencia, UUID pedidoId, int etapa,
        UUID entradaId, String nombre, String tipoDocumento, long tamanoBytes, long offsetConfirmado,
        Estado estado, UUID adjuntoReservadoId, UUID adjuntoId, UUID eventoId, Instant creadaEn, Instant actualizadaEn) {
    public static final int FRAGMENTO_BYTES = 4 * 1024 * 1024;
    public static final long MAXIMO_BYTES = 100L * 1024 * 1024;
    public enum Estado { RECIBIENDO, COMPLETADA }
    public record Fragmento(long inicio, int tamano, String sha256) { }
}
