package com.decokasa.seguimiento.asistente.application.port.out;

import java.util.Optional;

/**
 * Manifiesto revisado del piloto (F7-D3): archivo local fuera de Git ({@code piloto-ia/manifiesto.json}). Un PDF sin
 * entrada no existe para el piloto. El servidor solo usa su huella y su número de páginas.
 */
public interface ManifiestoPiloto {

    /** Vacío si no hay entrada válida para esa huella o el manifiesto falta o no se puede leer. */
    Optional<Entrada> porSha256(String sha256);

    record Entrada(String nombre, String sha256, int paginas) {
    }
}
