package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Misma tabla adjunto de fase 2; no crea otro catálogo ni toca contenido/relaciones del historial. */
public interface CopiaAdjuntoRepositorio {
    List<Pendiente> reclamar(int limite, Instant ahora, Instant hasta, UUID propietario);
    boolean reservarId(UUID adjuntoId, UUID propietario, String driveId, Instant ahora);
    boolean confirmar(UUID adjuntoId, UUID propietario, String driveId, String enlace, Instant ahora);
    boolean registrarFallo(UUID adjuntoId, UUID propietario, Causa causa, Instant siguiente, boolean terminal, Instant ahora);
    boolean renovar(UUID adjuntoId, UUID propietario, Instant ahora, Instant hasta);
    boolean sesion(UUID adjuntoId, UUID propietario, String carpeta, byte[] cifrada, long offset, Instant ahora);
    boolean avanzar(UUID adjuntoId, UUID propietario, long offset, Instant ahora);
    boolean ceder(UUID adjuntoId, UUID propietario, Instant ahora);
    record Pendiente(UUID id, UUID pedidoId, UUID entradaId, String nombre, long tamano,
            String sha256, String tipoDetectado, String claveLocal, String driveIdReservado, int intentos,
            byte[] sesionCifrada, long offsetDrive, String carpetaId) {
        public Pendiente { sesionCifrada=sesionCifrada==null ? null : sesionCifrada.clone(); }
        @Override public byte[] sesionCifrada() { return sesionCifrada==null ? null : sesionCifrada.clone(); }
        @Override public String toString() { return "CopiaPendiente[id="+id+"]"; }
    }
}
